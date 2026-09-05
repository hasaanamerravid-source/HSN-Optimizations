//! Zero-allocation 4x4 matrix multiply and spatial hash. C ABI.

const std = @import("std");

export fn hsn_zig_abi() i32 {
    return 0x5A494731; // 'ZIG1'
}

/// out = A * B for column-major 4x4 float matrices.
export fn hsn_zig_mul_mat4(a: [*]const f32, b: [*]const f32, out: [*]f32) void {
    var tmp: [16]f32 = undefined;
    var col: usize = 0;
    while (col < 4) : (col += 1) {
        const b0 = b[col * 4 + 0];
        const b1 = b[col * 4 + 1];
        const b2 = b[col * 4 + 2];
        const b3 = b[col * 4 + 3];
        tmp[col * 4 + 0] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3;
        tmp[col * 4 + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3;
        tmp[col * 4 + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3;
        tmp[col * 4 + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3;
    }
    var i: usize = 0;
    while (i < 16) : (i += 1) {
        out[i] = tmp[i];
    }
}

inline fn floor_to_i32(v: f32) i32 {
    const t: i32 = @intFromFloat(v);
    if (v < 0.0 and @as(f32, @floatFromInt(t)) != v) {
        return t - 1;
    }
    return t;
}

inline fn hash3(ix: i32, iy: i32, iz: i32) u64 {
    var h: u64 = @as(u64, @bitCast(@as(i64, ix))) *% 0x9E3779B97F4A7C15;
    h ^= @as(u64, @bitCast(@as(i64, iy))) *% 0xBF58476D1CE4E5B9;
    h ^= @as(u64, @bitCast(@as(i64, iz))) *% 0x94D049BB133111EB;
    h ^= h >> 33;
    h *%= 0xff51afd7ed558ccd;
    h ^= h >> 33;
    return h;
}

export fn hsn_zig_spatial_hash(x: f32, y: f32, z: f32, cell: f32) u64 {
    const c = if (cell > 0.0) cell else 1.0;
    const inv = 1.0 / c;
    const ix = floor_to_i32(x * inv);
    const iy = floor_to_i32(y * inv);
    const iz = floor_to_i32(z * inv);
    return hash3(ix, iy, iz);
}

export fn hsn_zig_hash_batch(
    x: [*]const f32,
    y: [*]const f32,
    z: [*]const f32,
    cell: f32,
    out: [*]u64,
    n: usize,
) void {
    if (n == 0) return;
    const c = if (cell > 0.0) cell else 1.0;
    const inv = 1.0 / c;
    var i: usize = 0;
    while (i < n) : (i += 1) {
        const ix = floor_to_i32(x[i] * inv);
        const iy = floor_to_i32(y[i] * inv);
        const iz = floor_to_i32(z[i] * inv);
        out[i] = hash3(ix, iy, iz);
    }
}

test "mat4 identity" {
    const id = [_]f32{
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1,
    };
    var out: [16]f32 = undefined;
    hsn_zig_mul_mat4(&id, &id, &out);
    try std.testing.expectEqual(id, out);
}
