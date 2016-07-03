package InferenceOverFB.Joint_EL_RE;

import RelationExtraction.RelationExtractor;
import Util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by miaomiao on 16/6/30.
 */
public class Test {

    String[] testDecomPaths = {"resources/Test/test.questions.decomposed"};
    String[] testELPaths = {"resources/Test/test.EL.results"};
    String[] testQuestionPaths = {"resources/Test/test.questions"};
    String[] goldQAPaths = {"resources/WebQuestions/test.data"};
    String testDataFilePath = "resources/JointInference/Test/joint_inference.predicted.69";
    String finalResultFilePath = "resources/JointInference/Test/joint_inference.predicted.final";

    String wikiInferenceInputFilePath = "resources/JointInference/Test/wikiInference.input";

    HashMap<String, List<String>> question_decomposed;
    HashMap<String, HashMap<String, List<String>>> question_EL;
    HashMap<String, Set<String>> question_goldAns;

    public Test(){
        question_decomposed = new HashMap<String, List<String>>();
        question_EL = new HashMap<String, HashMap<String, List<String>>>();
        question_goldAns = new HashMap<String, Set<String>>();
        readTestData();
        readGoldData();
    }

    public Test(String[] testDecomPaths, String[] testELPaths, String[] testQuestionPaths, String[] goldQAPaths, String testDataFilePath, String finalResultFilePath, String wikiInferenceInputFilePath){
        this.testDecomPaths = testDecomPaths;
        this.testELPaths = testELPaths;
        this.testQuestionPaths = testQuestionPaths;
        this.goldQAPaths = goldQAPaths;
        this.testDataFilePath = testDataFilePath;
        this.finalResultFilePath = finalResultFilePath;
        this.wikiInferenceInputFilePath = wikiInferenceInputFilePath;

        question_decomposed = new HashMap<String, List<String>>();
        question_EL = new HashMap<String, HashMap<String, List<String>>>();
        question_goldAns = new HashMap<String, Set<String>>();
        readTestData();
        readGoldData();
    }

    public void readTestData(){
        try{
            JSONParser parser = new JSONParser();
            for(int index = 0; index < testDecomPaths.length; index++) {
                List<String> lines = FileUtil.readFile(testDecomPaths[index]);
                for (int i = 0; i < lines.size(); i++) {
                    JSONObject object = (JSONObject) parser.parse(lines.get(i));
                    String question = object.get("question").toString();
                    List<String> decomposed = (List<String>) object.get("decomposed");
                    question_decomposed.put(question, decomposed);
                }
            }

            for(int index = 0; index < testELPaths.length; index++){
                List<String> lines = FileUtil.readFile(testELPaths[index]);
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

    public void Joint_Inference(){
        RelationExtractor relationExtractor = new RelationExtractor(false, "", "resources/RE/param/params.69");
        TripleSolver solver = new TripleSolver(relationExtractor, question_decomposed, question_EL);

        List<String> questions = new ArrayList<String>();
        for(String path:testQuestionPaths){
            questions.addAll(FileUtil.readFile(path));
        }

        List<String> outputs = new ArrayList<String>();
        List<String> wikiInferenceInput = new ArrayList<String>();
        for(String question:questions) {
            try {
                List<List<Triple>> groundedResults = solver.solve(question);
                if (groundedResults == null) {
                    System.err.println("there is no grounded result when solving question " + question);
                    outputs.add(question+"\t"+"[]");
                    continue;
                }

                Set<String> subjMids = new HashSet<String>();
                List<String> answers = new ArrayList<String>();

                JSONArray array = new JSONArray();
                for(int i = 0; i < groundedResults.size(); i++){
                    List<Triple> triples = groundedResults.get(i);
                    for(Triple triple:triples){
                        double[] vectors = FeatureExtractor.instance.extractFeatureVector(triple);
                        triple.featureVector = vectors;
                        if( !question_goldAns.containsKey(question) ){
                            System.err.println("we did not find the gold ans for :"+question);
                            continue;
                        }
                    }

                    List<Triple> rankedTriples = SvmTest.instance.findBestTriple(triples);
                    if( rankedTriples.size() > 0 ) {
                        Triple top_1_triple = rankedTriples.get(0);
                        subjMids.add(top_1_triple.subj);
                        for (String answer : top_1_triple.answers) {
                            if (answer.startsWith("\"") && answer.endsWith("\""))
                                answer = answer.substring(1, answer.length() - 1);
                            array.add(answer);
                            answers.add(answer);
                        }
                    }
                }

                System.err.println("#"+questions.indexOf(question)+":\t"+question+"\t"+array.toString());
                outputs.add(question+"\t"+array.toString());

                JSONObject input = new JSONObject();
                input.put("question", question);
                input.put("topics", new ArrayList(subjMids));
                input.put("answers", answers);
                wikiInferenceInput.add(input.toString());
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        FileUtil.writeFile(outputs, testDataFilePath);
        FileUtil.writeFile(wikiInferenceInput, wikiInferenceInputFilePath);
    }

    /*
    *   mainly do some easy transfer for the time string
     */
    public void rewriteAns(){
        List<String> lines = FileUtil.readFile(this.testDataFilePath);
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

            FileUtil.writeFile(outputs, this.finalResultFilePath);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Test test = new Test();
        test.Joint_Inference();
//        test.rewriteAns();

        //we need to perform the inference apporach for the training data
//        String[] testDecomPaths = {"resources/Train/train.questions.decomposed"};
//        String[] testELPaths = {"resources/Train/train.EL.results"};
//        String[] testQuestionPaths = {"resources/Train/train.questions"};
//        String[] goldQAPaths = {"resources/WebQuestions/train.data", "resources/WebQuestions/dev.data"};
//        String testDataFilePath = "resources/JointInference/Train/joint_inference.predicted.69";
//        String finalResultFilePath = "resources/JointInference/Train/joint_inference.predicted.final";
//        String wikiInferenceInputFilePath = "resources/JointInference/Train/wikiInference.input";
//        Test inference_on_training = new Test(testDecomPaths, testELPaths, testQuestionPaths, goldQAPaths, testDataFilePath, finalResultFilePath, wikiInferenceInputFilePath);
//        inference_on_training.Joint_Inference();

    }
}
