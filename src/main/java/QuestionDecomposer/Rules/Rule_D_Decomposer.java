package QuestionDecomposer.Rules;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by miaomiao on 16/6/15.
 */
public class Rule_D_Decomposer extends RuleBasedDecomposer{

     /*
      * the rule is described in appendix Figure 3 (d)
     */

    @Override
    public List<String> decompose(String question) {

        List<String> result = new ArrayList<String>();

        List<String> verb_POS = Arrays.asList(new String[]{"VB"});
        List<String> prep_POS = Arrays.asList(new String[]{"PP"});
        List<String> np_POS = Arrays.asList(new String[]{"NP"});

        Tree root = this.synParse(question);
        Tree orginalRoot = root;
        List<Tree> nodes = new ArrayList<Tree>();
        nodes.add(root);

        while( nodes.size() > 0 ) {
            root = nodes.remove(0);
            nodes.addAll(root.getChildrenAsList());
            if( verb_POS.contains(root.label().toString()) ){
                List<Tree> sliblings = root.parent(orginalRoot).getChildrenAsList();

                if( sliblings.size() != 2 ){
                    continue;
                }

                //the PP part which should have a child that has the tag NP
                Tree PP_node = sliblings.get(1);
                if( !prep_POS.contains(PP_node.label().toString()) )
                    continue;

                if( !checkChildrenPOS( PP_node.getChildrenAsList(), np_POS) )
                    continue;

                String sub_question_1 = question.replace(this.obtainSpan(PP_node), "");
                result.add(sub_question_1);

                Tree grandpa_node = root.parent(orginalRoot).parent(orginalRoot);
                List<Tree> children = grandpa_node.getChildrenAsList();
                int index = children.indexOf(root.parent(orginalRoot));
                String sub_question_2 = question;
                for(int i = 0; i < index; i++){
                    if( np_POS.contains(children.get(i).label().toString()) ){
                        sub_question_2 = sub_question_2.replace(this.obtainSpan(children.get(i)), "");
                    }
                }
                result.add(sub_question_2.replace("  ", " "));
            }
        }

        return result;
    }

    public static void main(String[] args) {
        Rule_D_Decomposer composer = new Rule_D_Decomposer();
        System.err.println(composer.decompose("what character did john noble play in lord of the rings"));
    }
}
