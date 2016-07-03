#include "selectiontree.h"
selectiontree::selectiontree(int k)
{
	int i = 1;
	int j;
	while(i < k)
		i *= 2;
	this->num_leaves = k;
	this->num_nonleaves = i-1;
	node = new tree_node[i+k];
	for (j=0;j<i+k;j++)
	{
		node[j].size = 0;
		node[j].xv = 0;
	}
}

selectiontree::~selectiontree()
{
	delete[] node;
}

void selectiontree::insert_node(int key, double value)
{
	key += this->num_nonleaves;
	for (;key>0;key/=2)
	{
		node[key].xv += value;
		node[key].size++;
	}
}

void selectiontree::larger(int key, int *l_plus_ret, double *alpha_plus_ret)
{
	if (key >= this->num_leaves)
	{
		*l_plus_ret = 0;
		*alpha_plus_ret = 0;
		return;
	}
	int l_plus = 0;
	double alpha_plus = 0;
	key += num_nonleaves;
	for (;key>1;key/=2)
		if (key % 2 == 0)
		{
			l_plus += node[key+1].size;
			alpha_plus += node[key+1].xv;
		}
	*l_plus_ret = l_plus;
	*alpha_plus_ret = alpha_plus;
}

void selectiontree::smaller(int key, int *l_minus_ret, double *alpha_minus_ret)
{
	if (key <= 1)
	{
		*l_minus_ret = 0;
		*alpha_minus_ret = 0;
		return;
	}
	int l_minus = 0;
	double alpha_minus = 0;
	key += num_nonleaves;
	for (;key>1;key/=2)
		if (key % 2 == 1)
		{
			l_minus += node[key-1].size;
			alpha_minus += node[key-1].xv;
		}
	*l_minus_ret = l_minus;
	*alpha_minus_ret = alpha_minus;
}

double selectiontree::xv_smaller(int key)
{
	if (key <= 1)
		return 0;
	double alpha_minus = 0;
	key += num_nonleaves;
	for (;key>1;key/=2)
		if (key % 2 == 1)
			alpha_minus += node[key-1].xv;
	return alpha_minus;
}

double selectiontree::xv_larger(int key)
{
	if (key >= this->num_leaves)
		return 0;
	double alpha_plus = 0;
	key += num_nonleaves;
	for (;key>1;key/=2)
		if (key % 2 == 0)
			alpha_plus += node[key+1].xv;
	return alpha_plus;
}
