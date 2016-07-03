/* blasp.h  --  C prototypes for BLAS                         Ver 1.0 */
/* Jesse Bennett                                       March 23, 2000 */

/* Functions  listed in alphabetical order */

#ifdef F2C_COMPAT

void cdotc_(fcomplex *dotval, long *n, fcomplex *cx, long *incx,
            fcomplex *cy, long *incy);

void cdotu_(fcomplex *dotval, long *n, fcomplex *cx, long *incx,
            fcomplex *cy, long *incy);

double sasum_(long *n, float *sx, long *incx);

double scasum_(long *n, fcomplex *cx, long *incx);

double scnrm2_(long *n, fcomplex *x, long *incx);

double sdot_(long *n, float *sx, long *incx, float *sy, long *incy);

double snrm2_(long *n, float *x, long *incx);

void zdotc_(dcomplex *dotval, long *n, dcomplex *cx, long *incx,
            dcomplex *cy, long *incy);

void zdotu_(dcomplex *dotval, long *n, dcomplex *cx, long *incx,
            dcomplex *cy, long *incy);

#else

fcomplex cdotc_(long *n, fcomplex *cx, long *incx, fcomplex *cy, long *incy);

fcomplex cdotu_(long *n, fcomplex *cx, long *incx, fcomplex *cy, long *incy);

float sasum_(long *n, float *sx, long *incx);

float scasum_(long *n, fcomplex *cx, long *incx);

float scnrm2_(long *n, fcomplex *x, long *incx);

float sdot_(long *n, float *sx, long *incx, float *sy, long *incy);

float snrm2_(long *n, float *x, long *incx);

dcomplex zdotc_(long *n, dcomplex *cx, long *incx, dcomplex *cy, long *incy);

dcomplex zdotu_(long *n, dcomplex *cx, long *incx, dcomplex *cy, long *incy);

#endif

/* Remaining functions listed in alphabetical order */

int caxpy_(long *n, fcomplex *ca, fcomplex *cx, long *incx, fcomplex *cy,
           long *incy);

int ccopy_(long *n, fcomplex *cx, long *incx, fcomplex *cy, long *incy);

int cgbmv_(char *trans, long *m, long *n, long *kl, long *ku,
           fcomplex *alpha, fcomplex *a, long *lda, fcomplex *x, long *incx,
           fcomplex *beta, fcomplex *y, long *incy);

int cgemm_(char *transa, char *transb, long *m, long *n, long *k,
           fcomplex *alpha, fcomplex *a, long *lda, fcomplex *b, long *ldb,
           fcomplex *beta, fcomplex *c, long *ldc);

int cgemv_(char *trans, long *m, long *n, fcomplex *alpha, fcomplex *a,
           long *lda, fcomplex *x, long *incx, fcomplex *beta, fcomplex *y,
           long *incy);

int cgerc_(long *m, long *n, fcomplex *alpha, fcomplex *x, long *incx,
           fcomplex *y, long *incy, fcomplex *a, long *lda);

int cgeru_(long *m, long *n, fcomplex *alpha, fcomplex *x, long *incx,
           fcomplex *y, long *incy, fcomplex *a, long *lda);

int chbmv_(char *uplo, long *n, long *k, fcomplex *alpha, fcomplex *a,
           long *lda, fcomplex *x, long *incx, fcomplex *beta, fcomplex *y,
           long *incy);

int chemm_(char *side, char *uplo, long *m, long *n, fcomplex *alpha,
           fcomplex *a, long *lda, fcomplex *b, long *ldb, fcomplex *beta,
           fcomplex *c, long *ldc);

int chemv_(char *uplo, long *n, fcomplex *alpha, fcomplex *a, long *lda,
           fcomplex *x, long *incx, fcomplex *beta, fcomplex *y, long *incy);

int cher_(char *uplo, long *n, float *alpha, fcomplex *x, long *incx,
          fcomplex *a, long *lda);

int cher2_(char *uplo, long *n, fcomplex *alpha, fcomplex *x, long *incx,
           fcomplex *y, long *incy, fcomplex *a, long *lda);

int cher2k_(char *uplo, char *trans, long *n, long *k, fcomplex *alpha,
            fcomplex *a, long *lda, fcomplex *b, long *ldb, float *beta,
            fcomplex *c, long *ldc);

int cherk_(char *uplo, char *trans, long *n, long *k, float *alpha,
           fcomplex *a, long *lda, float *beta, fcomplex *c, long *ldc);

int chpmv_(char *uplo, long *n, fcomplex *alpha, fcomplex *ap, fcomplex *x,
           long *incx, fcomplex *beta, fcomplex *y, long *incy);

int chpr_(char *uplo, long *n, float *alpha, fcomplex *x, long *incx,
          fcomplex *ap);

int chpr2_(char *uplo, long *n, fcomplex *alpha, fcomplex *x, long *incx,
           fcomplex *y, long *incy, fcomplex *ap);

int crotg_(fcomplex *ca, fcomplex *cb, float *c, fcomplex *s);

int cscal_(long *n, fcomplex *ca, fcomplex *cx, long *incx);

int csscal_(long *n, float *sa, fcomplex *cx, long *incx);

int cswap_(long *n, fcomplex *cx, long *incx, fcomplex *cy, long *incy);

int csymm_(char *side, char *uplo, long *m, long *n, fcomplex *alpha,
           fcomplex *a, long *lda, fcomplex *b, long *ldb, fcomplex *beta,
           fcomplex *c, long *ldc);

int csyr2k_(char *uplo, char *trans, long *n, long *k, fcomplex *alpha,
            fcomplex *a, long *lda, fcomplex *b, long *ldb, fcomplex *beta,
            fcomplex *c, long *ldc);

int csyrk_(char *uplo, char *trans, long *n, long *k, fcomplex *alpha,
           fcomplex *a, long *lda, fcomplex *beta, fcomplex *c, long *ldc);

int ctbmv_(char *uplo, char *trans, char *diag, long *n, long *k,
           fcomplex *a, long *lda, fcomplex *x, long *incx);

int ctbsv_(char *uplo, char *trans, char *diag, long *n, long *k,
           fcomplex *a, long *lda, fcomplex *x, long *incx);

int ctpmv_(char *uplo, char *trans, char *diag, long *n, fcomplex *ap,
           fcomplex *x, long *incx);

int ctpsv_(char *uplo, char *trans, char *diag, long *n, fcomplex *ap,
           fcomplex *x, long *incx);

int ctrmm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, fcomplex *alpha, fcomplex *a, long *lda, fcomplex *b,
           long *ldb);

int ctrmv_(char *uplo, char *trans, char *diag, long *n, fcomplex *a,
           long *lda, fcomplex *x, long *incx);

int ctrsm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, fcomplex *alpha, fcomplex *a, long *lda, fcomplex *b,
           long *ldb);

int ctrsv_(char *uplo, char *trans, char *diag, long *n, fcomplex *a,
           long *lda, fcomplex *x, long *incx);

int daxpy_(long *n, double *sa, double *sx, long *incx, double *sy,
           long *incy);

int dcopy_(long *n, double *sx, long *incx, double *sy, long *incy);

int dgbmv_(char *trans, long *m, long *n, long *kl, long *ku,
           double *alpha, double *a, long *lda, double *x, long *incx,
           double *beta, double *y, long *incy);

int dgemm_(char *transa, char *transb, long *m, long *n, long *k,
           double *alpha, double *a, long *lda, double *b, long *ldb,
           double *beta, double *c, long *ldc);

int dgemv_(char *trans, long *m, long *n, double *alpha, double *a,
           long *lda, double *x, long *incx, double *beta, double *y, 
           long *incy);

int dger_(long *m, long *n, double *alpha, double *x, long *incx,
          double *y, long *incy, double *a, long *lda);

int drot_(long *n, double *sx, long *incx, double *sy, long *incy,
          double *c, double *s);

int drotg_(double *sa, double *sb, double *c, double *s);

int dsbmv_(char *uplo, long *n, long *k, double *alpha, double *a,
           long *lda, double *x, long *incx, double *beta, double *y, 
           long *incy);

int dscal_(long *n, double *sa, double *sx, long *incx);

  long i, m, nincx, nn, iincx;
int dspmv_(char *uplo, long *n, double *alpha, double *ap, double *x,
           long *incx, double *beta, double *y, long *incy);

int dspr_(char *uplo, long *n, double *alpha, double *x, long *incx,
          double *ap);

int dspr2_(char *uplo, long *n, double *alpha, double *x, long *incx,
           double *y, long *incy, double *ap);

int dswap_(long *n, double *sx, long *incx, double *sy, long *incy);

int dsymm_(char *side, char *uplo, long *m, long *n, double *alpha,
           double *a, long *lda, double *b, long *ldb, double *beta,
           double *c, long *ldc);

int dsymv_(char *uplo, long *n, double *alpha, double *a, long *lda,
           double *x, long *incx, double *beta, double *y, long *incy);

int dsyr_(char *uplo, long *n, double *alpha, double *x, long *incx,
          double *a, long *lda);

int dsyr2_(char *uplo, long *n, double *alpha, double *x, long *incx,
           double *y, long *incy, double *a, long *lda);

int dsyr2k_(char *uplo, char *trans, long *n, long *k, double *alpha,
            double *a, long *lda, double *b, long *ldb, double *beta,
            double *c, long *ldc);

int dsyrk_(char *uplo, char *trans, long *n, long *k, double *alpha,
           double *a, long *lda, double *beta, double *c, long *ldc);

int dtbmv_(char *uplo, char *trans, char *diag, long *n, long *k,
           double *a, long *lda, double *x, long *incx);

int dtbsv_(char *uplo, char *trans, char *diag, long *n, long *k,
           double *a, long *lda, double *x, long *incx);

int dtpmv_(char *uplo, char *trans, char *diag, long *n, double *ap,
           double *x, long *incx);

int dtpsv_(char *uplo, char *trans, char *diag, long *n, double *ap,
           double *x, long *incx);

int dtrmm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, double *alpha, double *a, long *lda, double *b, 
           long *ldb);

int dtrmv_(char *uplo, char *trans, char *diag, long *n, double *a,
           long *lda, double *x, long *incx);

int dtrsm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, double *alpha, double *a, long *lda, double *b, 
           long *ldb);

int dtrsv_(char *uplo, char *trans, char *diag, long *n, double *a,
           long *lda, double *x, long *incx);


int saxpy_(long *n, float *sa, float *sx, long *incx, float *sy, long *incy);

int scopy_(long *n, float *sx, long *incx, float *sy, long *incy);

int sgbmv_(char *trans, long *m, long *n, long *kl, long *ku,
           float *alpha, float *a, long *lda, float *x, long *incx,
           float *beta, float *y, long *incy);

int sgemm_(char *transa, char *transb, long *m, long *n, long *k,
           float *alpha, float *a, long *lda, float *b, long *ldb,
           float *beta, float *c, long *ldc);

int sgemv_(char *trans, long *m, long *n, float *alpha, float *a,
           long *lda, float *x, long *incx, float *beta, float *y, 
           long *incy);

int sger_(long *m, long *n, float *alpha, float *x, long *incx,
          float *y, long *incy, float *a, long *lda);

int srot_(long *n, float *sx, long *incx, float *sy, long *incy,
          float *c, float *s);

int srotg_(float *sa, float *sb, float *c, float *s);

int ssbmv_(char *uplo, long *n, long *k, float *alpha, float *a,
           long *lda, float *x, long *incx, float *beta, float *y, 
           long *incy);

int sscal_(long *n, float *sa, float *sx, long *incx);

int sspmv_(char *uplo, long *n, float *alpha, float *ap, float *x,
           long *incx, float *beta, float *y, long *incy);

int sspr_(char *uplo, long *n, float *alpha, float *x, long *incx,
          float *ap);

int sspr2_(char *uplo, long *n, float *alpha, float *x, long *incx,
           float *y, long *incy, float *ap);

int sswap_(long *n, float *sx, long *incx, float *sy, long *incy);

int ssymm_(char *side, char *uplo, long *m, long *n, float *alpha,
           float *a, long *lda, float *b, long *ldb, float *beta,
           float *c, long *ldc);

int ssymv_(char *uplo, long *n, float *alpha, float *a, long *lda,
           float *x, long *incx, float *beta, float *y, long *incy);

int ssyr_(char *uplo, long *n, float *alpha, float *x, long *incx,
          float *a, long *lda);

int ssyr2_(char *uplo, long *n, float *alpha, float *x, long *incx,
           float *y, long *incy, float *a, long *lda);

int ssyr2k_(char *uplo, char *trans, long *n, long *k, float *alpha,
            float *a, long *lda, float *b, long *ldb, float *beta,
            float *c, long *ldc);

int ssyrk_(char *uplo, char *trans, long *n, long *k, float *alpha,
           float *a, long *lda, float *beta, float *c, long *ldc);

int stbmv_(char *uplo, char *trans, char *diag, long *n, long *k,
           float *a, long *lda, float *x, long *incx);

int stbsv_(char *uplo, char *trans, char *diag, long *n, long *k,
           float *a, long *lda, float *x, long *incx);

int stpmv_(char *uplo, char *trans, char *diag, long *n, float *ap,
           float *x, long *incx);

int stpsv_(char *uplo, char *trans, char *diag, long *n, float *ap,
           float *x, long *incx);

int strmm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, float *alpha, float *a, long *lda, float *b, 
           long *ldb);

int strmv_(char *uplo, char *trans, char *diag, long *n, float *a,
           long *lda, float *x, long *incx);

int strsm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, float *alpha, float *a, long *lda, float *b, 
           long *ldb);

int strsv_(char *uplo, char *trans, char *diag, long *n, float *a,
           long *lda, float *x, long *incx);

int zaxpy_(long *n, dcomplex *ca, dcomplex *cx, long *incx, dcomplex *cy,
           long *incy);

int zcopy_(long *n, dcomplex *cx, long *incx, dcomplex *cy, long *incy);

int zdscal_(long *n, double *sa, dcomplex *cx, long *incx);

int zgbmv_(char *trans, long *m, long *n, long *kl, long *ku,
           dcomplex *alpha, dcomplex *a, long *lda, dcomplex *x, long *incx,
           dcomplex *beta, dcomplex *y, long *incy);

int zgemm_(char *transa, char *transb, long *m, long *n, long *k,
           dcomplex *alpha, dcomplex *a, long *lda, dcomplex *b, long *ldb,
           dcomplex *beta, dcomplex *c, long *ldc);

int zgemv_(char *trans, long *m, long *n, dcomplex *alpha, dcomplex *a,
           long *lda, dcomplex *x, long *incx, dcomplex *beta, dcomplex *y,
           long *incy);

int zgerc_(long *m, long *n, dcomplex *alpha, dcomplex *x, long *incx,
           dcomplex *y, long *incy, dcomplex *a, long *lda);

int zgeru_(long *m, long *n, dcomplex *alpha, dcomplex *x, long *incx,
           dcomplex *y, long *incy, dcomplex *a, long *lda);

int zhbmv_(char *uplo, long *n, long *k, dcomplex *alpha, dcomplex *a,
           long *lda, dcomplex *x, long *incx, dcomplex *beta, dcomplex *y,
           long *incy);

int zhemm_(char *side, char *uplo, long *m, long *n, dcomplex *alpha,
           dcomplex *a, long *lda, dcomplex *b, long *ldb, dcomplex *beta,
           dcomplex *c, long *ldc);

int zhemv_(char *uplo, long *n, dcomplex *alpha, dcomplex *a, long *lda,
           dcomplex *x, long *incx, dcomplex *beta, dcomplex *y, long *incy);

int zher_(char *uplo, long *n, double *alpha, dcomplex *x, long *incx,
          dcomplex *a, long *lda);

int zher2_(char *uplo, long *n, dcomplex *alpha, dcomplex *x, long *incx,
           dcomplex *y, long *incy, dcomplex *a, long *lda);

int zher2k_(char *uplo, char *trans, long *n, long *k, dcomplex *alpha,
            dcomplex *a, long *lda, dcomplex *b, long *ldb, double *beta,
            dcomplex *c, long *ldc);

int zherk_(char *uplo, char *trans, long *n, long *k, double *alpha,
           dcomplex *a, long *lda, double *beta, dcomplex *c, long *ldc);

int zhpmv_(char *uplo, long *n, dcomplex *alpha, dcomplex *ap, dcomplex *x,
           long *incx, dcomplex *beta, dcomplex *y, long *incy);

int zhpr_(char *uplo, long *n, double *alpha, dcomplex *x, long *incx,
          dcomplex *ap);

int zhpr2_(char *uplo, long *n, dcomplex *alpha, dcomplex *x, long *incx,
           dcomplex *y, long *incy, dcomplex *ap);

int zrotg_(dcomplex *ca, dcomplex *cb, double *c, dcomplex *s);

int zscal_(long *n, dcomplex *ca, dcomplex *cx, long *incx);

int zswap_(long *n, dcomplex *cx, long *incx, dcomplex *cy, long *incy);

int zsymm_(char *side, char *uplo, long *m, long *n, dcomplex *alpha,
           dcomplex *a, long *lda, dcomplex *b, long *ldb, dcomplex *beta,
           dcomplex *c, long *ldc);

int zsyr2k_(char *uplo, char *trans, long *n, long *k, dcomplex *alpha,
            dcomplex *a, long *lda, dcomplex *b, long *ldb, dcomplex *beta,
            dcomplex *c, long *ldc);

int zsyrk_(char *uplo, char *trans, long *n, long *k, dcomplex *alpha,
           dcomplex *a, long *lda, dcomplex *beta, dcomplex *c, long *ldc);

int ztbmv_(char *uplo, char *trans, char *diag, long *n, long *k,
           dcomplex *a, long *lda, dcomplex *x, long *incx);

int ztbsv_(char *uplo, char *trans, char *diag, long *n, long *k,
           dcomplex *a, long *lda, dcomplex *x, long *incx);

int ztpmv_(char *uplo, char *trans, char *diag, long *n, dcomplex *ap,
           dcomplex *x, long *incx);

int ztpsv_(char *uplo, char *trans, char *diag, long *n, dcomplex *ap,
           dcomplex *x, long *incx);

int ztrmm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, dcomplex *alpha, dcomplex *a, long *lda, dcomplex *b,
           long *ldb);

int ztrmv_(char *uplo, char *trans, char *diag, long *n, dcomplex *a,
           long *lda, dcomplex *x, long *incx);

int ztrsm_(char *side, char *uplo, char *transa, char *diag, long *m,
           long *n, dcomplex *alpha, dcomplex *a, long *lda, dcomplex *b,
           long *ldb);

int ztrsv_(char *uplo, char *trans, char *diag, long *n, dcomplex *a,
           long *lda, dcomplex *x, long *incx);
