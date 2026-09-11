/* System V AMD64 ABI
 * void hsn_simd_rsqrt_f32(const float *in, float *out, uint64_t n)
 * AVX2 rsqrt + one Newton step. Values <= 1e-12 become 0.
 */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_simd_rsqrt_f32
        .type   hsn_simd_rsqrt_f32, @function
hsn_simd_rsqrt_f32:
        test    rdx, rdx
        jz      .Lrs_ret
        test    rdi, rdi
        jz      .Lrs_ret
        test    rsi, rsi
        jz      .Lrs_ret
        vxorps  ymm5, ymm5, ymm5
        vbroadcastss ymm6, dword ptr [rip + .Lrs_eps]
        vmovaps ymm7, ymmword ptr [rip + .Lrs_half]
        vmovaps ymm8, ymmword ptr [rip + .Lrs_three_halves]
        mov     r8, rdx
        and     r8, -8
        xor     rcx, rcx
        test    r8, r8
        jz      .Lrs_tail
.Lrs_u:
        vmovups ymm0, [rdi + rcx*4]
        vcmpps  ymm2, ymm0, ymm6, 14
        vmaxps  ymm1, ymm0, ymm6
        vrsqrtps ymm3, ymm1
        vmulps  ymm4, ymm3, ymm3
        vmulps  ymm4, ymm4, ymm1
        vmulps  ymm4, ymm4, ymm7
        vsubps  ymm4, ymm8, ymm4
        vmulps  ymm3, ymm3, ymm4
        vblendvps ymm3, ymm5, ymm3, ymm2
        vmovups [rsi + rcx*4], ymm3
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lrs_u
        vzeroupper
.Lrs_tail:
        cmp     rcx, rdx
        jae     .Lrs_ret
        movss   xmm6, dword ptr [rip + .Lrs_eps]
        movss   xmm7, dword ptr [rip + .Lrs_half]
        movss   xmm8, dword ptr [rip + .Lrs_three_halves]
.Lrs_s:
        movss   xmm0, dword ptr [rdi + rcx*4]
        ucomiss xmm0, xmm6
        jbe     .Lrs_z
        rsqrtss xmm1, xmm0
        movss   xmm3, xmm1
        mulss   xmm3, xmm1
        mulss   xmm3, xmm0
        mulss   xmm3, xmm7
        movss   xmm4, xmm8
        subss   xmm4, xmm3
        mulss   xmm1, xmm4
        movss   dword ptr [rsi + rcx*4], xmm1
        jmp     .Lrs_n
.Lrs_z:
        mov     dword ptr [rsi + rcx*4], 0
.Lrs_n:
        inc     rcx
        cmp     rcx, rdx
        jb      .Lrs_s
.Lrs_ret:
        vzeroupper
        ret
        .size   hsn_simd_rsqrt_f32, .-hsn_simd_rsqrt_f32

        .p2align 4
        .globl  hsn_simd_abi
        .type   hsn_simd_abi, @function
hsn_simd_abi:
        mov     eax, 0x53494D44
        ret
        .size   hsn_simd_abi, .-hsn_simd_abi

        .section .rodata
        .p2align 5
.Lrs_half:
        .rept 8
        .float  0.5
        .endr
.Lrs_three_halves:
        .rept 8
        .float  1.5
        .endr
.Lrs_eps:
        .float  1.0e-12
        .section .note.GNU-stack,"",@progbits
