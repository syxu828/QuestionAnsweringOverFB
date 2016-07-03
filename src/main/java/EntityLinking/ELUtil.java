package EntityLinking;

import Util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by miaomiao on 16/6/17.
 */
public class ELUtil {

    public static void convertSTAGGFile(String questionMapFilePath, String filterFilePath, String elResultPath){
        List<String> lines = FileUtil.readFile(questionMapFilePath);
        HashMap<String, String> index_question_map = new HashMap<String, String>();
        for(String line:lines){
            String[] info = line.split("\t");
            String index = info[0];
            String question = info[1];
            index_question_map.put(index, question);
        }

        lines = FileUtil.readFile(filterFilePath);
        HashMap<String, HashMap<String, List<String>>> map = new HashMap<String, HashMap<String, List<String>>>();
        for(String line:lines){
            String[] info = line.split("\t");
            String index = info[0];
            String question = index_question_map.get(index);
            String mention = info[1];
            String id = info[4].substring(1).replace("/", ".");
            double score = Double.parseDouble(info[6]);
            if( !map.containsKey(question) )
                map.put(question, new HashMap<String, List<String>>());
            HashMap<String, List<String>> mention_idList = map.get(question);
            if( !mention_idList.containsKey(mention) )
                mention_idList.put(mention, new ArrayList<String>());
            List<String> ids = mention_idList.get(mention);
            ids.add(id+"&"+score);
        }

        List<String> outputs = new ArrayList<String>();
        for(String question:map.keySet()){
            JSONObject jo = new JSONObject();
            jo.put("question", question);
            JSONArray array = new JSONArray();
            for(String mention:map.get(question).keySet()){
                JSONObject mention_cands = new JSONObject();
                mention_cands.put(mention, map.get(question).get(mention));
                array.add(mention_cands);
            }
            jo.put("mentions", array);
            outputs.add(jo.toJSONString());
        }

        FileUtil.writeFile(outputs, elResultPath);
    }

    public static void main(String[] args){
//        ELUtil.convertSTAGGFile("resources/STAGG/webquestions.examples.train.questionmap.tsv","resources/STAGG/webquestions.examples.train.e2e.top10.filter.tsv", "resources/Train/train.EL.results");
//        ELUtil.convertSTAGGFile("resources/STAGG/webquestions.examples.test.questionmap.tsv", "resources/STAGG/webquestions.examples.test.e2e.top10.filter.tsv", "resources/Test/test.EL.results");
    }
}
