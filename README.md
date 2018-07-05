# QuestionAnsweringOverFB

For details, please read our paper:
```
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
```
## Preparations to run our code

1. This is a maven project. To run our code, please include all necessary jars in the project lib path.
   If you can not download these jars, you can directly include the jars in the folder `target/lib` 
   except for `stanford-english-corenlp-2016-01-10-models.jar`, becaue this jar is too large to be 
   uploaded to the github. But you can download this jar from 
   <http://nlp.stanford.edu/software/stanford-english-corenlp-2016-01-10-models.jar>.

2. Our project uses the OpenLink Virtuoso engine to query the Freebase data.
   Thanks to the contribution of Siva, the ~60GB DB now can be downloaded [from our Dropbox](https://www.dropbox.com/sh/zxv2mos2ujjyxnu/AAACCR4AJ1MMTCe8ElfBN39Ha?dl=0).
   Note that you should change the file paths of `DatabaseFile`, `ErrorLogFile`, 
   `LockFile`, `TransactionFile`, `xa_persistent_file`, `DatabaseFile`, `TransactionFile` to
   the directory to which you downloaded these files. If you have any problems in using this 
   tool, please contact me, Kun Xu (@syxu828, xukun@pku.edu.cn).  
   
   _*Note* -- these files were 
   produced with Virtuoso 6.1, without consulting OpenLink Software, and some extra steps 
   may be needed to use these files with Virtuoso 7.x, 8.x, or later._

3. Before running our code, you should start the Virtuoso engine at port `1111`.  

## Instructions to run our code

To reproduce our results, there are two main steps: KB-based joint inference and Wiki-based inference.
You should perform the inferences in the following pipeline literature.

1. Perform the Freebase based joint inference by executing `Joint_EL_RE/Test.java`.
2. Perform the wikipedia based inference by executing `InferenceOverWiki/Test.java`.

Please contact Kun Xu (xukun@pku.edu.cn) if you have any question.

-- Kun Xu, 2017-07-27.
