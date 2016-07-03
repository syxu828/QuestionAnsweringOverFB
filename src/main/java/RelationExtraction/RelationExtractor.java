package RelationExtraction;

import RelationExtraction.CNN.Item;
import RelationExtraction.CNN.Test;
import Util.FileUtil;
import Util.NLPUtil;

import java.util.*;

/**
 * Created by miaomiao on 16/6/13.
 */
public class RelationExtractor{
    public Test predicater;
    public List<String> relLabels;
    public int topK = -1;
    public NLPUtil nlpUtil;
    public static RelationExtractor instance;

    static {
        instance = new RelationExtractor(false, "", "resources/RE/param/params.14");
    }

    public RelationExtractor(boolean ifUseCache, String cachePath, String paramPath) {
        relLabels = new ArrayList<String>();
        relLabels = FileUtil.readFile("resources/RE/classLabels.txt");
        predicater = new Test(ifUseCache, cachePath, paramPath);
        nlpUtil = new NLPUtil("LEMMA");
    }

    public HashMap<String, Double> solve(String text){
        Item item = new Item();
        if( text.length() == 0 )
            return new LinkedHashMap<String, Double>();
        item.question = nlpUtil.extractLemmaSequence(text);
        List<String> rels = predicater.executeOneDecode(item, topK);
        HashMap<String, Double> RE_results = new LinkedHashMap<String, Double>();
        for(int i = 0; i < rels.size(); i++){
            String[] info = rels.get(i).split(" ");
            RE_results.put(this.relLabels.get(Integer.parseInt(info[0])), Double.parseDouble(info[1]));
        }
        return RE_results;
    }

    public static void main(String[] args){
        RelationExtractor extractor = new RelationExtractor(false, "", "resources/RE/param/params.14");
        System.err.println(extractor.solve("is born at"));
    }
}
