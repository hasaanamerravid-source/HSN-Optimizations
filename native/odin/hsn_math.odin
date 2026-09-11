package hsn_odin

lerp_f32 :: proc(a, b: [^]f32, t: f32, out: [^]f32, n: u64) {
	if a == nil || b == nil || out == nil || n == 0 {
		return
	}
	u := 1 - t
	for i: u64 = 0; i < n; i += 1 {
		out[i] = a[i] * u + b[i] * t
	}
}

add_f32 :: proc(a, b, out: [^]f32, n: u64) {
	if a == nil || b == nil || out == nil {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = a[i] + b[i]
	}
}

madd_f32 :: proc(a, b, c, out: [^]f32, n: u64) {
	if a == nil || b == nil || c == nil || out == nil {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		out[i] = a[i] * b[i] + c[i]
	}
}

minmax_f32 :: proc(in_v: [^]f32, n: u64) -> (mn, mx: f32) {
	if in_v == nil || n == 0 {
		return 0, 0
	}
	mn = in_v[0]
	mx = in_v[0]
	for i: u64 = 1; i < n; i += 1 {
		v := in_v[i]
		if v < mn { mn = v }
		if v > mx { mx = v }
	}
	return
}

extract_planes :: proc(viewproj, planes: [^]f32) {
	if viewproj == nil || planes == nil {
		return
	}
	store :: proc(planes: [^]f32, idx: int, a, b, c, d: f32) {
		len2 := a * a + b * b + c * c
		inv: f32 = 1
		if len2 > 1e-12 {
			inv = 1 / math.sqrt(len2)
		}
		o := idx * 4
		planes[o] = a * inv
		planes[o + 1] = b * inv
		planes[o + 2] = c * inv
		planes[o + 3] = d * inv
	}
	m := viewproj
	store(planes, 0, m[3] + m[0], m[7] + m[4], m[11] + m[8],  m[15] + m[12])
	store(planes, 1, m[3] - m[0], m[7] - m[4], m[11] - m[8],  m[15] - m[12])
	store(planes, 2, m[3] + m[1], m[7] + m[5], m[11] + m[9],  m[15] + m[13])
	store(planes, 3, m[3] - m[1], m[7] - m[5], m[11] - m[9],  m[15] - m[13])
	store(planes, 4, m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14])
	store(planes, 5, m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14])
}

combined_mask :: proc(x, y, z: [^]f64, ox, oy, oz, limit_sq, fx, fz: f64, out: [^]u8, n: u64) {
	if x == nil || y == nil || z == nil || out == nil || n == 0 {
		return
	}
	for i: u64 = 0; i < n; i += 1 {
		dx := x[i] - ox
		dy := y[i] - oy
		dz := z[i] - oz
		far := dx * dx + dy * dy + dz * dz > limit_sq
		behind := dx * fx + dz * fz < 0
		out[i] = u8(far || behind)
	}
}

compact_xyz :: proc(x, y, z: [^]f64, drop: [^]u8, ox, oy, oz: [^]f64, n: u64) -> u64 {
	if x == nil || y == nil || z == nil || drop == nil || ox == nil || oy == nil || oz == nil {
		return 0
	}
	w: u64 = 0
	for i: u64 = 0; i < n; i += 1 {
		if drop[i] != 0 {
			continue
		}
		ox[w] = x[i]
		oy[w] = y[i]
		oz[w] = z[i]
		w += 1
	}
	return w
}

@(export)
hsn_odin_extract_planes :: proc "c" (viewproj, planes: [^]f32) {
	context = runtime_default_context()
	extract_planes(viewproj, planes)
}

@(export)
hsn_odin_combined_mask :: proc "c" (x, y, z: [^]f64, ox, oy, oz, limit_sq, fx, fz: f64, out: [^]u8, n: u64) {
	context = runtime_default_context()
	combined_mask(x, y, z, ox, oy, oz, limit_sq, fx, fz, out, n)
}
