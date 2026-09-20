/* System V AMD64 — 8-wide AVX2 distance mask + byte masks. */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_cull_f64_8
        .type   hsn_asm_cull_f64_8, @function
hsn_asm_cull_f64_8:
        test    rdx, rdx
        jz      .L8ret
        xor     rcx, rcx
        vbroadcastsd ymm3, xmm0
        mov     r8, rdx
        and     r8, -8
        jz      .L8tail
        mov     r10, 0x01010101
.L8loop:
        vmovupd ymm0, [rdi + rcx*8]
        vcmppd  ymm1, ymm0, ymm3, 14
        vmovmskpd eax, ymm1
        pdep    eax, eax, r10d
        mov     dword ptr [rsi + rcx], eax
        vmovupd ymm0, [rdi + rcx*8 + 32]
        vcmppd  ymm1, ymm0, ymm3, 14
        vmovmskpd eax, ymm1
        pdep    eax, eax, r10d
        mov     dword ptr [rsi + rcx + 4], eax
        add     rcx, 8
        cmp     rcx, r8
        jb      .L8loop
        vzeroupper
.L8tail:
        cmp     rcx, rdx
        jae     .L8ret
.L8scalar:
        vmovsd  xmm0, qword ptr [rdi + rcx*8]
        vucomisd xmm0, xmm3
        seta    al
        mov     byte ptr [rsi + rcx], al
        inc     rcx
        cmp     rcx, rdx
        jb      .L8scalar
.L8ret:
        vzeroupper
        ret
        .size   hsn_asm_cull_f64_8, .-hsn_asm_cull_f64_8

        .p2align 4
        .globl  hsn_asm_or_mask
        .type   hsn_asm_or_mask, @function
hsn_asm_or_mask:
        test    rcx, rcx
        jz      .Lor_ret
        xor     r8, r8
        mov     r9, rcx
        and     r9, -32
        jz      .Lor_tail
.Lor_loop:
        vmovdqu ymm0, [rdi + r8]
        vpor    ymm0, ymm0, [rsi + r8]
        vmovdqu [rdx + r8], ymm0
        add     r8, 32
        cmp     r8, r9
        jb      .Lor_loop
        vzeroupper
.Lor_tail:
        cmp     r8, rcx
        jae     .Lor_ret
.Lor_byte:
        mov     al, byte ptr [rdi + r8]
        or      al, byte ptr [rsi + r8]
        mov     byte ptr [rdx + r8], al
        inc     r8
        cmp     r8, rcx
        jb      .Lor_byte
.Lor_ret:
        vzeroupper
        ret
        .size   hsn_asm_or_mask, .-hsn_asm_or_mask

        .p2align 4
        .globl  hsn_asm_and_mask
        .type   hsn_asm_and_mask, @function
hsn_asm_and_mask:
        test    rcx, rcx
        jz      .Land_ret
        xor     r8, r8
        mov     r9, rcx
        and     r9, -32
        jz      .Land_tail
.Land_loop:
        vmovdqu ymm0, [rdi + r8]
        vpand   ymm0, ymm0, [rsi + r8]
        vmovdqu [rdx + r8], ymm0
        add     r8, 32
        cmp     r8, r9
        jb      .Land_loop
        vzeroupper
.Land_tail:
        cmp     r8, rcx
        jae     .Land_ret
.Land_byte:
        mov     al, byte ptr [rdi + r8]
        and     al, byte ptr [rsi + r8]
        mov     byte ptr [rdx + r8], al
        inc     r8
        cmp     r8, rcx
        jb      .Land_byte
.Land_ret:
        vzeroupper
        ret
        .size   hsn_asm_and_mask, .-hsn_asm_and_mask

        .p2align 4
        .globl  hsn_asm_count_keep
        .type   hsn_asm_count_keep, @function
hsn_asm_count_keep:
        xor     eax, eax
        test    rsi, rsi
        jz      .Lck_ret
        xor     rcx, rcx
        mov     r8, rsi
        and     r8, -32
        jz      .Lck_tail
        vpxor   ymm1, ymm1, ymm1
.Lck_loop:
        vmovdqu ymm0, [rdi + rcx]
        vpsadbw ymm0, ymm0, ymm1
        vextracti128 xmm2, ymm0, 1
        vpaddw  xmm0, xmm0, xmm2
        pshufd  xmm2, xmm0, 0x4E
        paddw   xmm0, xmm2
        vmovd   r9d, xmm0
        movzx   r9d, r9w
        add     rax, r9
        add     rcx, 32
        cmp     rcx, r8
        jb      .Lck_loop
        vzeroupper
.Lck_tail:
        cmp     rcx, rsi
        jae     .Lck_ret
        movzx   edx, byte ptr [rdi + rcx]
        add     rax, rdx
        inc     rcx
        jmp     .Lck_tail
.Lck_ret:
        ret
        .size   hsn_asm_count_keep, .-hsn_asm_count_keep

        .p2align 4
        .globl  hsn_asm_fill_u8
        .type   hsn_asm_fill_u8, @function
hsn_asm_fill_u8:
        test    rdx, rdx
        jz      .Lfill_ret
        xor     rcx, rcx
        vmovd   xmm0, esi
        vpbroadcastb ymm0, xmm0
        mov     r8, rdx
        and     r8, -32
        jz      .Lfill_tail
.Lfill_loop:
        vmovdqu [rdi + rcx], ymm0
        add     rcx, 32
        cmp     rcx, r8
        jb      .Lfill_loop
        vzeroupper
.Lfill_tail:
        cmp     rcx, rdx
        jae     .Lfill_ret
.Lfill_byte:
        mov     byte ptr [rdi + rcx], sil
        inc     rcx
        cmp     rcx, rdx
        jb      .Lfill_byte
.Lfill_ret:
        vzeroupper
        ret
        .size   hsn_asm_fill_u8, .-hsn_asm_fill_u8

        .section .note.GNU-stack,"",@progbits
