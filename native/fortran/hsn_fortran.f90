! HSN distance-mask kernel. Fortran 2003 iso_c_binding, C ABI.
! Unrolled inner loop so gfortran can emit packed compares.
module hsn_fortran
  use iso_c_binding
  implicit none
contains
  subroutine hsn_fortran_cull_f64(dist_sq, limit_sq, out_mask, n) &
       bind(C, name="hsn_fortran_cull_f64")
    real(c_double), intent(in)        :: dist_sq(*)
    real(c_double), value, intent(in) :: limit_sq
    integer(c_int8_t), intent(out)    :: out_mask(*)
    integer(c_long), value, intent(in):: n
    integer(c_long) :: i, last

    if (n <= 0_c_long) return

    last = n - 8_c_long
    i = 1_c_long
    do while (i <= last)
      out_mask(i)     = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i)     > limit_sq)
      out_mask(i + 1) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 1) > limit_sq)
      out_mask(i + 2) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 2) > limit_sq)
      out_mask(i + 3) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 3) > limit_sq)
      out_mask(i + 4) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 4) > limit_sq)
      out_mask(i + 5) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 5) > limit_sq)
      out_mask(i + 6) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 6) > limit_sq)
      out_mask(i + 7) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i + 7) > limit_sq)
      i = i + 8_c_long
    end do
    do while (i <= n)
      out_mask(i) = merge(1_c_int8_t, 0_c_int8_t, dist_sq(i) > limit_sq)
      i = i + 1_c_long
    end do
  end subroutine hsn_fortran_cull_f64
end module hsn_fortran
