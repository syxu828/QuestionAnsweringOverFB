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


## Before installation

Let's setup Freebase server
1. Install virtuso. See http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSUbuntuNotes
2. Download our Freebase version at https://www.dropbox.com/sh/zxv2mos2ujjyxnu/AAACCR4AJ1MMTCe8ElfBN39Ha?dl=0
3. cd to the folder which you just downloaded
4. Run pwd
5. Replace /dev/shm/vdb/ in virtuoso.ini with the output of Step 4.
6. Run "virtuoso-t -f"

## Installation

Run the following commands for installation
> git clone https://github.com/sivareddyg/QuestionAnsweringOverFB.git
> sh install.sh

## Replicating experiments in the paper

To reproduce our results, there are two main steps, i.e., KB-based joint inference and Wiki-based inference.
You should perform the inferences in the following order

1. Perform the Freebase based joint inference.
> java -cp target/classes:target/lib/* Joint_EL_RE/Test
This will write the output in resources/JointInference/Test/joint_inference.predicted.final

2. Perform the wikipedia based inference by executing InferenceOverWiki/Test.java.
> java -cp target/classes:target/lib/* InferenceOverWiki/Test
This will write the output in resources/WikiInference/Test/predicted.8_30

### To train your own models:

The following command will split questions to subquestions and store them in resources/JointInference/Train/train.data. It also creates a feature file at resources/RE/param/params.69 required to train SVMRank model for relation prediction.
> java -cp target/classes:target/lib/* Joint_EL_RE/Train

You can train SVMRank using the features to create resources/tool/libsvm-ranksvm-3.20/svm.model required by Joint_EL_RE/Test. [TODO:add the commands]
