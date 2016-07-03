#include "blas.h"

int dgemv_(char *trans, long *m, long *n, double *alpha, double *a,
           long *lda, double *x, long *incx, double *beta, double *y, 
           long *incy)
{
  long info;
  double temp, *pa, aalpha, bbeta;
  long lenx, leny, i, j, dima, mm, nn, iincx, iincy;
  long ix, iy, jx, jy, kx, ky;
  blasbool notrans;

  /* Dependencies */
  extern int xerbla_(char *, long *);


/*  Purpose   
    =======   

    DGEMV  performs one of the matrix-vector operations   

       y := alpha*A*x + beta*y,   or   y := alpha*A'*x + beta*y,   

    where alpha and beta are scalars, x and y are vectors and A is an   
    m by n matrix.   

    Parameters   
    ==========   

    TRANS  - CHARACTER*1.   
             On entry, TRANS specifies the operation to be performed as   
             follows:   

                TRANS = 'N' or 'n'   y := alpha*A*x + beta*y.   

                TRANS = 'T' or 't'   y := alpha*A'*x + beta*y.   

                TRANS = 'C' or 'c'   y := alpha*A'*x + beta*y.   

             Unchanged on exit.   

    M      - INTEGER.   
             On entry, M specifies the number of rows of the matrix A.   
             M must be at least zero.   
             Unchanged on exit.   

    N      - INTEGER.   
             On entry, N specifies the number of columns of the matrix A. 
  
             N must be at least zero.   
             Unchanged on exit.   

    ALPHA  - DOUBLE PRECISION.   
             On entry, ALPHA specifies the scalar alpha.   
             Unchanged on exit.   

    A      - DOUBLE PRECISION array of DIMENSION ( LDA, n ).   
             Before entry, the leading m by n part of the array A must   
             contain the matrix of coefficients.   
             Unchanged on exit.   

    LDA    - INTEGER.   
             On entry, LDA specifies the first dimension of A as declared 
             in the calling (sub) program. LDA must be at least   
             max( 1, m ).   
             Unchanged on exit.   

    X      - DOUBLE PRECISION array of DIMENSION at least   
             ( 1 + ( n - 1 )*abs( INCX ) ) when TRANS = 'N' or 'n'   
             and at least   
             ( 1 + ( m - 1 )*abs( INCX ) ) otherwise.   
             Before entry, the incremented array X must contain the   
             vector x.   
             Unchanged on exit.   

    INCX   - INTEGER.   
             On entry, INCX specifies the increment for the elements of   
             X. INCX must not be zero.   
             Unchanged on exit.   

    BETA   - DOUBLE PRECISION.   
             On entry, BETA specifies the scalar beta. When BETA is   
             supplied as zero then Y need not be set on input.   
             Unchanged on exit.   

    Y      - DOUBLE PRECISION array of DIMENSION at least   
             ( 1 + ( m - 1 )*abs( INCY ) ) when TRANS = 'N' or 'n'   
             and at least   
             ( 1 + ( n - 1 )*abs( INCY ) ) otherwise.   
             Before entry with BETA non-zero, the incremented array Y   
             must contain the vector y. On exit, Y is overwritten by the 
             updated vector y.   

    INCY   - INTEGER.   
             On entry, INCY specifies the increment for the elements of   
             Y. INCY must not be zero.   
             Unchanged on exit.   


    Level 2 Blas routine.   

    -- Written on 22-October-1986.   
       Jack Dongarra, Argonne National Lab.   
       Jeremy Du Croz, Nag Central Office.   
       Sven Hammarling, Nag Central Office.   
       Richard Hanson, Sandia National Labs.   
*/


  /* Dereference inputs */
  nn = *n;
  mm = *m;
  dima = *lda;
  aalpha = *alpha;
  bbeta = *beta;
  iincx = *incx;
  iincy = *incy;

  info = 0;

  switch( *trans )
  {
    case 'N':
    case 'n':
      notrans = TRUE;
      break;
    case 'T':
    case 't':
    case 'C':
    case 'c':
      notrans = FALSE;
      break;
    default:
      notrans = TRUE;
      info = 1;
  }

  if( info == 0 )
  {
    if (mm < 0) {
      info = 2;
    } else if (nn < 0) {
      info = 3;
    } else if (dima < MAX(1,mm)) {
      info = 6;
    } else if (iincx == 0) {
      info = 8;
    } else if (iincy == 0) {
      info = 11;
    }
  }

  if (info != 0)
  {
    xerbla_("DGEMV ", &info);
    return 0;
  }

  /* Quick return if possible. */

  if (mm != 0 && nn != 0 && (aalpha != 0.0 || bbeta != 1.0))
  {

    /* Set  LENX  and  LENY, the lengths of the vectors x and y, and set 
       up the start points in  X  and  Y. */

    if (notrans)
    {
      lenx = nn;
      leny = mm;
    }
    else
    {
      lenx = mm;
      leny = nn;
    }
    kx = iincx > 0 ? 0 : (1 - lenx) * iincx;
    ky = iincy > 0 ? 0 : (1 - leny) * iincy;

    /* Start the operations. In this version the elements of A are   
       accessed sequentially with one pass through A. */

    /* First form  y := beta*y. */

    if (bbeta != 1.0)
    {
      if (iincy == 1)
      {
        if (bbeta == 0.0)
        {
          for (i = 0; i < leny; ++i)
            y[i] = 0.0;
        }
        else  /* bbeta != 0.0 */
        {
          for (i = 0; i < leny; ++i)
            y[i] = bbeta * y[i];
        }
      }
      else  /* iincy != 1 */
      {
        iy = ky;
        if (bbeta == 0.0)
        {
          for (i = 0; i < leny; ++i)
          {
            y[iy] = 0.0;
            iy += iincy;
          }
        }
        else  /* bbeta != 0.0 */
        {
          for (i = 0; i < leny; ++i)
          {
            y[iy] = bbeta * y[iy];
            iy += iincy;
          }
        }
      }
    }
    if (aalpha != 0.0)
    {
      if (notrans) /* Form  y := alpha*A*x + y. */
      {
        jx = kx;
        if (iincy == 1)
        {
          for (pa=a, j = 0; j < nn; ++j, pa+=dima)
          {
            if (x[jx] != 0.0)
            {
              temp = aalpha * x[jx];
              for (i = 0; i < mm; ++i)
                y[i] += temp * pa[i];  /* y[i] += temp * A(i,j); */
            }
            jx += iincx;
          }
        }
        else
        {
          for (pa=a, j = 0; j < nn; ++j, pa+=dima)
          {
            if (x[jx] != 0.0)
            {
              temp = aalpha * x[jx];
              iy = ky;
              for (i = 0; i < mm; ++i)
              {
                y[iy] += temp * pa[i];  /* y[iy] += temp * A(i,j); */
                iy += iincy;
              }
            }
            jx += iincx;
          }
        }
      }
      else /* Form  y := alpha*A'*x + y. */
      {
        jy = ky;
        if (iincx == 1)
        {
          for (pa=a, j = 0; j < nn; ++j, pa+=dima)
          {
            temp = 0.0;
            for (i = 0; i < mm; ++i)
              temp += pa[i] * x[i];  /* temp += A(i,j) * x[i]; */
            y[jy] += aalpha * temp;
            jy += iincy;
          }
        }
        else
        {
          for (pa=a, j = 0; j < nn; ++j, pa+=dima)
          {
            temp = 0.0;
            ix = kx;
            for (i = 0; i < mm; ++i)
            {
              temp += pa[i] * x[ix];  /* temp += A(i,j) * x[ix]; */
              ix += iincx;
            }
            y[jy] += aalpha * temp;
            jy += iincy;
          }
        }
      }
    }
  }

  return 0;
} /* dgemv_ */
