use std::env;
use std::process;
use kui_cli::build;
use kui_cli::cli::{Command, CommandParser, Help, ParsedInvocation, DISPLAY_NAME};
use kui_cli::device;
use kui_cli::doctor;
use kui_cli::project;

fn main() {
    #[cfg(windows)]
    let _ = colored::control::set_virtual_terminal(true);

    let raw_args: Vec<String> = env::args().skip(1).collect();

    let exit_code = match CommandParser::parse(&raw_args) {
        ParsedInvocation::Empty => {
            Help::print_help(None);
            0
        }
        ParsedInvocation::ShowVersion => {
            println!("{}", DISPLAY_NAME);
            0
        }
        ParsedInvocation::ShowHelp(target) => {
            Help::print_help(target);
            0
        }
        ParsedInvocation::UnknownCommand { raw_name, suggestion } => {
            eprintln!("kui: '{}' is not a recognized command.", raw_name);
            if let Some(cmd) = suggestion {
                eprintln!("Did you mean: '{}'?", cmd.command_name());
            }
            eprintln!("Run 'kui --help' for available commands.");
            1
        }
        ParsedInvocation::ExecuteCommand { command, args, flags } => {
            match command {
                Command::Version => {
                    println!("{}", DISPLAY_NAME);
                    0
                }
                Command::Help => {
                    let target = args.first().and_then(|s| Command::from_str(s));
                    Help::print_help(target);
                    0
                }
                Command::Doctor => doctor::execute_doctor(),
                Command::Devices => device::execute_devices(),
                Command::New => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    project::execute_new(&args, &flags, &cwd)
                }
                Command::Info => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    project::execute_info(&cwd)
                }
                Command::Clean => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    build::execute_clean(&cwd)
                }
                Command::Build => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    if flags.contains_key("jvm") {
                        build::bridge_to_jvm(&raw_args)
                    } else {
                        build::execute_build(&args, &cwd).0
                    }
                }
                Command::Install => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    if flags.contains_key("jvm") {
                        build::bridge_to_jvm(&raw_args)
                    } else {
                        build::execute_install(&args, &cwd)
                    }
                }
                Command::Run | Command::Launch => {
                    let cwd = env::current_dir().unwrap_or_else(|_| ".".into());
                    if flags.contains_key("jvm") {
                        build::bridge_to_jvm(&raw_args)
                    } else {
                        build::execute_run(&args, &cwd)
                    }
                }
                // Delegated JVM commands for tests / bench / profile
                Command::Test
                | Command::Bench
                | Command::Profile
                | Command::UiTree => {
                    build::bridge_to_jvm(&raw_args)
                }
            }
        }
    };

    process::exit(exit_code);
}
