package Joint_EL_RE;

import Util.FileUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by miaomiao on 16/6/19.
 */
public class SVMRanker {
    public List<Triple> triples;

    public SVMRanker(List<Triple> triples){
        this.triples = triples;
    }

    public void train(){
        
    }

    public List<Triple> findBestTriple(){
        List<String> outputs = new ArrayList<String>();
        for(Triple triple : triples){
            StringBuffer buffer = new StringBuffer();
            buffer.append("0 qid:0 ");
            for(int i = 0; i < triple.featureVector.length; i++){
                buffer.append((i+1)+":"+triple.featureVector[i]+" ");
            }
            outputs.add(buffer.toString().trim());
        }
        FileUtil.writeFile(outputs, "resources/tool/libsvm-ranksvm-3.20/test", false);

        Process process;
        String[] scale_cmd = { "resources/tool/libsvm-ranksvm-3.20/svm-scale",
                "-r", "resources/tool/libsvm-ranksvm-3.20/dev.svm.train.param",
                "resources/tool/libsvm-ranksvm-3.20/test"};
        String[] rank_cmd = { "resources/tool/libsvm-ranksvm-3.20/svm-predict",
                "resources/tool/libsvm-ranksvm-3.20/test.scaled",
                "resources/tool/libsvm-ranksvm-3.20/svm.model",
                "resources/tool/libsvm-ranksvm-3.20/predicted" };

        try {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(scale_cmd);

            InputStream stream = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            List<String> lines = new ArrayList<String>();
            String line = null;
            while( (line = br.readLine()) != null){
                lines.add(line);
            }
            FileUtil.writeFile(lines, "resources/tool/libsvm-ranksvm-3.20/test.scaled", false);

            int exitVal = process.waitFor();

            if( exitVal == 0 ){
                process = runtime.exec(rank_cmd);
                exitVal = process.waitFor();
            }

            if( exitVal == 0 ){
                lines = FileUtil.readFile("resources/tool/libsvm-ranksvm-3.20/predicted");
                class Item{
                    int index;
                    double score;
                }
                List<Item> items = new ArrayList<Item>();
                for(int i = 0; i < lines.size(); i++){
                    Item item = new Item();
                    item.index = i;
                    item.score = Double.parseDouble(lines.get(i));
                    items.add(item);
                }
                Collections.sort(items, new Comparator<Item>(){
                    public int compare(Item i1, Item i2){
                        if( i2.score > i1.score )
                            return 1;
                        else if( i2.score < i1.score )
                            return -1;
                        else
                            return 0;
                    }
                });

                List<Triple> rankedTriples = new ArrayList<Triple>();
                for(int i = 0; i < items.size(); i++){
                    rankedTriples.add( this.triples.get(items.get(i).index) );
                    this.triples.get(items.get(i).index).sumScore = items.get(i).score;
                }
                return rankedTriples;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
