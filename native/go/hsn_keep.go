package main

import "C"

import (
	"unsafe"
)

//export hsn_go_abi
func hsn_go_abi() int32 {
	return 0x474F4C41
}

//export hsn_go_hash01
func hsn_go_hash01(a, b, c float64, tick int64) float64 {
	h := floatBits(a)*0x9E3779B97F4A7C15 ^
		floatBits(b)*0xBF58476D1CE4E5B9 ^
		floatBits(c)*0x94D049BB133111EB ^
		uint64(tick)*0x2545F4914F6CDD1D
	h ^= h >> 33
	h *= 0xff51afd7ed558ccd
	h ^= h >> 33
	return float64(h>>11) * (1.0 / 9007199254740992.0)
}

//export hsn_go_keep_mask
func hsn_go_keep_mask(hashes *float64, keep float64, out *byte, n int64) {
	if hashes == nil || out == nil || n <= 0 {
		return
	}
	hs := unsafe.Slice(hashes, int(n))
	dst := unsafe.Slice(out, int(n))
	for i := int64(0); i < n; i++ {
		if hs[i] > keep {
			dst[i] = 1
		} else {
			dst[i] = 0
		}
	}
}

func floatBits(v float64) uint64 {
	return *(*uint64)(unsafe.Pointer(&v))
}

func main() {}
