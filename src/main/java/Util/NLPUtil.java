package Util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.io.StringReader;
import java.util.*;

/**
 * Created by miaomiao on 16/2/17.
 */
public class NLPUtil {

    public static NLPUtil parser_instance;
    public static NLPUtil lemma_instance;

    static {
        parser_instance = new NLPUtil("PARSE");
        lemma_instance = new NLPUtil("LEMMA");
    }

    public Properties props = new Properties();
    public StanfordCoreNLP pipeline;

    public String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    public LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
    public TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");

    public NLPUtil(String type){
        if( type.equals("POS") ){
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
            pipeline = new StanfordCoreNLP(props);
        }
        else if( type.equals("PARSE") ){
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
            pipeline = new StanfordCoreNLP(props);
        }
        else if( type.equals("LEMMA") ){
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            pipeline = new StanfordCoreNLP(props);
        }
    }

    public String postag(String text){
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        StringBuffer buffer = new StringBuffer();
        for(CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                buffer.append(word+"/"+pos+"/"+ne+"\t");
            }
        }
        return buffer.toString();
    }

    public Tree extractSyntacticTree(String text){
        Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(text));
        List<CoreLabel> rawWords2 = tok.tokenize();
        Tree parse = lp.apply(rawWords2);
        return parse;
    }

    public String extractPOSSequence(String text) {
        StringBuffer sequence = new StringBuffer();
        if( text.length() == 0 ) return sequence.toString();
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        if( sentences.size() == 0 ) return sequence.toString();
        for (CoreLabel token : sentences.get(0).get(CoreAnnotations.TokensAnnotation.class)) {
            String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            String word = token.get(CoreAnnotations.TextAnnotation.class);
            if( !word.startsWith("e_") )
                sequence.append(pos + " ");
            else
                sequence.append(word + " ");
        }
        return sequence.toString().trim();
    }

    public String extractWordSequence(String text) {
        StringBuffer sequence = new StringBuffer();
        if( text.length() == 0 ) return sequence.toString();
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreLabel token : sentences.get(0).get(CoreAnnotations.TokensAnnotation.class)) {
            String word = token.get(CoreAnnotations.TextAnnotation.class);
            sequence.append(word + " ");
        }
        return sequence.toString().trim();
    }

    public String extractLemmaSequence(String text){
        StringBuffer sequence = new StringBuffer();
        if( text.length() == 0 ) return sequence.toString();
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        if( sentences.size() == 0  )
            return "";
        for (CoreLabel token : sentences.get(0).get(CoreAnnotations.TokensAnnotation.class)) {
            String word = token.get(CoreAnnotations.LemmaAnnotation.class);
            sequence.append(word + " ");
        }
        return sequence.toString().trim();
    }

    public String extractLemmaSequence(String text, Set<String> stopWords){
        StringBuffer sequence = new StringBuffer();
        if( text.length() == 0 ) return sequence.toString();
        text = text.replace("�", "");
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        if( sentences.size() == 0  )
            return "";
        for (CoreLabel token : sentences.get(0).get(CoreAnnotations.TokensAnnotation.class)) {
            String word = token.get(CoreAnnotations.LemmaAnnotation.class);
            if( !stopWords.contains(word) )
                sequence.append(word + " ");
        }
        return sequence.toString().trim();
    }

    public List<String> extractSentences(String text){
        StringBuffer tempBuffer = new StringBuffer();
        for(int i = 0; i < text.length(); i++){
            if( text.charAt(i) == '.' && text.length() > i+1 && text.charAt(i+1) >= 'A' && text.charAt(i+1) <= 'Z' ){
                tempBuffer.append(text.charAt(i)+" ");
            }
            else
                tempBuffer.append(text.charAt(i));
        }

        List<String> result = new ArrayList<String>();
        Annotation document = new Annotation(tempBuffer.toString());
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences){
            String[] temp = sentence.toString().split(";");
            result.addAll(Arrays.asList(temp));
        }
        return result;
    }

    public static void main(String[] args){
        NLPUtil.parser_instance.extractSyntacticTree("what character did john noble play in lord of the rings");
    }

}
