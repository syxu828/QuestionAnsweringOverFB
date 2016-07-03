package RelationExtraction.CNN;

public class TrainOption {
	int batchSize = 20;
	double learningRate = 0.01;
	int epochs = 80;
	public boolean loadWordVector = true;
	public double regClassification = 0.001;
	public double regWordVector = 0.0001;
	public double regActivationVector = 0.0001;
	public double regConvolutionVector = 0.0001;
	
}
