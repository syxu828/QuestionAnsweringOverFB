package EntityLinking;

import Util.FileUtil;
import Util.NLPUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by miaomiao on 16/6/16.
 */
public class EntityMentionDetector {

    public static String EL_RULE_PATH = "resources/EL/entitylinking.rule";
    public static List<String> rules;
    public static EntityMentionDetector instance = new EntityMentionDetector();
    public static NLPUtil nlpHelper = new NLPUtil("POS");
    public EntityMentionDetector(){
        init();
    }

    public void init(){
        rules = FileUtil.readFile(EL_RULE_PATH);
    }

    public static Set<String> detectByNERRule(String[] tokenInfos){
        String[] nerTags = {"PERSON", "LOCATION", "ORGANIZATION", "MISC"};
        Set<String> ELPhrases = new HashSet<String>();
        for(String nerTag : nerTags){
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < tokenInfos.length; i++){
                String token = tokenInfos[i].split("/")[0];
                String tag = tokenInfos[i].split("/")[2];
                if( tag.equals(nerTag) ){
                    buffer.append(token+" ");
                }
                else{
                    if( buffer.length() > 0 )
                        ELPhrases.add(buffer.toString().trim());
                    buffer = new StringBuffer();
                }
            }
            if( buffer.length() > 0 )
                ELPhrases.add(buffer.toString().trim());
        }
        return ELPhrases;
    }

    public static boolean matchRule( String type, int index, String[] patterns, String[] sentenceInfo){
        int begin = 0;
        if( type.equals("previous") ){
            if( index+1 < patterns.length )
                return false;
            begin = index-patterns.length;
            if( begin < 0 )
                return false;
        }else if( type.equals("current") ){
            if( index + patterns.length - 1 >= sentenceInfo.length  )
                return false;
            begin = index;
        }
//        else if( type.equals("next") ){
//            begin = index+1;
//            if( begin+patterns.length-1 >= sentenceInfo.length )
//                return false;
//        }

        boolean match = true;
        for(int j = 0; j < patterns.length; j++){
            String[] temp = sentenceInfo[begin+j].split("/");
            String token = temp[0];
            String pos = temp[1];
            String ner = temp[2];
            if( !patterns[j].equals(token) && !patterns[j].equals(pos) && !patterns[j].equals(ner) ){
                match = false;
                break;
            }
        }
        return match;
    }

    public List<String> detect(String text){
        String[] info = nlpHelper.postag(text).split("\t");

        System.err.println(Arrays.asList(info));

        Set<String> ELPhrases = new HashSet<String>();

        Pattern pattern = Pattern.compile("(\\[.*?\\])");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String phrase = matcher.group();
            ELPhrases.add(phrase);
        }

        // first use the NER rules to detect the entity mentions
        ELPhrases.addAll(detectByNERRule(info));

        // then use the handcraft rules to detect the entity mentions
        for(int i = 0; i < info.length; i++){
            for(String rule : rules) {

                String[] sections = rule.split("&&");
                boolean match = true;
                int phraseLen = 0;
                for (String section : sections) {
                    section = section.trim();
                    if (section.startsWith("P")) {
                        if (!matchRule("previous", i, section.replace("P:", "").split(" "), info)) {
                            match = false;
                            break;
                        }
                    } else if (section.startsWith("C")) {
                        phraseLen = section.replace("C:", "").split(" ").length;
                        if (!matchRule("current", i, section.replace("C:", "").split(" "), info)) {
                            match = false;
                            break;
                        }
                    }
//                    else if (section.startsWith("N")) {
//                        phraseLen = section.replace("N:", "").split(" ").length;
//                        if (!matchRule("next", i, section.replace("N:", "").split(" "), info)) {
//                            match = false;
//                            break;
//                        }
//                    }
                }
                if( match ){
                    StringBuffer buffer = new StringBuffer();
                    for(int j = 0; j < phraseLen; j++){
                        buffer.append(info[i+j].split("/")[0]+" ");
                    }
                    if( buffer.toString().trim().length() > 0 ) {
                        if( buffer.toString().trim().equals("-LSB- ans -RSB-") ){
                            ELPhrases.add("[ans]");
                        }else if( buffer.toString().trim().equals("-LSB- var_0 -RSB-") ){
                            ELPhrases.add("[var_0]");
                        }
                        else
                            ELPhrases.add(buffer.toString().trim());
                    }
                }
            }
        }

        return new ArrayList<String>(ELPhrases);
    }

    public static void main(String[] args){
        System.err.println(EntityMentionDetector.instance.detect("who played dorothy in the film wizard of oz"));
    }
}
