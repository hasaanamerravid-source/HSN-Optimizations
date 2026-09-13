# Native kernels for HSN Optimizations.
# Missing toolchain = no .so. Java falls back.
OUT ?= prebuilt/linux-x86_64
CXX ?= g++
CC  ?= gcc
CARGO ?= cargo

CFLAGS_BASE   = -O3 -ffast-math -fPIC -fvisibility=hidden -fno-plt -fno-semantic-interposition -fomit-frame-pointer
CFLAGS_SO     = $(CFLAGS_BASE) -shared -mavx2
CXXFLAGS_SO   = $(CFLAGS_BASE) -shared -mavx2 -mfma -std=c++23 -pthread
TMPDIR_HSN   ?= /tmp/hsn-build
.DELETE_ON_ERROR:

export RUSTFLAGS = -C target-cpu=x86-64-v3 -C opt-level=3

ASM_SRC = asm/hsn_dist.S asm/hsn_mask8.S asm/hsn_rsqrt.S asm/hsn_soa_extra.S \
          asm/hsn_touch.S asm/hsn_batch_extra.S asm/hsn_horizon.S
CPP_SRC = cpp/hsn_batch.cpp cpp/hsn_frustum.cpp cpp/hsn_simd_math.cpp cpp/hsn_soa.cpp \
          cpp/hsn_extra.cpp cpp/hsn_lod.cpp cpp/hsn_boot.cpp

.PHONY: all clean rust rust_lod pipeline verify avx512

all: $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_c.so $(OUT)/libhsn_horizon.so pipeline avx512

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

verify: all
	$(CC) -O2 -ldl -o /tmp/hsn_verify test/verify_kernels.c
	/tmp/hsn_verify $(OUT)

clean:
	rm -f $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_c.so
	rm -f $(OUT)/libhsn_pipeline.so $(OUT)/libhsn_lod.so
	rm -f $(OUT)/libhsn_horizon.so $(OUT)/libhsn_asm512.so $(OUT)/libhsn_avx512.so
