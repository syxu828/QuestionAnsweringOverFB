package InferenceOverWiki;

import Util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by miaomiao on 16/7/2.
 */
public class Test {

    String inputPath = "resources/JointInference/Test/wikiInference.input";
    String qa_file_path = "resources/WikiInference/Test/qa_pair";
    String feature_output_path = "resources/WikiInference/Test/feature_output";
    String outputPath = "resources/WikiInference/Test/wikiInference.output";
    String FbWikiIdMappingFilePath = "resources/WikiInference/fbId_wikiId.mapping";

    HashMap<String, Integer> gram_index;
    HashMap<String, List<String>> fbId_wikiId_map;

    public Test(){
        fbId_wikiId_map = new HashMap<String, List<String>>();

        loadFbWikiMapping();

        gram_index = new HashMap<String, Integer>();
        List<String> twoGrams = FileUtil.readFile("resources/WikiInference/2grams.txt");
        int gindex = 1;
        for(String line : twoGrams){
            gram_index.put(line, gindex);
            gindex++;
        }
    }

    public void loadFbWikiMapping(){
        List<String> lines = FileUtil.readFile(this.FbWikiIdMappingFilePath);
        for(String line:lines){
            String[] info = line.split("\t");
            String fbId = info[0];
            String[] temp = info[1].replace("\"", "").replace("[", "").replace("]", "").split(",");
            ArrayList<String> wikiIds = new ArrayList<String>();
            for(String s:temp){
                wikiIds.add(s);
            }
            fbId_wikiId_map.put(fbId, wikiIds);
        }
    }

    public HashMap<String, List<String>> loadKBInferedResult(){
        HashMap<String, List<String>> question_KBInfered = new HashMap<String, List<String>>();
        List<String> lines = FileUtil.readFile(inputPath);
        JSONParser parser = new JSONParser();
        try{
            for (String line : lines) {
                JSONObject jo = (JSONObject) parser.parse(line);
                String question = jo.get("question").toString();
                List<String> answers = (List<String>) jo.get("answers");
                question_KBInfered.put(question, answers);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return question_KBInfered;
        }
    }

    public void begin(){
        List<String> twoGrams = FileUtil.readFile("resources/WikiInference/2grams.txt");
        HashMap<String, Integer> gram_index = new HashMap<String, Integer>();
        int gindex = 1;
        for(String line : twoGrams){
            gram_index.put(line, gindex);
            gindex++;
        }

        JSONParser parser = new JSONParser();
        try {
            List<String> lines = FileUtil.readFile(inputPath);
            List<String> qa_pair = new ArrayList<String>();
            List<String> feature_outputs = new ArrayList<String>();
            for (String line : lines) {
                JSONObject jo = (JSONObject) parser.parse(line);
                String question = jo.get("question").toString();
                List<String> topics = (List<String>) jo.get("topics");
                List<String> answers = (List<String>) jo.get("answers");

                for (String topic : topics) {
                    List<String> wikiIds = fbId_wikiId_map.get(topic);
                    if( wikiIds == null ){
                        continue;
                    }
                    else{
                        for (String wikiId : wikiIds) {
                            HashMap<String, List<String>> ans_evidences = ExtractorUtil.instance.extractEvidences(wikiId, answers);
                            for(String ans:ans_evidences.keySet()){
                                List<String> evidences = ans_evidences.get(ans);
                                for(String evidence:evidences){
                                    String featureVector = toFeatureVector(question, evidence);
                                    feature_outputs.add(featureVector);
                                    qa_pair.add(question+"\t"+ans);
                                }
                            }
                        }
                    }
                }
            }
            FileUtil.writeFile(qa_pair, qa_file_path);
            FileUtil.writeFile(feature_outputs, feature_output_path);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String toFeatureVector(String question, String evidence){
        HashSet<String> grams = ExtractorUtil.instance.extract2Grams(question, evidence);
        List<Integer> indexes = new ArrayList<Integer>();
        for (String gram : grams) {
            if( !gram_index.containsKey(gram) ){
                continue;
            }
            indexes.add(gram_index.get(gram));
        }
        Collections.sort(indexes, new Comparator<Integer>(){
            public int compare(Integer i1, Integer i2){
                if( i2 > i1 )
                    return -1;
                else if( i1 > i2 )
                    return 1;
                else
                    return 0;
            }
        });
        StringBuffer buffer = new StringBuffer();
        buffer.append("0 ");
        for(int temp : indexes){
            buffer.append(temp + ":1 ");
        }

        return buffer.toString().trim();
    }

    public void merge(){

        List<String> goldQuestions = new ArrayList<String>();
        List<String> lines = FileUtil.readFile("resources/WikiInference/Test/webquestions-predictions-gold.txt");
        for(String line:lines){
            String[] info = line.split("\t");
            goldQuestions.add(info[0]);
        }

        List<String> qa_pair = FileUtil.readFile("resources/WikiInference/Test/qa_pair");
        List<String> predicted = FileUtil.readFile("resources/WikiInference/Test/svm.predicted");
        HashMap<String, Set<String>> question_wikiInfered = new HashMap<String, Set<String>>();
        HashMap<String, Set<String>> question_foundEvi = new HashMap<String, Set<String>>();
        for(int i = 0; i < predicted.size(); i++){
            Double value = Double.parseDouble(predicted.get(i));
            String[] info = qa_pair.get(i).split("\t");
            String question = info[0];
            String ans = info[1];

            if( value >= 0.6 ) {
                if (!question_wikiInfered.containsKey(question)) {
                    question_wikiInfered.put(question, new HashSet<String>());
                }
                Set<String> answers = question_wikiInfered.get(question);
                answers.add(ans);
            }

            if (!question_foundEvi.containsKey(question)) {
                question_foundEvi.put(question, new HashSet<String>());
            }
            Set<String> answers = question_foundEvi.get(question);
            answers.add(ans);
        }

        HashMap<String, List<String>> question_finalResult = new HashMap<String, List<String>>();
        HashMap<String, List<String>> question_kbInfered = loadKBInferedResult();
        for(String question:question_kbInfered.keySet()){
            List<String> kbInfered_result = question_kbInfered.get(question);

            if( question_wikiInfered.containsKey(question) ){
                List<String> wikiInfered_result = new ArrayList<String>(question_wikiInfered.get(question));

                if( wikiInfered_result.size() == 0 ){
                    question_finalResult.put(question, kbInfered_result);
                }
                else{
                    question_finalResult.put(question, new ArrayList<String>(question_wikiInfered.get(question)));

                    double wiki_result_size = wikiInfered_result.size();
                    double kb_result_size = kbInfered_result.size();

                    if( wiki_result_size / kb_result_size >= 0.5 ){
                        question_finalResult.put(question, kbInfered_result);
                    }
                }
            }else{
                if( question_foundEvi.containsKey(question) ){
                    for(String s:question_foundEvi.get(question)){
                        kbInfered_result.remove(s);
                    }
                }

                question_finalResult.put(question, kbInfered_result);
            }
            goldQuestions.remove(question);
        }

        List<String> outputs = new ArrayList<String>();
        for(String question:question_finalResult.keySet()){
            JSONArray array = new JSONArray();
            List<String> result = question_finalResult.get(question);
            for(String s:result){
                array.add(s);
            }
            outputs.add(question+"\t"+array.toString().replace("\\/", "/"));
        }

        for(String question:goldQuestions){
            outputs.add(question+"\t[]");
        }
        FileUtil.writeFile(outputs, outputPath);
    }

    public void rewriteAns(){
        List<String> lines = FileUtil.readFile(outputPath);
        List<String> outputs = new ArrayList<String>();
        JSONParser parser = new JSONParser();
        try {
            for (String line : lines) {
                String[] info = line.split("\t");
                String question = info[0];
                JSONArray array = (JSONArray) parser.parse(info[1]);
                JSONArray newArray = new JSONArray();
                for(int i = 0; i < array.size(); i++){
                    String answer = array.get(i).toString();
                    String[] parts = answer.split("-");
                    if( answer.length() == 10 && answer.charAt(4)=='-' && answer.charAt(7)=='-' && parts.length == 3){
                        StringBuffer buffer = new StringBuffer();
                        if( parts[1].startsWith("0") )
                            buffer.append(parts[1].substring(1));
                        else
                            buffer.append(parts[1]);

                        buffer.append("/");

                        if( parts[2].startsWith("0") )
                            buffer.append(parts[2].substring(1));
                        else
                            buffer.append(parts[2]);

                        buffer.append("/");
                        buffer.append(parts[0]);
                        newArray.add(buffer.toString());
                    }
                    else
                        newArray.add(array.get(i));
                }
                outputs.add(question+"\t"+newArray.toString().replace("\\/", "/"));
            }

            FileUtil.writeFile(outputs, outputPath+".rewrited");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Test test = new Test();

//        test.begin();

//      then utilize the regression model to predict for the test data
        test.merge();
        test.rewriteAns();
    }
}
