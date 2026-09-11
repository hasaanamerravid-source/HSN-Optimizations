/// View-projection plane extraction and combined frustum + distance tests.

@inline(__always)
func hsnInvLen(_ a: Float, _ b: Float, _ c: Float) -> Float {
    let s = a * a + b * b + c * c
    if s <= 1e-12 { return 1 }
    return 1 / s.squareRoot()
}

@_cdecl("hsn_swift_extract_planes")
public func hsnSwiftExtractPlanes(_ viewproj: UnsafePointer<Float>?,
                                  _ planes: UnsafeMutablePointer<Float>?) {
    guard let viewproj, let planes else { return }
    func store(_ idx: Int, _ a: Float, _ b: Float, _ c: Float, _ d: Float) {
        let inv = hsnInvLen(a, b, c)
        let o = idx * 4
        planes[o] = a * inv
        planes[o + 1] = b * inv
        planes[o + 2] = c * inv
        planes[o + 3] = d * inv
    }
    let m = viewproj
    store(0, m[3] + m[0], m[7] + m[4], m[11] + m[8],  m[15] + m[12])
    store(1, m[3] - m[0], m[7] - m[4], m[11] - m[8],  m[15] - m[12])
    store(2, m[3] + m[1], m[7] + m[5], m[11] + m[9],  m[15] + m[13])
    store(3, m[3] - m[1], m[7] - m[5], m[11] - m[9],  m[15] - m[13])
    store(4, m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14])
    store(5, m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14])
}

@_cdecl("hsn_swift_combined_mask")
public func hsnSwiftCombined(_ x: UnsafePointer<Double>?,
                             _ y: UnsafePointer<Double>?,
                             _ z: UnsafePointer<Double>?,
                             _ ox: Double,
                             _ oy: Double,
                             _ oz: Double,
                             _ limitSq: Double,
                             _ fx: Double,
                             _ fz: Double,
                             _ out: UnsafeMutablePointer<UInt8>?,
                             _ n: UInt64) {
    guard let x, let y, let z, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        let dx = x[idx] - ox
        let dy = y[idx] - oy
        let dz = z[idx] - oz
        let far = dx * dx + dy * dy + dz * dz > limitSq
        let behind = dx * fx + dz * fz < 0
        out[idx] = (far || behind) ? 1 : 0
        i += 1
    }
}

@_cdecl("hsn_swift_lerp_f32")
public func hsnSwiftLerp(_ a: UnsafePointer<Float>?,
                         _ b: UnsafePointer<Float>?,
                         _ t: Float,
                         _ out: UnsafeMutablePointer<Float>?,
                         _ n: UInt64) {
    guard let a, let b, let out, n > 0 else { return }
    let u = 1 - t
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        out[idx] = a[idx] * u + b[idx] * t
        i += 1
    }
}

@_cdecl("hsn_swift_smoothstep")
public func hsnSwiftSmooth(_ x: UnsafePointer<Double>?,
                           _ edge0: Double,
                           _ edge1: Double,
                           _ out: UnsafeMutablePointer<Double>?,
                           _ n: UInt64) {
    guard let x, let out, n > 0 else { return }
    let span = edge1 - edge0
    var i: UInt64 = 0
    if !(span > 0) {
        while i < n {
            out[Int(i)] = x[Int(i)] < edge0 ? 0 : 1
            i += 1
        }
        return
    }
    while i < n {
        var t = (x[Int(i)] - edge0) / span
        if t < 0 { t = 0 }
        if t > 1 { t = 1 }
        out[Int(i)] = t * t * (3 - 2 * t)
        i += 1
    }
}

@_cdecl("hsn_swift_minmax_f32")
public func hsnSwiftMinMax(_ input: UnsafePointer<Float>?,
                           _ mn: UnsafeMutablePointer<Float>?,
                           _ mx: UnsafeMutablePointer<Float>?,
                           _ n: UInt64) {
    guard let input, let mn, let mx, n > 0 else {
        mn?.pointee = 0
        mx?.pointee = 0
        return
    }
    var lo = input[0]
    var hi = input[0]
    var i: UInt64 = 1
    while i < n {
        let v = input[Int(i)]
        if v < lo { lo = v }
        if v > hi { hi = v }
        i += 1
    }
    mn.pointee = lo
    mx.pointee = hi
}
