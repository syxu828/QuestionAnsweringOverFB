struct tree_node
{
	int size;
	double xv;
};

class selectiontree
{
public:
	selectiontree(int k); //k leaves
	~selectiontree();
	void insert_node(int key, double value);
	void larger(int key, int *l_plus_ret, double *alpha_plus_ret);
	void smaller(int key, int *l_minus_ret, double *alpha_minus_ret);
	double xv_larger(int key);
	double xv_smaller(int key);

private:
	int num_nonleaves;
	int num_leaves;
	tree_node *node;
};

