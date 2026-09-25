use std::collections::HashMap;
use super::command::Command;

#[derive(Debug, PartialEq, Eq)]
pub enum ParsedInvocation {
    Empty,
    ShowVersion,
    ShowHelp(Option<Command>),
    ExecuteCommand {
        command: Command,
        args: Vec<String>,
        flags: HashMap<String, String>,
    },
    UnknownCommand {
        raw_name: String,
        suggestion: Option<Command>,
    },
}

pub struct CommandParser;

impl CommandParser {
    pub fn parse<I, S>(args: I) -> ParsedInvocation
    where
        I: IntoIterator<Item = S>,
        S: AsRef<str>,
    {
        let tokens: Vec<String> = args.into_iter().map(|s| s.as_ref().trim().to_string()).collect();
        if tokens.is_empty() {
            return ParsedInvocation::Empty;
        }

        let first = &tokens[0];

        if first == "--version" || first == "-v" {
            return ParsedInvocation::ShowVersion;
        }

        if first == "--help" || first == "-h" {
            let sub = tokens.get(1).and_then(|s| Command::from_str(s));
            return ParsedInvocation::ShowHelp(sub);
        }

        if let Some(cmd) = Command::from_str(first) {
            if cmd == Command::Help {
                let sub = tokens.get(1).and_then(|s| Command::from_str(s));
                return ParsedInvocation::ShowHelp(sub);
            }
            if cmd == Command::Version {
                return ParsedInvocation::ShowVersion;
            }

            let mut positional = Vec::new();
            let mut flags = HashMap::new();
            let mut i = 1;

            while i < tokens.len() {
                let token = &tokens[i];
                if let Some(stripped) = token.strip_prefix("--") {
                    if let Some(eq_idx) = stripped.find('=') {
                        let key = &stripped[..eq_idx];
                        let val = &stripped[eq_idx + 1..];
                        flags.insert(key.to_string(), val.to_string());
                    } else if i + 1 < tokens.len() && !tokens[i + 1].starts_with('-') {
                        flags.insert(stripped.to_string(), tokens[i + 1].clone());
                        i += 1;
                    } else {
                        flags.insert(stripped.to_string(), "true".to_string());
                    }
                } else if token.starts_with('-') && token.len() > 1 {
                    flags.insert(token[1..].to_string(), "true".to_string());
                } else {
                    positional.push(token.clone());
                }
                i += 1;
            }

            return ParsedInvocation::ExecuteCommand {
                command: cmd,
                args: positional,
                flags,
            };
        }

        let suggestion = Command::find_closest(first);
        ParsedInvocation::UnknownCommand {
            raw_name: first.clone(),
            suggestion,
        }
    }
}
