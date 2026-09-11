/* System V AMD64 — extra AVX2 batch kernels.
 * rsqrt lives only in hsn_rsqrt.S so this file no longer exports a duplicate.
 */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_dist_sq_f32
        .type   hsn_asm_dist_sq_f32, @function
hsn_asm_dist_sq_f32:
        test    r8, r8
        jz      .Lds_ret
        sub     rsp, 16
        movss   dword ptr [rsp], xmm0
        movss   dword ptr [rsp + 4], xmm1
        movss   dword ptr [rsp + 8], xmm2
        vbroadcastss ymm3, xmm0
        vbroadcastss ymm4, xmm1
        vbroadcastss ymm5, xmm2
        xor     r9, r9
        mov     r10, r8
        and     r10, -8
        jz      .Lds_tail
.Lds_loop:
        vmovups ymm0, [rdi + r9*4]
        vmovups ymm1, [rsi + r9*4]
        vmovups ymm2, [rdx + r9*4]
        vsubps  ymm0, ymm0, ymm3
        vsubps  ymm1, ymm1, ymm4
        vsubps  ymm2, ymm2, ymm5
        vmulps  ymm0, ymm0, ymm0
        vmulps  ymm1, ymm1, ymm1
        vmulps  ymm2, ymm2, ymm2
        vaddps  ymm0, ymm0, ymm1
        vaddps  ymm0, ymm0, ymm2
        vmovups [rcx + r9*4], ymm0
        add     r9, 8
        cmp     r9, r10
        jb      .Lds_loop
        vzeroupper
.Lds_tail:
        cmp     r9, r8
        jae     .Lds_done
        movss   xmm3, dword ptr [rsp]
        movss   xmm4, dword ptr [rsp + 4]
        movss   xmm5, dword ptr [rsp + 8]
.Lds_scalar:
        movss   xmm0, dword ptr [rdi + r9*4]
        movss   xmm1, dword ptr [rsi + r9*4]
        movss   xmm2, dword ptr [rdx + r9*4]
        subss   xmm0, xmm3
        subss   xmm1, xmm4
        subss   xmm2, xmm5
        mulss   xmm0, xmm0
        mulss   xmm1, xmm1
        mulss   xmm2, xmm2
        addss   xmm0, xmm1
        addss   xmm0, xmm2
        movss   dword ptr [rcx + r9*4], xmm0
        add     r9, 1
        cmp     r9, r8
        jb      .Lds_scalar
.Lds_done:
        add     rsp, 16
.Lds_ret:
        ret
        .size   hsn_asm_dist_sq_f32, .-hsn_asm_dist_sq_f32

        .p2align 4
        .globl  hsn_asm_cull_f32
        .type   hsn_asm_cull_f32, @function
hsn_asm_cull_f32:
        test    rdx, rdx
        jz      .Lcf_ret
        vbroadcastss ymm3, xmm0
        xor     rcx, rcx
        mov     r8, rdx
        and     r8, -8
        jz      .Lcf_tail
        mov     r11, 0x0101010101010101
.Lcf_loop:
        vmovups ymm0, [rdi + rcx*4]
        vcmpps  ymm1, ymm0, ymm3, 14
        vmovmskps eax, ymm1
        pdep    rax, rax, r11
        mov     qword ptr [rsi + rcx], rax
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lcf_loop
        vzeroupper
.Lcf_tail:
        cmp     rcx, rdx
        jae     .Lcf_ret
        movss   xmm0, dword ptr [rdi + rcx*4]
        ucomiss xmm0, xmm3
        seta    al
        mov     byte ptr [rsi + rcx], al
        add     rcx, 1
        jmp     .Lcf_tail
.Lcf_ret:
        ret
        .size   hsn_asm_cull_f32, .-hsn_asm_cull_f32

        .p2align 4
        .globl  hsn_asm_keep_u8
        .type   hsn_asm_keep_u8, @function
hsn_asm_keep_u8:
        test    rcx, rcx
        jz      .Lkp_ret
        vmovd   xmm0, esi
        vpbroadcastb ymm0, xmm0
        xor     r8, r8
        mov     r9, rcx
        and     r9, -32
        jz      .Lkp_tail
.Lkp_loop:
        vmovdqu ymm1, [rdi + r8]
        vpcmpgtb ymm1, ymm0, ymm1
        vpand   ymm1, ymm1, ymmword ptr [rip + .Lonebytes]
        vmovdqu [rdx + r8], ymm1
        add     r8, 32
        cmp     r8, r9
        jb      .Lkp_loop
        vzeroupper
.Lkp_tail:
        cmp     r8, rcx
        jae     .Lkp_ret
        movzx   eax, sil
.Lkp_byte:
        movzx   r9d, byte ptr [rdi + r8]
        cmp     r9d, eax
        setb    r9b
        mov     byte ptr [rdx + r8], r9b
        add     r8, 1
        cmp     r8, rcx
        jb      .Lkp_byte
.Lkp_ret:
        vzeroupper
        ret
        .size   hsn_asm_keep_u8, .-hsn_asm_keep_u8

        .p2align 4
        .globl  hsn_asm_mad_f32
        .type   hsn_asm_mad_f32, @function
hsn_asm_mad_f32:
        test    rcx, rcx
        jz      .Lmd_ret
        vbroadcastss ymm2, xmm0
        xor     r8, r8
        mov     r9, rcx
        and     r9, -8
        jz      .Lmd_tail
.Lmd_loop:
        vmovups ymm0, [rdi + r8*4]
        vmovups ymm1, [rsi + r8*4]
        vmulps  ymm1, ymm1, ymm2
        vaddps  ymm0, ymm0, ymm1
        vmovups [rdx + r8*4], ymm0
        add     r8, 8
        cmp     r8, r9
        jb      .Lmd_loop
        vzeroupper
.Lmd_tail:
        cmp     r8, rcx
        jae     .Lmd_ret
        movss   xmm0, dword ptr [rdi + r8*4]
        movss   xmm1, dword ptr [rsi + r8*4]
        mulss   xmm1, xmm2
        addss   xmm0, xmm1
        movss   dword ptr [rdx + r8*4], xmm0
        add     r8, 1
        jmp     .Lmd_tail
.Lmd_ret:
        ret
        .size   hsn_asm_mad_f32, .-hsn_asm_mad_f32

        .p2align 4
        .globl  hsn_asm_minmax_f32
        .type   hsn_asm_minmax_f32, @function
hsn_asm_minmax_f32:
        test    rsi, rsi
        jz      .Lmm_empty
        vbroadcastss ymm0, dword ptr [rdi]
        vmovaps ymm1, ymm0
        xor     rcx, rcx
        mov     r8, rsi
        and     r8, -8
        jz      .Lmm_tail
.Lmm_loop:
        vmovups ymm2, [rdi + rcx*4]
        vminps  ymm0, ymm0, ymm2
        vmaxps  ymm1, ymm1, ymm2
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lmm_loop
.Lmm_tail:
        cmp     rcx, rsi
        jae     .Lmm_hsum
        vmovss  xmm2, dword ptr [rdi + rcx*4]
        vminss  xmm0, xmm0, xmm2
        vmaxss  xmm1, xmm1, xmm2
        add     rcx, 1
        jmp     .Lmm_tail
.Lmm_hsum:
        vextractf128 xmm2, ymm0, 1
        vminps  xmm0, xmm0, xmm2
        vshufps xmm2, xmm0, xmm0, 0x4E
        vminps  xmm0, xmm0, xmm2
        vshufps xmm2, xmm0, xmm0, 0xB1
        vminss  xmm0, xmm0, xmm2
        vextractf128 xmm2, ymm1, 1
        vmaxps  xmm1, xmm1, xmm2
        vshufps xmm2, xmm1, xmm1, 0x4E
        vmaxps  xmm1, xmm1, xmm2
        vshufps xmm2, xmm1, xmm1, 0xB1
        vmaxss  xmm1, xmm1, xmm2
        vzeroupper
        movss   dword ptr [rdx], xmm0
        movss   dword ptr [rdx + 4], xmm1
        ret
.Lmm_empty:
        pxor    xmm0, xmm0
        movss   dword ptr [rdx], xmm0
        movss   dword ptr [rdx + 4], xmm0
        ret
        .size   hsn_asm_minmax_f32, .-hsn_asm_minmax_f32

        .p2align 4
        .globl  hsn_asm_lod_band_f32
        .type   hsn_asm_lod_band_f32, @function
hsn_asm_lod_band_f32:
        test    rdx, rdx
        jz      .Llb_ret
        xor     rcx, rcx
.Llb_loop:
        movss   xmm2, dword ptr [rdi + rcx*4]
        xor     eax, eax
        ucomiss xmm2, xmm0
        jb      .Llb_store
        mov     eax, 1
        ucomiss xmm2, xmm1
        jb      .Llb_store
        mov     eax, 2
.Llb_store:
        mov     byte ptr [rsi + rcx], al
        add     rcx, 1
        cmp     rcx, rdx
        jb      .Llb_loop
.Llb_ret:
        ret
        .size   hsn_asm_lod_band_f32, .-hsn_asm_lod_band_f32

        .p2align 4
        .globl  hsn_asm_and_mask_u8
        .type   hsn_asm_and_mask_u8, @function
hsn_asm_and_mask_u8:
        test    rcx, rcx
        jz      .Lan_ret
        xor     r8, r8
        mov     r9, rcx
        and     r9, -32
        jz      .Lan_tail
.Lan_loop:
        vmovdqu ymm0, [rdi + r8]
        vpand   ymm0, ymm0, [rsi + r8]
        vmovdqu [rdx + r8], ymm0
        add     r8, 32
        cmp     r8, r9
        jb      .Lan_loop
        vzeroupper
.Lan_tail:
        cmp     r8, rcx
        jae     .Lan_ret
        movzx   eax, byte ptr [rdi + r8]
        and     al, byte ptr [rsi + r8]
        mov     byte ptr [rdx + r8], al
        add     r8, 1
        jmp     .Lan_tail
.Lan_ret:
        vzeroupper
        ret
        .size   hsn_asm_and_mask_u8, .-hsn_asm_and_mask_u8

        .p2align 4
        .globl  hsn_asm_popcount_u8
        .type   hsn_asm_popcount_u8, @function
hsn_asm_popcount_u8:
        xor     eax, eax
        test    rsi, rsi
        jz      .Lpc_ret
        xor     rcx, rcx
        mov     r8, rsi
        and     r8, -32
        jz      .Lpc_tail
        vpxor   ymm1, ymm1, ymm1
.Lpc_loop:
        vmovdqu ymm0, [rdi + rcx]
        vpcmpeqb ymm0, ymm0, ymm1
        vpmovmskb r9d, ymm0
        not     r9d
        popcnt  r9d, r9d
        add     eax, r9d
        add     rcx, 32
        cmp     rcx, r8
        jb      .Lpc_loop
        vzeroupper
.Lpc_tail:
        cmp     rcx, rsi
        jae     .Lpc_ret
        cmp     byte ptr [rdi + rcx], 0
        setne   dl
        movzx   edx, dl
        add     eax, edx
        add     rcx, 1
        jmp     .Lpc_tail
.Lpc_ret:
        ret
        .size   hsn_asm_popcount_u8, .-hsn_asm_popcount_u8

        .section .rodata
        .p2align 5
.Lonebytes:
        .rept 32
        .byte 1
        .endr
        .section .note.GNU-stack,"",@progbits
