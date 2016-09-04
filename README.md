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

Preparations to run our code:

1. This is a maven project, to run our code, please include all necessary jars in the project lib path.
   If you can not download these jars, you can directly include the jars in the folder target/lib except for the jar "stanford-english-corenlp-2016-01-10-models.jar".
   Becaue this jar is too large to be uploaded to the github. But You can download this jar from the url "http://nlp.stanford.edu/software/stanford-english-corenlp-2016-01-10-models.jar".

2. Our project needs to use the virtuoso engine to query the Freebase.
   Thanks the contribution of Siva, the freebase version now can be downloaded from "https://www.dropbox.com/sh/zxv2mos2ujjyxnu/AAACCR4AJ1MMTCe8ElfBN39Ha?dl=0".
   Note that, you should change the file path of "DatabaseFile,ErrorLogFile,LockFile,TransactionFile,xa_persistent_file,DatabaseFile,TransactionFile" by
   replacing the directory with the directory you used. If you have any problems in using this tool, please contact me.

3. Before runing our code, you should start the virtuoso engine at port 1111.

Instructions to run our code:

1. To reproduce our results, there are two main steps, i.e., KB-based joint inference and Wiki-based inference.
   You should perform the inferences in the following pipeline literature.

   (a) Perform the Freebase based joint inference by executing Joint_EL_RE/Test.java.
   (b) Perform the wikipedia based inference by executing InferenceOverWiki/Test.java.

Please contact Kun Xu (xukun@pku.edu.cn) if you have any question.
-- Kun Xu, July 27th, 2016.
