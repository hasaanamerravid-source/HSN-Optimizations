/* System V AMD64 ABI
 * void hsn_simd_rsqrt_f32(const float *in, float *out, uint64_t n)
 *   rdi = in, rsi = out, rdx = n
 *
 * Inverse square root of packed f32 using AVX2 vsqrtps + vdivps.
 * 32-byte aligned load/store when both buffers are 32-byte aligned;
 * unaligned vmovups otherwise. Values <= 1e-12 become 0.
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
        vbroadcastss ymm4, dword ptr [rip + .Lrs_one]
        vbroadcastss ymm6, dword ptr [rip + .Lrs_eps]

        mov     r8, rdx
        and     r8, -8
        xor     rcx, rcx
        test    r8, r8
        jz      .Lrs_tail

        mov     eax, edi
        or      eax, esi
        and     eax, 31
        jnz     .Lrs_u

.Lrs_a:
        vmovaps ymm0, [rdi + rcx*4]
        vcmpps  ymm2, ymm0, ymm6, 14            /* GT_OQ vs eps */
        vmaxps  ymm1, ymm0, ymm6
        vsqrtps ymm1, ymm1
        vdivps  ymm1, ymm4, ymm1
        vblendvps ymm1, ymm5, ymm1, ymm2
        vmovaps [rsi + rcx*4], ymm1
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lrs_a
        jmp     .Lrs_tail_prep

.Lrs_u:
        vmovups ymm0, [rdi + rcx*4]
        vcmpps  ymm2, ymm0, ymm6, 14
        vmaxps  ymm1, ymm0, ymm6
        vsqrtps ymm1, ymm1
        vdivps  ymm1, ymm4, ymm1
        vblendvps ymm1, ymm5, ymm1, ymm2
        vmovups [rsi + rcx*4], ymm1
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lrs_u

.Lrs_tail_prep:
        vzeroupper
.Lrs_tail:
        cmp     rcx, rdx
        jae     .Lrs_ret
        movss   xmm4, dword ptr [rip + .Lrs_one]
        movss   xmm6, dword ptr [rip + .Lrs_eps]
.Lrs_s:
        movss   xmm0, dword ptr [rdi + rcx*4]
        ucomiss xmm0, xmm6
        jbe     .Lrs_z
        sqrtss  xmm1, xmm0
        divss   xmm4, xmm1
        movss   dword ptr [rsi + rcx*4], xmm4
        movss   xmm4, dword ptr [rip + .Lrs_one]
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
        mov     eax, 0x53494D44                 /* 'SIMD' */
        ret
        .size   hsn_simd_abi, .-hsn_simd_abi

        .section .rodata
        .p2align 2
.Lrs_one:
        .float  1.0
.Lrs_eps:
        .float  1.0e-12

        .section .note.GNU-stack,"",@progbits
