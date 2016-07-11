# QuestionAnsweringOverFB

For details, please read our paper:

Kun Xu, Siva Reddy, Yansong Feng, Songfang Huang and Dongyan Zhao.
Question Answering on Freebase via Relation Extraction and Textual Evidence.
In Proceedings of ACL-2016.

@inproceedings{kun_question_2016,
  author    = {Kun Xu and
               Siva Reddy and
               Yansong Feng and
               Songfang Huang and
               Dongyan Zhao},
  title = {{Question Answering on Freebase via Relation Extraction and Textual Evidence}},
  booktitle={Proceedings of the Association for Computational Linguistics (ACL 2016)},
  month     = {August},
  year      = {2016},
  address   = {Berlin, Germany},
  publisher = {Association for Computational Linguistics},
  url = {http://sivareddy.in/papers/kun2016question.pdf},
}

Instructions

1. This is a maven project, to run our code, please download all necessary jars before using our code to reproduce the results.
   After downloading all jars, please include them in the project lib path.

2. Our project needs to use the virtuoso engine to query the Freebase. Detail of loading Freebase dump
   can be found in the url "http://sivareddy.in/load-freebase-dump-into-virtuoso-sparql-sql".
   
3. To reproduce our results, there are two main steps, i.e., KB-based joint inference and Wiki-based inference, in a pipeline manner.
   Specifically, firstly, run Joint_EL_RE/Test.java to perform the KB-based joint inference.
   Secondly, run InferenceOverWiki/Test.java to perform the Wiki-based inference.

Please contact Kun Xu (xukun@pku.edu.cn) if you have any question.
-- Kun Xu, July 8th, 2016.
