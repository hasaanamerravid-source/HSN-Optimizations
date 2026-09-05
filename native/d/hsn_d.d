// HSN distance-mask kernel. D compiles to a C ABI .so.
// No GC, no TLS, no runtime init on the call: betterC + nogc + nothrow.
module hsn_d;

extern(C):

export void hsn_d_cull_f64(const(double)* distSq, double limitSq,
        ubyte* outMask, long n) @nogc nothrow
{
    if (distSq is null || outMask is null || n <= 0)
        return;

    long i = 0;
    const long last = n - 8;
    while (i <= last)
    {
        outMask[i]     = distSq[i]     > limitSq;
        outMask[i + 1] = distSq[i + 1] > limitSq;
        outMask[i + 2] = distSq[i + 2] > limitSq;
        outMask[i + 3] = distSq[i + 3] > limitSq;
        outMask[i + 4] = distSq[i + 4] > limitSq;
        outMask[i + 5] = distSq[i + 5] > limitSq;
        outMask[i + 6] = distSq[i + 6] > limitSq;
        outMask[i + 7] = distSq[i + 7] > limitSq;
        i += 8;
    }
    while (i < n)
    {
        outMask[i] = distSq[i] > limitSq;
        ++i;
    }
}
