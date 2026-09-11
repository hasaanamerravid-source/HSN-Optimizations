/* System V AMD64 — rsqrt with one Newton step, length2, clamp. */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_rsqrt_f32
        .type   hsn_asm_rsqrt_f32, @function
hsn_asm_rsqrt_f32:
        test    rdx, rdx
        jz      .Lrs_ret
        test    rdi, rdi
        jz      .Lrs_ret
        test    rsi, rsi
        jz      .Lrs_ret
        xor     rcx, rcx
        mov     r8, rdx
        and     r8, -8
        jz      .Lrs_tail
        vmovaps ymm4, ymmword ptr [rip + .Lhalf]
        vmovaps ymm5, ymmword ptr [rip + .Lthree_halves]
.Lrs_loop:
        vmovups ymm0, [rdi + rcx*4]
        vrsqrtps ymm1, ymm0
        vmulps  ymm2, ymm1, ymm1
        vmulps  ymm2, ymm2, ymm0
        vmulps  ymm2, ymm2, ymm4
        vsubps  ymm2, ymm5, ymm2
        vmulps  ymm1, ymm1, ymm2
        vmovups [rsi + rcx*4], ymm1
        add     rcx, 8
        cmp     rcx, r8
        jb      .Lrs_loop
        vzeroupper
.Lrs_tail:
        cmp     rcx, rdx
        jae     .Lrs_ret
.Lrs_scalar:
        vmovss  xmm0, dword ptr [rdi + rcx*4]
        vrsqrtss xmm1, xmm0, xmm0
        vmulss  xmm2, xmm1, xmm1
        vmulss  xmm2, xmm2, xmm0
        vmulss  xmm2, xmm2, dword ptr [rip + .Lhalf]
        vmovss  xmm3, dword ptr [rip + .Lthree_halves]
        vsubss  xmm2, xmm3, xmm2
        vmulss  xmm1, xmm1, xmm2
        vmovss  dword ptr [rsi + rcx*4], xmm1
        inc     rcx
        cmp     rcx, rdx
        jb      .Lrs_scalar
.Lrs_ret:
        vzeroupper
        ret
        .size   hsn_asm_rsqrt_f32, .-hsn_asm_rsqrt_f32

        .p2align 4
        .globl  hsn_asm_dot3_f32
        .type   hsn_asm_dot3_f32, @function
hsn_asm_dot3_f32:
        test    rcx, rcx
        jz      .Ldot_ret
        test    rdi, rdi
        jz      .Ldot_ret
        test    rsi, rsi
        jz      .Ldot_ret
        test    rdx, rdx
        jz      .Ldot_ret
        xor     r8, r8
.Ldot_loop:
        lea     rax, [r8 + r8*2]
        vmovss  xmm0, dword ptr [rdi + rax*4]
        vmulss  xmm0, xmm0, dword ptr [rsi + rax*4]
        vmovss  xmm1, dword ptr [rdi + rax*4 + 4]
        vmulss  xmm1, xmm1, dword ptr [rsi + rax*4 + 4]
        vaddss  xmm0, xmm0, xmm1
        vmovss  xmm1, dword ptr [rdi + rax*4 + 8]
        vmulss  xmm1, xmm1, dword ptr [rsi + rax*4 + 8]
        vaddss  xmm0, xmm0, xmm1
        vmovss  dword ptr [rdx + r8*4], xmm0
        inc     r8
        cmp     r8, rcx
        jb      .Ldot_loop
.Ldot_ret:
        ret
        .size   hsn_asm_dot3_f32, .-hsn_asm_dot3_f32

        .p2align 4
        .globl  hsn_asm_length2_f64
        .type   hsn_asm_length2_f64, @function
hsn_asm_length2_f64:
        test    r8, r8
        jz      .Ll2_ret
        test    rdi, rdi
        jz      .Ll2_ret
        test    rsi, rsi
        jz      .Ll2_ret
        test    rdx, rdx
        jz      .Ll2_ret
        test    rcx, rcx
        jz      .Ll2_ret
        vbroadcastsd ymm3, xmm0
        vbroadcastsd ymm4, xmm1
        vbroadcastsd ymm5, xmm2
        xor     r9, r9
        mov     r10, r8
        and     r10, -4
        jz      .Ll2_tail
.Ll2_loop:
        vmovupd ymm0, [rdi + r9*8]
        vsubpd  ymm0, ymm0, ymm3
        vmulpd  ymm0, ymm0, ymm0
        vmovupd ymm1, [rsi + r9*8]
        vsubpd  ymm1, ymm1, ymm4
        vmulpd  ymm1, ymm1, ymm1
        vaddpd  ymm0, ymm0, ymm1
        vmovupd ymm1, [rdx + r9*8]
        vsubpd  ymm1, ymm1, ymm5
        vmulpd  ymm1, ymm1, ymm1
        vaddpd  ymm0, ymm0, ymm1
        vmovupd [rcx + r9*8], ymm0
        add     r9, 4
        cmp     r9, r10
        jb      .Ll2_loop
        vzeroupper
.Ll2_tail:
        cmp     r9, r8
        jae     .Ll2_ret
.Ll2_scalar:
        vmovsd  xmm0, qword ptr [rdi + r9*8]
        vsubsd  xmm0, xmm0, xmm3
        vmulsd  xmm0, xmm0, xmm0
        vmovsd  xmm1, qword ptr [rsi + r9*8]
        vsubsd  xmm1, xmm1, xmm4
        vmulsd  xmm1, xmm1, xmm1
        vaddsd  xmm0, xmm0, xmm1
        vmovsd  xmm1, qword ptr [rdx + r9*8]
        vsubsd  xmm1, xmm1, xmm5
        vmulsd  xmm1, xmm1, xmm1
        vaddsd  xmm0, xmm0, xmm1
        vmovsd  qword ptr [rcx + r9*8], xmm0
        inc     r9
        cmp     r9, r8
        jb      .Ll2_scalar
.Ll2_ret:
        vzeroupper
        ret
        .size   hsn_asm_length2_f64, .-hsn_asm_length2_f64

        .p2align 4
        .globl  hsn_asm_prefetch_f64
        .type   hsn_asm_prefetch_f64, @function
hsn_asm_prefetch_f64:
        test    rsi, rsi
        jz      .Lpf_ret
        test    rdi, rdi
        jz      .Lpf_ret
        xor     rcx, rcx
.Lpf_loop:
        prefetcht0 byte ptr [rdi + rcx*8]
        add     rcx, 8
        cmp     rcx, rsi
        jb      .Lpf_loop
.Lpf_ret:
        ret
        .size   hsn_asm_prefetch_f64, .-hsn_asm_prefetch_f64

        .p2align 4
        .globl  hsn_asm_clamp01_f32
        .type   hsn_asm_clamp01_f32, @function
hsn_asm_clamp01_f32:
        test    rdx, rdx
        jz      .Lcl_ret
        xor     rcx, rcx
        vxorps  ymm2, ymm2, ymm2
        vbroadcastss ymm3, dword ptr [rip + .Lone]
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
.Lcl_scalar:
        vmovss  xmm0, dword ptr [rdi + rcx*4]
        vmaxss  xmm0, xmm0, xmm2
        vminss  xmm0, xmm0, xmm3
        vmovss  dword ptr [rsi + rcx*4], xmm0
        inc     rcx
        cmp     rcx, rdx
        jb      .Lcl_scalar
.Lcl_ret:
        vzeroupper
        ret
        .size   hsn_asm_clamp01_f32, .-hsn_asm_clamp01_f32

        .section .rodata
        .p2align 5
.Lhalf:
        .rept 8
        .float 0.5
        .endr
.Lthree_halves:
        .rept 8
        .float 1.5
        .endr
.Lone:
        .float 1.0
        .section .note.GNU-stack,"",@progbits
