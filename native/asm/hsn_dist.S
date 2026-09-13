/* System V AMD64 — AVX2 distance + frustum kernels. */
        .intel_syntax noprefix
        .text
        .p2align 4
        .globl  hsn_asm_cull_f64
        .type   hsn_asm_cull_f64, @function
hsn_asm_cull_f64:
        test    rdx, rdx
        jz      .Lret
        test    rdi, rdi
        jz      .Lret
        test    rsi, rsi
        jz      .Lret
        xor     rcx, rcx
        vbroadcastsd ymm3, xmm0
        mov     r8, rdx
        and     r8, -4
        jz      .Ltail
        mov     r10, 0x01010101
.Lloop:
        vmovupd ymm0, [rdi + rcx*8]
        vcmppd  ymm1, ymm0, ymm3, 14
        vmovmskpd eax, ymm1
        pdep    eax, eax, r10d
        mov     dword ptr [rsi + rcx], eax
        add     rcx, 4
        cmp     rcx, r8
        jb      .Lloop
        vzeroupper
.Ltail:
        cmp     rcx, rdx
        jae     .Lret
.Lscalar:
        vmovsd  xmm0, qword ptr [rdi + rcx*8]
        vucomisd xmm0, xmm3
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
        xor     r8, r8
        vxorps  xmm15, xmm15, xmm15
.Laabb_box:
        vmovss  xmm0, dword ptr [rsi]
        vmovss  xmm1, dword ptr [rsi + 4]
        vmovss  xmm2, dword ptr [rsi + 8]
        vmovss  xmm3, dword ptr [rsi + 12]
        vmovss  xmm4, dword ptr [rsi + 16]
        vmovss  xmm5, dword ptr [rsi + 20]
        mov     r9, rdi
        mov     r10d, 6
.Laabb_plane:
        vmovss  xmm6, dword ptr [r9]
        vmovss  xmm7, dword ptr [r9 + 4]
        vmovss  xmm8, dword ptr [r9 + 8]
        vmovss  xmm9, dword ptr [r9 + 12]
        vucomiss xmm6, xmm15
        jae     .Laabb_pxmax
        vmovaps xmm10, xmm0
        jmp     .Laabb_px
.Laabb_pxmax:
        vmovaps xmm10, xmm3
.Laabb_px:
        vucomiss xmm7, xmm15
        jae     .Laabb_pymax
        vmovaps xmm11, xmm1
        jmp     .Laabb_py
.Laabb_pymax:
        vmovaps xmm11, xmm4
.Laabb_py:
        vucomiss xmm8, xmm15
        jae     .Laabb_pzmax
        vmovaps xmm12, xmm2
        jmp     .Laabb_pz
.Laabb_pzmax:
        vmovaps xmm12, xmm5
.Laabb_pz:
        vmulss  xmm6, xmm6, xmm10
        vmulss  xmm7, xmm7, xmm11
        vaddss  xmm6, xmm6, xmm7
        vmulss  xmm8, xmm8, xmm12
        vaddss  xmm6, xmm6, xmm8
        vaddss  xmm6, xmm6, xmm9
        vucomiss xmm6, xmm15
        jb      .Laabb_drop
        add     r9, 16
        dec     r10d
        jnz     .Laabb_plane
        mov     byte ptr [rdx + r8], 0
        jmp     .Laabb_next
.Laabb_drop:
        mov     byte ptr [rdx + r8], 1
.Laabb_next:
        add     rsi, 24
        inc     r8
        cmp     r8, rcx
        jb      .Laabb_box
.Laabb_ret:
        ret
        .size   hsn_asm_cull_aabb_f32, .-hsn_asm_cull_aabb_f32

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
        vmovss  xmm0, dword ptr [rsi]
        vmovss  xmm1, dword ptr [rsi + 4]
        vmovss  xmm2, dword ptr [rsi + 8]
        vmovss  xmm3, dword ptr [rsi + 12]
        vxorps  xmm4, xmm4, xmm4
        vsubss  xmm4, xmm4, xmm3
        mov     r9, rdi
        mov     r10d, 6
.Lsph_plane:
        vmovss  xmm5, dword ptr [r9]
        vmulss  xmm5, xmm5, xmm0
        vmovss  xmm6, dword ptr [r9 + 4]
        vmulss  xmm6, xmm6, xmm1
        vaddss  xmm5, xmm5, xmm6
        vmovss  xmm6, dword ptr [r9 + 8]
        vmulss  xmm6, xmm6, xmm2
        vaddss  xmm5, xmm5, xmm6
        vaddss  xmm5, xmm5, dword ptr [r9 + 12]
        vucomiss xmm5, xmm4
        jb      .Lsph_drop
        add     r9, 16
        dec     r10d
        jnz     .Lsph_plane
        mov     byte ptr [rdx + r8], 0
        jmp     .Lsph_next
.Lsph_drop:
        mov     byte ptr [rdx + r8], 1
.Lsph_next:
        add     rsi, 16
        inc     r8
        cmp     r8, rcx
        jb      .Lsph_item
.Lsph_ret:
        ret
        .size   hsn_asm_cull_sphere_f32, .-hsn_asm_cull_sphere_f32

        .section .note.GNU-stack,"",@progbits
