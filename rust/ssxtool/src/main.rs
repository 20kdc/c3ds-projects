// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

extern crate miniz_oxide;
extern crate norncli;
extern crate openssl;

use norncli::*;
use openssl::hash::*;
use openssl::pkey::*;

use std::path::*;

fn keypls(key: &[u8]) -> openssl::rsa::Rsa<Private> {
    // 26 bytes skips some sort of insane wrapping layer
    // DER is cursed
    openssl::rsa::Rsa::private_key_from_der(&key[26..]).unwrap()
}

fn pkeypls(key: &[u8]) -> PKey<openssl::pkey::Private> {
    openssl::pkey::PKey::from_rsa(keypls(key)).unwrap()
}

const CHUNK_SIZE_ENCODED: usize = 64;
const CHUNK_SIZE_DECODED: usize = 22;

fn decode_ssx(data: &[u8], key: &[u8]) -> Vec<u8> {
    let key2 = pkeypls(key);
    if (data.len() % CHUNK_SIZE_ENCODED) != 0 {
        panic!(
            "chunksize {} is wrong for data length {}, modulus size {}",
            CHUNK_SIZE_ENCODED,
            data.len(),
            key2.rsa().unwrap().n().num_bytes()
        );
    }

    let mut dec = openssl::encrypt::Decrypter::new(&key2).unwrap();
    dec.set_rsa_padding(openssl::rsa::Padding::PKCS1_OAEP)
        .unwrap();
    dec.set_rsa_mgf1_md(MessageDigest::sha1().to_owned())
        .unwrap();
    dec.set_rsa_oaep_md(MessageDigest::sha1().to_owned())
        .unwrap();

    let mut total = Vec::new();

    for i in 0..data.len() / CHUNK_SIZE_ENCODED {
        let chk = &data[i * CHUNK_SIZE_ENCODED..(i + 1) * CHUNK_SIZE_ENCODED];
        let mut dvc: Vec<u8> = Vec::new();
        dvc.resize(dec.decrypt_len(chk).unwrap(), 0);
        let finlen = dec.decrypt(chk, &mut dvc).unwrap();
        // println!("{}", finlen);
        dvc.resize(finlen, 0);
        total.append(&mut dvc);
    }

    // ok, so:
    // first, there are header bytes
    // secondly, there will be a checksum at the end
    // thirdly, there's unreliable padding after that
    // these are Problems
    for i in 0..CHUNK_SIZE_DECODED {
        if let Ok(res) = miniz_oxide::inflate::decompress_to_vec(&total[2..total.len() - (i + 4)]) {
            return res;
        }
    }
    panic!("unable to find valid cutting that makes it decompress, sorry");
}

fn encode_ssx(data: &[u8], key: &[u8]) -> Vec<u8> {
    let mut data_c = miniz_oxide::deflate::compress_to_vec_zlib(
        data,
        miniz_oxide::deflate::CompressionLevel::UberCompression as u8,
    );
    while (data_c.len() % CHUNK_SIZE_DECODED) != 0 {
        data_c.push(0);
    }

    let key2 = pkeypls(key);
    let mut enc = openssl::encrypt::Encrypter::new(&key2).unwrap();
    enc.set_rsa_padding(openssl::rsa::Padding::PKCS1_OAEP)
        .unwrap();
    enc.set_rsa_mgf1_md(MessageDigest::sha1().to_owned())
        .unwrap();
    enc.set_rsa_oaep_md(MessageDigest::sha1().to_owned())
        .unwrap();

    let mut total = Vec::new();

    for i in 0..data_c.len() / CHUNK_SIZE_DECODED {
        let chk = &data_c[i * CHUNK_SIZE_DECODED..(i + 1) * CHUNK_SIZE_DECODED];
        let mut dvc: Vec<u8> = Vec::new();
        dvc.resize(enc.encrypt_len(chk).unwrap(), 0);
        let finlen = enc.encrypt(chk, &mut dvc).unwrap();
        // println!("{}", finlen);
        dvc.resize(finlen, 0);
        total.append(&mut dvc);
    }

    total
}

const DECODE_SCRIPT: &'static CLIVecCmd =
    &CLIVecCmd("<IN> <OUT> <HESKX>", "", "decodes SSX/SAX files", |args| {
        if args.len() != 3 {
            return;
        }
        let indata = &std::fs::read(&args[0]).unwrap();
        let inkey = &std::fs::read(&args[2]).unwrap();
        let data = decode_ssx(indata, inkey);
        std::fs::write(&args[1], data).unwrap();
        std::process::exit(0);
    });

const ENCODE_SCRIPT: &'static CLIVecCmd =
    &CLIVecCmd("<OUT> <IN> <HESKX>", "", "encodes SSX/SAX files", |args| {
        if args.len() != 3 {
            return;
        }
        let indata = &std::fs::read(&args[1]).unwrap();
        let inkey = &std::fs::read(&args[2]).unwrap();
        let data = encode_ssx(indata, inkey);
        std::fs::write(&args[0], data).unwrap();
        std::process::exit(0);
    });

fn do_batch_operation(paths: &mut Vec<PathBuf>, path: &Path, ext: &str) {
    //println!("{}", path.to_str().unwrap());
    let metadata = std::fs::metadata(path).unwrap();
    if metadata.is_file() {
        //println!("!!! is a file");
        if path.to_str().unwrap().ends_with(ext) {
            //println!("!!! ends with ext");
            paths.push(path.to_path_buf());
        }
    } else if metadata.is_dir() {
        for v in std::fs::read_dir(path).unwrap() {
            do_batch_operation(paths, &v.unwrap().path(), ext);
        }
    }
}

const DECODE_BATCH_SCRIPT: &'static CLIVecCmd = &CLIVecCmd(
    "<DIR> <HESKX>",
    "",
    "decodes SSX/SAX files in a directory",
    |args| {
        if args.len() != 2 {
            return;
        }
        let inkey = &std::fs::read(&args[1]).unwrap();
        let mut paths: Vec<PathBuf> = Vec::new();
        do_batch_operation(&mut paths, &PathBuf::from(&args[0]), ".ssx");
        do_batch_operation(&mut paths, &PathBuf::from(&args[0]), ".sax");
        for path in &paths {
            let indata = &std::fs::read(path).unwrap();
            let data = decode_ssx(indata, inkey);
            let adjpath = PathBuf::from(&(path.clone().to_str().unwrap().to_string() + ".lua"));
            std::fs::write(adjpath, data).unwrap();
        }
        std::process::exit(0);
    },
);

const ENCODE_BATCH_SCRIPT: &'static CLIVecCmd = &CLIVecCmd(
    "<DIR> <HESKX>",
    "",
    "encodes SSX/SAX files in a directory",
    |args| {
        if args.len() != 2 {
            return;
        }
        let inkey = &std::fs::read(&args[1]).unwrap();
        let mut paths: Vec<PathBuf> = Vec::new();
        do_batch_operation(&mut paths, &PathBuf::from(&args[0]), ".lua");
        for path in &paths {
            let indata = &std::fs::read(path).unwrap();
            let data = encode_ssx(indata, inkey);
            let adjpath = PathBuf::from(&path.to_str().unwrap().strip_suffix(".lua").unwrap());
            std::fs::write(adjpath, data).unwrap();
        }
        std::process::exit(0);
    },
);

const COMMAND: &'static dyn CLIElement = &CLISubcommands(
    "handle files which are DEFLATE-compressed and then encrypted with a specific RSA AES OAEP SHA-1 key",
    &[("decSSX", DECODE_SCRIPT), ("encSSX", ENCODE_SCRIPT), ("decSSXDir", DECODE_BATCH_SCRIPT), ("encSSXDir", ENCODE_BATCH_SCRIPT)],
);

fn main() {
    COMMAND.main();
}
