#if os(Linux)
import Glibc
#else
import Darwin
#endif

/// Extra Swift kernels (mask / LOD / Morton). Optional sidecar.

@inline(__always)
func hsnPart1By2(_ raw: UInt32) -> UInt32 {
    var n = raw & 0x0000_03ff
    n = (n | (n << 16)) & 0x0300_00FF
    n = (n | (n << 8)) & 0x0300_F00F
    n = (n | (n << 4)) & 0x030C_30C3
    n = (n | (n << 2)) & 0x0924_9249
    return n
}

@_cdecl("hsn_swift_lod_band")
public func hsnSwiftLodBand(_ distSq: UnsafePointer<Double>?,
                            _ nearSq: Double,
                            _ midSq: Double,
                            _ out: UnsafeMutablePointer<UInt8>?,
                            _ n: UInt64) {
    guard let distSq, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let d = distSq[Int(i)]
        if d < nearSq {
            out[Int(i)] = 0
        } else if d < midSq {
            out[Int(i)] = 1
        } else {
            out[Int(i)] = 2
        }
        i += 1
    }
}

@_cdecl("hsn_swift_rsqrt_f64")
public func hsnSwiftRsqrt(_ input: UnsafePointer<Double>?,
                          _ out: UnsafeMutablePointer<Double>?,
                          _ n: UInt64) {
    guard let input, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let v = input[Int(i)]
        if v > 0 {
            var y = 1.0 / v.squareRoot()
            y = y * (1.5 - 0.5 * v * y * y)
            out[Int(i)] = y
        } else {
            out[Int(i)] = 0
        }
        i += 1
    }
}

@_cdecl("hsn_swift_morton3")
public func hsnSwiftMorton3(_ x: UnsafePointer<Int32>?,
                            _ y: UnsafePointer<Int32>?,
                            _ z: UnsafePointer<Int32>?,
                            _ out: UnsafeMutablePointer<UInt32>?,
                            _ n: UInt64) {
    guard let x, let y, let z, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let idx = Int(i)
        let xi = hsnPart1By2(UInt32(bitPattern: x[idx]) & 0x3ff)
        let yi = hsnPart1By2(UInt32(bitPattern: y[idx]) & 0x3ff)
        let zi = hsnPart1By2(UInt32(bitPattern: z[idx]) & 0x3ff)
        out[idx] = xi | (yi << 1) | (zi << 2)
        i += 1
    }
}

@_cdecl("hsn_swift_keep_u8")
public func hsnSwiftKeep(_ rnd: UnsafePointer<UInt8>?,
                         _ thresh: UInt8,
                         _ out: UnsafeMutablePointer<UInt8>?,
                         _ n: UInt64) {
    guard let rnd, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        out[Int(i)] = rnd[Int(i)] < thresh ? 1 : 0
        i += 1
    }
}

@_cdecl("hsn_swift_frustum_aabb")
public func hsnSwiftFrustumAabb(_ planes: UnsafePointer<Float>?,
                                _ aabb: UnsafePointer<Float>?,
                                _ out: UnsafeMutablePointer<UInt8>?,
                                _ n: UInt64) {
    guard let planes, let aabb, let out, n > 0 else { return }
    var i: UInt64 = 0
    while i < n {
        let b = Int(i) * 6
        let minx = aabb[b], miny = aabb[b + 1], minz = aabb[b + 2]
        let maxx = aabb[b + 3], maxy = aabb[b + 4], maxz = aabb[b + 5]
        var drop: UInt8 = 0
        var p = 0
        while p < 6 {
            let o = p * 4
            let a = planes[o], b0 = planes[o + 1], c = planes[o + 2], d = planes[o + 3]
            let px = a >= 0 ? maxx : minx
            let py = b0 >= 0 ? maxy : miny
            let pz = c >= 0 ? maxz : minz
            if a * px + b0 * py + c * pz + d < 0 {
                drop = 1
                break
            }
            p += 1
        }
        out[Int(i)] = drop
        i += 1
    }
}

@_cdecl("hsn_swift_mask_abi")
public func hsnSwiftMaskAbi() -> Int32 {
    return 0x53575446 // "SWTF"
}
