package hsn.modod.polyglot

object GpuTier {
    @JvmStatic
    fun pick(renderer: String?, integratedHint: Boolean): String {
        val r = renderer.orEmpty().lowercase()
        if (listOf("llvmpipe", "softpipe", "software rasterizer", "gma",
                "hd graphics 2000", "hd graphics 3000", "hd graphics 4000").any { r.contains(it) }) {
            return "ULTRA_LOW"
        }
        if (integratedHint) return "SAFE"
        if (listOf("rtx 50", "rtx50", "rtx 4090", "rtx 4080", "rtx 5090", "rtx 5080",
                "rx 7900", "rx 9070", "rx 8090").any { r.contains(it) }) {
            return "COMPETITIVE"
        }
        return "BALANCED"
    }
}
