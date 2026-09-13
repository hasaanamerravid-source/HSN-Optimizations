use std::env;
use std::path::PathBuf;
use std::process::Command;

fn main() {
    println!("cargo:rerun-if-changed=src/avx512.c");
    println!("cargo:rerun-if-changed=build.rs");

    let target = env::var("TARGET").unwrap_or_default();
    if !target.contains("x86_64") {
        return;
    }

    let out = PathBuf::from(env::var("OUT_DIR").unwrap());
    let obj = out.join("avx512.o");
    let lib = out.join("libhsn_avx512.a");
    let src = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap()).join("src/avx512.c");

    let cc = env::var("CC").unwrap_or_else(|_| "gcc".to_string());
    let compiled = Command::new(&cc)
        .args([
            "-c",
            "-O3",
            "-fPIC",
            "-mavx512f",
            "-std=c11",
            src.to_str().unwrap(),
            "-o",
            obj.to_str().unwrap(),
        ])
        .status();

    match compiled {
        Ok(s) if s.success() => {
            let archived = Command::new("ar")
                .args(["crs", lib.to_str().unwrap(), obj.to_str().unwrap()])
                .status();
            match archived {
                Ok(a) if a.success() => {
                    println!("cargo:rustc-link-search=native={}", out.display());
                    println!("cargo:rustc-link-lib=static=hsn_avx512");
                    println!("cargo:rustc-cfg=hsn_has_avx512_obj");
                }
                other => {
                    println!("cargo:warning=AVX-512 archive failed ({other:?}). Scalar/AVX2 only.");
                }
            }
        }
        Ok(s) => {
            println!(
                "cargo:warning=AVX-512 C kernel not built ({} exited {}). Scalar/AVX2 only.",
                cc, s
            );
        }
        Err(e) => {
            println!(
                "cargo:warning=AVX-512 C kernel skipped ({cc} unavailable: {e}). Scalar/AVX2 only."
            );
        }
    }
}
