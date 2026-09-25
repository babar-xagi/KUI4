#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Command {
    New,
    Doctor,
    Info,
    Build,
    Run,
    Install,
    Launch,
    Test,
    Clean,
    Bench,
    Profile,
    UiTree,
    Devices,
    Help,
    Version,
}

impl Command {
    pub fn all() -> &'static [Command] {
        &[
            Command::New,
            Command::Doctor,
            Command::Info,
            Command::Build,
            Command::Run,
            Command::Install,
            Command::Launch,
            Command::Test,
            Command::Clean,
            Command::Bench,
            Command::Profile,
            Command::UiTree,
            Command::Devices,
            Command::Help,
            Command::Version,
        ]
    }

    pub fn command_name(&self) -> &'static str {
        match self {
            Command::New => "new",
            Command::Doctor => "doctor",
            Command::Info => "info",
            Command::Build => "build",
            Command::Run => "run",
            Command::Install => "install",
            Command::Launch => "launch",
            Command::Test => "test",
            Command::Clean => "clean",
            Command::Bench => "bench",
            Command::Profile => "profile",
            Command::UiTree => "ui-tree",
            Command::Devices => "devices",
            Command::Help => "help",
            Command::Version => "version",
        }
    }

    pub fn description(&self) -> &'static str {
        match self {
            Command::New => "create project",
            Command::Doctor => "verify environment",
            Command::Info => "show project information",
            Command::Build => "compile/package",
            Command::Run => "build + install + launch",
            Command::Install => "install current APK",
            Command::Launch => "launch installed app",
            Command::Test => "run tests",
            Command::Clean => "remove generated build output",
            Command::Bench => "benchmarks",
            Command::Profile => "profiling report",
            Command::UiTree => "print runtime UI tree later",
            Command::Devices => "list connected devices",
            Command::Help => "show help for commands",
            Command::Version => "print version information",
        }
    }

    pub fn usage(&self) -> &'static str {
        match self {
            Command::New => "kui new <name>",
            Command::Doctor => "kui doctor",
            Command::Info => "kui info",
            Command::Build => "kui build [options]",
            Command::Run => "kui run [options]",
            Command::Install => "kui install",
            Command::Launch => "kui launch",
            Command::Test => "kui test",
            Command::Clean => "kui clean",
            Command::Bench => "kui bench [benchmark-name]",
            Command::Profile => "kui profile",
            Command::UiTree => "kui ui-tree",
            Command::Devices => "kui devices",
            Command::Help => "kui help [command]",
            Command::Version => "kui version",
        }
    }

    pub fn from_str(name: &str) -> Option<Command> {
        let lower = name.to_lowercase();
        Self::all().iter().copied().find(|c| c.command_name() == lower)
    }

    pub fn find_closest(name: &str) -> Option<Command> {
        let lower = name.to_lowercase();
        Self::all()
            .iter()
            .map(|&cmd| (cmd, levenshtein_distance(&lower, cmd.command_name())))
            .filter(|&(_, dist)| dist <= 2)
            .min_by_key(|&(_, dist)| dist)
            .map(|(cmd, _)| cmd)
    }
}

pub fn levenshtein_distance(s1: &str, s2: &str) -> usize {
    let s1_chars: Vec<char> = s1.chars().collect();
    let s2_chars: Vec<char> = s2.chars().collect();
    let m = s1_chars.len();
    let n = s2_chars.len();

    let mut dp = vec![vec![0usize; n + 1]; m + 1];

    for i in 0..=m {
        dp[i][0] = i;
    }
    for j in 0..=n {
        dp[0][j] = j;
    }

    for i in 1..=m {
        for j in 1..=n {
            let cost = if s1_chars[i - 1] == s2_chars[j - 1] { 0 } else { 1 };
            dp[i][j] = (dp[i - 1][j] + 1)
                .min(dp[i][j - 1] + 1)
                .min(dp[i - 1][j - 1] + cost);
        }
    }

    dp[m][n]
}
