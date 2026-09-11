/* System V AMD64 — page touch / prefetch without reading past len. */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_touch_pages
        .type   hsn_asm_touch_pages, @function
hsn_asm_touch_pages:
        test    rdi, rdi
        jz      .Ldone
        test    rsi, rsi
        jz      .Ldone
        lea     rsi, [rdi + rsi]
        xor     eax, eax
.Lloop:
        lea     rdx, [rdi + 64]
        cmp     rdx, rsi
        ja      .Llast
        prefetcht0 byte ptr [rdi]
        prefetcht0 byte ptr [rdi + 32]
        mov     rdi, rdx
        add     eax, 1
        cmp     eax, 262144
        jb      .Lloop
        jmp     .Ldone
.Llast:
        cmp     rdi, rsi
        jae     .Ldone
        prefetcht0 byte ptr [rdi]
.Ldone:
        ret
        .size   hsn_asm_touch_pages, .-hsn_asm_touch_pages

        .p2align 4
        .globl  hsn_asm_sum_u64
        .type   hsn_asm_sum_u64, @function
hsn_asm_sum_u64:
        xor     eax, eax
        test    rdi, rdi
        jz      .Lsumdone
        test    rsi, rsi
        jz      .Lsumdone
        xor     ecx, ecx
        mov     rdx, rsi
        and     rdx, -4
        jz      .Lsumtail
.Lsumvec:
        add     rax, [rdi + rcx*8]
        add     rax, [rdi + rcx*8 + 8]
        add     rax, [rdi + rcx*8 + 16]
        add     rax, [rdi + rcx*8 + 24]
        add     rcx, 4
        cmp     rcx, rdx
        jb      .Lsumvec
.Lsumtail:
        cmp     rcx, rsi
        jae     .Lsumdone
        add     rax, [rdi + rcx*8]
        add     rcx, 1
        jmp     .Lsumtail
.Lsumdone:
        ret
        .size   hsn_asm_sum_u64, .-hsn_asm_sum_u64

        .section .note.GNU-stack,"",@progbits
