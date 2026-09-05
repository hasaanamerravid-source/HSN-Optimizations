package hsn.modod.polyglot

object KeepMask {
  def apply(hashes: Array[Double], keep: Double, out: Array[Byte], n: Int): Int = {
    if (hashes == null || out == null || n <= 0) return 0
    val len = math.min(n, math.min(hashes.length, out.length))
    var i = 0
    val bound = len - 3
    while (i < bound) {
      out(i)     = if (hashes(i)     > keep) 1.toByte else 0.toByte
      out(i + 1) = if (hashes(i + 1) > keep) 1.toByte else 0.toByte
      out(i + 2) = if (hashes(i + 2) > keep) 1.toByte else 0.toByte
      out(i + 3) = if (hashes(i + 3) > keep) 1.toByte else 0.toByte
      i += 4
    }
    while (i < len) {
      out(i) = if (hashes(i) > keep) 1.toByte else 0.toByte
      i += 1
    }
    len
  }
}
