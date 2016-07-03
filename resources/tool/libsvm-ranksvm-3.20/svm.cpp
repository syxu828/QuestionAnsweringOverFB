#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <float.h>
#include <string.h>
#include <stdarg.h>
#include <limits.h>
#include <locale.h>
#include "svm.h"
#include "tron.h"
#include "selectiontree.h"
#include "blas.h"

int block_size;

int libsvm_version = LIBSVM_VERSION;
#ifndef min
template <class T> static inline T min(T x,T y) { return (x<y)?x:y; }
#endif
#ifndef max
template <class T> static inline T max(T x,T y) { return (x>y)?x:y; }
#endif
template <class T> static inline void swap(T& x, T& y) { T t=x; x=y; y=t; }

static inline double powi(double base, int times)
{
	double tmp = base, ret = 1.0;

	for(int t=times; t>0; t/=2)
	{
		if(t%2==1) ret*=tmp;
		tmp = tmp * tmp;
	}
	return ret;
}
#define Malloc(type,n) (type *)malloc((n)*sizeof(type))

struct id_and_value
{
	int id;
	double value;
};

static int compare_id_and_value(const void *a, const void *b)
{
	struct id_and_value *ia = (struct id_and_value *)a;
	struct id_and_value *ib = (struct id_and_value *)b;
	if(ia->value > ib->value)
		return -1;
	if(ia->value < ib->value)
		return 1;
	return 0;
}

static void print_string_stdout(const char *s)
{
	fputs(s,stdout);
	fflush(stdout);
}

static void (*svm_print_string) (const char *) = &print_string_stdout;
#if 1
static void info(const char *fmt,...)
{
	char buf[BUFSIZ];
	va_list ap;
	va_start(ap,fmt);
	vsprintf(buf,fmt,ap);
	va_end(ap);
	(*svm_print_string)(buf);
}
#else
static void info(const char *fmt,...) {}
#endif

//
// Kernel evaluation
//
// the static method k_function is for doing single kernel evaluation
// the constructor of Kernel prepares to calculate the l*l kernel matrix
// the member function Qv is for matrix-product between Q and v
//


class Kernel_Q
{
	public:
		Kernel_Q(const svm_problem& prob, const svm_parameter& param, int block_size);
		~Kernel_Q();
		void Qv(double *s, double *Qs);
		static double dot(const svm_node *px, const svm_node *py);
		static double k_function(const svm_node *x, const svm_node *y, const svm_parameter &param);
	protected:
		const svm_parameter &param;
		double *qv_tmp_vector;
		double *get_partial_Q(int row_begin, int col_begin);
		double *dense_x;
		double *Q_block;
		double *cache;
		int number_of_cached_row;
		int l;
		int n;
		int block_size;
		double *ones;
		double *x_square;
		double* get_cached_Q(){return cache;}
		double (Kernel_Q::*kernel_function) (double) const;
		double gamma;
		double coef0;
		int degree;
		int nr_blocks;
		double kernel_poly(double dot) const
		{
			return powi(gamma * dot + coef0, degree);
		}
		double kernel_rbf(double norm) const
		{
			return exp(-gamma * norm);
		}
};

	Kernel_Q::Kernel_Q(const svm_problem& prob, const svm_parameter& _param, int _block_size)
: param(_param),
	qv_tmp_vector(NULL),
	Q_block(NULL),
	cache(NULL),
	number_of_cached_row(0),
	l(prob.l),
	n(prob.n),
	block_size(_block_size), 
	ones(NULL),
	x_square(NULL)

{
	int i, j;
	long int inc = 1;
	long int _n = n;


	//create dense X matrix
	dense_x = (double *)MALLOC(sizeof(double) * l * n);
	memset(dense_x, 0, sizeof(double) * l * n);
	if (param.kernel_type == PRECOMPUTED)
	{
		int *index = new int[l];
#pragma omp parallel for private(i)
		for (i = 0; i < l; i++)
			index[int(prob.x[i][0].value) - 1] = i;

#pragma omp parallel for private(i, j)
		for (i = 0; i < l; i++)
			for (j = 1; prob.x[i][j].index != -1; j++)
				dense_x[i * n + index[prob.x[i][j].index - 1]] = prob.x[i][j].value;

		delete[] index;
		return ;
	}
	else
	{
#pragma omp parallel for private(i, j)
		for (i = 0; i < l; i++)
			for (j = 0; prob.x[i][j].index != -1; j++)
				dense_x[i * n + prob.x[i][j].index - 1] = prob.x[i][j].value;
	}

	qv_tmp_vector = (double *)MALLOC(sizeof(double) * l);
	if (param.kernel_type == LINEAR)
		return ;
	
	

	//calculate the memory usage for one row of Q
	double need_for_one_row = sizeof(double) * (double)l / 1024.0 / 1024.0; //MB
	//calculate the number of cached row
	number_of_cached_row = int(param.cache_size / need_for_one_row);
	if (number_of_cached_row > l)
		number_of_cached_row = l;


	//allocate memory for partial Q
	Q_block = (double *)MALLOC(sizeof(double) * block_size * block_size);

	switch (param.kernel_type)
	{
		case POLY:
			kernel_function = &Kernel_Q::kernel_poly;
			coef0 = param.coef0;
			degree = param.degree;
			gamma = param.gamma;
			break;
		case RBF:
			kernel_function = &Kernel_Q::kernel_rbf;
			gamma = param.gamma;
			ones = (double *)MALLOC(sizeof(double) * l);
			//create e vector
#pragma omp parallel for private(i)
			for (i = 0; i < l; i++)
				ones[i] = 1.0;
			//calculate square_x
			x_square = (double*)MALLOC(sizeof(double) * l);
#pragma omp parallel for private(i, j)
			for (i = 0; i < l; i++)
				x_square[i] = ddot_(&_n, &dense_x[i*n], &inc, &dense_x[i*n], &inc);
			break;
		default:
			fprintf(stderr, "Kernel type not supported\n");
			exit(0);
	}

	nr_blocks = (l - number_of_cached_row + block_size - 1) / block_size;



	if (number_of_cached_row > 0 && param.kernel_type)
	{
		long int _m =  number_of_cached_row;
		long int _n = l;
		long int _k = n;
		double minus_2 = -2.0;
		double one = 1.0;
		double zero = 0.0;

		cache = (double*)MALLOC(sizeof(double) * l * number_of_cached_row);
		//compute cached row
		if (param.kernel_type == RBF)
		{
			dgemm_("T", "N", &_m, &_n, &_k, &minus_2, dense_x, &_k, dense_x, &_k, &zero, cache, &_m);
			dger_(&_m, &_n, &one, x_square, &inc, ones, &inc, cache, &_m);
			dger_(&_m, &_n, &one, ones, &inc, x_square, &inc, cache, &_m);
		}
		else
			dgemm_("T", "N", &_m, &_n, &_k, &one, dense_x, &_k, dense_x, &_k, &zero, cache, &_m);

#pragma omp parallel for private(i, j)
		for (i = 0; i < number_of_cached_row; i++)
		{
			long int base = ((long int)i) * l;
			for (j = 0; j < l; j++)
				cache[base + j] = (this->*kernel_function)(cache[base + j]);
		}

	}
}

Kernel_Q::~Kernel_Q()
{
	FREE(Q_block);
	FREE(dense_x);
	if (qv_tmp_vector)
		FREE(qv_tmp_vector);
	if (cache)
		FREE(cache);
	if (ones)
		FREE(ones);
	if (x_square)
		FREE(x_square);
}

void Kernel_Q::Qv(double *s, double *Qs)
{
	int i, j, k;
	double one = 1.0;
	double zero = 0.0;
	long int inc = 1;
	long int _m, _n;

	if (param.kernel_type == LINEAR)
	{
		_m = n;
		_n = l;
		dgemv_("N", &_m, &_n, &one, dense_x, &_m, s, &inc, &zero, qv_tmp_vector, &inc);
		dgemv_("T", &_m, &_n, &one, dense_x, &_m, qv_tmp_vector, &inc, &zero, Qs, &inc);
		return ;
	}
	else if (param.kernel_type == PRECOMPUTED)
	{
		_m = l;
		dgemv_("T", &_m, &_m, &one, dense_x, &_m, s, &inc, &zero, Qs, &inc);
		return ;
	}

	memset(Qs, 0, sizeof(double) * l);

	if (number_of_cached_row > 0)
	{
		_m = number_of_cached_row;
		_n = l;
		dgemv_("N", &_m, &_n, &one, cache, &_m, s, &inc, &zero, Qs, &inc);

		_m = number_of_cached_row;
		_n = l - number_of_cached_row;
		dgemv_("T", &_m, &_n, &one, cache + _m * _m, &_m, s, &inc, &one, Qs + number_of_cached_row, &inc);
	}

	for (i=0;i<nr_blocks;i++)
	{
		int row_start = block_size * i + number_of_cached_row;
		int row_end = min(l, row_start + block_size);
		double *partial_Q;


		_n = _m = row_end - row_start;
		partial_Q = get_partial_Q(row_start, row_start);
		dgemv_("N", &_m, &_n, &one, partial_Q, &_m, s + row_start, &inc, &zero, qv_tmp_vector, &inc);

#pragma omp parallel for private(j)
		for (j = row_start ; j < row_end; j++)
			Qs[j] += qv_tmp_vector[j - row_start];

		for (j=i+1;j<nr_blocks;j++)
		{
			int col_start = block_size * j + number_of_cached_row;
			int col_end = min(l, col_start + block_size);
			_n = col_end - col_start;
			partial_Q = get_partial_Q(row_start, col_start);
			dgemv_("N", &_m, &_n, &one, partial_Q, &_m, s + col_start, &inc, &zero, qv_tmp_vector, &inc);
#pragma omp parallel for private(k)
			for (k = row_start; k < row_end; k++)
				Qs[k] += qv_tmp_vector[k - row_start];

			dgemv_("T", &_m, &_n, &one, partial_Q, &_m, s + row_start, &inc, &zero, qv_tmp_vector, &inc);
#pragma omp parallel for private(k)
			for (k = col_start; k < col_end; k++)
				Qs[k] += qv_tmp_vector[k - col_start];
		}
	}
}

double *Kernel_Q::get_partial_Q(int row_begin, int col_begin)
{
	int row_end = min(l, row_begin + block_size);
	int col_end = min(l, col_begin + block_size);
	long int _m = row_end - row_begin;
	long int _n = col_end - col_begin;
	long int _k = n;
	double minus_2 = -2.0;
	double one = 1.0;
	double zero = 0.0;
	long int inc = 1;
	int i, j;

	if (param.kernel_type == RBF)
	{
		dgemm_("T", "N", &_m, &_n, &_k, &minus_2, dense_x + n * row_begin, &_k, dense_x + col_begin * n, &_k, &zero, Q_block, &_m);
		dger_(&_m, &_n, &one, x_square + row_begin, &inc, ones, &inc, Q_block, &_m);
		dger_(&_m, &_n, &one, ones, &inc, x_square + col_begin, &inc, Q_block, &_m);
	}
	else
		dgemm_("T", "N", &_m, &_n, &_k, &one, dense_x + n * row_begin, &_k, dense_x + col_begin * n, &_k, &zero, Q_block, &_m);

#pragma omp parallel for private(i, j)
	for (i = 0; i < _n; i++)
	{
		long int base = i * _m;
		for (j = 0; j < _m; j++)
			Q_block[base + j] = (this->*kernel_function)(Q_block[base + j]);
	}

	return Q_block;
}

double Kernel_Q::dot(const svm_node *px, const svm_node *py)
{
	double sum = 0;
	while(px->index != -1 && py->index != -1)
	{
		if(px->index == py->index)
		{
			sum += px->value * py->value;
			++px;
			++py;
		}
		else
		{
			if(px->index > py->index)
				++py;
			else
				++px;
		}			
	}
	return sum;
}

double Kernel_Q::k_function(const svm_node *x, const svm_node *y,
		const svm_parameter& param)
{
	switch(param.kernel_type)
	{
		case LINEAR:
			return dot(x,y);
		case POLY:
			return powi(param.gamma*dot(x,y)+param.coef0,param.degree);
		case RBF:
			{
				double sum = 0;
				while(x->index != -1 && y->index !=-1)
				{
					if(x->index == y->index)
					{
						double d = x->value - y->value;
						sum += d*d;
						++x;
						++y;
					}
					else
					{
						if(x->index > y->index)
						{	
							sum += y->value * y->value;
							++y;
						}
						else
						{
							sum += x->value * x->value;
							++x;
						}
					}
				}

				while(x->index != -1)
				{
					sum += x->value * x->value;
					++x;
				}

				while(y->index != -1)
				{
					sum += y->value * y->value;
					++y;
				}

				return exp(-param.gamma*sum);
			}
		case PRECOMPUTED:  //x: test (validation), y: SV
			return x[(int)(y->value)].value;
		default:
			return 0;  // Unreachable
	}
}

static void group_queries(const int *query_id, int l, int *nr_query_ret, int **start_ret, int **count_ret, int *perm)
{
	int max_nr_query = 16;
	int nr_query = 0;
	int *query = Malloc(int,max_nr_query);
	int *count = Malloc(int,max_nr_query);
	int *data_query = Malloc(int,l);
	int i;

	for(i=0;i<l;i++)
	{
		int this_query = (int)query_id[i];
		int j;
		for(j=0;j<nr_query;j++)
		{
			if(this_query == query[j])
			{
				++count[j];
				break;
			}
		}
		data_query[i] = j;
		if(j == nr_query)
		{
			if(nr_query == max_nr_query)
			{
				max_nr_query *= 2;
				query = (int *)realloc(query,max_nr_query * sizeof(int));
				count = (int *)realloc(count,max_nr_query * sizeof(int));
			}
			query[nr_query] = this_query;
			count[nr_query] = 1;
			++nr_query;
		}
	}

	int *start = Malloc(int,nr_query);
	start[0] = 0;
	for(i=1;i<nr_query;i++)
		start[i] = start[i-1] + count[i-1];
	for(i=0;i<l;i++)
	{
		perm[start[data_query[i]]] = i;
		++start[data_query[i]];
	}
	start[0] = 0;
	for(i=1;i<nr_query;i++)
		start[i] = start[i-1] + count[i-1];

	*nr_query_ret = nr_query;
	*start_ret = start;
	*count_ret = count;
	free(query);
	free(data_query);
}

class l2r_rank_fun: public function
{
	public:
		l2r_rank_fun(const svm_problem *prob, const svm_parameter *param, double *C);
		~l2r_rank_fun();

		double fun(double *w);
		void grad(double *w, double *g);
		void Hv(double *s, double *Hs);
		int get_nr_variable(void);
	private:
		void Qv(double *s, double *Qs);
		double *C;
		double *z;
		double *tmp_vector;
		double *wa;
		const svm_problem *prob;
		const svm_parameter *param;
		Kernel_Q Q;
		double *ATAQb;
		double *ATe;
		int *l_plus;
		int *l_minus;
		double *gamma_plus;
		double *gamma_minus;
		int nr_query;
		int *perm;
		int *start;
		int *count;
		id_and_value **pi;
		int *nr_class;
		int *int_y;
};

l2r_rank_fun::l2r_rank_fun(const svm_problem *prob, const svm_parameter *param , double *C): Q(*prob, *param, block_size)
{
	int l=prob->l;
	int i,j,k;
	this->param = param;
	this->prob = prob;
	this->C = C;
	z = (double*)MALLOC(sizeof(double) * l);
	tmp_vector = (double*)MALLOC(sizeof(double) * l);
	wa = (double*)MALLOC(sizeof(double) * l);
	perm = new int[l];
	group_queries(prob->query, l, &nr_query,&start, &count, perm);
	pi = new id_and_value* [nr_query];
	for (i=0;i<nr_query;i++)
		pi[i] = new id_and_value[count[i]];
	double *y=prob->y;
	int_y = new int[prob->l];
	nr_class = new int[nr_query];
	l_plus = new int[l];
	l_minus = new int[l];
	gamma_plus = new double[l];
	gamma_minus = new double[l];
	ATAQb = new double[l];
	ATe = new double[l];
#pragma omp parallel for private(i,j,k)	
	for (i=0;i<nr_query;i++)
	{
		k=1;
		for (j=0;j<count[i];j++)
		{
			pi[i][j].id = perm[j+start[i]];
			pi[i][j].value = y[perm[j+start[i]]];
		}
		qsort(pi[i], count[i], sizeof(id_and_value), compare_id_and_value);
		int_y[pi[i][count[i]-1].id]=1;
		for(j=count[i]-2;j>=0;j--)
		{
			if (pi[i][j].value>pi[i][j+1].value)
				k++;
			int_y[pi[i][j].id]=k;
		}
		nr_class[i]=k;
	}
}

l2r_rank_fun::~l2r_rank_fun()
{
	FREE(z);
	FREE(tmp_vector);
	FREE(wa);
	delete[] l_plus;
	delete[] l_minus;
	delete[] gamma_plus;
	delete[] gamma_minus;
	for (int i=0;i<nr_query;i++)
		delete[] pi[i];
	delete[] pi;
	delete[] int_y;
	delete[] nr_class;
	delete[] ATe;
	delete[] ATAQb;
	delete[] perm;
}

double l2r_rank_fun::fun(double *w)
{
	int i,j,k;
	double f=0;
	int l=prob->l;
	selectiontree *T;
	Qv(w,z);
#pragma omp parallel for private(i,j,k,T)
	for (i=0;i<nr_query;i++)
	{
		for (j=0;j<count[i];j++)
		{
			pi[i][j].id = perm[j+start[i]];
			pi[i][j].value = z[perm[j+start[i]]];
		}
		qsort(pi[i], count[i], sizeof(id_and_value), compare_id_and_value);

		T=new selectiontree(nr_class[i]);
		k=0;
		for (j=0;j<count[i];j++)
		{
			while (k<count[i]&&(1-pi[i][j].value+pi[i][k].value>0))
			{
				T->insert_node(int_y[pi[i][k].id],pi[i][k].value);

				k++;
			}
			T->smaller(int_y[pi[i][j].id],&l_minus[pi[i][j].id], &gamma_minus[pi[i][j].id]);
		}
		delete T;
		k=count[i]-1;

		T = new selectiontree(nr_class[i]);
		for (j=count[i]-1;j>=0;j--)
		{
			while (k>=0&&(1+pi[i][j].value-pi[i][k].value>0))
			{
				T->insert_node(int_y[pi[i][k].id],pi[i][k].value);
				k--;
			}
			T->larger(int_y[pi[i][j].id],&l_plus[pi[i][j].id], &gamma_plus[pi[i][j].id]);
		}
		delete T;
	}

	for(i=0;i<l;i++)
		f += w[i]*z[i];
	f /= 2.0;
	for (i=0;i<l;i++)
	{
		ATe[i] = l_minus[i] - l_plus[i];
		ATAQb[i] = (l_plus[i]+l_minus[i])*z[i]-gamma_plus[i]-gamma_minus[i];
	}
	for (i=0;i<l;i++)
		f += C[i]*(z[i]*(ATAQb[i] - 2 * ATe[i]) + l_minus[i]);
	return(f);
}

void l2r_rank_fun::grad(double *w, double *g)
{
	int i;
	int l=prob->l;
	for (i=0;i<l;i++)
		tmp_vector[i] = ATAQb[i] - ATe[i];
	Qv(tmp_vector, g);
	for(i=0;i<l;i++)
		g[i] = z[i] + 2*C[i]*g[i];
}

int l2r_rank_fun::get_nr_variable(void)
{
	return prob->l;
}

void l2r_rank_fun::Hv(double *s, double *Hs)
{
	int i,j,k;
	int l=prob->l;
	selectiontree *T;
	int tmp_value;
	double gamma_tmp;
	Qv(s, wa);
#pragma omp parallel for private(i,j,k,T,gamma_tmp)
	for (i=0;i<nr_query;i++)
	{
		T=new selectiontree(nr_class[i]);
		k=0;
		for (j=0;j<count[i];j++)
		{
			while (k<count[i]&&(1-pi[i][j].value+pi[i][k].value>0))
			{
				T->insert_node(int_y[pi[i][k].id],wa[pi[i][k].id]);
				k++;
			}
			T->smaller(int_y[pi[i][j].id],&tmp_value, &tmp_vector[pi[i][j].id]);
		}
		delete T;

		k=count[i]-1;
		T = new selectiontree(nr_class[i]);
		for (j=count[i]-1;j>=0;j--)
		{
			while (k>=0&&(1+pi[i][j].value-pi[i][k].value>0))
			{
				T->insert_node(int_y[pi[i][k].id],wa[pi[i][k].id]);
				k--;
			}
			T->larger(int_y[pi[i][j].id],&tmp_value, &gamma_tmp);
			tmp_vector[pi[i][j].id] += gamma_tmp;
		}
		delete T;
	}
	for (i=0;i<l;i++)
		tmp_vector[i]=wa[i]*((double)l_plus[i]+(double)l_minus[i])-tmp_vector[i];
	Qv(tmp_vector, Hs);
	for(i=0;i<l;i++)
		Hs[i] = wa[i] + 2*C[i]*Hs[i];
}

void l2r_rank_fun::Qv(double *s, double *Qs)
{
	Q.Qv(s, Qs);
}


//
// decision_function
//
struct decision_function
{
	double *alpha;
};

static decision_function svm_train_one(
		const svm_problem *prob, const svm_parameter *param,
		double Cp, double Cn)
{
	double *alpha;
	int l = prob->l;
	function *fun_obj=NULL;
	alpha = Malloc(double,l);
	double *C = new double[l];
	switch(param->svm_type)
	{
		case L2R_RANK:
		{
			for (int i=0;i<l;i++)
				C[i] = param->C;
			fun_obj=new l2r_rank_fun(prob, param, C);
			break;
		}
	}
	TRON tron_obj(fun_obj, param->eps);
	tron_obj.set_print_string(svm_print_string);
	tron_obj.tron(alpha);
	delete fun_obj;
	delete[] C;


	// output SVs

	int nSV = 0;
	for(int i=0;i<l;i++)
		if(fabs(alpha[i]) > 0)
			++nSV;

	info("nSV = %d\n",nSV);

	decision_function f;
	f.alpha = alpha;
	return f;
}

//
// Interface functions
//
svm_model *svm_train(const svm_problem *prob, const svm_parameter *param)
{
	block_size = 512;
	svm_model *model = Malloc(svm_model,1);
	model->param = *param;
	model->free_sv = 0;	// XXX

	if(param->svm_type == L2R_RANK)
	{
		model->nr_class = 2;
		model->sv_coef = Malloc(double *,1);

		decision_function f = svm_train_one(prob,param,0,0);

		int nSV = 0;
		int i;
		for(i=0;i<prob->l;i++)
			if(fabs(f.alpha[i]) > 0) ++nSV;
		model->l = nSV;
		model->SV = Malloc(svm_node *,nSV);
		model->sv_coef[0] = Malloc(double,nSV);
		model->sv_indices = Malloc(int,nSV);
		int j = 0;
		for(i=0;i<prob->l;i++)
			if(fabs(f.alpha[i]) > 0)
			{
				model->SV[j] = prob->x[i];
				model->sv_coef[0][j] = f.alpha[i];
				model->sv_indices[j] = i+1;
				++j;
			}		

		free(f.alpha);
	}
	
	return model;
}

//query-wise cross validation for ranksvm
void rank_cross_validation(const svm_problem *prob, const svm_parameter *param, int nr_fold, double *result)
{
	int i,q;
	int *fold_start;
	int l = prob->l;
	int *query_set;
	double *target = Malloc(double,l);
	int nr_query;
	int *start = NULL;
	int *count = NULL;
	int *perm = Malloc(int,l);
	int *query_perm;
	group_queries(prob->query, l, &nr_query, &start, &count, perm);
	if (nr_query == 1)
	{
		if (nr_fold > prob->l / 2)
		{
			nr_fold = l / 2; // each fold should include at least 2 instances to form pairs
			fprintf(stderr,"WARNING: # folds > # data / 2. Will use # folds = # data / 2 instead (Every fold should contain 2 data to form a pair)\n");
		}
		nr_query = nr_fold;
// Treat each fold as a query in performance evaluation
// to avoid ranking inconsistency between models.
		start = (int *)realloc(start,nr_query * sizeof(int));
		count = (int *)realloc(count,nr_query * sizeof(int));
		query_set = Malloc(int,l);
		for(q=0;q<nr_query;q++)
		{
			count[q] = 0;
			start[q] = 0;
		}
		for(i=0;i<l;i++)
		{
			int j = rand() % nr_query;
			query_set[i] = j;
			count[j]++;
		}
		start[0] = 0;
		for(q=1;q<nr_query;q++)
			start[q] = start[q-1] + count[q-1];
		for(i=0;i<l;i++)
		{
			perm[start[query_set[i]]] = i;
			++start[query_set[i]];
		}
		start[0] = 0;
		for(q=1;q<nr_query;q++)
			start[q] = start[q-1] + count[q-1];
	}
	else
	{
		query_set = prob->query;
		if (nr_query < nr_fold)
		{
			nr_fold = nr_query;
			fprintf(stderr,"WARNING: # folds > # query. Will use # folds = # query instead.\n");
		}
	}
	fold_start = Malloc(int,nr_fold+1);
	query_perm = Malloc(int,nr_query);
	for(i=0;i<=nr_fold;i++)
		fold_start[i] = i * nr_query / nr_fold;
	for (q=0;q<nr_query;q++)
		query_perm[q] = q;
	for (q=0;q<nr_query;q++)
	{
		i = q + rand() % (nr_query-q);
		swap(query_perm[q], query_perm[i]);
	}

	for(i=0;i<nr_fold;i++)
	{
		int begin = fold_start[i];
		int end = fold_start[i+1];
		int j,m,counter;
		struct svm_problem subprob;
		counter = 0;

		for (q=begin;q<end;q++)
			counter += count[query_perm[q]];
		subprob.l = l - counter;
		subprob.n = prob->n;
		subprob.x = Malloc(struct svm_node*,subprob.l);
		subprob.y = Malloc(double,subprob.l);
		subprob.query = Malloc(int,subprob.l);

		m = 0;
		for(q=0;q<begin;q++)
		{
			int *perm_q = &perm[start[query_perm[q]]];
			for (j=0;j<count[query_perm[q]];j++)
			{
				subprob.x[m] = prob->x[perm_q[j]];
				subprob.y[m] = prob->y[perm_q[j]];
				subprob.query[m] = prob->query[perm_q[j]];
				++m;
			}
		}
		for(q=end;q<nr_query;q++)
		{
			int *perm_q = &perm[start[query_perm[q]]];
			for (j=0;j<count[query_perm[q]];j++)
			{
				subprob.x[m] = prob->x[perm_q[j]];
				subprob.y[m] = prob->y[perm_q[j]];
				subprob.query[m] = prob->query[perm_q[j]];
				++m;
			}
		}
		svm_model *submodel = svm_train(&subprob,param);

		for(q=begin;q<end;q++)
		{
			int *perm_q = &perm[start[query_perm[q]]];
			for (j=0;j<count[query_perm[q]];j++)
				target[perm_q[j]] = svm_predict(submodel,prob->x[perm_q[j]]);
		}

		svm_free_and_destroy_model(&submodel);
		free(subprob.x);
		free(subprob.y);
		free(subprob.query);
	}
	eval_list(prob->y,target,query_set,l,result);
	free(fold_start);
	free(count);
	free(start);
	free(perm);
	free(target);
	free(query_perm);
	if (nr_query == 1)
		free(query_set);
}

int svm_get_svm_type(const svm_model *model)
{
	return model->param.svm_type;
}

int svm_get_nr_class(const svm_model *model)
{
	return model->nr_class;
}

void svm_get_sv_indices(const svm_model *model, int* indices)
{
	if (model->sv_indices != NULL)
		for(int i=0;i<model->l;i++)
			indices[i] = model->sv_indices[i];
}

int svm_get_nr_sv(const svm_model *model)
{
	return model->l;
}

double svm_predict_values(const svm_model *model, const svm_node *x, double* dec_values)
{
	int i;

	if(model->param.svm_type == L2R_RANK)
	{
		double *sv_coef = model->sv_coef[0];
		double sum = 0;
		for(i=0;i<model->l;i++)
			sum += sv_coef[i] * Kernel_Q::k_function(x,model->SV[i],model->param);
		*dec_values = sum;
		return sum;
	}
	else
		return 0;
}

double svm_predict(const svm_model *model, const svm_node *x)
{
	double *dec_values;
	dec_values = Malloc(double, 1);
	double pred_result = svm_predict_values(model, x, dec_values);
	free(dec_values);
	return pred_result;
}

static const char *svm_type_table[] =
{
	"l2r_rank",NULL
};

static const char *kernel_type_table[]=
{
	"linear","polynomial","rbf","precomputed",NULL
};

int svm_save_model(const char *model_file_name, const svm_model *model)
{
	FILE *fp = fopen(model_file_name,"w");
	if(fp==NULL) return -1;

	char *old_locale = strdup(setlocale(LC_ALL, NULL));
	setlocale(LC_ALL, "C");

	const svm_parameter& param = model->param;

	fprintf(fp,"svm_type %s\n", svm_type_table[param.svm_type]);
	fprintf(fp,"kernel_type %s\n", kernel_type_table[param.kernel_type]);

	if(param.kernel_type == POLY)
		fprintf(fp,"degree %d\n", param.degree);

	if(param.kernel_type == POLY || param.kernel_type == RBF)
		fprintf(fp,"gamma %g\n", param.gamma);

	if(param.kernel_type == POLY)
		fprintf(fp,"coef0 %g\n", param.coef0);

	int nr_class = model->nr_class;
	int l = model->l;
	fprintf(fp, "nr_class %d\n", nr_class);
	fprintf(fp, "total_sv %d\n",l);


	fprintf(fp, "SV\n");
	const double * const *sv_coef = model->sv_coef;
	const svm_node * const *SV = model->SV;

	for(int i=0;i<l;i++)
	{
		for(int j=0;j<nr_class-1;j++)
			fprintf(fp, "%.16g ",sv_coef[j][i]);

		const svm_node *p = SV[i];

		if(param.kernel_type == PRECOMPUTED)
			fprintf(fp,"0:%d ",(int)(p->value));
		else
			while(p->index != -1)
			{
				fprintf(fp,"%d:%.8g ",p->index,p->value);
				p++;
			}
		fprintf(fp, "\n");
	}

	setlocale(LC_ALL, old_locale);
	free(old_locale);

	if (ferror(fp) != 0 || fclose(fp) != 0) return -1;
	else return 0;
}

static char *line = NULL;
static int max_line_len;

static char* readline(FILE *input)
{
	int len;

	if(fgets(line,max_line_len,input) == NULL)
		return NULL;

	while(strrchr(line,'\n') == NULL)
	{
		max_line_len *= 2;
		line = (char *) realloc(line,max_line_len);
		len = (int) strlen(line);
		if(fgets(line+len,max_line_len-len,input) == NULL)
			break;
	}
	return line;
}

//
// FSCANF helps to handle fscanf failures.
// Its do-while block avoids the ambiguity when
// if (...)
//    FSCANF();
// is used
//
#define FSCANF(_stream, _format, _var) do{ if (fscanf(_stream, _format, _var) != 1) return false; }while(0)
bool read_model_header(FILE *fp, svm_model* model)
{
	svm_parameter& param = model->param;
	char cmd[81];
	while(1)
	{
		FSCANF(fp,"%80s",cmd);

		if(strcmp(cmd,"svm_type")==0)
		{
			FSCANF(fp,"%80s",cmd);
			int i;
			for(i=0;svm_type_table[i];i++)
			{
				if(strcmp(svm_type_table[i],cmd)==0)
				{
					param.svm_type=i;
					break;
				}
			}
			if(svm_type_table[i] == NULL)
			{
				fprintf(stderr,"unknown svm type.\n");
				return false;
			}
		}
		else if(strcmp(cmd,"kernel_type")==0)
		{		
			FSCANF(fp,"%80s",cmd);
			int i;
			for(i=0;kernel_type_table[i];i++)
			{
				if(strcmp(kernel_type_table[i],cmd)==0)
				{
					param.kernel_type=i;
					break;
				}
			}
			if(kernel_type_table[i] == NULL)
			{
				fprintf(stderr,"unknown kernel function.\n");	
				return false;
			}
		}
		else if(strcmp(cmd,"degree")==0)
			FSCANF(fp,"%d",&param.degree);
		else if(strcmp(cmd,"gamma")==0)
			FSCANF(fp,"%lf",&param.gamma);
		else if(strcmp(cmd,"coef0")==0)
			FSCANF(fp,"%lf",&param.coef0);
		else if(strcmp(cmd,"nr_class")==0)
			FSCANF(fp,"%d",&model->nr_class);
		else if(strcmp(cmd,"total_sv")==0)
			FSCANF(fp,"%d",&model->l);
		else if(strcmp(cmd,"SV")==0)
		{
			while(1)
			{
				int c = getc(fp);
				if(c==EOF || c=='\n') break;
			}
			break;
		}
		else
		{
			fprintf(stderr,"unknown text in model file: [%s]\n",cmd);
			return false;
		}
	}

	return true;

}

svm_model *svm_load_model(const char *model_file_name)
{
	FILE *fp = fopen(model_file_name,"rb");
	if(fp==NULL) return NULL;

	char *old_locale = strdup(setlocale(LC_ALL, NULL));
	setlocale(LC_ALL, "C");

	// read parameters

	svm_model *model = Malloc(svm_model,1);
	model->rho = NULL;
	model->sv_indices = NULL;
	
	// read header
	if (!read_model_header(fp, model))
	{
		fprintf(stderr, "ERROR: fscanf failed to read model\n");
		setlocale(LC_ALL, old_locale);
		free(old_locale);
		free(model->rho);
		free(model);
		return NULL;
	}
	
	// read sv_coef and SV

	int elements = 0;
	long pos = ftell(fp);

	max_line_len = 1024;
	line = Malloc(char,max_line_len);
	char *p,*endptr,*idx,*val;

	while(readline(fp)!=NULL)
	{
		p = strtok(line,":");
		while(1)
		{
			p = strtok(NULL,":");
			if(p == NULL)
				break;
			++elements;
		}
	}
	elements += model->l;

	fseek(fp,pos,SEEK_SET);

	int m = model->nr_class - 1;
	int l = model->l;
	model->sv_coef = Malloc(double *,m);
	int i;
	for(i=0;i<m;i++)
		model->sv_coef[i] = Malloc(double,l);
	model->SV = Malloc(svm_node*,l);
	svm_node *x_space = NULL;
	if(l>0) x_space = Malloc(svm_node,elements);

	int j=0;
	for(i=0;i<l;i++)
	{
		readline(fp);
		model->SV[i] = &x_space[j];

		p = strtok(line, " \t");
		model->sv_coef[0][i] = strtod(p,&endptr);
		for(int k=1;k<m;k++)
		{
			p = strtok(NULL, " \t");
			model->sv_coef[k][i] = strtod(p,&endptr);
		}

		while(1)
		{
			idx = strtok(NULL, ":");
			val = strtok(NULL, " \t");

			if(val == NULL)
				break;
			x_space[j].index = (int) strtol(idx,&endptr,10);
			x_space[j].value = strtod(val,&endptr);

			++j;
		}
		x_space[j++].index = -1;
	}
	free(line);

	setlocale(LC_ALL, old_locale);
	free(old_locale);

	if (ferror(fp) != 0 || fclose(fp) != 0)
		return NULL;

	model->free_sv = 1;	// XXX
	return model;
}

void svm_free_model_content(svm_model* model_ptr)
{
	if(model_ptr->free_sv && model_ptr->l > 0 && model_ptr->SV != NULL)
		free((void *)(model_ptr->SV[0]));
	if(model_ptr->sv_coef)
	{
		for(int i=0;i<model_ptr->nr_class-1;i++)
			free(model_ptr->sv_coef[i]);
	}

	free(model_ptr->SV);
	model_ptr->SV = NULL;

	free(model_ptr->sv_coef);
	model_ptr->sv_coef = NULL;

	free(model_ptr->sv_indices);
	model_ptr->sv_indices = NULL;

}

void svm_free_and_destroy_model(svm_model** model_ptr_ptr)
{
	if(model_ptr_ptr != NULL && *model_ptr_ptr != NULL)
	{
		svm_free_model_content(*model_ptr_ptr);
		free(*model_ptr_ptr);
		*model_ptr_ptr = NULL;
	}
}

const char *svm_check_parameter(const svm_problem *prob, const svm_parameter *param)
{
	// svm_type

	int svm_type = param->svm_type;
	if(svm_type != L2R_RANK)
		return "unknown svm type";

	// kernel_type, degree

	int kernel_type = param->kernel_type;
	if(kernel_type != LINEAR &&
			kernel_type != POLY &&
			kernel_type != RBF &&
			kernel_type != PRECOMPUTED)
		return "unknown kernel type";

	if(param->gamma < 0)
		return "gamma < 0";

	if(param->degree < 0)
		return "degree of polynomial kernel < 0";

	// cache_size,eps,C,nu,p,shrinking

	if(param->cache_size <= 0)
		return "cache_size <= 0";

	if(param->eps <= 0)
		return "eps <= 0";

	if(param->C <= 0)
		return "C <= 0";

	return NULL;
}

void svm_set_print_string_function(void (*print_func)(const char *))
{
	if(print_func == NULL)
		svm_print_string = &print_string_stdout;
	else
		svm_print_string = print_func;
}

void eval_list(double *label, double *target, int *query, int l, double *result_ret)
{
	int q,i,j,k;
	int nr_query;
	int *start = NULL;
	int *count = NULL;
	int *perm = Malloc(int, l);
	id_and_value *order_perm;
	long long totalnc = 0, totalnd = 0;
	long long nc = 0;
	long long nd = 0;
	double tmp;
	double accuracy = 0;
	int *l_plus;
	int *int_y;
	int same_y = 0;
	double *ideal_dcg;
	double *dcg;
	double meanndcg = 0;
	double ndcg;
	selectiontree *T;
	group_queries(query, l, &nr_query, &start, &count, perm);
	for (q=0;q<nr_query;q++)
	{
		//We use selection trees to compute pairwise accuracy
		nc = 0;
		nd = 0;
		l_plus = new int[count[q]];
		int_y = new int[count[q]];
		order_perm = new id_and_value[count[q]];
		int *perm_q = &perm[start[q]];
		for (i=0;i<count[q];i++)
		{
			order_perm[i].id = i;
			order_perm[i].value = label[perm_q[i]];
		}
		qsort(order_perm, count[q], sizeof(id_and_value), compare_id_and_value);
		int_y[order_perm[count[q]-1].id] = 1;
		same_y = 0;
		k = 1;
		for(i=count[q]-2;i>=0;i--)
		{
			if (order_perm[i].value != order_perm[i+1].value)
			{
				same_y = 0;
				k++;
			}
			else
				same_y++;
			int_y[order_perm[i].id] = k;
			nc += (count[q]-1 - i - same_y);
		}
		for (i=0;i<count[q];i++)
		{
			order_perm[i].id = i;
			order_perm[i].value = target[perm_q[i]];
		}
		qsort(order_perm, count[q], sizeof(id_and_value), compare_id_and_value);
		//total pairs
		T = new selectiontree(k);
		j = count[q] - 1;
		for (i=count[q] - 1;i>=0;i--)
		{
			while (j>=0 && ( order_perm[j].value < order_perm[i].value))
			{
				T->insert_node(int_y[order_perm[j].id], tmp);
				j--;
			}
			T->larger(int_y[order_perm[i].id], &l_plus[order_perm[i].id], &tmp);
		}
		delete T;

		for (i=0;i<count[q];i++)
			nd += l_plus[i];
		nc -= nd;
		if (nc != 0 || nd != 0)
			accuracy += double(nc)/double(nc+nd);
		totalnc += nc;
		totalnd += nd;
		delete[] l_plus;
		delete[] int_y;
		delete[] order_perm;
	}
	result_ret[0] = (double)totalnc/(double)(totalnc+totalnd);
	for (q=0;q<nr_query;q++)
	{
		ideal_dcg = new double[count[q]];
		dcg = new double[count[q]];
		ndcg = 0;
		order_perm = new id_and_value[count[q]];
		int *perm_q = &perm[start[q]];
		for (i=0;i<count[q];i++)
		{
			order_perm[i].id = perm_q[i];
			order_perm[i].value = label[perm_q[i]];
		}
		qsort(order_perm, count[q], sizeof(id_and_value), compare_id_and_value);
		ideal_dcg[0] = pow(2.0,order_perm[0].value) - 1;
		for (i=1;i<count[q];i++)
			ideal_dcg[i] = ideal_dcg[i-1] + (pow(2.0,order_perm[i].value) - 1) * log(2.0) / log(i+1.0);
		for (i=0;i<count[q];i++)
		{
			order_perm[i].id = perm_q[i];
			order_perm[i].value = target[perm_q[i]];
		}
		qsort(order_perm, count[q], sizeof(id_and_value), compare_id_and_value);
		dcg[0] = pow(2.0, label[order_perm[0].id]) - 1;
		for (i=1;i<count[q];i++)
			dcg[i] = dcg[i-1] + (pow(2.0, label[order_perm[i].id]) - 1) * log(2.0) / log(i + 1.0);
		if (ideal_dcg[0]>0)
			for (i=0;i<count[q];i++)
				ndcg += dcg[i]/ideal_dcg[i];
		else
			ndcg = 0;
		meanndcg += ndcg/count[q];
		delete[] order_perm;
		delete[] ideal_dcg;
		delete[] dcg;
	}
	meanndcg /= nr_query;
	result_ret[1] = meanndcg;
	free(start);
	free(count);
	free(perm);
}
