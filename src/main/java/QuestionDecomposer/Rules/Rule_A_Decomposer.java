package QuestionDecomposer.Rules;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kun on 16/6/14.
 */
public class Rule_A_Decomposer extends RuleBasedDecomposer{

    /*
    * the rule is described in appendix Figure 3 (a)
     */

    @Override
    public List<String> decompose(String question) {

        List<String> result = new ArrayList<String>();

        Tree root = this.synParse(question);
        Tree orginalRoot = root;
        List<Tree> nodes = new ArrayList<Tree>();
        nodes.add(root);

        List<String> verb_POS = Arrays.asList(new String[]{"VBZ", "VBD", "VB"});
        List<String> prep_POS = Arrays.asList(new String[]{"PP"});
        List<String> noun_POS = Arrays.asList(new String[]{"NP"});

        while( nodes.size() > 0 ){
            root = nodes.remove(0);
            nodes.addAll(root.getChildrenAsList());
            //match the verb
            if( verb_POS.contains(root.label().toString()) ){
                List<Tree> sliblings = root.parent(orginalRoot).getChildrenAsList();

                if( sliblings.size() != 3 )
                    continue;

                // the NP
                String label_1 = sliblings.get(1).label().toString();

                //the PP, should contain at least one NP
                Tree PP_node = sliblings.get(2);
                String label_2 = PP_node.label().toString();

                List<Tree> child_of_PP = PP_node.getChildrenAsList();
                if( child_of_PP.size() == 1 )
                    continue;

                if( sliblings.size() == 3 && noun_POS.contains(label_1) && prep_POS.contains(label_2)){
                    String span = this.obtainSpan(root.parent(orginalRoot));

                    if( question.indexOf(span) == -1 ){
                        // this exception is due to some special character such as 's
                        question = question.replace("'s", " 's");
                    }

                    String prev = question.substring(0, question.indexOf(span));

                    String sub_question_1 = prev.trim()+" "+this.obtainSpan(root)+" "+this.obtainSpan(sliblings.get(1));
                    result.add(sub_question_1);

                    String sub_question_2 = prev.trim()+" "+this.obtainSpan(root)+" "+this.obtainSpan(sliblings.get(2));
                    result.add(sub_question_2);
                }
            }

        }

        return result;
    }

    public static void main(String[] args){
        Rule_A_Decomposer composer = new Rule_A_Decomposer();
        composer.decompose("what character did john noble play in lord of the rings");
//        List<String> lines = FileUtil.readFile("resources/Train/train.questions");
//        for(String line : lines){
//            List<String> subQuestions = composer.decompose(line);
//            if( subQuestions.size() > 1 ){
//                System.err.println(line+"\t"+subQuestions);
//            }
//        }
    }
}
