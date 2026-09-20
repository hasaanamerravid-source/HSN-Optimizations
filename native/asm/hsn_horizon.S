/* Packed Horizon Y for libhsn_asm.so
 * void hsn_asm_horizon_y_f32(const float *max_y, float floor, uint8_t *out, size_t n)
 * SysV: rdi=max_y, xmm0=floor, rsi=out, rdx=n
 */
        .text
        .p2align 4
        .globl hsn_asm_horizon_y_f32
        .type  hsn_asm_horizon_y_f32, @function
hsn_asm_horizon_y_f32:
        test    %rdx, %rdx
        je      .Ldone
        test    %rdi, %rdi
        je      .Ldone
        test    %rsi, %rsi
        je      .Ldone
        xor     %rcx, %rcx
        shufps  $0, %xmm0, %xmm0
.Lloop:
        cmp     %rdx, %rcx
        jae     .Ldone
        movss   (%rdi, %rcx, 4), %xmm1
        comiss  %xmm0, %xmm1
        setb    %al
        mov     %al, (%rsi, %rcx)
        inc     %rcx
        jmp     .Lloop
.Ldone:
        ret
        .size hsn_asm_horizon_y_f32, .-hsn_asm_horizon_y_f32
