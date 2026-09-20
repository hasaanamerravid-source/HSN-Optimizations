/* System V AMD64 — AVX-512 kernels
 *
 * hsn_asm512_cull_f64(const double *sq, double limit_sq, uint8_t *out, uint64_t n)
 *   rdi=sq  xmm0=limit_sq  rsi=out  rdx=n
 *
 * hsn_asm512_cull_xyz_f32(...) rdi=x rsi=y rdx=z xmm0=ox xmm1=oy xmm2=oz xmm3=lim rcx=out r8=n
 *
 * hsn_asm512_dot4_f32(const float *v4, const float *planes, uint8_t *out, uint64_t n)
 *
 * Vector loops never overrun n. Remainder goes through a scalar tail.
 * 8-bit compare masks are expanded with BMI2 PDEP (present on every AVX-512 CPU).
 */
        .intel_syntax noprefix
        .text

        .p2align 5
        .globl  hsn_asm512_cull_f64
        .type   hsn_asm512_cull_f64, @function
hsn_asm512_cull_f64:
        test    rdx, rdx
        jz      .L512_ret
        test    rdi, rdi
        jz      .L512_ret
        test    rsi, rsi
        jz      .L512_ret
        vbroadcastsd zmm1, xmm0
        xor     rcx, rcx
        mov     r8, rdx
        and     r8, -8
        jz      .L512_tail
        mov     r9, 0x0101010101010101
.L512_loop:
        vmovupd zmm0, [rdi + rcx*8]
        vcmppd  k1, zmm0, zmm1, 14
        kmovb   eax, k1
        pdep    r10, rax, r9
        mov     qword ptr [rsi + rcx], r10
        add     rcx, 8
        cmp     rcx, r8
        jb      .L512_loop
        vzeroupper
.L512_tail:
        cmp     rcx, rdx
        jae     .L512_ret
.L512_tail_loop:
        vmovsd  xmm2, qword ptr [rdi + rcx*8]
        vucomisd xmm2, xmm0
        seta    al
        mov     byte ptr [rsi + rcx], al
        inc     rcx
        cmp     rcx, rdx
        jb      .L512_tail_loop
.L512_ret:
        vzeroupper
        ret
        .size   hsn_asm512_cull_f64, .-hsn_asm512_cull_f64

        .p2align 5
        .globl  hsn_asm512_cull_xyz_f32
        .type   hsn_asm512_cull_xyz_f32, @function
hsn_asm512_cull_xyz_f32:
        test    r8, r8
        jz      .Lxyz_ret
        test    rdi, rdi
        jz      .Lxyz_ret
        test    rsi, rsi
        jz      .Lxyz_ret
        test    rdx, rdx
        jz      .Lxyz_ret
        test    rcx, rcx
        jz      .Lxyz_ret
        vbroadcastss zmm4, xmm0
        vbroadcastss zmm5, xmm1
        vbroadcastss zmm6, xmm2
        vbroadcastss zmm7, xmm3
        xor     r9, r9
        mov     r10, r8
        and     r10, -16
        jz      .Lxyz_tail
        mov     r11, 0x0101010101010101
.Lxyz_loop:
        vmovups zmm16, [rdi + r9*4]
        vmovups zmm17, [rsi + r9*4]
        vmovups zmm18, [rdx + r9*4]
        vsubps  zmm16, zmm16, zmm4
        vsubps  zmm17, zmm17, zmm5
        vsubps  zmm18, zmm18, zmm6
        vmulps  zmm16, zmm16, zmm16
        vfmadd231ps zmm16, zmm17, zmm17
        vfmadd231ps zmm16, zmm18, zmm18
        vcmpps  k1, zmm16, zmm7, 14
        kmovw   eax, k1
        pdep    rax, rax, r11
        mov     qword ptr [rcx + r9], rax
        kmovw   eax, k1
        shr     eax, 8
        pdep    rax, rax, r11
        mov     qword ptr [rcx + r9 + 8], rax
        add     r9, 16
        cmp     r9, r10
        jb      .Lxyz_loop
        vzeroupper
.Lxyz_tail:
        cmp     r9, r8
        jae     .Lxyz_ret
.Lxyz_scalar:
        vmovss  xmm8, dword ptr [rdi + r9*4]
        vsubss  xmm8, xmm8, xmm4
        vmulss  xmm8, xmm8, xmm8
        vmovss  xmm9, dword ptr [rsi + r9*4]
        vsubss  xmm9, xmm9, xmm5
        vfmadd231ss xmm8, xmm9, xmm9
        vmovss  xmm10, dword ptr [rdx + r9*4]
        vsubss  xmm10, xmm10, xmm6
        vfmadd231ss xmm8, xmm10, xmm10
        vucomiss xmm8, xmm7
        seta    al
        mov     byte ptr [rcx + r9], al
        inc     r9
        cmp     r9, r8
        jb      .Lxyz_scalar
.Lxyz_ret:
        vzeroupper
        ret
        .size   hsn_asm512_cull_xyz_f32, .-hsn_asm512_cull_xyz_f32

        .p2align 5
        .globl  hsn_asm512_dot4_f32
        .type   hsn_asm512_dot4_f32, @function
hsn_asm512_dot4_f32:
        test    rcx, rcx
        jz      .Ldot_ret
        test    rdi, rdi
        jz      .Ldot_ret
        test    rsi, rsi
        jz      .Ldot_ret
        test    rdx, rdx
        jz      .Ldot_ret
        vmovss  xmm0, dword ptr [rdi]
        vmovss  xmm1, dword ptr [rdi + 4]
        vmovss  xmm2, dword ptr [rdi + 8]
        vxorps  xmm8, xmm8, xmm8
        xor     r8, r8
        mov     r9, rcx
        and     r9, -4
        jz      .Ldot_tail
.Ldot_loop:
        lea     r10, [rsi + r8*8]
        lea     r10, [r10 + r8*8]
        vmovss  xmm4, dword ptr [r10]
        vmulss  xmm4, xmm4, xmm0
        vfmadd231ss xmm4, xmm1, dword ptr [r10 + 4]
        vfmadd231ss xmm4, xmm2, dword ptr [r10 + 8]
        vaddss  xmm4, xmm4, dword ptr [r10 + 12]
        vucomiss xmm4, xmm8
        setb    al
        mov     byte ptr [rdx + r8], al
        vmovss  xmm4, dword ptr [r10 + 16]
        vmulss  xmm4, xmm4, xmm0
        vfmadd231ss xmm4, xmm1, dword ptr [r10 + 20]
        vfmadd231ss xmm4, xmm2, dword ptr [r10 + 24]
        vaddss  xmm4, xmm4, dword ptr [r10 + 28]
        vucomiss xmm4, xmm8
        setb    al
        mov     byte ptr [rdx + r8 + 1], al
        vmovss  xmm4, dword ptr [r10 + 32]
        vmulss  xmm4, xmm4, xmm0
        vfmadd231ss xmm4, xmm1, dword ptr [r10 + 36]
        vfmadd231ss xmm4, xmm2, dword ptr [r10 + 40]
        vaddss  xmm4, xmm4, dword ptr [r10 + 44]
        vucomiss xmm4, xmm8
        setb    al
        mov     byte ptr [rdx + r8 + 2], al
        vmovss  xmm4, dword ptr [r10 + 48]
        vmulss  xmm4, xmm4, xmm0
        vfmadd231ss xmm4, xmm1, dword ptr [r10 + 52]
        vfmadd231ss xmm4, xmm2, dword ptr [r10 + 56]
        vaddss  xmm4, xmm4, dword ptr [r10 + 60]
        vucomiss xmm4, xmm8
        setb    al
        mov     byte ptr [rdx + r8 + 3], al
        add     r8, 4
        cmp     r8, r9
        jb      .Ldot_loop
.Ldot_tail:
        cmp     r8, rcx
        jae     .Ldot_ret
.Ldot_tail_loop:
        lea     r10, [rsi + r8*8]
        lea     r10, [r10 + r8*8]
        vmovss  xmm4, dword ptr [r10]
        vmulss  xmm4, xmm4, xmm0
        vfmadd231ss xmm4, xmm1, dword ptr [r10 + 4]
        vfmadd231ss xmm4, xmm2, dword ptr [r10 + 8]
        vaddss  xmm4, xmm4, dword ptr [r10 + 12]
        vucomiss xmm4, xmm8
        setb    al
        mov     byte ptr [rdx + r8], al
        inc     r8
        cmp     r8, rcx
        jb      .Ldot_tail_loop
.Ldot_ret:
        vzeroupper
        ret
        .size   hsn_asm512_dot4_f32, .-hsn_asm512_dot4_f32

        .section .note.GNU-stack,"",@progbits
