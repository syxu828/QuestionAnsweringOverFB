package RelationExtraction.CNN;
import org.ejml.simple.SimpleMatrix;

import java.util.HashSet;


public class Item {
	public SimpleMatrix predicatedDis;
	public SimpleMatrix goldDistribution;
	public SimpleMatrix SentenceVector;
	public SimpleMatrix ConvolutedVector;
	public SimpleMatrix ActivatedVector;
	public SimpleMatrix maxIndexMatrix;
	public SimpleMatrix nodeVector;
	public int goldClass;
	public HashSet<String> golds;
	public String[] tokens;
	public String question;
	public int predicatedClass;
	
	public SimpleMatrix finalFeatureVector;
	
	public Item(){
		
	}
	
	Item(Item t){
		goldDistribution = t.goldDistribution;
		goldClass = t.goldClass;
		question = t.question;
		golds = new HashSet<String>();
	}
	
}
