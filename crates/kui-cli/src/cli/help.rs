use super::command::Command;
use super::version::DISPLAY_NAME;

pub struct Help;

impl Help {
    pub fn general_help() -> String {
        let mut out = String::new();
        out.push_str(&format!("{DISPLAY_NAME} - Pure Kotlin Application Platform Toolchain\n\n"));
        out.push_str("Usage:\n  kui [command] [options]\n\nCommands:\n");

        for cmd in Command::all() {
            let display_name = if *cmd == Command::New {
                "new <name>".to_string()
            } else {
                cmd.command_name().to_string()
            };
            out.push_str(&format!("  {:<14} {}\n", display_name, cmd.description()));
        }

        out.push_str("\nOptions:\n");
        out.push_str("  -v, --version  print version information\n");
        out.push_str("  -h, --help     print this help message\n");
        out
    }

    pub fn command_help(cmd: Command) -> String {
        format!("{} - {}\n\nUsage:\n  {}\n", cmd.command_name(), cmd.description(), cmd.usage())
    }

    pub fn print_help(target: Option<Command>) {
        if let Some(cmd) = target {
            print!("{}", Self::command_help(cmd));
        } else {
            print!("{}", Self::general_help());
        }
    }
}
