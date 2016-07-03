package Joint_EL_RE;

import Util.FileUtil;
import edu.pku.wip.RelationExtractor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by miaomiao on 16/6/19.
 */
public class Train {

    String[] trainingDataPaths = {"resources/Train/train.KBTraining.txt"};
    String[] trainingDecomPaths = {"resources/Train/train.questions.decomposed"};
    String[] trainingELPaths = {"resources/Train/train.EL.results"};
    String[] trainingQuestionPaths = {"resources/Train/train.questions"};

    String[] goldQAPaths = {"resources/WebQuestions/train.data",
                                "resources/WebQuestions/dev.data",
                                    "resources/WebQuestions/test.data"};

    String trainingDataFilePath = "resources/JointInference/Train/train.data";

    HashMap<String, String> question_goldInfo;
    HashMap<String, List<String>> question_decomposed;
    HashMap<String, HashMap<String, List<String>>> question_EL;
    HashMap<String, Set<String>> question_goldAns;

    public Train(){
        question_goldInfo = new HashMap<String, String>();
        question_decomposed = new HashMap<String, List<String>>();
        question_EL = new HashMap<String, HashMap<String, List<String>>>();
        question_goldAns = new HashMap<String, Set<String>>();
        this.readTrainingData();
        this.readGoldData();
    }

    public void readTrainingData(){
        try{
            for(int index = 0; index < trainingDataPaths.length; index++) {
                List<String> lines = FileUtil.readFile(trainingDataPaths[index]);
                for (int i = 0; i < lines.size(); i += 5) {
                    String question = lines.get(i).replace("?", "");
                    String goldMid = lines.get(i + 2);
                    String goldRel = lines.get(i + 3);
                    question_goldInfo.put(question, goldMid + " " + goldRel);
                }
            }

            JSONParser parser = new JSONParser();
            for(int index = 0; index < trainingDecomPaths.length; index++) {
                List<String> lines = FileUtil.readFile(trainingDecomPaths[index]);
                for (int i = 0; i < lines.size(); i++) {
                    JSONObject object = (JSONObject) parser.parse(lines.get(i));
                    String question = object.get("question").toString();
                    List<String> decomposed = (List<String>) object.get("decomposed");
                    question_decomposed.put(question, decomposed);
                }
            }

            for(int index = 0; index < trainingELPaths.length; index++){
                List<String> lines = FileUtil.readFile(trainingELPaths[index]);
                for(String line:lines){
                    JSONObject object = (JSONObject) parser.parse(line);
                    String question = object.get("question").toString();
                    HashMap<String, List<String>> EL_results = new HashMap<String, List<String>>();
                    question_EL.put(question, EL_results);

                    JSONArray array = (JSONArray) object.get("mentions");
                    for(int i = 0; i < array.size(); i++){
                        JSONObject mention = (JSONObject) array.get(i);
                        for(Object key:mention.keySet()){
                            List<String> cands = (List<String>) mention.get(key);
                            EL_results.put((String) key, cands);
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void readGoldData(){
        List<String> lines = new ArrayList<String>();
        for(String path:goldQAPaths){
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
        RelationExtractor relationExtractor = new RelationExtractor(false, "", "resources/RE/classLabels.txt", "resources/RE/param/params.69");
        TripleSolver solver = new TripleSolver(relationExtractor, question_decomposed, question_EL);

        List<String> questions = new ArrayList<String>();
        for(String path:trainingQuestionPaths){
            questions.addAll(FileUtil.readFile(path));
        }

        List<String> outputs = new ArrayList<String>();
        int curIndex = 1;
        for(String question:questions){
            try {
                List<List<Triple>> groundedResults = solver.solve(question);

                if( groundedResults == null ){
                    System.err.println("there is no grounded result when solving question "+question);
                    continue;
                }

                for(int i = 0; i < groundedResults.size(); i++){
                    List<Triple> triples = groundedResults.get(i);
                    HashMap<Triple, Double> triple_rScore_map = new HashMap<Triple, Double>();
                    for(Triple triple:triples){
                        double[] vectors = FeatureExtractor.instance.extractFeatureVector(triple);
                        triple.featureVector = vectors;
                        if( !question_goldAns.containsKey(question) ){
                            System.err.println("we did not find the gold ans for :"+question);
                            continue;
                        }
                        double rScore = computeRankingScore(question_goldAns.get(question), triple.answers );
                        triple_rScore_map.put(triple, rScore);
                    }

                    List<Map.Entry<Triple, Double>> entryList = new ArrayList<Map.Entry<Triple, Double>>(triple_rScore_map.entrySet());
                    Collections.sort(entryList, new Comparator<Map.Entry<Triple, Double>>() {
                        public int compare(Map.Entry<Triple, Double> o1, Map.Entry<Triple, Double> o2) {
                            if( o2.getValue() > o1.getValue() )
                                return 1;
                            else if( o2.getValue() < o1.getValue() )
                                return -1;
                            else
                                return 0;
                        }
                    });

                    for(Map.Entry<Triple, Double> entry:entryList){
                        String featureString = toFeatureString(entry.getValue(), curIndex, entry.getKey());
                        outputs.add(featureString);
                    }

                    curIndex++;

                    System.err.println(curIndex);
                }
            }catch (Exception e){
                System.err.println("solving question: "+question + " errors");
                e.printStackTrace();
            }
        }

        FileUtil.writeFile(outputs, trainingDataFilePath);
    }

    public double computeRankingScore(Set<String> gold, Set<String> predicted){
        double correct = 0;
        for(String item : predicted){
            if( item.startsWith("\"") && item.endsWith("\"") )
                item = item.replace("\"", "");
            if( gold.contains(item) )
                correct++;
        }
        double precision = correct / predicted.size();
        double recall = correct / gold.size();
        if( correct == 0 )
            return 0;
        return 1/ (1/precision + 1/recall);
    }

    public String toFeatureString(double rank, int qid, Triple triple){
        StringBuffer buffer = new StringBuffer();
        double[] vector = triple.featureVector;
        buffer.append(rank+" qid:"+qid+" ");
        for(int i = 0; i < vector.length; i++){
            buffer.append((i+1)+":"+vector[i]+" ");
        }
        return buffer.toString().trim();
    }

    public static void main(String[] args){
        Train train = new Train();
        train.genTrainingData();
    }
}
