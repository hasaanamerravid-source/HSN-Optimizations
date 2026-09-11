package hsn_odin

import "core:c"
import "core:math"

// Optional batch kernels. Built with `odin build -build-mode:shared` when
// the Odin compiler is on PATH. Java never requires this library.

when ODIN_OS == .Linux && ODIN_ARCH == .amd64 {
	// Linux x86_64 is the only shipped ABI.
}

cull_f64 :: proc(input: [^]f64, limit_sq: f64, out: [^]u8, n: u64) {
	if input == nil || out == nil || n == 0 {
		return
	}
	i: u64 = 0
	for i + 8 <= n {
		out[i + 0] = u8(input[i + 0] > limit_sq)
		out[i + 1] = u8(input[i + 1] > limit_sq)
		out[i + 2] = u8(input[i + 2] > limit_sq)
		out[i + 3] = u8(input[i + 3] > limit_sq)
		out[i + 4] = u8(input[i + 4] > limit_sq)
		out[i + 5] = u8(input[i + 5] > limit_sq)
		out[i + 6] = u8(input[i + 6] > limit_sq)
		out[i + 7] = u8(input[i + 7] > limit_sq)
		i += 8
	}
	for i < n {
		out[i] = u8(input[i] > limit_sq)
		i += 1
	}
}

dist_sq_xyz :: proc(x, y, z: [^]f64, ox, oy, oz: f64, out: [^]f64, n: u64) {
	if x == nil || y == nil || z == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		dx := x[i] - ox
		dy := y[i] - oy
		dz := z[i] - oz
		out[i] = dx * dx + dy * dy + dz * dz
	}
}

cone_mask :: proc(x, z: [^]f64, ox, oz, fx, fz: f64, out: [^]u8, n: u64) {
	if x == nil || z == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = u8((x[i] - ox) * fx + (z[i] - oz) * fz < 0)
	}
}

lod_band :: proc(dist_sq: [^]f64, near_sq, mid_sq, far_sq: f64, out: [^]u8, n: u64) {
	if dist_sq == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		d := dist_sq[i]
		band: u8 = 3
		if d <= near_sq {
			band = 0
		} else if d <= mid_sq {
			band = 1
		} else if d <= far_sq {
			band = 2
		}
		out[i] = band
	}
}

aabb_out :: proc(planes: [^]f32, box: [^]f32) -> u8 {
	for p in 0 ..< 6 {
		o := p * 4
		a := planes[o]
		b := planes[o + 1]
		c := planes[o + 2]
		d := planes[o + 3]
		px := box[3] if a >= 0 else box[0]
		py := box[4] if b >= 0 else box[1]
		pz := box[5] if c >= 0 else box[2]
		if a * px + b * py + c * pz + d < 0 {
			return 1
		}
	}
	return 0
}

cull_aabb :: proc(planes: [^]f32, aabb: [^]f32, out: [^]u8, n: u64) {
	if planes == nil || aabb == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = aabb_out(planes, aabb[i * 6:])
	}
}

cull_sphere :: proc(planes: [^]f32, xyzr: [^]f32, out: [^]u8, n: u64) {
	if planes == nil || xyzr == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		base := i * 4
		x := xyzr[base]
		y := xyzr[base + 1]
		z := xyzr[base + 2]
		r := xyzr[base + 3]
		drop: u8 = 0
		for p in 0 ..< 6 {
			o := p * 4
			d := planes[o] * x + planes[o + 1] * y + planes[o + 2] * z + planes[o + 3]
			if d < -r {
				drop = 1
				break
			}
		}
		out[i] = drop
	}
}

smoothstep :: proc(x, edge0, edge1: f64) -> f64 {
	span := edge1 - edge0
	if span <= 0 {
		return 0 if x < edge0 else 1
	}
	t := (x - edge0) / span
	if t < 0 { t = 0 }
	if t > 1 { t = 1 }
	return t * t * (3 - 2 * t)
}

quality_curve :: proc(dist_sq: [^]f64, max_dist, start_factor, min_q: f64, out: [^]f64, n: u64) {
	if dist_sq == nil || out == nil || n == 0 {
		return
	}
	if max_dist <= 0 {
		for i: u64 = 0; i < n; i += 1 {
			out[i] = 1
		}
		return
	}
	sf := math.clamp(start_factor, 0.15, 0.95)
	mq := math.clamp(min_q, 0.05, 1.0)
	start_sq := (max_dist * sf) * (max_dist * sf)
	max_sq := max_dist * max_dist
	span := max_sq - start_sq
	for i: u64 = 0; i < n; i += 1 {
		d := dist_sq[i]
		if d <= start_sq {
			out[i] = 1
			continue
		}
		if d >= max_sq || span <= 0.0001 {
			out[i] = mq
			continue
		}
		t := math.clamp((d - start_sq) / span, 0.0, 1.0)
		t = t * t * (3 - 2 * t)
		out[i] = math.clamp(1 - t * (1 - mq), mq, 1.0)
	}
}

count_keep :: proc(mask: [^]u8, n: u64) -> u64 {
	if mask == nil || n == 0 {
		return 0
	}
	keep: u64 = 0
	for i: u64 = 0; i < n; i += 1 {
		if mask[i] == 0 {
			keep += 1
		}
	}
	return keep
}

or_mask :: proc(a, b, out: [^]u8, n: u64) {
	if a == nil || b == nil || out == nil {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = a[i] | b[i]
	}
}

and_mask :: proc(a, b, out: [^]u8, n: u64) {
	if a == nil || b == nil || out == nil {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = a[i] & b[i]
	}
}

rsqrt_f32 :: proc(in_v, out_v: [^]f32, n: u64) {
	if in_v == nil || out_v == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		x := in_v[i]
		if x <= 0 {
			out_v[i] = 0
			continue
		}
		out_v[i] = 1.0 / math.sqrt(x)
	}
}

@(export)
hsn_odin_cull_f64 :: proc "c" (input: [^]f64, limit_sq: f64, out: [^]u8, n: u64) {
	context = runtime_default_context()
	cull_f64(input, limit_sq, out, n)
}

@(export)
hsn_odin_dist_sq :: proc "c" (x, y, z: [^]f64, ox, oy, oz: f64, out: [^]f64, n: u64) {
	context = runtime_default_context()
	dist_sq_xyz(x, y, z, ox, oy, oz, out, n)
}

@(export)
hsn_odin_cone_mask :: proc "c" (x, z: [^]f64, ox, oz, fx, fz: f64, out: [^]u8, n: u64) {
	context = runtime_default_context()
	cone_mask(x, z, ox, oz, fx, fz, out, n)
}

@(export)
hsn_odin_lod_band :: proc "c" (dist_sq: [^]f64, near_sq, mid_sq, far_sq: f64, out: [^]u8, n: u64) {
	context = runtime_default_context()
	lod_band(dist_sq, near_sq, mid_sq, far_sq, out, n)
}

@(export)
hsn_odin_cull_aabb :: proc "c" (planes, aabb: [^]f32, out: [^]u8, n: u64) {
	context = runtime_default_context()
	cull_aabb(planes, aabb, out, n)
}

@(export)
hsn_odin_cull_sphere :: proc "c" (planes, xyzr: [^]f32, out: [^]u8, n: u64) {
	context = runtime_default_context()
	cull_sphere(planes, xyzr, out, n)
}

@(export)
hsn_odin_quality :: proc "c" (dist_sq: [^]f64, max_dist, start_factor, min_q: f64, out: [^]f64, n: u64) {
	context = runtime_default_context()
	quality_curve(dist_sq, max_dist, start_factor, min_q, out, n)
}

@(export)
hsn_odin_count_keep :: proc "c" (mask: [^]u8, n: u64) -> u64 {
	context = runtime_default_context()
	return count_keep(mask, n)
}

@(export)
hsn_odin_abi :: proc "c" () -> c.int {
	return 0x4F444E31 // "ODN1"
}

runtime_default_context :: proc() -> runtime.Context {
	return runtime.default_context()
}

import "base:runtime"
