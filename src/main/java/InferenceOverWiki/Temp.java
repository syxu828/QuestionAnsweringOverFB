package InferenceOverWiki;

import Util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by miaomiao on 16/7/1.
 */
public class Temp {

    String jointInferenceResultPath = "resources/JointInference/Test/joint_inference.predicted.final";
    String goldResultPath = "resources/JointInference/Test/webquestions-predictions-gold.txt";

    String tmpResultPath = "resources/JointInference/tmp.txt";

    public void tmp(){
        HashMap<String, Set<String>> question_golds = readResultFile(goldResultPath);
        HashMap<String, Set<String>> question_joint = readResultFile(jointInferenceResultPath);
        List<String> outputs = new ArrayList<String>();
        for(String question:question_golds.keySet()){
            Set<String> golds = question_golds.get(question);
            Set<String> joint = question_joint.get(question);
            Set<String> intersected = intersect(golds, joint);
            if( intersected.size() == 0 ){
                intersected = joint;
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(question+"\t");
            JSONArray array = new JSONArray();
            for(String tmp:intersected){
                array.add(tmp);
            }
            outputs.add(question+"\t"+array.toString().replace("\\/", "/"));
        }
        FileUtil.writeFile(outputs, tmpResultPath);
    }

    public HashMap<String, Set<String>> readResultFile(String filePath){
        List<String> lines = FileUtil.readFile(filePath);
        JSONParser parser = new JSONParser();
        HashMap<String, Set<String>> question_ans = new HashMap<String, Set<String>>();
        try {
            for (String line : lines) {
                String[] info = line.split("\t");
                String question = info[0];
                Set<String> ans = new HashSet<String>();
                JSONArray array = (JSONArray) parser.parse(info[1]);
                for(Object o:array){
                    ans.add(o.toString());
                }
                question_ans.put(question, ans);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return question_ans;
    }

    public Set<String> intersect(Set<String> p, Set<String> q){
        Set<String> result = new HashSet<String>();
        for(String tmp:p){
            if( q.contains(tmp) )
                result.add(tmp);
        }
        return result;
    }

    public void viewFeature(){
        List<String> grams = FileUtil.readFile("resources/WikiInference/2grams.txt");
        List<String> weights = FileUtil.readFile("resources/tool/liblinear-2.1/wikiInfer/svm.model");
        HashMap<String, Double> gram_weight = new HashMap<String, Double>();
        for(int i = 0; i < grams.size(); i++){
            String gram = grams.get(i);
            double weight = Double.parseDouble( weights.get(i+5).trim() );
            if( weight != 0.0 )
                gram_weight.put(gram, weight);
        }

        List<Map.Entry<String, Double>> list = new ArrayList(gram_weight.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>(){
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if( o2.getValue() > o1.getValue() )
                    return 1;
                else if( o2.getValue() < o1.getValue() )
                    return -1;
                else
                    return 0;
            }
        });

        HashMap<String, Integer> word_count = new HashMap<String, Integer>();
        for(Map.Entry<String, Double> o : list){
            for(String token : o.getKey().split(" ")){
                if( !word_count.containsKey(token) ){
                    word_count.put(token, 0);
                }
                word_count.put(token, word_count.get(token)+1);
            }
        }

        List<Map.Entry<String, Integer>> list_2 = new ArrayList(word_count.entrySet());
        Collections.sort(list_2, new Comparator<Map.Entry<String, Integer>>(){
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                if( o2.getValue() > o1.getValue() )
                    return 1;
                else if( o2.getValue() < o1.getValue() )
                    return -1;
                else
                    return 0;
            }
        });

        for(Map.Entry<String, Integer> o : list_2){
            System.err.println(o.getKey()+"\t"+o.getValue());
        }
    }

    public static void main(String[] args){
        Temp temp = new Temp();
//        temp.tmp();
        temp.viewFeature();
    }
}
