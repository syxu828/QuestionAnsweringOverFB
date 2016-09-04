package InferenceOverWiki;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by miaomiao on 16/9/3.
 */
public class LuceneSearchAPI {
    public static String IndexDir = "/Users/kun/Documents/wikipedia/index3";
    public static IndexSearcher searcher;
    public static Analyzer analyzer;
    public static int NumLimit = 10000;

    static{
        try{
            searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(IndexDir))));
            analyzer = new StandardAnalyzer();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String> cleverFilter(String surface, List<String> sentences){
        List<String> result = new ArrayList<String>();
        for(String sentence : sentences){
            if( sentence.trim().toLowerCase().indexOf(surface) != -1 ){
                result.add(sentence);
            }
        }
        if( result.size() == 0 )
            result = sentences;
        return result;
    }

    public static List<String> findSentences(String input){
        List<String> sentences = new ArrayList<String>();
        try{
            Query q = new QueryParser("sentence", analyzer).parse(input);
            TopScoreDocCollector collector = TopScoreDocCollector.create(NumLimit);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            for (ScoreDoc hit : hits)
                sentences.add(searcher.doc(hit.doc).get("sentence"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return cleverFilter(input, sentences);
    }

    public static List<String> findParagraphs(String input){
        List<String> paragraphs = new ArrayList<String>();
        try{
            Query q = new QueryParser("paragraph", analyzer).parse(input);
            TopScoreDocCollector collector = TopScoreDocCollector.create(NumLimit);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            for (int i = 0; i < hits.length; i++)
                paragraphs.add("#"+i+":"+searcher.doc(hits[i].doc).get("paragraph"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return paragraphs;
    }

    public static List<String> findTitles(String input){
        List<String> titles = new ArrayList<String>();
        try{
            Query q = new QueryParser("paragraph", analyzer).parse(input);
            TopScoreDocCollector collector = TopScoreDocCollector.create(NumLimit);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            for (int i = 0; i < hits.length; i++)
                titles.add(searcher.doc(hits[i].doc).get("title"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return titles;
    }

}
