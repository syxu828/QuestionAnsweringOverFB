CC = gcc
CXX ?= g++
CFLAGS = -Wall -Wconversion -O3 -fPIC

ifneq ($(NO_OPENMP), 1)
	OMPFLAGS += -fopenmp
endif

MKL_ROOT =
MKL_ARCH = "INTEL64"
MKLLIBS= mkl_intel_thread mkl_core mkl_intel_ilp64 mkl_core mkl_gnu_thread mkl_rt iomp5

ifeq ($(MKL_ARCH), "INTEL64")
	MKL_ARCH_NAME = intel64
else ifeq ($(MKL_ARCH), "IA32")
	MKL_ARCH_NAME = ia32
else ifeq ($(MKL_ARCH), "MIC")
	MKL_ARCH_NAME = mic
else
	$(error Unknown Intel architecture $(MKL_ARCH))
endif

ifeq ($(USE_MKL), 1)
	CFLAGS += -I$(MKL_ROOT)/mkl/include/
	MKLLIBFLAGS += -L$(MKL_ROOT)/mkl/lib/$(MKL_ARCH_NAME)/
	MKLLIBFLAGS += -L$(MKL_ROOT)/compiler/lib/$(MKL_ARCH_NAME)/
	MKLLIBFLAGS += $(addprefix -l,$(MKLLIBS))
else
	LIBS = blas/blas.a
endif

all: svm-train svm-predict svm-scale

svm-predict: svm-predict.c svm.o tron.o selectiontree.o $(LIBS)
	$(CXX) $(CFLAGS) $(OMPFLAGS) $+ -o svm-predict $(MKLLIBFLAGS)
svm-train: svm-train.c svm.o tron.o selectiontree.o $(LIBS)
	$(CXX) $(CFLAGS) $(OMPFLAGS) $+ -o svm-train $(MKLLIBFLAGS)
svm-scale: svm-scale.c
	$(CXX) $(CFLAGS) $+ -o svm-scale
svm.o: svm.cpp svm.h
	$(CXX) $(CFLAGS) $(OMPFLAGS) -c svm.cpp
tron.o: tron.cpp tron.h
	$(CXX) $(CFLAGS) -c -o tron.o tron.cpp
selectiontree.o: selectiontree.cpp selectiontree.h
	$(CXX) $(CFLAGS) -c -o selectiontree.o selectiontree.cpp
blas/blas.a: blas/*.c blas/*.h
	make -C blas OPTFLAGS='$(CFLAGS)' CC='$(CC)';
clean:
	make -C blas clean
	rm -f *~ *.o svm-train svm-predict svm-scale
