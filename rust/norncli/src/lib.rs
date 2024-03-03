// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

/// Element of the CLI.
pub trait CLIElement {
    /// Runs the element. `help_prefix` is the prefix for help.
    fn run(&self, remainder: &mut dyn Iterator<Item = String>, help_prefix: &str);
    /// Writes help for the element to the given string.
    /// Example format:
    /// ```text
    /// bat bonk USERNAME [forcefully]: bonk USERNAME
    ///     if forcefully is specified, bonk is very hard
    /// ```
    fn help(&self, target: &mut String, help_prefix: &str, indent: &str);
    /// print help to stderr
    fn help_eprint(&self, help_prefix: &str) {
        let mut target = String::new();
        self.help(&mut target, help_prefix, "");
        eprint!("{}", target);
    }
    /// print help to stdout
    fn help_print(&self, help_prefix: &str) {
        let mut target = String::new();
        self.help(&mut target, help_prefix, "");
        print!("{}", target);
    }
    /// Runs the element using the OS-provided args.
    fn main(&self) {
        let mut args = std::env::args();
        if let Some(program) = args.next() {
            self.run(&mut args, &program);
        } else {
            eprintln!("norncli: could not determine program name for help printing, and no further args -- what happened here?");
        }
    }
}

/// For programs split into subcommands.
pub struct CLISubcommands<'a>(pub &'a str, pub &'a [(&'a str, &'a dyn CLIElement)]);

impl CLIElement for CLISubcommands<'_> {
    fn run(&self, remainder: &mut dyn Iterator<Item = String>, help_prefix: &str) {
        if let Some(sc) = remainder.next() {
            if sc.eq_ignore_ascii_case("help")
                || sc.eq_ignore_ascii_case("-h")
                || sc.eq_ignore_ascii_case("-?")
                || sc.eq_ignore_ascii_case("--help")
            {
                self.help_print(help_prefix);
                std::process::exit(0);
            } else {
                for v in self.1 {
                    if v.0.eq_ignore_ascii_case(&sc) {
                        // okie!
                        v.1.run(remainder, &format!("{} {}", help_prefix, v.0));
                        return;
                    }
                }
                // oh no
                eprintln!("{}: no sub-command {}", help_prefix, sc);
                eprintln!("");
                self.help_eprint(help_prefix);
                std::process::exit(1);
            }
        } else {
            self.help_print(help_prefix);
            std::process::exit(0);
        }
    }

    fn help(&self, target: &mut String, help_prefix: &str, indent: &str) {
        target.push_str(indent);
        target.push_str(help_prefix);
        target.push_str(" ... : ");
        target.push_str(self.0);
        target.push_str("\n");
        let indented = format!("{}\t", indent);
        for v in self.1 {
            v.1.help(target, v.0, &indented);
        }
    }
}

/// Takes a vector, does whatever.
/// The three strings are syntax, short-help and long-help.
/// Must call os::process::exit or it will be assumed something went wrong and help will be printed.
/// Notably, help is printed to standard error in this event (not standard output).
/// An (admittedly questionable) feature is that:
/// * A leading arg of `--` is removed
/// * A leading arg of `--help`, `-h`, or `-?` causes help print instead
pub struct CLIVecCmd<'a>(pub &'a str, pub &'a str, pub &'a str, pub fn(Vec<String>));

impl CLIElement for CLIVecCmd<'_> {
    fn run(&self, remainder: &mut dyn Iterator<Item = String>, help_prefix: &str) {
        let mut vec: Vec<String> = remainder.collect();
        let mut need_to_cut = false;
        if let Some(opener) = vec.first() {
            if opener.eq_ignore_ascii_case("--help")
                || opener.eq_ignore_ascii_case("-h")
                || opener.eq_ignore_ascii_case("-?")
            {
                self.help_print(help_prefix);
                std::process::exit(0);
            } else if opener.eq("--") {
                need_to_cut = true;
            }
        }
        if need_to_cut {
            vec.remove(0);
        }
        self.3(vec);
        self.help_eprint(help_prefix);
        std::process::exit(1);
    }

    fn help(&self, target: &mut String, help_prefix: &str, indent: &str) {
        if self.1.is_empty() {
            if self.0.is_empty() {
                target.push_str(&format!("{}{}\n", indent, help_prefix));
            } else {
                target.push_str(&format!("{}{} {}\n", indent, help_prefix, self.0));
            }
        } else {
            if self.0.is_empty() {
                target.push_str(&format!("{}{}: {}\n", indent, help_prefix, self.1));
            } else {
                target.push_str(&format!(
                    "{}{} {}: {}\n",
                    indent, help_prefix, self.0, self.1
                ));
            }
        }
        if !self.2.is_empty() {
            for v in self.2.split('\n') {
                target.push_str(&format!("{}\t{}\n", indent, v));
            }
        }
    }
}
