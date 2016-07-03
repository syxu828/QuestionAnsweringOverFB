#ifdef MKL
#define MALLOC(__size__) mkl_malloc(__size__, 64)
#define FREE(__var__) mkl_free(__var__)
#else
#include <cstdlib>
#define MALLOC(__size__) malloc(__size__)
#define FREE(__var__) free(__var__)

#ifdef __cplusplus
extern "C"{
#endif

int dger_(long int *m, long int *n, double *alpha,
		double *x, long int *incx, double *y, long int *incy,
		double *a, long int *lda);
int dgemv_(const char *trans, long int *m, long int *n, double *alpha, double *a,
		long int *lda, double *x, long int *incx, double *beta, double *y,
		long int *incy);
int dgemm_(const char *transa, const char *transb, long int *m, long int *
		n, long int *k, double *alpha, double *a, long int *lda,
		double *b, long int *ldb, double *beta, double *c, long int
		*ldc);
double ddot_(long int *n, double *x, long int *incx, double *y, long int *incy);

double dnrm2_(long int *, double *, long int *);
double ddot_(long int *, double *, long int *, double *, long int *);
int daxpy_(long int *, double *, double *, long int *, double *, long int *);
int dscal_(long int *, double *, double *, long int *);
#ifdef __cplusplus
}
#endif
#endif
