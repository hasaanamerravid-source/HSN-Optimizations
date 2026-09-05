Rust + C AVX-512 object for libhsn_hotpath.so.

    cd rust/hsn_hotpath && cargo build --release
    cp target/release/libhsn_hotpath.so ../../src/main/resources/natives/linux-x86_64/

C++, Go and assembly kernels are in native/.

`rust_lod` writes LOD bands (0..3) from packed XYZ with no heap allocation.
