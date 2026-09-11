#if os(Linux)
import Glibc
#else
import Darwin
#endif

/// Optional Swift batch kernels. Built with `swiftc -emit-library` when
/// a toolchain is present. The Fabric jar never requires libhsn_swift.so.

@inline(__always)
func hsnDrop(_ value: Double, _ limitSq: Double) -> UInt8 {
    value > limitSq ? 1 : 0
}

@_cdecl("hsn_swift_cull_f64")
public func hsnSwiftCullF64(_ input: UnsafePointer<Double>?,
                            _ limitSq: Double,
                            _ out: UnsafeMutablePointer<UInt8>?,
                            _ n: UInt64) {
    guard let input, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i + 8 <= n {
        let base = Int(i)
        out[base + 0] = hsnDrop(input[base + 0], limitSq)
        out[base + 1] = hsnDrop(input[base + 1], limitSq)
        out[base + 2] = hsnDrop(input[base + 2], limitSq)
        out[base + 3] = hsnDrop(input[base + 3], limitSq)
        out[base + 4] = hsnDrop(input[base + 4], limitSq)
        out[base + 5] = hsnDrop(input[base + 5], limitSq)
        out[base + 6] = hsnDrop(input[base + 6], limitSq)
        out[base + 7] = hsnDrop(input[base + 7], limitSq)
        i += 8
    }
    while i < n {
        let idx = Int(i)
        out[idx] = hsnDrop(input[idx], limitSq)
        i += 1
    }
}

@_cdecl("hsn_swift_dist_sq")
public func hsnSwiftDistSq(_ x: UnsafePointer<Double>?,
                           _ y: UnsafePointer<Double>?,
                           _ z: UnsafePointer<Double>?,
                           _ ox: Double,
                           _ oy: Double,
                           _ oz: Double,
                           _ out: UnsafeMutablePointer<Double>?,
                           _ n: UInt64) {
    guard let x, let y, let z, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        let dx = x[idx] - ox
        let dy = y[idx] - oy
        let dz = z[idx] - oz
        out[idx] = dx * dx + dy * dy + dz * dz
        i += 1
    }
}

@_cdecl("hsn_swift_cone_mask")
public func hsnSwiftConeMask(_ x: UnsafePointer<Double>?,
                             _ z: UnsafePointer<Double>?,
                             _ ox: Double,
                             _ oz: Double,
                             _ fx: Double,
                             _ fz: Double,
                             _ out: UnsafeMutablePointer<UInt8>?,
                             _ n: UInt64) {
    guard let x, let z, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        out[idx] = ((x[idx] - ox) * fx + (z[idx] - oz) * fz) < 0 ? 1 : 0
        i += 1
    }
}

@inline(__always)
func planeRejects(_ a: Float, _ b: Float, _ c: Float, _ d: Float,
                  _ minx: Float, _ miny: Float, _ minz: Float,
                  _ maxx: Float, _ maxy: Float, _ maxz: Float) -> Bool {
    let px = a >= 0 ? maxx : minx
    let py = b >= 0 ? maxy : miny
    let pz = c >= 0 ? maxz : minz
    return a * px + b * py + c * pz + d < 0
}

@_cdecl("hsn_swift_cull_aabb")
public func hsnSwiftCullAabb(_ planes: UnsafePointer<Float>?,
                             _ aabb: UnsafePointer<Float>?,
                             _ out: UnsafeMutablePointer<UInt8>?,
                             _ n: UInt64) {
    guard let planes, let aabb, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let box = Int(i) * 6
        var drop: UInt8 = 0
        var p = 0
        while p < 6 {
            let o = p * 4
            if planeRejects(planes[o], planes[o + 1], planes[o + 2], planes[o + 3],
                            aabb[box], aabb[box + 1], aabb[box + 2],
                            aabb[box + 3], aabb[box + 4], aabb[box + 5]) {
                drop = 1
                break
            }
            p += 1
        }
        out[Int(i)] = drop
        i += 1
    }
}

@_cdecl("hsn_swift_cull_sphere")
public func hsnSwiftCullSphere(_ planes: UnsafePointer<Float>?,
                               _ xyzr: UnsafePointer<Float>?,
                               _ out: UnsafeMutablePointer<UInt8>?,
                               _ n: UInt64) {
    guard let planes, let xyzr, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let base = Int(i) * 4
        let x = xyzr[base]
        let y = xyzr[base + 1]
        let z = xyzr[base + 2]
        let r = xyzr[base + 3]
        var drop: UInt8 = 0
        var p = 0
        while p < 6 {
            let o = p * 4
            let d = planes[o] * x + planes[o + 1] * y + planes[o + 2] * z + planes[o + 3]
            if d < -r {
                drop = 1
                break
            }
            p += 1
        }
        out[Int(i)] = drop
        i += 1
    }
}

@_cdecl("hsn_swift_lod_band")
public func hsnSwiftLodBand(_ distSq: UnsafePointer<Double>?,
                            _ nearSq: Double,
                            _ midSq: Double,
                            _ farSq: Double,
                            _ out: UnsafeMutablePointer<UInt8>?,
                            _ n: UInt64) {
    guard let distSq, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let d = distSq[Int(i)]
        let band: UInt8
        if d <= nearSq {
            band = 0
        } else if d <= midSq {
            band = 1
        } else if d <= farSq {
            band = 2
        } else {
            band = 3
        }
        out[Int(i)] = band
        i += 1
    }
}

@_cdecl("hsn_swift_quality")
public func hsnSwiftQuality(_ distSq: UnsafePointer<Double>?,
                            _ maxDist: Double,
                            _ startFactor: Double,
                            _ minQ: Double,
                            _ out: UnsafeMutablePointer<Double>?,
                            _ n: UInt64) {
    guard let distSq, let out, n > 0 else { return }
    if !(maxDist > 0) {
        var i: UInt64 = 0
        while i < n {
            out[Int(i)] = 1
            i += 1
        }
        return
    }
    let sf = min(max(startFactor, 0.15), 0.95)
    let mq = min(max(minQ, 0.05), 1.0)
    let startSq = (maxDist * sf) * (maxDist * sf)
    let maxSq = maxDist * maxDist
    let span = maxSq - startSq
    var i: UInt64 = 0
    while i < n {
        let d = distSq[Int(i)]
        if d <= startSq {
            out[Int(i)] = 1
        } else if d >= maxSq || span <= 0.0001 {
            out[Int(i)] = mq
        } else {
            var t = (d - startSq) / span
            if t < 0 { t = 0 }
            if t > 1 { t = 1 }
            t = t * t * (3 - 2 * t)
            var q = 1 - t * (1 - mq)
            if q < mq { q = mq }
            if q > 1 { q = 1 }
            out[Int(i)] = q
        }
        i += 1
    }
}

@_cdecl("hsn_swift_count_keep")
public func hsnSwiftCountKeep(_ mask: UnsafePointer<UInt8>?, _ n: UInt64) -> UInt64 {
    guard let mask, n > 0 else { return 0 }
    var keep: UInt64 = 0
    var i: UInt64 = 0
    while i < n {
        if mask[Int(i)] == 0 { keep += 1 }
        i += 1
    }
    return keep
}

@_cdecl("hsn_swift_or_mask")
public func hsnSwiftOrMask(_ a: UnsafePointer<UInt8>?,
                           _ b: UnsafePointer<UInt8>?,
                           _ out: UnsafeMutablePointer<UInt8>?,
                           _ n: UInt64) {
    guard let a, let b, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        out[idx] = a[idx] | b[idx]
        i += 1
    }
}

@_cdecl("hsn_swift_and_mask")
public func hsnSwiftAndMask(_ a: UnsafePointer<UInt8>?,
                            _ b: UnsafePointer<UInt8>?,
                            _ out: UnsafeMutablePointer<UInt8>?,
                            _ n: UInt64) {
    guard let a, let b, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        out[idx] = a[idx] & b[idx]
        i += 1
    }
}

@_cdecl("hsn_swift_rsqrt_f32")
public func hsnSwiftRsqrt(_ input: UnsafePointer<Float>?,
                          _ out: UnsafeMutablePointer<Float>?,
                          _ n: UInt64) {
    guard let input, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let x = input[Int(i)]
        out[Int(i)] = x > 0 ? 1.0 / x.squareRoot() : 0
        i += 1
    }
}

@_cdecl("hsn_swift_compact_xyz")
public func hsnSwiftCompact(_ x: UnsafePointer<Double>?,
                            _ y: UnsafePointer<Double>?,
                            _ z: UnsafePointer<Double>?,
                            _ drop: UnsafePointer<UInt8>?,
                            _ ox: UnsafeMutablePointer<Double>?,
                            _ oy: UnsafeMutablePointer<Double>?,
                            _ oz: UnsafeMutablePointer<Double>?,
                            _ n: UInt64,
                            _ kept: UnsafeMutablePointer<UInt64>?) {
    guard let x, let y, let z, let drop, let ox, let oy, let oz else {
        kept?.pointee = 0
        return
    }
    var w: UInt64 = 0
    var i: UInt64 = 0
    while i < n {
        if drop[Int(i)] == 0 {
            let wi = Int(w)
            let ii = Int(i)
            ox[wi] = x[ii]
            oy[wi] = y[ii]
            oz[wi] = z[ii]
            w += 1
        }
        i += 1
    }
    kept?.pointee = w
}

@_cdecl("hsn_swift_abi")
public func hsnSwiftAbi() -> Int32 {
    return 0x53575431
}
