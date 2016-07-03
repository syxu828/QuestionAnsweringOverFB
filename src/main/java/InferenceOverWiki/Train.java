package InferenceOverWiki;

import Util.FileUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by miaomiao on 16/7/2.
 */
public class Train {

    String[] inputPath = {"resources/JointInference/Train/wikiInference.input"};
    String outputPath = "resources/WikiInference/Train/train.data";
    String[] goldFilePaths = {"resources/WebQuestions/train.data", "resources/WebQuestions/dev.data", "resources/WebQuestions/test.data"};
    String FbWikiIdMappingFilePath = "resources/WikiInference/fbId_wikiId.mapping";

    String svmTrainingDataFilePath = "resources/WikiInference/Train/svm.train.data";

    HashMap<String, List<String>> fbId_wikiId_map;
    HashMap<String, Set<String>> question_goldAns;
    public Train(){
        fbId_wikiId_map = new HashMap<String, List<String>>();
        question_goldAns = new HashMap<String, Set<String>>();
        loadFbWikiMapping();
        readGoldData();
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

    public void readGoldData(){
        List<String> lines = new ArrayList<String>();
        for(String path:goldFilePaths){
            lines.addAll(FileUtil.readFile(path));
        }
        for(int i = 0; i < lines.size(); i+=4){
            String question = lines.get(i);
            String[] info = lines.get(i+2).split("\t");
            Set<String> set = new HashSet<String>();
            for(String s:info){
                set.add(s);
            }
            question_goldAns.put(question, set);
        }
    }

    public void genTrainingData(){
        JSONParser parser = new JSONParser();
        List<String> lines = new ArrayList<String>();
        for(String path:inputPath){
            lines.addAll(FileUtil.readFile(path));
        }

        List<String> outputs = new ArrayList<String>();
        try{
            for(String line:lines){
                JSONObject jo = (JSONObject) parser.parse(line);

                String question = jo.get("question").toString();

                Set<String> goldAns = question_goldAns.get(question);
                List<String> topics = (List<String>) jo.get("topics");
                List<String> answers = (List<String>) jo.get("answers");

                StringBuffer posEvidences = new StringBuffer();
                StringBuffer negEvidences = new StringBuffer();
                for(String fbId:topics){
                    List<String> wikiIds = fbId_wikiId_map.get(fbId);
                    if( wikiIds == null )
                        continue;
                    for(String wikiId : wikiIds){
                        for(String ans:goldAns){
                            if( !answers.contains(ans) )
                                answers.add(ans);
                        }
                        HashMap<String, List<String>> ans_evidences = ExtractorUtil.instance.extractEvidences(wikiId, answers);
                        for(String ans:ans_evidences.keySet()){
                            List<String> evidences = ans_evidences.get(ans);
                            if( evidences.size() == 0 )
                                continue;

                            if( goldAns.contains(ans) ) {
                                for(String evidence:evidences)
                                    posEvidences.append(evidence + "\t");
                            }
                            else{
                                for(String evidence:evidences)
                                    negEvidences.append(evidence + "\t");
                            }
                        }
                    }
                }
                outputs.add(question+"\t1\t"+posEvidences.toString().trim());
                outputs.add(question+"\t0\t"+negEvidences.toString().trim());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        FileUtil.writeFile(outputs, outputPath);
    }

    public void extract2Grams(){
        List<String> lines = FileUtil.readFile("resources/WikiInference/Train/train.data");
        HashSet<String> twoGrams = new HashSet<String>();
        for(String line : lines){
            String[] info = line.split("\t");
            String question = info[0];
            for(int i = 2; i < info.length; i++){
                String feature = info[i];
                twoGrams.addAll( ExtractorUtil.instance.extract2Grams(question, feature) );
            }
        }
        System.err.println("there are #"+twoGrams.size()+" 2 grams");
        FileUtil.writeFile(new ArrayList<String>(twoGrams), "resources/WikiInference/2grams.txt");
    }

    public void begin(){
        try {
            List<String> outputs = new ArrayList<String>();
            List<String> lines = FileUtil.readFile("resources/WikiInference/Train/train.data");
            List<String> twoGrams = FileUtil.readFile("resources/WikiInference/2grams.txt");
            HashMap<String, Integer> gram_index = new HashMap<String, Integer>();
            HashMap<String, Integer> positiveSentencesNum = new HashMap<String, Integer>();
            int gindex = 1;
            for(String line : twoGrams){
                gram_index.put(line, gindex);
                gindex++;
            }
            int index = 0;
            for (String line : lines) {
                index++;
                if (index % 100 == 0)
                    System.err.println((double) index / lines.size());
                String[] info = line.split("\t");
                String question = info[0];
                String tag = info[1];
                if( tag.equals("1") ){
                    positiveSentencesNum.put(question, info.length-2);
                }
                if( tag.equals("0") ){

                }
                for (int i = 2; i < info.length; i++) {
                    StringBuffer buffer = new StringBuffer();
                    String feature = info[i];
                    buffer.append(tag + " ");
                    Set<String> grams = ExtractorUtil.instance.extract2Grams(question, feature);
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

                    for(int temp : indexes){
                        buffer.append(temp + ":1 ");
                    }
                    outputs.add(buffer.toString().trim());
                }
            }
            FileUtil.writeFile(outputs, this.svmTrainingDataFilePath);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Train train = new Train();
        train.genTrainingData();
        train.extract2Grams();
        train.begin();
    }
}
