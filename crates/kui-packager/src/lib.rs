pub mod apk;
pub mod axml;
pub mod pipeline;
pub mod signing;

pub use apk::{ApkEntry, ApkEntryInfo, ApkWriter};
pub use axml::{constants as axml_constants, AxmlAttribute, AxmlWriter, ManifestGenerator};
pub use pipeline::{build_minimal_dex, PackagingOptions, PackagingPipeline, PackagingResult};
pub use signing::{
    compute_apk_digest, ApkV2Signer, ApkV2Verifier, KeyManager, SigningConfig, VerificationSuccess,
};

pub fn packager_version() -> &'static str {
    "0.1.0"
}
