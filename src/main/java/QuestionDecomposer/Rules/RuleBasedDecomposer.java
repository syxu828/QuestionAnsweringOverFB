package QuestionDecomposer.Rules;

import Util.NLPUtil;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.Tree;

import java.util.List;

/**
 * Created by miaomiao on 16/6/14.
 */
public abstract class RuleBasedDecomposer {
    public abstract List<String> decompose(String question);

    public Tree synParse(String question){
        Tree tree = NLPUtil.parser_instance.extractSyntacticTree(question);
        return tree;
    }

    public String obtainSpan(Tree root){
        StringBuffer span = new StringBuffer();
        List<Word> words = root.yieldWords();
        for(Word word : words){
            span.append(word.word()+" ");
        }
        return span.toString().trim();
    }

    public boolean checkChildrenPOS(List<Tree> children, List<String> POS_Patterns){
        for(Tree child:children){
            String label = child.label().toString();
            for(String pattern:POS_Patterns){
                if( label.equals(pattern) ){
                    return true;
                }
            }
        }
        return false;
    }
}
