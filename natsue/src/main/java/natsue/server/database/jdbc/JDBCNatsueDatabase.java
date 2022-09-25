/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.database.jdbc;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import natsue.config.Config;
import natsue.config.ConfigDB;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.database.INatsueDatabase;
import natsue.server.database.NatsueUserInfo;

/**
 * JDBC-based Natsue database implementation.
 * REMEMBER: STUFF HERE CAN BE ACCESSED FROM MULTIPLE THREADS.
 */
public class JDBCNatsueDatabase implements INatsueDatabase, ILogSource {
	private final ILogProvider logParent;
	private final JDBCNatsueTxns txns = new JDBCNatsueTxns();
	private final ConfigDB config;
	private final ILDBTxnHost txnHost;

	public JDBCNatsueDatabase(ILogProvider ilp, ConfigDB cfg) throws SQLException {
		config = cfg;
		logParent = ilp;
		log("JDBCNatsueDatabase, configured for " + cfg.dbType.valueToString());
		// this needs to be read from config if/when stuff hits that needs it
		ILDBVariant variant = cfg.dbType.getValue();
		try (Connection conn = DriverManager.getConnection(config.dbConnection.getValue())) {
			ILMigrations.migrate(conn, variant, this);
		}
		txnHost = new ILDBTxnHostOneRequestOneConnection(cfg, this);
	}

	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	@Override
	public NatsueUserInfo getUserByUID(int uid) {
		synchronized (this) {
			txns.userByUID.uid = uid;
			return txns.userByUID.executeOuter(txnHost);
		}
	}

	@Override
	public NatsueUserInfo getUserByFoldedNickname(String nickname) {
		synchronized (this) {
			txns.userByFoldedNickname.nicknameFolded = nickname;
			return txns.userByFoldedNickname.executeOuter(txnHost);
		}
	}

	@Override
	public boolean spoolMessage(int uid, byte[] pm) {
		synchronized (this) {
			txns.storeOnSpool.uid = uid;
			txns.storeOnSpool.data = pm;
			return txns.storeOnSpool.executeOuter(txnHost);
		}
	}

	@Override
	public byte[] popFirstSpooledMessage(int uid) {
		synchronized (this) {
			txns.popFromSpool.uid = uid;
			return txns.popFromSpool.executeOuter(txnHost);
		}
	}

	@Override
	public boolean ensureCreature(String moniker, int firstUID, int ch0, int ch1, int ch2, int ch3, int ch4, String name, String userText) {
		synchronized (this) {
			txns.addCreature.moniker = moniker;
			txns.addCreature.firstUID = firstUID;
			txns.addCreature.ch0 = ch0;
			txns.addCreature.ch1 = ch1;
			txns.addCreature.ch2 = ch2;
			txns.addCreature.ch3 = ch3;
			txns.addCreature.ch4 = ch4;
			txns.addCreature.name = name;
			txns.addCreature.userText = userText;
			return txns.addCreature.executeOuter(txnHost);
		}
	}

	@Override
	public boolean updateCreatureText(String moniker, String name, String userText) {
		synchronized (this) {
			txns.updateCreatureText.moniker = moniker;
			txns.updateCreatureText.name = name;
			txns.updateCreatureText.userText = userText;
			return txns.updateCreatureText.executeOuter(txnHost);
		}
	}

	@Override
	public boolean ensureCreatureEvent(int senderUID, String moniker, int index, int type, int worldTime, int ageTicks, int unixTime, int lifeStage, String param1, String param2, String worldName, String worldID, String userID) {
		synchronized (this) {
			txns.addCreatureEvent.senderUID = senderUID;
			txns.addCreatureEvent.moniker = moniker;
			txns.addCreatureEvent.eventIndex = index;
			txns.addCreatureEvent.eventType = type;
			txns.addCreatureEvent.worldTime = worldTime;
			txns.addCreatureEvent.ageTicks = ageTicks;
			txns.addCreatureEvent.unixTime = unixTime;
			txns.addCreatureEvent.lifeStage = lifeStage;
			txns.addCreatureEvent.param1 = param1;
			txns.addCreatureEvent.param2 = param2;
			txns.addCreatureEvent.worldName = worldName;
			txns.addCreatureEvent.worldID = worldID;
			txns.addCreatureEvent.userID = userID;
			return txns.addCreatureEvent.executeOuter(txnHost);
		}
	}

	@Override
	public boolean tryCreateUser(NatsueUserInfo userInfo) {
		synchronized (this) {
			txns.createUser.uid = userInfo.uid;
			txns.createUser.nickname = userInfo.nickname;
			txns.createUser.nicknameFolded = userInfo.nicknameFolded;
			txns.createUser.passwordHash = userInfo.passwordHash;
			txns.createUser.flags = userInfo.flags;
			return txns.createUser.executeOuter(txnHost);
		}
	}

	@Override
	public boolean updateUserAuth(int uid, String hash, int flags) {
		synchronized (this) {
			txns.createUser.uid = uid;
			txns.createUser.passwordHash = hash;
			txns.createUser.flags = flags;
			return txns.createUser.executeOuter(txnHost);
		}
	}
}
