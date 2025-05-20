fn main() {
    // Workaround for "multiple definition" linker errors on Linux
    if cfg!(target_os = "linux") {
        println!("cargo:rustc-link-arg=-Wl,--allow-multiple-definition");
    }
}
