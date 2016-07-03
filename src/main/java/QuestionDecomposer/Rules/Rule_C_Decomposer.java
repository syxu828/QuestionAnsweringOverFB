package QuestionDecomposer.Rules;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by miaomiao on 16/6/14.
 */
public class Rule_C_Decomposer extends RuleBasedDecomposer {

      /*
      * the rule is described in appendix Figure 3 (c)
     */
    @Override
    public List<String> decompose(String quetion) {

        List<String> result = new ArrayList<String>();

        Tree root = this.synParse(quetion);
        Tree orginalRoot = root;
        List<Tree> nodes = new ArrayList<Tree>();
        nodes.add(root);

        List<String> clause_POS = Arrays.asList(new String[]{"SBARQ"});
        List<String> and_POS = Arrays.asList(new String[]{"CC"});

        while( nodes.size() > 0 ) {
            root = nodes.remove(0);
            nodes.addAll(root.getChildrenAsList());

            //SBARQ + and + SBARQ

            //match the and
            if( and_POS.contains(root.label().toString()) ){
                List<Tree> sliblings = root.parent(orginalRoot).getChildrenAsList();
                if( sliblings.size() != 3 )
                    continue;
                // the first SBARQ
                String label_1 = sliblings.get(0).label().toString();
                // the first SBARQ
                String label_2 = sliblings.get(2).label().toString();

                if( sliblings.size() == 3 && clause_POS.contains(label_1) && clause_POS.contains(label_2)){
                    String sub_question_1 = this.obtainSpan(sliblings.get(0));
                    String sub_question_2 = this.obtainSpan(sliblings.get(2));
//                    System.err.println(sub_question_1+"\n"+sub_question_2);
                    result.add(sub_question_1);
                    result.add(sub_question_2);
                }
            }
        }

        return result;
    }

    public static void main(String[] args){
        Rule_C_Decomposer composer = new Rule_C_Decomposer();
        System.err.println(composer.decompose("who were demeter's brothers and sisters?"));
//        List<String> lines = FileUtil.readFile("resources/Train/train.questions");
//        for(String line : lines){
//            List<String> subQuestions = composer.decompose(line);
//            if( subQuestions.size() > 1 ){
//                System.err.println(line);
//            }
//        }

    }

}
