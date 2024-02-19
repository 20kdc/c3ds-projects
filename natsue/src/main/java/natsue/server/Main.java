/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import natsue.config.*;
import natsue.log.*;
import natsue.server.cryo.CryoFrontend;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.jdbc.JDBCNatsueDatabase;
import natsue.server.firewall.*;
import natsue.server.glst.FileGLSTStorage;
import natsue.server.glst.GLSTStoreMode;
import natsue.server.glst.IGLSTStorage;
import natsue.server.glst.NullGLSTStorage;
import natsue.server.http.HTTPHandlerImpl;
import natsue.server.http.IHTTPHandler;
import natsue.server.hub.ServerHub;
import natsue.server.packet.*;
import natsue.server.photo.FilePhotoStorage;
import natsue.server.photo.IPhotoStorage;
import natsue.server.photo.PhotoFunctions;
import natsue.server.session.LoginSessionState;
import natsue.server.system.SystemUserHubClient;

/**
 * It all starts here.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		// really early static init stuff, i.e. resources
		PhotoFunctions.ensureResourceInit();

		if (args.length != 0)
			throw new RuntimeException("Natsue Server expects no parameters.");

		ILogProvider ilp = new StdoutLogProvider();
		ILogSource mySource = ilp.logExtend(Main.class.toString());

		mySource.log("Started logger.");

		Config config = new Config();
		IConfigProvider configProvider = new NCFConfigProvider(new File("ntsuconf.txt"));
		config.visit(configProvider);
		configProvider.configFinished();

		mySource.log("Read configuration.");

		INatsueDatabase actualDB = new JDBCNatsueDatabase(ilp, config.db);

		mySource.log("DB abstraction initialized.");

		QuotaManager qm = new QuotaManager(config.connectionQuotas);

		mySource.log("Quota management initialized.");

		CryoFrontend cryo = new CryoFrontend(config, ilp);

		mySource.log("Cryogenics initialized.");

		IPhotoStorage photo = new FilePhotoStorage(new File(config.photos.photosDir.getValue()), ilp);

		mySource.log("Photo storage initialized.");

		IGLSTStorage glst;
		GLSTStoreMode glstMode = config.glst.glstMode.getValue();
		switch (glstMode) {
		default:
			glst = new NullGLSTStorage();
			break;
		case decompressed:
		case compressed:
			glst = new FileGLSTStorage(new File(config.glst.glstDir.getValue()), glstMode == GLSTStoreMode.compressed);
			break;
		}

		mySource.log("GLST storage initialized.");

		final ServerHub serverHub = new ServerHub(config, qm, ilp, actualDB, cryo);
		// determine the firewall
		IFWModule[] firewall = null;
		switch (config.firewallLevel.getValue()) {
		case minimal:
			mySource.log("Firewall level: minimal: MINIMAL, HAZARDOUS TO VANILLA CLIENTS");
			firewall = new IFWModule[] {
				new HypercallFWModule(ilp, serverHub, config),
				new DataExtractorFWModule(serverHub, false, photo, glst),
				new SpoolListFWModule(serverHub)
			};
			break;
		case vanillaSafe:
			mySource.log("Firewall level: vanillaSafe: Should be safe enough.");
			firewall = new IFWModule[] {
				new HypercallFWModule(ilp, serverHub, config),
				new NetWritFWModule(serverHub, config.allowNetWrit.getValue()),
				new PRAYBlockListsFWModule(serverHub, false),
				new CreatureCheckingFWModule(serverHub),
				new ComplexFWModule(serverHub),
				new DataExtractorFWModule(serverHub, true, photo, glst),
				new SpoolListFWModule(serverHub)
			};
			break;
		case full:
		default:
			mySource.log("Firewall level: full: No fun allowed.");
			firewall = new IFWModule[] {
				new HypercallFWModule(ilp, serverHub, config),
				new NetWritFWModule(serverHub, config.allowNetWrit.getValue()),
				new PRAYBlockListsFWModule(serverHub, true),
				new CreatureCheckingFWModule(serverHub),
				new ComplexFWModule(serverHub),
				new DataExtractorFWModule(serverHub, true, photo, glst),
				new SpoolListFWModule(serverHub)
			};
			break;
		case rejectAll:
			mySource.log("Firewall level: rejectAll: FOR TESTING ONLY");
			firewall = new IFWModule[] {
				new HypercallFWModule(ilp, serverHub, config),
				new DataExtractorFWModule(serverHub, false, photo, glst),
				new RejectAllFWModule(serverHub)
			};
			break;
		}
		serverHub.setFirewall(firewall, new Rejector(serverHub, SystemUserHubClient.IDENTITY));
		// login the system user
		serverHub.clientLogin(new SystemUserHubClient(config, ilp, serverHub), () -> {});

		String apiKey = config.httpAPIKey.getValue();
		if (apiKey.equals(""))
			apiKey = null;
		HTTPHandlerImpl hhi = new HTTPHandlerImpl(serverHub, config.httpAPIPublic.getValue(), apiKey, actualDB, photo, config.photos.photosDownloadEnabled.getValue());

		mySource.log("ServerHub initialized.");

		int portSSL = config.conn.portSSL.getValue();
		if (portSSL != -1) {
			try {
				SSLContext ctx = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				// this object will not be stored
				char[] veryPointlessPassword = new char[0];
				Certificate[] theCertChain = PEM.loadCertChain(new File(config.conn.sslFullChainPEM.getValue()));
				ILogSource certChainLogSource = new ILogSource() {
					public ILogProvider getLogParent() {
						return mySource;
					}
					@Override
					public String toString() {
						return "SSL certificate chain";
					}
				};
				for (Certificate c : theCertChain)
					if (c instanceof X509Certificate)
						certChainLogSource.log(((X509Certificate) c).getSubjectDN().toString());
				Key thePrivKey = PEM.loadPrivKey(new File(config.conn.sslPrivKeyPEM.getValue()), config.conn.sslPrivKeyAlgorithm.getValue());
				keyStore.load(null, null);
				keyStore.setKeyEntry("engine", thePrivKey, veryPointlessPassword, theCertChain);
				kmf.init(keyStore, veryPointlessPassword);
				ctx.init(kmf.getKeyManagers(), null, null);
				ServerSocket sv = ctx.getServerSocketFactory().createServerSocket(portSSL);
				mySource.log("Bound SSLServerSocket to port " + portSSL);
				new Thread() {
					public void run() {
						doSocketAcceptLoop(sv, qm, serverHub, hhi, ilp, config);
					}
				}.start();
			} catch (Exception ex) {
				mySource.log(ex);
			}
		}

		int port = config.conn.port.getValue();
		try (ServerSocket sv = new ServerSocket(port)) {
			mySource.log("Bound ServerSocket to port " + port + " - ready to accept connections.");
			doSocketAcceptLoop(sv, qm, serverHub, hhi, ilp, config);
		}
	}
	public static void doSocketAcceptLoop(ServerSocket sv, QuotaManager qm, ServerHub hub, IHTTPHandler hhi, ILogProvider ilp, Config config) {
		while (true) {
			Socket skt;
			try {
				skt = sv.accept();
			} catch (IOException e) {
				// !??!?!!?
				e.printStackTrace();
				continue;
			}
			if (!qm.socketStart(skt)) {
				try {
					try {
						// Abort the connection as hard as possible.
						// We don't want to make another thread for this.
						skt.setSoLinger(true, 0);
					} catch (Exception ex) {
						// nuh-uh
					}
					skt.close();
				} catch (Exception ex) {
					// bye!
				}
				continue;
			}
			new SocketThread(skt, qm, (st) -> {
				return new LoginSessionState(config, st, hub);
			}, hhi, ilp, config).start();
		}
	}
}
