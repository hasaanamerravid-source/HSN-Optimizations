# Builds native kernels next to the Rust/C hotpath.
OUT ?= ../src/main/resources/natives/linux-x86_64
CXX ?= g++
CC  ?= gcc
GO  ?= go
CARGO ?= cargo
ZIG ?= zig
NIM ?= nim
LDC ?= ldc2
GFORTRAN ?= gfortran

CFLAGS_SO = -O3 -ffast-math -fPIC -shared -fvisibility=hidden -mavx2 -fno-plt
CXXFLAGS_SO = -O3 -ffast-math -fPIC -shared -fvisibility=hidden -mavx2 -fno-plt -std=c++23

.PHONY: all clean rust rust_lod pipeline zig nim dlang fortran extra_langs verify

all: $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_c.so $(OUT)/libhsn_go.so pipeline extra_langs

$(OUT)/libhsn_cpp.so: cpp/hsn_batch.cpp
	mkdir -p $(OUT)
	$(CXX) $(CXXFLAGS_SO) -o $@ $<

$(OUT)/libhsn_asm.so: asm/hsn_dist.S
	mkdir -p $(OUT)
	$(CC) -O3 -mavx2 -fPIC -shared -o $@ $<

$(OUT)/libhsn_c.so: c/hsn_c.c
	mkdir -p $(OUT)
	$(CC) $(CFLAGS_SO) -o $@ $<

$(OUT)/libhsn_go.so: go/hsn_keep.go go/go.mod
	mkdir -p $(OUT)
	cd go && $(GO) build -buildmode=c-shared -ldflags="-s -w" -o $(abspath $(OUT)/libhsn_go.so)

$(OUT)/libhsn_pipeline.so: pipeline/simd_math.s pipeline/native_engine.c pipeline/cpp_vulkan.cpp
	mkdir -p $(OUT)
	$(CC) -O3 -mavx2 -fPIC -c -o /tmp/hsn_simd_math.o pipeline/simd_math.s
	$(CC) -O3 -ffast-math -fPIC -fvisibility=hidden -mavx2 -c -o /tmp/hsn_native_engine.o pipeline/native_engine.c
	$(CXX) -O3 -ffast-math -fPIC -fvisibility=hidden -mavx2 -std=c++23 -c -o /tmp/hsn_cpp_vulkan.o pipeline/cpp_vulkan.cpp
	$(CXX) -O3 -fPIC -shared -fvisibility=hidden -mavx2 -o $@ /tmp/hsn_simd_math.o /tmp/hsn_native_engine.o /tmp/hsn_cpp_vulkan.o

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
		$(LDC) -O3 -release -boundscheck=off -betterC -shared -of=$(OUT)/libhsn_d.so d/hsn_d.d; \
	elif command -v dmd >/dev/null 2>&1; then \
		dmd -O -release -boundscheck=off -betterC -shared -of=$(OUT)/libhsn_d.so d/hsn_d.d; \
	else \
		echo "ldc2/dmd not installed; skipping libhsn_d.so"; \
	fi

fortran:
	mkdir -p $(OUT)
	@if command -v $(GFORTRAN) >/dev/null 2>&1; then \
		$(GFORTRAN) -O3 -ffast-math -fPIC -shared -fvisibility=hidden -mavx2 \
			-o $(OUT)/libhsn_fortran.so fortran/hsn_fortran.f90; \
	else \
		echo "gfortran not installed; skipping libhsn_fortran.so"; \
	fi

extra_langs: nim dlang fortran

verify: all
	$(CC) -O2 -ldl -o /tmp/hsn_verify test/verify_kernels.c
	/tmp/hsn_verify $(OUT)

clean:
	rm -f $(OUT)/libhsn_cpp.so $(OUT)/libhsn_asm.so $(OUT)/libhsn_go.so $(OUT)/libhsn_c.so
	rm -f $(OUT)/libhsn_pipeline.so $(OUT)/libhsn_lod.so $(OUT)/libhsn_zig.so
	rm -f $(OUT)/libhsn_nim.so $(OUT)/libhsn_d.so $(OUT)/libhsn_fortran.so
	rm -f $(OUT)/libhsn_go.h go/libhsn_go.h
