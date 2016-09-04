package InferenceOverWiki;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miaomiao on 16/9/3.
 */
public class EvidenceFinder {

    public static List<String> extract(String topicEntity, String answer){
        List<String> sentences = LuceneSearchAPI.findSentences(topicEntity);
        List<String> res = new ArrayList<String>();
        for(String sentence:sentences){
            String ls = sentence.toLowerCase();
            if( ls.indexOf(" "+answer.toLowerCase()) != -1 ){
                int index = ls.indexOf(":");
                String left = ls.substring(index+1);
                res.add(left);
            }
        }
        return res;
    }

    public static void main(String[] args){
        EvidenceFinder finder = new EvidenceFinder();
        List<String> evidences =  finder.extract("taylor swift", "Reading");
        for(String tmp:evidences){
            System.err.println(tmp);
        }
    }
}
