package QuestionDecomposer;

import QuestionDecomposer.Rules.RuleBasedDecomposer;
import QuestionDecomposer.Rules.Rule_A_Decomposer;
import QuestionDecomposer.Rules.Rule_C_Decomposer;
import QuestionDecomposer.Rules.Rule_D_Decomposer;
import Util.FileUtil;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miaomiao on 16/6/13.
 */
public class Decomposer {

    public static Decomposer instance;
    public List<RuleBasedDecomposer> decomposerList;

    static {
        instance = new Decomposer();
    }

    Decomposer(){
        decomposerList = new ArrayList<RuleBasedDecomposer>();
        decomposerList.add(new Rule_A_Decomposer());
        decomposerList.add(new Rule_C_Decomposer());
        decomposerList.add(new Rule_D_Decomposer());
    }

    public void test(String questionFilePath, String decomposedFilePath){
        List<String> lines = FileUtil.readFile(questionFilePath);
        List<String> outputs = new ArrayList<String>();
        int decomposed_size = 0;
        int single_question = 0;
        for(String line : lines){
            List<String> subQuestions = new ArrayList<String>();
            for(RuleBasedDecomposer decomposer:decomposerList){
                subQuestions.addAll(decomposer.decompose(line));
            }

            if( subQuestions.size() == 0 ) {
                single_question++;
                subQuestions.add(line);
            }

            JSONObject jo = new JSONObject();
            jo.put("question", line);
            jo.put("decomposed", subQuestions);
            outputs.add(jo.toJSONString());
            decomposed_size += subQuestions.size();
        }
        FileUtil.writeFile(outputs, decomposedFilePath);
        System.err.println("there are "+lines.size()+" questions, and "+single_question+" simple questions, for the left," +
                "our system producing "+decomposed_size+" sub-questions");
    }

    public static void main(String[] args){
//        Decomposer.instance.test("resources/Train/train.questions", "resources/Train/train.questions.decomposed");
//        Decomposer.instance.test("resources/Test/test.questions", "resources/Test/test.questions.decomposed");
    }
}
