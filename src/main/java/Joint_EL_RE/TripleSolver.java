package Joint_EL_RE;

import Util.QuestionUtil;
import Util.Virtuoso;
import edu.pku.wip.RelationExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by miaomiao on 16/6/17.
 */
public class TripleSolver {

//    public String question;
//    public String text;
//    public String[] subjSurfaces;
//    public String objSurface;
    public Virtuoso instance;
//    public String answerTypeSurface;
//    public boolean hasAnswerTypeConstraint = false;
//    public int AnswerNumConstraint = -1;
    public int entity_topK = 5;
    public int relation_topK = 5;


    public RelationExtractor relationSolver;
    public HashMap<String, String> expectedTypeForRel;
    public HashMap<String, List<String>> type_includedTypes;
    public HashMap<String, List<String>> qw_expectedTypes;
    public HashMap<String, List<String>> question_decomposed;
    public HashMap<String, HashMap<String, List<String>>> question_EL;

    public TripleSolver(RelationExtractor relationSolver,
                        HashMap<String, List<String>> question_decomposed,
                        HashMap<String, HashMap<String, List<String>>> question_EL){
        this.relationSolver = relationSolver;
        this.question_decomposed = question_decomposed;
        this.question_EL = question_EL;
        this.expectedTypeForRel = PreLoader.instance.expectedTypeForRel;
        this.type_includedTypes = PreLoader.instance.type_includedTypes;
        this.qw_expectedTypes = PreLoader.instance.qw_expectedTypes;
        instance = Virtuoso.instance;
    }

    //type constraint by the question word
    public boolean ifFilteredByRules(Triple triple){
        String pred = triple.pred;
        if( pred.indexOf("..") != -1 )
            pred = pred.split("\\.\\.")[1];
        String expectedType = this.expectedTypeForRel.get("ns:"+pred).replace("ns:", "");
        String question = triple.question;
        String qw = QuestionUtil.detectQW(question);
        if( qw.length() > 0 && !this.qw_expectedTypes.get(qw).contains(expectedType) )
            return true;
        return false;
    }

    public List<List<Triple>> solve(String question){
        List<String> subQuestions = question_decomposed.get(question);
        HashMap<String, List<String>> question_el_results = question_EL.get(question);
        if( question_el_results == null ){
            System.err.println("did not find mentions for "+question);
            return null;
        }

        Set<String> mentions = question_el_results.keySet();

        List<List<Triple>> result = new ArrayList<List<Triple>>();

        for(String subQuestion:subQuestions){
            List<Triple> triples = new ArrayList<Triple>();
            result.add(triples);
            String qw = QuestionUtil.detectQW(question);
            for(String mention:mentions) {
                if (subQuestion.indexOf(mention) == -1)
                    continue;
                String relationMention = subQuestion.replace(mention, "").replace("  ", " ").replace(qw, "").replace("  ", " ").trim().replace("?", "");
                HashMap<String, Double> relation_score_map = relationSolver.solve(relationMention);

                List<String> needRemoved = new ArrayList<String>();
                for (String relation : relation_score_map.keySet()) {
                    String pred = relation;
                    if (pred.indexOf("..") != -1) {
                        pred = pred.split("\\.\\.")[1];
                    }
                    String expectedType = this.expectedTypeForRel.get("ns:" + pred);

                    String ansTypeSurface = QuestionUtil.detectAnsTypeSurface(question);
                    if (ansTypeSurface.length() > 0) {
                        if (ansTypeSurface.equals("year")) {
                            if (!expectedType.equals("ns:type.datetime") && (!this.type_includedTypes.containsKey(expectedType) || !this.type_includedTypes.get(expectedType).contains("ns:type.datetime"))) {
                                needRemoved.add(relation);
                            }
                        }
                        if (ansTypeSurface.equals("country")) {
                            if (!expectedType.equals("ns:location.location") && (!this.type_includedTypes.containsKey(expectedType) || !this.type_includedTypes.get(expectedType).contains("ns:location.location"))) {
                                needRemoved.add(relation);
                            }
                        }
                    }
                }
                for(String tmp :needRemoved){
                    relation_score_map.remove(tmp);
                }

                HashMap<String, List<String>> el_results = question_EL.get(question);

                List<String> cands = el_results.get(mention);
                List<Triple> tmpTriples = new ArrayList<Triple>();
                int count_entity = 0;
                for (String cand : cands) {
                    if( count_entity >= entity_topK )
                        break;
                    String[] info = cand.split("&");
                    String entity = info[0];
                    double score = Double.parseDouble(info[1]);
                    int count_relation = 0;
                    for (String relation :relation_score_map.keySet()){
                        if( count_relation >= relation_topK )
                            break;

                        Set<String> possibleAns = instance.executeSPQuery("ns:"+entity+"\tns:"+relation);
                        // we will filter the combination that has more than 100 answers
                        if( possibleAns.size() > 0 && possibleAns.size() <= 100 ){
                            Triple triple = new Triple();
                            triple.subj = entity;
                            triple.subjSurface = mention;
                            triple.subjScore = score;
                            triple.pred = relation;
                            triple.predScore = relation_score_map.get(relation);
                            triple.question = question;
                            triple.answers = possibleAns;
                            if( !ifFilteredByRules(triple) ){
                                count_relation++;
                                tmpTriples.add(triple);
                            }
                        }
                    }
                    if( tmpTriples.size() > 0 )
                        count_entity++;
                }
                triples.addAll(tmpTriples);
            }
        }
        return result;
    }
}
