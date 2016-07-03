import Util.FileUtil;

import java.util.HashSet;
import java.util.List;

/**
 * Created by miaomiao on 16/6/13.
 */
public class Temp {
    public static void main(String[] args){
        List<String> questions = FileUtil.readFile(ClassLoader.getSystemResourceAsStream("resources/Train/train.questions"));
        List<String> lines = FileUtil.readFile(ClassLoader.getSystemResourceAsStream("resources/Train/train.KBTraining.txt"));

        HashSet<String> relations = new HashSet<String>();
        for(int i = 0; i < lines.size(); i+=5){
            String question = lines.get(i);
            questions.remove(question);
            relations.add(lines.get(i+3));
        }

        for(int i = 0; i < questions.size(); i++){
            System.err.println("#"+i+":"+questions.get(i));
        }
        System.err.println(relations.size());
    }
}
