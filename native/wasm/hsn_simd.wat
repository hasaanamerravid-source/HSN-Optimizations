;; WASM SIMD batch compare: out[i] = (in[i] > limit)
;; Linear memory layout: [0, 8n) f64 inputs, [8n, 9n) i8 outputs
(module
  (memory (export "memory") 16)
  (func (export "cull_f64") (param $ptr i32) (param $n i32) (param $limit f64) (param $out i32)
    (local $i i32)
    (local $lim v128)
    (local.set $lim (f64x2.splat (local.get $limit)))
    (block $done
      (loop $top
        (br_if $done (i32.ge_u (local.get $i) (local.get $n)))
        (if (i32.le_u (i32.add (local.get $i) (i32.const 2)) (local.get $n))
          (then
            (v128.store
              (i32.add (local.get $out) (local.get $i))
              (f64x2.gt
                (v128.load (i32.add (local.get $ptr) (i32.shl (local.get $i) (i32.const 3))))
                (local.get $lim)))
            (local.set $i (i32.add (local.get $i) (i32.const 2))))
          (else
            (i32.store8
              (i32.add (local.get $out) (local.get $i))
              (f64.gt
                (f64.load (i32.add (local.get $ptr) (i32.shl (local.get $i) (i32.const 3))))
                (local.get $limit)))
            (local.set $i (i32.add (local.get $i) (i32.const 1)))))
        (br $top))))
)
