/* System V AMD64 — extra SoA AVX2 kernels. */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_scale_f32
        .type   hsn_asm_scale_f32, @function
hsn_asm_scale_f32:
        test    rdx, rdx
        jz      .Lsc_ret
        vbroadcastss ymm1, xmm0
        xor     rcx, rcx
        mov     r8, rdx
        and     r8, -8
        jz      .Lsc_tail
.Lsc_loop:
        vmulps  ymm0, ymm1, [rdi + rcx*4]
        vmovups [rsi + rcx*4], ymm0
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lsc_loop
        vzeroupper
.Lsc_tail:
        cmp     rcx, rdx
        jae     .Lsc_ret
        movss   xmm0, dword ptr [rdi + rcx*4]
        mulss   xmm0, xmm1
        movss   dword ptr [rsi + rcx*4], xmm0
        add     rcx, 1
        jmp     .Lsc_tail
.Lsc_ret:
        ret
        .size   hsn_asm_scale_f32, .-hsn_asm_scale_f32

        .p2align 4
        .globl  hsn_asm_add_f32
        .type   hsn_asm_add_f32, @function
hsn_asm_add_f32:
        test    rcx, rcx
        jz      .Lad_ret
        xor     r8, r8
        mov     r9, rcx
        and     r9, -8
        jz      .Lad_tail
.Lad_loop:
        vmovups ymm0, [rdi + r8*4]
        vaddps  ymm0, ymm0, [rsi + r8*4]
        vmovups [rdx + r8*4], ymm0
        add     r8, 8
        cmp     r8, r9
        jb      .Lad_loop
        vzeroupper
.Lad_tail:
        cmp     r8, rcx
        jae     .Lad_ret
        movss   xmm0, dword ptr [rdi + r8*4]
        addss   xmm0, dword ptr [rsi + r8*4]
        movss   dword ptr [rdx + r8*4], xmm0
        add     r8, 1
        jmp     .Lad_tail
.Lad_ret:
        ret
        .size   hsn_asm_add_f32, .-hsn_asm_add_f32

        .p2align 4
        .globl  hsn_asm_clamp_f32
        .type   hsn_asm_clamp_f32, @function
hsn_asm_clamp_f32:
        test    rdx, rdx
        jz      .Lcl_ret
        vbroadcastss ymm2, xmm0
        vbroadcastss ymm3, xmm1
        xor     rcx, rcx
        mov     r8, rdx
        and     r8, -8
        jz      .Lcl_tail
.Lcl_loop:
        vmovups ymm0, [rdi + rcx*4]
        vmaxps  ymm0, ymm0, ymm2
        vminps  ymm0, ymm0, ymm3
        vmovups [rsi + rcx*4], ymm0
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lcl_loop
        vzeroupper
.Lcl_tail:
        cmp     rcx, rdx
        jae     .Lcl_ret
        movss   xmm0, dword ptr [rdi + rcx*4]
        maxss   xmm0, xmm2
        minss   xmm0, xmm3
        movss   dword ptr [rsi + rcx*4], xmm0
        add     rcx, 1
        jmp     .Lcl_tail
.Lcl_ret:
        ret
        .size   hsn_asm_clamp_f32, .-hsn_asm_clamp_f32

        .p2align 4
        .globl  hsn_asm_copy_mask_f32
        .type   hsn_asm_copy_mask_f32, @function
hsn_asm_copy_mask_f32:
        test    rcx, rcx
        jz      .Lcm_ret
        xor     r8, r8
.Lcm_loop:
        cmp     byte ptr [rsi + r8], 0
        je      .Lcm_zero
        movss   xmm0, dword ptr [rdi + r8*4]
        movss   dword ptr [rdx + r8*4], xmm0
        jmp     .Lcm_next
.Lcm_zero:
        mov     dword ptr [rdx + r8*4], 0
.Lcm_next:
        add     r8, 1
        cmp     r8, rcx
        jb      .Lcm_loop
.Lcm_ret:
        ret
        .size   hsn_asm_copy_mask_f32, .-hsn_asm_copy_mask_f32

        .p2align 4
        .globl  hsn_asm_dot3_f32_soa
        .type   hsn_asm_dot3_f32_soa, @function
hsn_asm_dot3_f32_soa:
        mov     r10, qword ptr [rsp + 8]
        mov     r11, qword ptr [rsp + 16]
        test    r11, r11
        jz      .Ldt_ret
        test    r10, r10
        jz      .Ldt_ret
        xor     rax, rax
.Ldt_loop:
        lea     rax, [rax + 8]
        cmp     rax, r11
        ja      .Ldt_rewind
        sub     rax, 8
        vmovups ymm0, [rdi + rax*4]
        vmulps  ymm0, ymm0, [rcx + rax*4]
        vmovups ymm1, [rsi + rax*4]
        vmulps  ymm1, ymm1, [r8 + rax*4]
        vaddps  ymm0, ymm0, ymm1
        vmovups ymm1, [rdx + rax*4]
        vmulps  ymm1, ymm1, [r9 + rax*4]
        vaddps  ymm0, ymm0, ymm1
        vmovups [r10 + rax*4], ymm0
        add     rax, 8
        jmp     .Ldt_loop
.Ldt_rewind:
        sub     rax, 8
        vzeroupper
.Ldt_tail:
        cmp     rax, r11
        jae     .Ldt_ret
        movss   xmm0, dword ptr [rdi + rax*4]
        mulss   xmm0, dword ptr [rcx + rax*4]
        movss   xmm1, dword ptr [rsi + rax*4]
        mulss   xmm1, dword ptr [r8 + rax*4]
        addss   xmm0, xmm1
        movss   xmm1, dword ptr [rdx + rax*4]
        mulss   xmm1, dword ptr [r9 + rax*4]
        addss   xmm0, xmm1
        movss   dword ptr [r10 + rax*4], xmm0
        add     rax, 1
        jmp     .Ldt_tail
.Ldt_ret:
        ret
        .size   hsn_asm_dot3_f32_soa, .-hsn_asm_dot3_f32_soa

        .p2align 4
        .globl  hsn_asm_sum_f32
        .type   hsn_asm_sum_f32, @function
hsn_asm_sum_f32:
        vxorps  ymm0, ymm0, ymm0
        test    rsi, rsi
        jz      .Lsm_ret
        xor     rcx, rcx
        mov     rdx, rsi
        and     rdx, -8
        jz      .Lsm_tail
.Lsm_loop:
        vaddps  ymm0, ymm0, [rdi + rcx*4]
        add     rcx, 8
        cmp     rcx, rdx
        jb      .Lsm_loop
        vextractf128 xmm1, ymm0, 1
        vaddps  xmm0, xmm0, xmm1
        vhaddps xmm0, xmm0, xmm0
        vhaddps xmm0, xmm0, xmm0
        vzeroupper
.Lsm_tail:
        cmp     rcx, rsi
        jae     .Lsm_ret
        addss   xmm0, dword ptr [rdi + rcx*4]
        add     rcx, 1
        jmp     .Lsm_tail
.Lsm_ret:
        ret
        .size   hsn_asm_sum_f32, .-hsn_asm_sum_f32

        .section .note.GNU-stack,"",@progbits
