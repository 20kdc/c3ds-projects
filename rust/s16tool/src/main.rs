// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

extern crate image;
extern crate libkc3ds;
extern crate norncli;

use libkc3ds::imaging::*;
use libkc3ds::io::cs16::CS16HeaderCommon;
use libkc3ds::io::*;
use norncli::*;
use std::path::{Path, PathBuf};

mod imaging_ex;

fn cdmode_parse(x: Option<&String>) -> &dyn BitDitherMethod {
    let res = x.cloned().unwrap_or(String::from("floor"));
    for v in ALL_BITDITHER_METHODS {
        if v.0.eq_ignore_ascii_case(&res) {
            return v.1;
        }
    }
    eprintln!("{} is not a valid CDMODE", res);
    std::process::exit(1);
}
fn admode_parse(x: Option<&String>) -> &dyn BitDitherMethod {
    let res = x.cloned().unwrap_or(String::from("nearest"));
    for v in ALL_BITDITHER_METHODS {
        if v.0.eq_ignore_ascii_case(&res) {
            return v.1;
        }
    }
    eprintln!("{} is not a valid ADMODE", res);
    std::process::exit(1);
}

const COMMAND: &'static dyn CLIElement = &CLISubcommands(
    "various tools for manipulating .s16 files",
    &[
        (
            "info",
            &CLIVecCmd("<IN...>", "", "information on c16/s16 files", |args| {
                for f in args {
                    if let Ok(bytes) = std::fs::read(&f) {
                        if let Ok(headers) = cs16::identify_and_headers(&bytes) {
                            println!("{}: {}", f, headers.variant_cs16());
                            let sz = headers.image_sizes();
                            println!("{} frames", sz.len());
                            for (i, s) in sz.iter().enumerate() {
                                println!(" {}: {}x{}", i, s.0, s.1);
                            }
                        } else {
                            println!("{}: Failed", f);
                        }
                    } else {
                        println!("{}: Unreadable", f);
                    }
                }
                std::process::exit(0);
            }),
        ),
        (
            "infoShort",
            &CLIVecCmd(
                "<IN...>",
                "",
                "short information on c16/s16 files",
                |args| {
                    for f in args {
                        if let Ok(bytes) = std::fs::read(&f) {
                            if let Ok(headers) = cs16::identify_and_headers(&bytes) {
                                println!(
                                    "{}: {}, {} frames",
                                    f,
                                    headers.variant_cs16(),
                                    headers.image_sizes().len()
                                );
                            } else {
                                println!("{}: Failed", f);
                            }
                        } else {
                            println!("{}: Unreadable", f);
                        }
                    }
                    std::process::exit(0);
                },
            ),
        ),
        (
            "encodeS16",
            &CLIVecCmd(
                "<INDIR> <OUT> [<CDMODE> [<ADMODE>]]",
                "",
                "encodes 565 S16 file from directory",
                |args| {
                    sc16_encoding_command(args, CS16Type::S16(S16Type::S16_565));
                },
            ),
        ),
        (
            "encodeC16",
            &CLIVecCmd(
                "<INDIR> <OUT> [<CDMODE> [<ADMODE>]]",
                "",
                "encodes 565 C16 file from directory",
                |args| {
                    sc16_encoding_command(args, CS16Type::C16(C16Type::C16_565));
                },
            ),
        ),
        (
            "encodeBLK",
            &CLIVecCmd(
                "<IN> <OUT> [<CDMODE>]",
                "",
                "encodes 565 BLK file from source",
                |args| {
                    if args.len() < 2 || args.len() > 3 {
                        return;
                    }
                    let img = imaging_ex::import(&args[0]);
                    let cdmode = cdmode_parse(args.get(2));
                    let dithered = s16dither::blk(&ARGB32::raster_rgb(&img), &CM_RGB565, cdmode);
                    let blk16 = BLK16::split(S16Type::S16_565, &dithered);
                    std::fs::write(&args[1], blk16::build_blk(blk16)).unwrap();
                    std::process::exit(0);
                },
            ),
        ),
        (
            "decode",
            &CLIVecCmd(
                "<IN> <OUTDIR>",
                "",
                "decodes S16 or C16 files
s16/c16 files are converted to directories of numbered PNG files.
This process is lossless, though RGB555 files are converted to RGB565.
The inverse conversion is of course not lossless (lower bits are dropped).",
                |args| {
                    if args.len() != 2 {
                        return;
                    }
                    let sheet =
                        cs16::identify_and_decompress(&std::fs::read(&args[0]).unwrap()).unwrap();
                    let path_base = Path::new(&args[1]);
                    let _ = std::fs::create_dir(path_base);
                    for (i, frame) in sheet.frames.iter().enumerate() {
                        imaging_ex::export_spr(
                            &path_base.join(format!("{}.png", i)).to_string_lossy(),
                            frame,
                            sheet.id.to_cm16(),
                            false,
                        );
                    }
                    std::process::exit(0);
                },
            ),
        ),
        (
            "decodeBLK",
            &CLIVecCmd(
                "<IN> <OUT>",
                "",
                "decodes a BLK file to a PNG file",
                |args| {
                    if args.len() != 2 {
                        return;
                    }
                    let sheet =
                        blk16::identify_and_read(&std::fs::read(&args[0]).unwrap()).unwrap();
                    imaging_ex::export_blk(&args[1], &sheet.join(), sheet.variant.to_cm16(), false);
                    std::process::exit(0);
                },
            ),
        ),
        (
            "genPalRef",
            &CLIVecCmd(
                "<DST>",
                "",
                "generates a PNG palette reference file",
                |args| {
                    if args.len() != 1 {
                        return;
                    }
                    let image = Raster::generate(256, 256, &mut |x, y| {
                        CM_RGB565.decode((x + (y * 256)) as u16)
                    });
                    imaging_ex::export_rgb(&args[0], &image, false);
                    std::process::exit(0);
                },
            ),
        ),
        (
            "dither",
            &CLIVecCmd(
                "<IN> <OUT> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]",
                "",
                "tests dithering - defaults to RGB565",
                |args| {
                    if args.len() < 2 || args.len() > 8 {
                        return;
                    }
                    dither_command(
                        args[0].clone(),
                        Some(args[1].clone()),
                        args.get(2).cloned(),
                        args.get(3).cloned(),
                        args.get(4).cloned(),
                        args.get(5).cloned(),
                        args.get(6).cloned(),
                        args.get(7).cloned(),
                    )
                },
            ),
        ),
        (
            "dithera",
            &CLIVecCmd(
                "<IN> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]",
                "",
                "dither command, but output name is inferred from details",
                |args| {
                    if args.len() < 1 || args.len() > 7 {
                        return;
                    }
                    dither_command(
                        args[0].clone(),
                        None,
                        args.get(1).cloned(),
                        args.get(2).cloned(),
                        args.get(3).cloned(),
                        args.get(4).cloned(),
                        args.get(5).cloned(),
                        args.get(6).cloned(),
                    )
                },
            ),
        ),
        (
            "dithering-help",
            &CLIVecCmd("", "", "information on dithering", |args| {
                if !args.is_empty() {
                    return;
                }
                println!("CDMODE and ADMODE controls dithering and such.");
                println!("Modes are:");
                println!(" floor: Floors the value");
                println!(" nearest: Nearest bitcopied value");
                println!(
                    " random: Randomly picks from the two nearest values, weighted by distance."
                );
                println!(" The following are considered 'pattern sets'.");
                println!(
                    " They are all internally ordered dithers that all have a '-random' variant."
                );
                println!(" This variant interpolates between the stages using randomness.");
                println!(" checkers: 2x2 checkerboard in the 25% to 75% range.");
                println!(
                    "           Adds less than a bit of \"effective depth\", but very reliable."
                );
                println!(" bayer[2, 4]: Bayer 2x2/4x4 as described by https://github.com/SixLabors/ImageSharp/blob/main/src/ImageSharp/Processing/Processors/Dithering/DHALF.TXT");
                println!(" bluenoise[9, 15]: 'bluenoise' contributed by Tomeno");
                println!("The default CDMODE is floor, and the default ADMODE is nearest.");
                println!("(This is because these modes are lossless with decode output.)");
                std::process::exit(0);
            }),
        ),
    ],
);

fn sc16_encoding_command(args: Vec<String>, t: CS16Type) {
    // INDIR OUT [CDMODE [ADMODE]]
    if args.len() < 2 || args.len() > 4 {
        return;
    }
    let base = Path::new(&args[0]);
    let exts: &'static [&str] = &[".png", ".jpg", ".bmp"];
    let mut idx = 0;
    let mut frames: Vec<S16Frame> = Vec::new();
    let cdmode = cdmode_parse(args.get(2));
    let admode = admode_parse(args.get(3));
    loop {
        let mut found: Option<PathBuf> = None;
        for ext in exts {
            let candidate = base.join(format!("{}{}", idx, ext));
            if candidate.exists() {
                found = Some(candidate);
                break;
            }
        }
        if let Some(found) = found {
            let imp = imaging_ex::import(&found.to_string_lossy());
            idx += 1;
            frames.push(s16dither::spr(&imp, &t.to_cm16(), cdmode, admode));
        } else {
            break;
        }
    }
    let res = cs16::build(&CS16Sheet { id: t, frames });
    std::fs::write(&args[1], res).unwrap();
    std::process::exit(0);
}

fn dither_command(
    ifn: String,
    ofn: Option<String>,
    cdmode: Option<String>,
    admode: Option<String>,
    rbits: Option<String>,
    gbits: Option<String>,
    bbits: Option<String>,
    abits: Option<String>,
) {
    let rbits = rbits
        .map(|v| u8::from_str_radix(&v, 10).unwrap())
        .unwrap_or(5);
    let gbits = gbits
        .map(|v| u8::from_str_radix(&v, 10).unwrap())
        .unwrap_or(6);
    let bbits = bbits
        .map(|v| u8::from_str_radix(&v, 10).unwrap())
        .unwrap_or(5);
    let abits = abits
        .map(|v| u8::from_str_radix(&v, 10).unwrap())
        .unwrap_or(1);
    let cdmode_c = cdmode.clone();
    let admode_c = admode.clone();
    let ofn = ofn.unwrap_or_else(|| {
        format!(
            "{}.{}{}{}{}.{}{}.png",
            ifn,
            cdmode_c.unwrap_or(String::from("floor")),
            rbits,
            gbits,
            bbits,
            admode_c.unwrap_or(String::from("nearest")),
            abits
        )
    });
    let cdmode = cdmode_parse(cdmode.as_ref());
    let admode = admode_parse(admode.as_ref());

    let input = imaging_ex::import(&ifn);
    let input_a = ARGB32::raster_a(&input);
    let input_rgb = ARGB32::raster_rgb(&input);
    let (input_r, input_g, input_b) = RGB24::separate_channels(&input_rgb);

    let output_r = cdmode.run(input_r, rbits, false);
    let output_g = cdmode.run(input_g, gbits, false);
    let output_b = cdmode.run(input_b, bbits, false);
    let output_a = admode.run(input_a, abits, true);

    imaging_ex::export_argb(
        &ofn,
        &ARGB32::combine_channels(
            &output_a,
            &RGB24::combine_channels(&output_r, &output_g, &output_b),
        ),
        false,
    );
    std::process::exit(0);
}

fn main() {
    COMMAND.main();
}
