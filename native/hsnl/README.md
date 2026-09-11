# HSNL — HSN Kernel Language

A small data-parallel language used only for packed batch kernels
(distance tests, keep-masks, LOD bands). It is **not** a general-purpose
language and it is never interpreted on the Minecraft tick thread.

```
kernel cull_xyz_f32(in f32 x, in f32 y, in f32 z,
                    imm f32 ox, imm f32 oy, imm f32 oz,
                    imm f32 limit, out u8 mask) {
    foreach i in n:
        mask[i] = (x[i]-ox)*(x[i]-ox) + (y[i]-oy)*(y[i]-oy) + (z[i]-oz)*(z[i]-oz) > limit
}
```

`hsnlc.py` lowers that to C (`generated/hsn_hsnl.c`). `make hsnl` then
builds `libhsn_hsnl.so`. If Python or gcc is missing the target is skipped
and Java keeps using the existing ASM / C / C++ / Rust path.
