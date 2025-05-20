fn main() {
    // Workaround for "multiple definition" linker errors on Linux (#SDKS-1088)
    if cfg!(target_os = "linux") {
        println!("cargo:rustc-link-arg=-Wl,--allow-multiple-definition");
    }
}
