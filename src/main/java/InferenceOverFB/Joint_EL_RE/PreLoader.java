package InferenceOverFB.Joint_EL_RE;

import Util.FileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by miaomiao on 16/6/19.
 */
public class PreLoader {
    public HashMap<String, String> expectedTypeForRel;
    public HashMap<String, List<String>> type_includedTypes;
    public HashMap<String, List<String>> qw_expectedTypes;

    public static PreLoader instance;

    static {
        instance = new PreLoader();
    }

    public PreLoader(){
        expectedTypeForRel = new HashMap<String, String>();
        type_includedTypes = new HashMap<String, List<String>>();
        qw_expectedTypes = new HashMap<String, List<String>>();

        loadFreebaseScheme();
        loadQWExpectedTypes();
    }

    /*
    *  this function is used to load rel_expectedType and type_includedTypes
     */
    public void loadFreebaseScheme(){
        List<String> lines = FileUtil.readFile("resources/Freebase/freebase.schema");
        for(String line : lines){
            String[] info = line.substring(0, line.length()-1).split("\t");
            String subj = info[0];
            String obj = info[2];
            String pred = info[1];
            if( pred.equals("ns:type.property.expected_type") ){
                expectedTypeForRel.put(subj, obj);
            }
            else if( pred.equals("ns:freebase.type_hints.included_types") ){
                if( !type_includedTypes.containsKey(subj) ){
                    type_includedTypes.put(subj, new ArrayList<String>());
                }
                type_includedTypes.get(subj).add(obj);
            }
        }
    }

    /*
    * this function is used to load qw_expectedTypes
     */
    public void loadQWExpectedTypes(){
        List<String> lines = FileUtil.readFile("resources/Freebase/qw_rel.cooccur");
        for(String line : lines){
            String[] info = line.split("\t");
            String qw = info[0].split(" ")[0];
            if( !this.qw_expectedTypes.containsKey(qw) ){
                this.qw_expectedTypes.put(qw, new ArrayList<String>());
            }
            this.qw_expectedTypes.get(qw).add(info[0].split(" ")[1]);
        }
    }

}
