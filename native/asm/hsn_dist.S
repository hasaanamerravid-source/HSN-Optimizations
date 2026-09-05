/* System V AMD64
 * hsn_asm_cull_f64(const double *in, double limit_sq, uint8_t *out, uint64_t n)
 *   rdi=in  xmm0=limit_sq  rsi=out  rdx=n
 */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_cull_f64
        .type   hsn_asm_cull_f64, @function
hsn_asm_cull_f64:
        test    rdx, rdx
        jz      .Lret
        xor     rcx, rcx
        vbroadcastsd ymm3, xmm0          /* ymm3 = limit_sq */
        mov     r8, rdx
        and     r8, -4
        jz      .Ltail
.Lloop:
        vmovupd ymm0, [rdi + rcx*8]
        vcmppd  ymm1, ymm0, ymm3, 14     /* GT_OQ: in > limit */
        vmovmskpd eax, ymm1
        mov     r9d, eax
        and     r9d, 1
        mov     byte ptr [rsi + rcx], r9b
        mov     r9d, eax
        shr     r9d, 1
        and     r9d, 1
        mov     byte ptr [rsi + rcx + 1], r9b
        mov     r9d, eax
        shr     r9d, 2
        and     r9d, 1
        mov     byte ptr [rsi + rcx + 2], r9b
        shr     eax, 3
        and     eax, 1
        mov     byte ptr [rsi + rcx + 3], al
        add     rcx, 4
        cmp     rcx, r8
        jb      .Lloop
        vzeroupper
.Ltail:
        cmp     rcx, rdx
        jae     .Lret
        vmovsd  xmm2, xmm3, xmm3         /* limit in xmm2 */
.Lscalar:
        vmovsd  xmm0, qword ptr [rdi + rcx*8]
        vucomisd xmm0, xmm2
        seta    al
        mov     byte ptr [rsi + rcx], al
        inc     rcx
        cmp     rcx, rdx
        jb      .Lscalar
.Lret:
        vzeroupper
        ret
        .size   hsn_asm_cull_f64, .-hsn_asm_cull_f64

        .globl  hsn_asm_abi
        .type   hsn_asm_abi, @function
hsn_asm_abi:
        mov     eax, 0x41534D32
        ret
        .size   hsn_asm_abi, .-hsn_asm_abi

/* void hsn_asm_cull_aabb_f32(const float *planes24, const float *aabb,
 *                            uint8_t *out, uint64_t n)
 *   rdi=planes  rsi=aabb  rdx=out  rcx=n
 * out[i]=1 if any of the 6 planes rejects the AABB. */
        .p2align 4
        .globl  hsn_asm_cull_aabb_f32
        .type   hsn_asm_cull_aabb_f32, @function
hsn_asm_cull_aabb_f32:
        test    rcx, rcx
        jz      .Laabb_ret
        test    rdi, rdi
        jz      .Laabb_ret
        test    rsi, rsi
        jz      .Laabb_ret
        test    rdx, rdx
        jz      .Laabb_ret
        xor     r8, r8                      /* i */
.Laabb_box:
        movss   xmm0, dword ptr [rsi]       /* minx */
        movss   xmm1, dword ptr [rsi + 4]
        movss   xmm2, dword ptr [rsi + 8]
        movss   xmm3, dword ptr [rsi + 12]  /* maxx */
        movss   xmm4, dword ptr [rsi + 16]
        movss   xmm5, dword ptr [rsi + 20]
        xor     r9d, r9d                    /* plane */
        xor     eax, eax                    /* drop */
.Laabb_plane:
        mov     r10, r9
        shl     r10, 4                      /* plane * 16 bytes */
        movss   xmm6, dword ptr [rdi + r10]      /* a */
        movss   xmm7, dword ptr [rdi + r10 + 4]  /* b */
        movss   xmm8, dword ptr [rdi + r10 + 8]  /* c */
        movss   xmm9, dword ptr [rdi + r10 + 12] /* d */
        xorps   xmm10, xmm10
        comiss  xmm6, xmm10
        jae     .Laabb_pxmax
        movaps  xmm11, xmm0
        jmp     .Laabb_px
.Laabb_pxmax:
        movaps  xmm11, xmm3
.Laabb_px:
        comiss  xmm7, xmm10
        jae     .Laabb_pymax
        movaps  xmm12, xmm1
        jmp     .Laabb_py
.Laabb_pymax:
        movaps  xmm12, xmm4
.Laabb_py:
        comiss  xmm8, xmm10
        jae     .Laabb_pzmax
        movaps  xmm13, xmm2
        jmp     .Laabb_pz
.Laabb_pzmax:
        movaps  xmm13, xmm5
.Laabb_pz:
        mulss   xmm6, xmm11
        mulss   xmm7, xmm12
        mulss   xmm8, xmm13
        addss   xmm6, xmm7
        addss   xmm6, xmm8
        addss   xmm6, xmm9
        comiss  xmm10, xmm6                 /* 0 ? d  →  CF if d < 0 */
        ja      .Laabb_drop                 /* 0 > d */
        inc     r9d
        cmp     r9d, 6
        jb      .Laabb_plane
        jmp     .Laabb_store
.Laabb_drop:
        mov     eax, 1
.Laabb_store:
        mov     byte ptr [rdx + r8], al
        add     rsi, 24
        inc     r8
        cmp     r8, rcx
        jb      .Laabb_box
.Laabb_ret:
        ret
        .size   hsn_asm_cull_aabb_f32, .-hsn_asm_cull_aabb_f32

/* void hsn_asm_cull_sphere_f32(const float *planes24, const float *xyzr,
 *                              uint8_t *out, uint64_t n) */
        .p2align 4
        .globl  hsn_asm_cull_sphere_f32
        .type   hsn_asm_cull_sphere_f32, @function
hsn_asm_cull_sphere_f32:
        test    rcx, rcx
        jz      .Lsph_ret
        test    rdi, rdi
        jz      .Lsph_ret
        test    rsi, rsi
        jz      .Lsph_ret
        test    rdx, rdx
        jz      .Lsph_ret
        xor     r8, r8
.Lsph_item:
        movss   xmm0, dword ptr [rsi]
        movss   xmm1, dword ptr [rsi + 4]
        movss   xmm2, dword ptr [rsi + 8]
        movss   xmm3, dword ptr [rsi + 12]  /* r */
        xorps   xmm4, xmm4
        subss   xmm4, xmm3                  /* -r */
        xor     r9d, r9d
        xor     eax, eax
.Lsph_plane:
        mov     r10, r9
        shl     r10, 4
        movss   xmm5, dword ptr [rdi + r10]
        movss   xmm6, dword ptr [rdi + r10 + 4]
        movss   xmm7, dword ptr [rdi + r10 + 8]
        movss   xmm8, dword ptr [rdi + r10 + 12]
        mulss   xmm5, xmm0
        mulss   xmm6, xmm1
        mulss   xmm7, xmm2
        addss   xmm5, xmm6
        addss   xmm5, xmm7
        addss   xmm5, xmm8                  /* dist */
        comiss  xmm4, xmm5                  /* -r ? dist */
        ja      .Lsph_drop                  /* -r > dist */
        inc     r9d
        cmp     r9d, 6
        jb      .Lsph_plane
        jmp     .Lsph_store
.Lsph_drop:
        mov     eax, 1
.Lsph_store:
        mov     byte ptr [rdx + r8], al
        add     rsi, 16
        inc     r8
        cmp     r8, rcx
        jb      .Lsph_item
.Lsph_ret:
        ret
        .size   hsn_asm_cull_sphere_f32, .-hsn_asm_cull_sphere_f32

        .section .note.GNU-stack,"",@progbits
