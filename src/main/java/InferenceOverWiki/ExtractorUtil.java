package InferenceOverWiki;

import Util.FileUtil;
import Util.NLPUtil;

import java.io.File;
import java.util.*;

/**
 * Created by miaomiao on 16/7/2.
 */
public class ExtractorUtil {

    public static ExtractorUtil instance = new ExtractorUtil();

    public HashMap<String, List<String>> page_cache = new HashMap<String, List<String>>();
    public Set<String> stopWords;
    public String stopWordsFilePath = "resources/WikiInference/stopwords";

    public ExtractorUtil(){
        stopWords = FileUtil.readFileAsSet(stopWordsFilePath);
    }

    public HashMap<String, List<String>> extractEvidences(String wikiId, List<String> answers){
        List<String> texts;
        if( page_cache.containsKey(wikiId) ){
            texts = page_cache.get(wikiId);
        }else{
            File f = new File("resources/WikiPages/"+wikiId+".txt");
            if( f.exists() ){
                texts = FileUtil.readFile("resources/WikiPages/"+wikiId+".txt");
                page_cache.put(wikiId, texts);
            }
            else
                return new HashMap<String, List<String>>();
        }

        HashMap<String, List<String>> ans_evidences_map = new HashMap<String, List<String>>();
        for(String text : texts){
            text = text.toLowerCase();
            for(String ans:answers) {
                if( !ans_evidences_map.containsKey(ans) )
                    ans_evidences_map.put(ans, new ArrayList<String>());
                List<String> sentence_involved = ans_evidences_map.get(ans);
                if( text.indexOf(ans.toLowerCase()) != -1 ){
                    String feature = extractInfo(text, ans.toLowerCase());
                    sentence_involved.add(feature);
                }
                ans_evidences_map.put(ans, sentence_involved);
            }
        }
       return ans_evidences_map;
    }

    public String extractInfo(String sentence, String ans){
        int windowSize = 10;
        int ansIndex = sentence.indexOf(ans);
        String[] tokens = sentence.substring(0, ansIndex).trim().split(" ");
        StringBuffer previousEnv = new StringBuffer();
        int i = 0;

        if( tokens.length >= windowSize )
            i = tokens.length-windowSize;
        for(; i < tokens.length; i++) {
            if( !stopWords.contains(tokens[i]) )
                previousEnv.append(tokens[i] + " ");
        }

        tokens = sentence.substring(ansIndex+ans.length()).trim().split(" ");
        int end = tokens.length;
        if( tokens.length >= windowSize )
            end = windowSize;

        StringBuffer lastEnv = new StringBuffer();
        for(i = 0; i < end; i++) {
            if( !stopWords.contains(tokens[i]) )
                lastEnv.append(tokens[i] + " ");
        }

        String feature = NLPUtil.lemma_instance.extractLemmaSequence(previousEnv.append(lastEnv).toString().trim(), stopWords);
        return feature;
    }

    public HashSet<String> extract2Grams(String p, String q){
        p = p.replace("?", "");
        q = q.replace("?", "");
        HashSet<String> pTokenSet = new HashSet<String>();
        HashSet<String> qTokenSet = new HashSet<String>();
        String[] pToken = p.split(" ");
        String[] qToken = q.split(" ");
        Set<String> stopWords = FileUtil.readFileAsSet(stopWordsFilePath);
        for(String token : pToken)
            pTokenSet.add(token);
        for(String token : qToken)
            qTokenSet.add(token);
        HashSet<String> ngrams = new HashSet<String>();
        for(String ptoken : pTokenSet){
            if( stopWords.contains(ptoken) )
                continue;
            for(String qtoken : qTokenSet){
                if( !stopWords.contains(qtoken) )
                    ngrams.add(ptoken+" "+qtoken);
            }
        }
        return ngrams;
    }
}
