/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

/**
 * Genetics (version, data) tuple.
 */
public final class GenPackage {
	public final GenVersion version;
	public final byte[] data;
	public GenPackage(GenVersion version, byte[] data) {
		this.version = version;
		this.data = data;
	}

	/**
	 * No surprises here, just puts a magic on it
	 */
	public byte[] toFileData() {
		if (version == GenVersion.C1)
			return data.clone();
		if (version == GenVersion.C2) {
			byte[] dataMagic = new byte[data.length + 4];
			dataMagic[0] = 'd';
			dataMagic[1] = 'n';
			dataMagic[2] = 'a';
			dataMagic[3] = '2';
			System.arraycopy(data, 0, dataMagic, 4, data.length);
			return dataMagic;
		}
		if (version == GenVersion.C3) {
			byte[] dataMagic = new byte[data.length + 4];
			dataMagic[0] = 'd';
			dataMagic[1] = 'n';
			dataMagic[2] = 'a';
			dataMagic[3] = '3';
			System.arraycopy(data, 0, dataMagic, 4, data.length);
			return dataMagic;
		}
		throw new RuntimeException("Invalid version???");
	}
}
