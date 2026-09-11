# HSN distance-mask kernel. Compiles to a C ABI shared object.
# Panama calls this with heap arrays; the proc never allocates.
proc hsn_nim_cull_f64*(distSq: ptr float64, limitSq: float64,
        outMask: ptr int8, n: int64) {.exportc, dynlib, cdecl, gcsafe.} =
  if distSq.isNil or outMask.isNil or n <= 0:
    return
  let src = cast[ptr UncheckedArray[float64]](distSq)
  let dst = cast[ptr UncheckedArray[int8]](outMask)
  var i: int64 = 0
  let last = n - 8
  while i <= last:
    dst[i]     = int8(ord(src[i]     > limitSq))
    dst[i + 1] = int8(ord(src[i + 1] > limitSq))
    dst[i + 2] = int8(ord(src[i + 2] > limitSq))
    dst[i + 3] = int8(ord(src[i + 3] > limitSq))
    dst[i + 4] = int8(ord(src[i + 4] > limitSq))
    dst[i + 5] = int8(ord(src[i + 5] > limitSq))
    dst[i + 6] = int8(ord(src[i + 6] > limitSq))
    dst[i + 7] = int8(ord(src[i + 7] > limitSq))
    i += 8
  while i < n:
    dst[i] = int8(ord(src[i] > limitSq))
    inc i
