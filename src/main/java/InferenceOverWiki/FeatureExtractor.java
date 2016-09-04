package InferenceOverWiki;

import Util.FileUtil;
import Util.NLPUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by miaomiao on 16/8/28.
 */
public class FeatureExtractor {

    String wikiDir = "/Users/kun/Documents/qa/WikiKBQA/IEResources/WikiPages/";
    String[] goldQAPairFilePath = new String[]{"resources/WebQuestions/train.data", "resources/WebQuestions/test.data", "resources/WebQuestions/dev.data"};
    String FBWikiIdMapFilePath = "resources/WikiInference/fbId_wikiId.mapping";
    String svmTrainingDataFilePath = "resources/WikiInference/Train/svm.train.data";
    String svmTestDataFilePath = "resources/WikiInference/Test/svm.test.data";
    String wordTagFilePath = "resources/WikiInference/wordnet.supertag";

    String[] stopWords = {"who",".", ",", "the", "a", "and", "in", "of", "that", "at", "for", "be", "to", "did", "he", "do", "'s",
            "where", "which", "where", "what", "as", "is", "are", "from", "was", "were", "we", "with", "by", "when", "does", "be",
            "(", ")"};
    HashMap<String, String> word_tag = new HashMap<String, String>();
    HashSet<String> stopWordSet;
    HashMap<String, List<String>> pages = new HashMap<String, List<String>>();

    public FeatureExtractor(){
        stopWordSet = new HashSet<String>();
        for (String word:stopWords) stopWordSet.add(word);
        List<String> lines = FileUtil.readFile(wordTagFilePath);
        for(String line:lines){
            String[] info = line.split("\t");
            if( info.length != 2 ) continue;
            word_tag.put(info[0].toLowerCase(), info[1]);
        }
    }

    private HashMap<String, List<String>> loadGoldQAPair(String[] filePaths){
        HashMap<String, List<String>> res = new HashMap<String, List<String>>();
        for(String filePath:filePaths){
            List<String> lines = FileUtil.readFile(filePath);
            for(int i = 0; i < lines.size(); i+=4){
                String question = lines.get(i);
                String[] ans = lines.get(i+2).split("\t");
                res.put(question, Arrays.asList(ans));
            }
        }
        return res;
    }

    private HashMap<String, List<String>> loadFBWikiMap(){
        HashMap<String, List<String>> res = new HashMap<String, List<String>>();
        List<String> lines = FileUtil.readFile(FBWikiIdMapFilePath);
        for(String line:lines){
            String[] info = line.split("\t");
            String fbId = info[0];
            List<String> wikiIds = Arrays.asList( info[1].substring(1, info[1].length()-1).replace("\"", "").split(",") );
            res.put(fbId, wikiIds);
        }
        return res;
    }

    public List<String> extractSentences(String wikiId, String subjSurface, String ans){
        try{
            List<String> texts;
            if( pages.containsKey(wikiId) )
                texts = pages.get(wikiId);
            else{
                File f = new File(wikiDir+wikiId+".txt");
                if( f.exists() ){
                    texts = FileUtil.readFile(wikiDir+wikiId+".txt");
                    pages.put(wikiId, texts);
                }
                else
                    return new ArrayList<String>();
            }

            List<String> result_fullMatched = new ArrayList<String>();
            List<String> result_objMatched = new ArrayList<String>();
            for(String text : texts){
                text = text.toLowerCase();
                if( text.indexOf(ans.toLowerCase()) != -1 && text.indexOf(subjSurface.toLowerCase()) != -1 ){
                    result_fullMatched.add(text);
                }
                if( text.indexOf(ans.toLowerCase()) != -1 )
                    result_objMatched.add(text);
            }
            if( result_fullMatched.size() == 0 )
                return result_objMatched;
            else
                return result_fullMatched;
        }catch(Exception e){
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }

    public void extractDataFeature(String[] inputPaths, String outputPath, String mode){
        List<String> lines = new ArrayList<String>();
        for(String inputPath:inputPaths){
            if( mode.equals("train") && inputPath.indexOf("Test") == -1 )
                lines.addAll( FileUtil.readFile(inputPath) );
            else if( mode.equals("train") ){
                List<String> tmp = FileUtil.readFile(inputPath);
                Collections.sort(tmp);
                for(int i = 0; i < tmp.size(); i++){
                    lines.add(tmp.get(i));
                }
            }else if( mode.equals("test") ){
                lines.addAll(FileUtil.readFile(inputPath));
            }
        }
        HashMap<String, List<JSONObject>> posSentences = new HashMap<String, List<JSONObject>>();
        HashMap<String, List<JSONObject>> negSentences = new HashMap<String, List<JSONObject>>();
        HashMap<String, List<String>> golds = this.loadGoldQAPair(goldQAPairFilePath);
        HashMap<String, List<String>> fbWikiIdMap = loadFBWikiMap();
        JSONParser parser = new JSONParser();
        try{
            for(String line : lines){
                JSONObject jo = (JSONObject) parser.parse(line);
                String question = jo.get("question").toString();
                String subjSurface = jo.get("topicSurface").toString();
                JSONArray array = (JSONArray) jo.get("topics");
                String wikiId = new String();
                if( array.size() > 0 ) wikiId = array.get(0).toString();
                JSONArray anses = (JSONArray) jo.get("answers");
                List<String> posAns = new ArrayList<String>();
                List<String> negAns = new ArrayList<String>();
                for(int i = 0; i < anses.size(); i++){
                    String ans = anses.get(i).toString();
                    List<String> goldAns = golds.get(question);
                    if( goldAns.contains(ans) ){
                        posAns.add(ans);
                    }else negAns.add(ans);
                }
                if( mode.equals("train") ){
                    for(String ans:golds.get(question)){
                        if( !fbWikiIdMap.containsKey(wikiId) ) continue;
                        List<String> sentences = this.extractSentences(fbWikiIdMap.get(wikiId).get(0), subjSurface, ans);
//                        List<String> sentences = EvidenceFinder.extract(subjSurface, ans);
                        JSONObject tmp = new JSONObject();
                        tmp.put("ans", ans); tmp.put("features", extractNovelFeature(sentences, ans.toLowerCase()));
                        if( !posSentences.containsKey(question) ){
                            posSentences.put(question, new ArrayList<JSONObject>());
                        }
                        posSentences.get(question).add(tmp);
                    }
                }else if( mode.equals("test") ){
                    for(String ans:posAns){
                        if( !fbWikiIdMap.containsKey(wikiId) ) continue;
                        List<String> sentences = this.extractSentences(fbWikiIdMap.get(wikiId).get(0), subjSurface, ans);
                        JSONObject tmp = new JSONObject();
                        tmp.put("ans", ans); tmp.put("features", extractNovelFeature(sentences, ans));
                        if( !posSentences.containsKey(question) ){
                            posSentences.put(question, new ArrayList<JSONObject>());
                        }
                        posSentences.get(question).add(tmp);
                    }
                }

                for(String ans:negAns){
                    if( !fbWikiIdMap.containsKey(wikiId) ) continue;
                    List<String> sentences = this.extractSentences(fbWikiIdMap.get(wikiId).get(0), subjSurface, ans);
                    JSONObject tmp = new JSONObject();
                    tmp.put("ans", ans); tmp.put("features", extractNovelFeature(sentences, ans));
                    if( !negSentences.containsKey(question) )
                        negSentences.put(question, new ArrayList<JSONObject>());
                    negSentences.get(question).add(tmp);
                }
            }
            List<String> outputs = new ArrayList<String>();
            Set<String> questions = new HashSet<String>();
            questions.addAll(posSentences.keySet());
            questions.addAll(negSentences.keySet());
            for(String question : questions){
                StringBuffer buffer = new StringBuffer();
                buffer.append(question+"\t1\t");
                List<JSONObject> positiveFeatures = posSentences.get(question);
                if( positiveFeatures != null ){
                    for(JSONObject featureJson : positiveFeatures){
                        String ans = featureJson.get("ans").toString();
                        List<String> features = (List<String>) featureJson.get("features");
                        for (String feature : features) {
                            if (mode.endsWith("test")) buffer.append(feature + "_" + ans + "\t");
                            else buffer.append(feature+"\t");
                        }
                    }
                    outputs.add(buffer.toString().trim());
                }

                List<JSONObject> negativeFeatures = negSentences.get(question);
                buffer = new StringBuffer();
                buffer.append(question+"\t0\t");
                if( negativeFeatures != null ){
                    for(JSONObject featureJson:negativeFeatures){
                        String ans = featureJson.get("ans").toString();
                        List<String> features = (List<String>) featureJson.get("features");
                        for (String feature : features) {
                            if (mode.endsWith("test")) buffer.append(feature + "_" + ans + "\t");
                            else buffer.append(feature+"\t");
                        }
                    }
                }
                outputs.add(buffer.toString().trim());
            }
            FileUtil.writeFile(outputs, outputPath, false);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<String> extractNovelFeature(List<String> sentences, String ans){
        List<String> res = new ArrayList<String>();
        ans = ans.toLowerCase();
        for(String sentence : sentences){
            int ansIndex = sentence.indexOf(ans);
            if( ansIndex == -1 ) continue;
            String prevSen = sentence.substring(0, ansIndex);
            String leftSen = sentence.substring(ansIndex+ans.length());
            String[] prevLemmas = NLPUtil.lemma_instance.extractLemmaSequence(prevSen).split(" ");
            String[] prevPos = NLPUtil.lemma_instance.extractPOSSequence(prevSen).split(" ");
            String[] leftLemmas = NLPUtil.lemma_instance.extractLemmaSequence(leftSen).split(" ");
            String[] leftPos = NLPUtil.lemma_instance.extractPOSSequence(leftSen).split(" ");
            int leftIdx = prevLemmas.length - 20;
            if( leftIdx < 0 ) leftIdx = 0;
            int n = prevLemmas.length - leftIdx;
            String[] tmpLemmas = new String[n];
            String[] tmpPos = new String[n];
            for(int i = 0; i < n; i++){
                tmpLemmas[i] = prevLemmas[i+leftIdx];
                tmpPos[i] = prevPos[i+leftIdx];
            }
            StringBuffer feature = new StringBuffer(this.extractNovelFeature(tmpLemmas, tmpPos));
            feature.append(" "+this.extractNovelFeature(leftLemmas, leftPos));
            res.add(feature.toString());
        }
        return res;
    }

    public String extractNovelFeature(String[] lemmas, String[] pos){
        if( lemmas == null || lemmas.length == 0 || pos == null || pos.length == 0 ) return "";
        if( lemmas.length != pos.length ) System.err.println("the lemmas length is not the same as pos length");
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < pos.length; i++){
            if( pos[i] == null || lemmas[i] == null ) continue;
            if( !stopWordSet.contains(lemmas[i]) )
                sb.append(lemmas[i]+" ");
        }
        return sb.toString().trim();
    }

    public HashMap<String, Integer> extract2Grams(String p, String q){
        HashSet<String> pTokenSet = new HashSet<String>();
        HashSet<String> qTokenSet = new HashSet<String>();
        String[] pToken = p.split(" ");
        String[] qToken = q.split(" ");
        String[] omitWords = {"who",".", ",", "the", "a", "and", "in", "of", "that", "at", "for", "be", "to", "did", "he", "do", "'s",
                "where", "which", "where", "what", "as", "is", "are", "from", "was", "were", "we", "with", "by", "when", "does"};
        for(String token : pToken)
            pTokenSet.add(token);
        for(String token : qToken)
            qTokenSet.add(token);
        HashMap<String, Integer> feature_count = new HashMap<String, Integer>();
        for(String ptoken : pTokenSet){
            if( Arrays.asList(omitWords).contains(ptoken) )
                continue;
            if( !word_tag.containsKey(ptoken.toLowerCase()) ) continue;
            for(String qtoken : qTokenSet){
                if( !word_tag.containsKey(qtoken.toLowerCase()) ) continue;
                if( !Arrays.asList(omitWords).contains(qtoken) && qtoken.length() > 0 && ptoken.length() > 0 ) {
//                    ngrams.add(ptoken + " " + qtoken);
//                    String feature = word_tag.get(ptoken.toLowerCase()) + " " + word_tag.get(qtoken.toLowerCase());
                    String feature = ptoken.toLowerCase() + " " + qtoken.toLowerCase();
                    if( !feature_count.containsKey(feature) )
                        feature_count.put(feature, 1);
                    else
                        feature_count.put(feature, feature_count.get(feature)+1);
                }
            }
        }
        return feature_count;
    }

    public void extract2GramsFromFile(String inputPath, String outputPath){
        List<String> lines = FileUtil.readFile(inputPath);
        HashSet<String> twoGrams = new HashSet<String>();
        for(String line : lines){
            String[] info = line.split("\t");
            String question = info[0].replace("?", "");
            for(int i = 2; i < info.length; i++){
                String feature = info[i];
                twoGrams.addAll( extract2Grams(question, feature).keySet() );
            }
        }
        System.err.println("there are #"+twoGrams.size()+" 2 grams");
        FileUtil.writeFile(new ArrayList<String>(twoGrams), outputPath, false);
    }

    public void extractTrainingLexicalFeatures(){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.svmTrainingDataFilePath));
            List<String> lines = FileUtil.readFile("resources/WikiInference/Train/ie.training.data");
            List<String> twoGrams = FileUtil.readFile("resources/WikiInference/2grams.txt");
            HashMap<String, Integer> feature_index = new HashMap<String, Integer>();
            HashMap<Integer, String> index_feature = new HashMap<Integer, String>();
            HashMap<String, Integer> positiveSentencesNum = new HashMap<String, Integer>();
            int fIdx = 1;
            for(String line : twoGrams){
                feature_index.put(line, fIdx);
                index_feature.put(fIdx, line);
                fIdx++;
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
                    String sentence = info[i];
                    buffer.append(tag + " ");
                    HashMap<String, Integer> feature_count = extract2Grams(question, sentence);
                    List<Integer> indexes = new ArrayList<Integer>();
                    for (String feature : feature_count.keySet()) {
                        if( !feature_index.containsKey(feature) ) continue;
                        indexes.add(feature_index.get(feature));
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
                        buffer.append(temp + ":"+feature_count.get(index_feature.get(temp))+" ");
                    }
                    bw.write(buffer.toString().trim()+"\n");
                }
            }
            bw.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void extractTestingLexicalFeatures(){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(svmTestDataFilePath));
            List<String> lines = FileUtil.readFile("resources/WikiInference/Test/ie.test.data");
            List<String> twoGrams = FileUtil.readFile("resources/WikiInference/2grams.txt");
            HashMap<String, Integer> feature_index = new HashMap<String, Integer>();
            HashMap<Integer, String> index_feature = new HashMap<Integer, String>();
            int fIdx = 1;
            for(String line : twoGrams){
                feature_index.put(line, fIdx);
                index_feature.put(fIdx, line);
                fIdx++;
            }
            List<String> outputs = new ArrayList<String>();
            List<String> sentenceEvidences = new ArrayList<String>();
            List<String> featureGrams = new ArrayList<String>();
            int index = 0;
            for (String line : lines) {
                index++;
                if (index % 100 == 0)
                    System.err.println((double) index / lines.size());
                String[] info = line.split("\t");
                String question = info[0];

                String tag = info[1];
                for (int i = 2; i < info.length; i++) {
                    StringBuffer buffer = new StringBuffer();
                    StringBuffer featureGram = new StringBuffer();
                    buffer.append(tag + " ");
                    int index_ = info[i].lastIndexOf("_");
                    String ans = info[i].substring(index_+1);
                    String sentence = info[i].substring(0, index_);

                    HashMap<String, Integer> feature_count = extract2Grams(question, sentence);
                    List<Integer> indexes = new ArrayList<Integer>();
                    for (String feature : feature_count.keySet()) {
                        if( !feature_index.containsKey(feature) ) continue;
                        indexes.add(feature_index.get(feature));
                        featureGram.append("["+feature+"] ");
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
                        buffer.append(temp + ":"+feature_count.get(index_feature.get(temp))+" ");
                    }
                    bw.write(buffer.toString().trim()+"\n");

                    outputs.add(question+"\t"+ans);
                    sentenceEvidences.add(question+"\t"+sentence);
                    featureGrams.add(question+"\t"+featureGram.toString());
                }
            }
            bw.close();
            FileUtil.writeFile(outputs, "resources/WikiInference/Test/svm.test.data.ans", false);
            FileUtil.writeFile(sentenceEvidences, "resources/WikiInference/Test/svm.test.data.evidences", false);
            FileUtil.writeFile(featureGrams, "resources/WikiInference/Test/svm.test.data.featureGrams", false);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        FeatureExtractor extractor = new FeatureExtractor();
        String[] trainInputPaths = new String[]{"resources/JointInference/Train/wikiInference.input"};
        extractor.extractDataFeature(trainInputPaths, "resources/WikiInference/Train/ie.training.data", "train");
        extractor.extract2GramsFromFile("resources/WikiInference/Train/ie.training.data", "resources/WikiInference/2grams.txt");
        extractor.extractTrainingLexicalFeatures();

        String[] testInputPath = new String[]{"resources/JointInference/Test/wikiInference.input"};
        extractor.extractDataFeature(testInputPath, "resources/WikiInference/Test/ie.test.data", "test");
        extractor.extractTestingLexicalFeatures();
    }
}
