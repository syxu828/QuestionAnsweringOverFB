package RelationExtraction.CNN;
import java.util.Random;


public class CNNOption {
	public int randomSeed = new Random().nextInt();
	public int numHid = 50;
	
	public int ConvolutionHid = 200;
	public int ActivationHid = 100;
	
	public int lexicalFeatureHid = 50 * 8;
	public int windowSize = 3;
	public int numClasses;
	public String unkWord = "*UNK*";
	TrainOption trainOption = new TrainOption();
}
