/* System V AMD64
 * hsn_asm_cull_aabb_f32(const float *planes24, const float *aabb6, uint8_t *out, uint64_t n)
 *   rdi=planes  rsi=aabb  rdx=out  rcx=n
 * Packed AABB: minx,miny,minz,maxx,maxy,maxz. out[i]=1 if any plane rejects.
 *
 * hsn_asm_cull_sphere_f32(const float *planes24, const float *xyzr, uint8_t *out, uint64_t n)
 *   rdi=planes  rsi=xyzr  rdx=out  rcx=n
 */
        .intel_syntax noprefix
        .text
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
        xorps   xmm7, xmm7
.Laabb_box:
        movss   xmm8, dword ptr [rsi]
        movss   xmm9, dword ptr [rsi + 4]
        movss   xmm10, dword ptr [rsi + 8]
        movss   xmm11, dword ptr [rsi + 12]
        movss   xmm12, dword ptr [rsi + 16]
        movss   xmm13, dword ptr [rsi + 20]
        mov     r9, rdi
        mov     r10d, 6
.Laabb_plane:
        movss   xmm0, dword ptr [r9]
        movss   xmm1, dword ptr [r9 + 4]
        movss   xmm2, dword ptr [r9 + 8]
        movss   xmm3, dword ptr [r9 + 12]
        comiss  xmm0, xmm7
        jae     1f
        movss   xmm4, xmm8
        jmp     2f
1:      movss   xmm4, xmm11
2:      comiss  xmm1, xmm7
        jae     3f
        movss   xmm5, xmm9
        jmp     4f
3:      movss   xmm5, xmm12
4:      comiss  xmm2, xmm7
        jae     5f
        movss   xmm6, xmm10
        jmp     6f
5:      movss   xmm6, xmm13
6:      mulss   xmm0, xmm4
        mulss   xmm1, xmm5
        mulss   xmm2, xmm6
        addss   xmm0, xmm1
        addss   xmm0, xmm2
        addss   xmm0, xmm3
        comiss  xmm0, xmm7
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
        xorps   xmm7, xmm7
.Lsph_item:
        movss   xmm4, dword ptr [rsi]
        movss   xmm5, dword ptr [rsi + 4]
        movss   xmm6, dword ptr [rsi + 8]
        movss   xmm3, dword ptr [rsi + 12]
        xorps   xmm3, dword ptr [rip + .Lsign]
        mov     r9, rdi
        mov     r10d, 6
.Lsph_plane:
        movss   xmm0, dword ptr [r9]
        movss   xmm1, dword ptr [r9 + 4]
        movss   xmm2, dword ptr [r9 + 8]
        mulss   xmm0, xmm4
        mulss   xmm1, xmm5
        mulss   xmm2, xmm6
        addss   xmm0, xmm1
        addss   xmm0, xmm2
        addss   xmm0, dword ptr [r9 + 12]
        comiss  xmm0, xmm3
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

        .section .rodata
        .p2align 4
.Lsign:
        .long   0x80000000, 0, 0, 0

        .section .note.GNU-stack,"",@progbits
