# Builds native kernels for HSN Optimizations.
# All targets fail-open: missing toolchain = no .so, Java falls back silently.
OUT ?= ../common/src/main/resources/natives/linux-x86_64
CXX ?= g++
CC  ?= gcc
GO  ?= go
CARGO ?= cargo
ZIG ?= zig
NIM ?= nim
LDC ?= ldc2
GFORTRAN ?= gfortran
SWIFTC ?= swiftc
GHC ?= ghc

# Baseline: kernels opt-in to AVX2/FMA via attributes. Shared objects still
# pass -mavx2 so the compiler may emit vzeroupper-safe helpers.
# Rust uses x86-64-v3 (AVX2+FMA). avx512 target is separate and fail-open.
CFLAGS_BASE   = -O3 -ffast-math -fPIC -fvisibility=hidden -fno-plt -fno-semantic-interposition -fomit-frame-pointer
CFLAGS_SO     = $(CFLAGS_BASE) -shared -mavx2
CXXFLAGS_SO   = $(CFLAGS_BASE) -shared -mavx2 -mfma -std=c++23 -pthread
TMPDIR_HSN   ?= /tmp/hsn-build
.DELETE_ON_ERROR:

# Export RUSTFLAGS so cargo picks up AVX2+FMA globally.
# x86-64-v3 = SSE4.2 + AVX + AVX2 + FMA — matches Haswell+.
export RUSTFLAGS = -C target-cpu=x86-64-v3 -C opt-level=3

ASM_SRC = asm/hsn_dist.S asm/hsn_mask8.S asm/hsn_rsqrt.S asm/hsn_soa_extra.S \
          asm/hsn_touch.S asm/hsn_batch_extra.S asm/hsn_horizon.S
CPP_SRC = cpp/hsn_batch.cpp cpp/hsn_frustum.cpp cpp/hsn_simd_math.cpp cpp/hsn_soa.cpp \
          cpp/hsn_extra.cpp cpp/hsn_lod.cpp cpp/hsn_boot.cpp

.PHONY: all clean rust rust_lod pipeline zig nim dlang fortran swift haskell ispc hsnl \
        extra_langs verify avx512

all: $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_c.so $(OUT)/libhsn_horizon.so $(OUT)/libhsn_go.so \
     pipeline extra_langs avx512

$(OUT)/libhsn_cpp.so: $(CPP_SRC)
	mkdir -p $(OUT)
	$(CXX) $(CXXFLAGS_SO) -o $@ $(CPP_SRC)

$(OUT)/libhsn_asm.so: $(ASM_SRC)
	mkdir -p $(OUT)
	$(CC) -O3 -mavx2 -fPIC -shared -o $@ $(ASM_SRC)

$(OUT)/libhsn_horizon.so: c/hsn_horizon.c
	mkdir -p $(OUT)
	$(CC) $(CFLAGS_SO) -o $@ $<

$(OUT)/libhsn_c.so: c/hsn_c.c
	mkdir -p $(OUT)
	$(CC) $(CFLAGS_SO) -o $@ $<

$(OUT)/libhsn_go.so: go/hsn_keep.go go/go.mod
	mkdir -p $(OUT)
	cd go && $(GO) build -buildmode=c-shared -ldflags="-s -w" -o $(abspath $(OUT)/libhsn_go.so)

$(OUT)/libhsn_pipeline.so: pipeline/simd_math.s pipeline/native_engine.c pipeline/cpp_vulkan.cpp
	mkdir -p $(OUT) $(TMPDIR_HSN)
	$(CC) -O3 -mavx2 -fPIC -c -o $(TMPDIR_HSN)/hsn_simd_math.o pipeline/simd_math.s
	$(CC) $(CFLAGS_BASE) -mavx2 -fPIC -c -o $(TMPDIR_HSN)/hsn_native_engine.o pipeline/native_engine.c
	$(CXX) $(CFLAGS_BASE) -mavx2 -std=c++23 -fPIC -c -o $(TMPDIR_HSN)/hsn_cpp_vulkan.o pipeline/cpp_vulkan.cpp
	$(CXX) -O3 -fPIC -shared -fvisibility=hidden -mavx2 -o $@ \
		$(TMPDIR_HSN)/hsn_simd_math.o $(TMPDIR_HSN)/hsn_native_engine.o $(TMPDIR_HSN)/hsn_cpp_vulkan.o

pipeline: $(OUT)/libhsn_pipeline.so

rust:
	cd ../rust/hsn_hotpath && $(CARGO) build --release
	mkdir -p $(OUT)
	cp ../rust/hsn_hotpath/target/release/libhsn_hotpath.so $(OUT)/libhsn_hotpath.so

rust_lod:
	cd ../rust/rust_lod && $(CARGO) build --release
	mkdir -p $(OUT)
	cp ../rust/rust_lod/target/release/libhsn_lod.so $(OUT)/libhsn_lod.so

zig:
	mkdir -p $(OUT)
	@if command -v $(ZIG) >/dev/null 2>&1; then \
		$(ZIG) build-lib -dynamic -OReleaseFast -fPIC -fcompiler-rt \
			-mcpu=x86_64_v3 \
			-femit-bin=$(OUT)/libhsn_zig.so zig_culler/culler.zig; \
	else \
		echo "zig not installed; skipping libhsn_zig.so"; \
	fi

nim:
	mkdir -p $(OUT)
	@if command -v $(NIM) >/dev/null 2>&1; then \
		$(NIM) c -d:release -d:danger --opt:speed --app:lib --noMain --gc:arc \
			--passC:-O3 --passC:-ffast-math --passC:-fPIC --passC:-mavx2 \
			--out:$(OUT)/libhsn_nim.so nim/hsn_nim.nim; \
	else \
		echo "nim not installed; skipping libhsn_nim.so"; \
	fi

dlang:
	mkdir -p $(OUT)
	@if command -v $(LDC) >/dev/null 2>&1; then \
		$(LDC) -O3 -release -boundscheck=off -betterC -mtriple=x86_64-linux-gnu \
			-mattr=+avx2,+fma -shared -of=$(OUT)/libhsn_d.so d/hsn_d.d; \
	elif command -v dmd >/dev/null 2>&1; then \
		dmd -O -release -boundscheck=off -betterC -shared -of=$(OUT)/libhsn_d.so d/hsn_d.d; \
	else \
		echo "ldc2/dmd not installed; skipping libhsn_d.so"; \
	fi

fortran:
	mkdir -p $(OUT)
	@if command -v $(GFORTRAN) >/dev/null 2>&1; then \
		$(GFORTRAN) -O3 -ffast-math -fPIC -shared -fvisibility=hidden -mavx2 -mfma \
			-o $(OUT)/libhsn_fortran.so fortran/hsn_fortran.f90; \
	else \
		echo "gfortran not installed; skipping libhsn_fortran.so"; \
	fi

swift:
	mkdir -p $(OUT)
	@if command -v $(SWIFTC) >/dev/null 2>&1; then \
		$(SWIFTC) -O -whole-module-optimization -parse-as-library -emit-library \
			-o $(OUT)/libhsn_swift.so swift/HSNCull.swift swift/HSNFrustum.swift swift/HSNMask.swift; \
	else \
		echo "swiftc not installed; skipping libhsn_swift.so"; \
	fi

haskell:
	mkdir -p $(OUT)
	@if command -v $(GHC) >/dev/null 2>&1; then \
		$(GHC) -O2 -dynamic -shared -fPIC -o $(OUT)/libhsn_haskell.so haskell/HSNMask.hs || \
			echo "ghc present but Haskell sidecar failed (fail-open)"; \
	else \
		echo "ghc not installed; skipping libhsn_haskell.so"; \
	fi

avx512:
	mkdir -p $(OUT)
	@if $(CC) -O3 -mavx512f -mavx512dq -fPIC -shared -o $(OUT)/libhsn_asm512.so asm/hsn_avx512.S 2>/dev/null; then \
		echo "built libhsn_asm512.so"; \
	else \
		echo "toolchain has no AVX-512 asm; skipping libhsn_asm512.so"; \
	fi
	@if test -f ../rust/hsn_hotpath/src/avx512.c; then \
		$(CC) -O3 -mavx512f -mavx512dq -mfma -fPIC -shared -fvisibility=hidden \
			-o $(OUT)/libhsn_avx512.so ../rust/hsn_hotpath/src/avx512.c \
			|| echo "skip libhsn_avx512.so"; \
	fi

ispc:
	mkdir -p $(OUT)
	@if command -v ispc >/dev/null 2>&1; then \
		ispc -O3 --pic --target=avx2-i32x8 -o /tmp/hsn_ispc.o \
			ispc/hsn_cull.ispc && \
		$(CC) -O3 -fPIC -shared -o $(OUT)/libhsn_ispc.so /tmp/hsn_ispc.o; \
	else \
		echo "ispc not installed; building scalar fallback libhsn_ispc.so"; \
		$(CC) $(CFLAGS_SO) -o $(OUT)/libhsn_ispc.so ispc/hsn_cull_fallback.c; \
	fi

hsnl:
	mkdir -p $(OUT) hsnl/generated
	python3 hsnl/hsnlc.py hsnl/cull.hsnl -o hsnl/generated/hsn_hsnl.c
	$(CC) $(CFLAGS_SO) -o $(OUT)/libhsn_hsnl.so hsnl/generated/hsn_hsnl.c

extra_langs: nim dlang fortran swift haskell ispc hsnl

verify: all
	$(CC) -O2 -ldl -o /tmp/hsn_verify test/verify_kernels.c
	/tmp/hsn_verify $(OUT)

clean:
	rm -f $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_go.so $(OUT)/libhsn_c.so
	rm -f $(OUT)/libhsn_pipeline.so $(OUT)/libhsn_lod.so $(OUT)/libhsn_zig.so
	rm -f $(OUT)/libhsn_nim.so $(OUT)/libhsn_d.so $(OUT)/libhsn_fortran.so
	rm -f $(OUT)/libhsn_swift.so $(OUT)/libhsn_haskell.so $(OUT)/libhsn_asm512.so
	rm -f $(OUT)/libhsn_ispc.so $(OUT)/libhsn_hsnl.so
	rm -f $(OUT)/libhsn_go.h go/libhsn_go.h
