wget http://nlp.stanford.edu/software/stanford-english-corenlp-2016-01-10-models.jar
mv stanford-english-corenlp-2016-01-10-models.jar target/lib
ant clean
ant
cd resources/tool/libsvm-ranksvm-3.20/ && make clean && make 
