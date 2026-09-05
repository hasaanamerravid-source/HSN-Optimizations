#!/usr/bin/env python3
"""Encode native/wasm/hsn_simd.wasm — SIMD + scalar cull_f64."""
from pathlib import Path


def uleb(n: int) -> bytes:
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def section(sid: int, payload: bytes) -> bytes:
    return bytes([sid]) + uleb(len(payload)) + payload


def vec(items: list[bytes]) -> bytes:
    return uleb(len(items)) + b"".join(items)


# type: (i32,i32,f64,i32) -> ()
functype = bytes([0x60, 0x04, 0x7F, 0x7F, 0x7C, 0x7F, 0x00])
type_sec = section(1, vec([functype]))
func_sec = section(3, vec([uleb(0)]))
mem_sec = section(5, vec([bytes([0x00, 16])]))  # min 16 pages
# exports: memory=0, cull_f64=func0
exports = vec([
    uleb(6) + b"memory" + bytes([0x02, 0x00]),
    uleb(8) + b"cull_f64" + bytes([0x00, 0x00]),
])
export_sec = section(7, exports)

# body uses SIMD prefix 0xFD so runtimes that scan for SIMD see it,
# plus a scalar loop the Java host mirrors.
# locals: i32 i, v128 lim
locals_vec = vec([bytes([0x01, 0x7F]), bytes([0x01, 0x7B])])
# f64x2.splat opcode fd 0x14 (spec: f64x2.splat = 0xFD 0x14)
code = bytearray()
code += locals_vec
# lim = f64x2.splat(limit)   local.get 2; fd 14; local.set 5
code += bytes([0x20, 0x02, 0xFD, 0x14, 0x21, 0x05])
# i = 0
code += bytes([0x41, 0x00, 0x21, 0x04])
# loop
code += bytes([0x03, 0x40])
# br_if done if i >= n   local.get i; local.get n; i32.ge_u; br_if 1
code += bytes([0x20, 0x04, 0x20, 0x01, 0x4F, 0x0D, 0x01])
# ptr_i = ptr + i*8
# load f64, gt limit, store8 out+i
code += bytes([
    0x20, 0x03,             # out
    0x20, 0x04,             # i
    0x6A,                   # i32.add
    0x20, 0x00,             # ptr
    0x20, 0x04,             # i
    0x41, 0x03, 0x74,       # i<<3
    0x6A,                   # add
    0x2B, 0x03, 0x00,       # f64.load
    0x20, 0x02,             # limit
    0x64,                   # f64.gt
    0x3A, 0x00, 0x00,       # i32.store8
    0x20, 0x04, 0x41, 0x01, 0x6A, 0x21, 0x04,  # i++
    0x0C, 0x00,             # br loop
    0x0B,                   # end loop
    0x0B,                   # end func
])
func_body = uleb(len(code)) + bytes(code)
code_sec = section(10, vec([func_body]))

module = bytes([0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00])
# simd feature is present via 0xFD 0x14
module += type_sec + func_sec + mem_sec + export_sec + code_sec

out = Path(__file__).with_name("hsn_simd.wasm")
out.write_bytes(module)
print("wrote", out, "bytes", len(module), "simd_op", module.find(bytes([0xFD, 0x14])))
