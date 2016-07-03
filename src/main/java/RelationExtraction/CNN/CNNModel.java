package RelationExtraction.CNN;

import Util.FileUtil;
import edu.stanford.nlp.neural.NeuralUtils;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class CNNModel {

	public HashMap<String, SimpleMatrix> wordVectors;
	
	public SimpleMatrix ConvolutionMatrix;

	public int ConvolutionMatrixSize;
	
	public SimpleMatrix ActivationMatrix;

	public int ActivationMatrixSize;
	
	public SimpleMatrix classificationMatrix;

	public int classificationMatrixSize;

	static final String UNKNOWN_WORD = "*UNK*";

	static final String START_WORD = "*START*";

	static final String END_WORD = "*END*";

	public Random rand;

	public CNNOption op;
	
	public CNNModel(CNNOption op, String paramPath) throws Exception {
		rand = new Random(new Random().nextInt());
		this.op = op;
		this.loadParamFrom(paramPath);
		ConvolutionMatrixSize = ConvolutionMatrix.numRows() * ConvolutionMatrix.numCols();
		ActivationMatrixSize = ActivationMatrix.numRows() * ActivationMatrix.numCols();
		classificationMatrixSize = classificationMatrix.numRows() * classificationMatrix.numCols();
	}

	public CNNModel(CNNOption op, List<Item> trainingInstances) throws Exception {
		rand = new Random(new Random().nextInt());
		this.op = op;
		for(Item item : trainingInstances){
			if( item.goldClass > op.numClasses ){
				op.numClasses = item.goldClass;
			}
		}
		op.numClasses++;
		
		ConvolutionMatrix = randomConvolutionMatrix();
		ConvolutionMatrixSize = ConvolutionMatrix.numRows() * ConvolutionMatrix.numCols();

		ActivationMatrix = randomActivationMatrix();
		ActivationMatrixSize = ActivationMatrix.numRows() * ActivationMatrix.numCols();

		classificationMatrix = randomClassificationMatrix();
		classificationMatrixSize = classificationMatrix.numRows() * classificationMatrix.numCols();

		initRandomWordVectors(trainingInstances);

	}

	public CNNModel(String paramFilePath) throws Exception {
		FileUtil util = new FileUtil();
		List<String> lines = util.readFile(paramFilePath);
		String line = lines.get(0);
		int row = Integer.parseInt(line.split("\t")[0]);
		int col = Integer.parseInt(line.split("\t")[1]);
		this.ConvolutionMatrix = new SimpleMatrix(row, col);
		int index = 1;
		int count = 0;
		while( count < row ){
			String[] ele = lines.get(index).split("\t");
			for (int j = 0; j < col; j++)
				this.ConvolutionMatrix.set(count, j, Double.parseDouble(ele[j]));
			index++;
			count++;
		}
		
		line = lines.get(index);
		row = Integer.parseInt(line.split("\t")[0]);
		col = Integer.parseInt(line.split("\t")[1]);
		this.ActivationMatrix = new SimpleMatrix(row, col);
		count = 0;
		while( count < row ){
			String[] ele = lines.get(index).split("\t");
			for (int j = 0; j < col; j++)
				this.ActivationMatrix.set(count, j, Double.parseDouble(ele[j]));
			index++;
			count++;
		}
		
		
		line = lines.get(index);
		row = Integer.parseInt(line.split("\t")[0]);
		col = Integer.parseInt(line.split("\t")[1]);
		this.classificationMatrix = new SimpleMatrix(row, col);
		count = 0;
		while( count < row ){
			String[] ele = lines.get(index).split("\t");
			for (int j = 0; j < col; j++)
				this.classificationMatrix.set(count, j, Double.parseDouble(ele[j]));
			index++;
			count++;
		}
		
		this.wordVectors = new HashMap<String, SimpleMatrix>();
		for(int i = index; i < lines.size(); i++){
			line = lines.get(i);
			String[] info = line.split("\t");
			String word = info[0];
			SimpleMatrix sm = new SimpleMatrix(info.length - 1, 1);
			for (int j = 1; j < info.length; j++)
				sm.set(j - 1, 0, Double.parseDouble(info[j]));
			this.wordVectors.put(word, sm);
		}

		this.op = new CNNOption();
	}

	public int totalParamSize() {
		int totalSize = ConvolutionMatrixSize + ActivationMatrixSize + classificationMatrixSize + wordVectors.size() * op.numHid;
		return totalSize;
	}
	
	SimpleMatrix randomActivationMatrix() {
		SimpleMatrix score = new SimpleMatrix(op.ActivationHid, op.ConvolutionHid + 1);
		double range = 1.0 / (Math.sqrt((double) op.ConvolutionHid));
		score.insertIntoThis(0, 0, SimpleMatrix.random(op.ActivationHid, op.ConvolutionHid, -range, range, rand));
		score.insertIntoThis(0, op.ConvolutionHid, SimpleMatrix.random(op.ActivationHid, 1, 0.0, 1.0, rand));
		return score;
	}
	
	SimpleMatrix randomConvolutionMatrix() {
		SimpleMatrix score = new SimpleMatrix(op.ConvolutionHid, op.windowSize * op.numHid + 1);
		double range = 1.0 / (Math.sqrt((double) op.windowSize * op.numHid ));
		score.insertIntoThis(0, 0, SimpleMatrix.random(op.ConvolutionHid, op.windowSize * op.numHid, -range, range, rand) );
		score.insertIntoThis(0, op.windowSize * op.numHid, SimpleMatrix.random(op.ConvolutionHid, 1, 0.0, 1.0, rand));
		return score;
	}
	

	SimpleMatrix randomClassificationMatrix() {
		SimpleMatrix score = new SimpleMatrix(op.numClasses, op.ActivationHid + 1);
		double range = 1.0 / (Math.sqrt((double) op.ActivationHid));
		score.insertIntoThis(0, 0, SimpleMatrix.random(op.numClasses, op.ActivationHid, -range, range, rand));
		score.insertIntoThis(0, op.ActivationHid, SimpleMatrix.random(op.numClasses, 1, 0.0, 1.0, rand));
		return score;
	}

	public HashMap<String, SimpleMatrix> loadWordVectors() throws Exception {
		FileUtil util = new FileUtil();
		HashMap<String, SimpleMatrix> embedding = new HashMap<String, SimpleMatrix>();
		List<String> words = util.readFile("resources/embeddings/words.lst");
		
		List<String> embeddings = util.readFile("resources/embeddings/embeddings.txt");
		for(int i = 0; i < words.size(); i++){
			String word = words.get(i);
			SimpleMatrix sm = null;
			if(!embedding.containsKey(word))
				embedding.put(word, new SimpleMatrix(this.op.numHid, 1));

			sm = embedding.get(word);

			String[] vector = embeddings.get(i).split(" ");
			for (int j = 0; j < this.op.numHid; j++)
				sm.set(j, 0, Double.parseDouble(vector[j]));
		}
		return embedding;
	}

	void initRandomWordVectors(List<Item> items) throws Exception {
		Set<String> words = new HashSet<String>();
		words.add(this.UNKNOWN_WORD);
		words.add(this.START_WORD);
		words.add(this.END_WORD);

		for (int i = 0; i < items.size(); i++) {
			String[] tokens = items.get(i).question.split(" ");
			for (int j = 0; j < tokens.length; j++) {
				words.add(tokens[j]);
			}
			
		}
		this.wordVectors = this.loadWordVectors();
		for (String word : words) {
			if(!wordVectors.containsKey(word)){
				SimpleMatrix vector = randomWordVector(op.numHid, rand);
				wordVectors.put(word, vector);
			}
		}
	}

	static SimpleMatrix randomWordVector(int size, Random rand) {
		return NeuralUtils.randomGaussian(size, 1, rand).scale(0.1);
	}

	public void loadParamFrom(String paramPath) throws Exception {
		FileUtil util = new FileUtil();
		List<String> lines = util.readFile(paramPath);
		String line = lines.get(0);
		int row = Integer.parseInt(line.split("\t")[0]);
		int col = Integer.parseInt(line.split("\t")[1]);
		this.ConvolutionMatrix = new SimpleMatrix(row, col);
		int index = 1;
		int count = 0;
		while( count < ConvolutionMatrix.numRows() ){
			line = lines.get(index);
			String[] info = line.trim().split("\t");
			for (int j = 0; j < ConvolutionMatrix.numCols(); j++) {
				ConvolutionMatrix.set(count, j, Double.parseDouble(info[j]));
			}
			count++;
			index++;
		}
		
		line = lines.get(index);
		row = Integer.parseInt(line.split("\t")[0]);
		col = Integer.parseInt(line.split("\t")[1]);
		ActivationMatrix = new SimpleMatrix(row, col);
		count = 0;
		index++;
		while( count < ActivationMatrix.numRows() ){
			line = lines.get(index);
			String[] info = line.trim().split("\t");
			for (int j = 0; j < ActivationMatrix.numCols(); j++) {
				ActivationMatrix.set(count, j, Double.parseDouble(info[j]));
			}
			count++;
			index++;
		}
		
		line = lines.get(index);
		row = Integer.parseInt(line.split("\t")[0]);
		col = Integer.parseInt(line.split("\t")[1]);
		classificationMatrix = new SimpleMatrix(row, col);
		count = 0;
		index++;
		while( count < classificationMatrix.numRows() ){
			line = lines.get(index);
			String[] info = line.trim().split("\t");
			for (int j = 0; j < classificationMatrix.numCols(); j++) {
				classificationMatrix.set(count, j, Double.parseDouble(info[j]));
			}
			count++;
			index++;
		}
		
		this.wordVectors = new HashMap<String, SimpleMatrix>();
		for(int i = index; i < lines.size(); i++){
			line = lines.get(i);
			String[] info = line.split("\t");
			String word = info[0];
			SimpleMatrix sm = new SimpleMatrix(info.length - 1, 1);
			for (int j = 1; j < info.length; j++)
				sm.set(j - 1, 0, Double.parseDouble(info[j]));
			this.wordVectors.put(word, sm);
		}

	}

	public void saveParam(String paramPath){
		List<String> outputs = new ArrayList<String>();
		outputs.add( this.ConvolutionMatrix.numRows()+"\t"+this.ConvolutionMatrix.numCols() );
		for(int i = 0; i < this.ConvolutionMatrix.numRows(); i++){
			StringBuffer buffer = new StringBuffer();
			for(int j = 0; j < this.ConvolutionMatrix.numCols(); j++){
				buffer.append(this.ConvolutionMatrix.get(i, j)+" ");
			}
			outputs.add( buffer.toString().trim() );
		}
		outputs.add( this.ActivationMatrix.numRows()+"\t"+this.ActivationMatrix.numCols() );
		for(int i = 0; i < this.ActivationMatrix.numRows(); i++){
			StringBuffer buffer = new StringBuffer();
			for(int j = 0; j < this.ActivationMatrix.numCols(); j++){
				buffer.append(this.ActivationMatrix.get(i, j)+" ");
			}
			outputs.add( buffer.toString().trim() );
		}
		outputs.add( this.classificationMatrix.numRows()+"\t"+this.classificationMatrix.numCols() );
		for(int i = 0; i < this.classificationMatrix.numRows(); i++){
			StringBuffer buffer = new StringBuffer();
			for(int j = 0; j < this.classificationMatrix.numCols(); j++){
				buffer.append(this.classificationMatrix.get(i, j)+" ");
			}
			outputs.add( buffer.toString().trim() );
		}
		for(String word : wordVectors.keySet()){
			StringBuffer buffer = new StringBuffer();
			buffer.append(word+" ");
			for(int i = 0; i < wordVectors.get(word).numRows(); i++){
				buffer.append(wordVectors.get(word).get(i, 0)+" ");
			}
			outputs.add(buffer.toString().trim());
		}
		FileUtil util = new FileUtil();
		util.writeFile(outputs, paramPath, false);
	}
	
	public double[] paramsToVector() {
		int totalSize = totalParamSize();
		double[] vector = new double[totalSize];
		int index = 0;
		SimpleMatrix sm = this.ConvolutionMatrix;
		int numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			vector[index++] = sm.get(i);
		}

		sm = this.ActivationMatrix;
		numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			vector[index++] = sm.get(i);
		}
		
		sm = this.classificationMatrix;
		numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			vector[index++] = sm.get(i);
		}
		
		Iterator<SimpleMatrix> matrixIterator = wordVectors.values().iterator();
		while (matrixIterator.hasNext()) {
			SimpleMatrix matrix = matrixIterator.next();
			numElements = matrix.getNumElements();
			for (int i = 0; i < numElements; ++i) {
				vector[index++] = matrix.get(i);
			}
		}

		return vector;
	}

	public void vectorToParams(double[] theta) {
		int index = 0;
		SimpleMatrix sm = this.ConvolutionMatrix;
		int numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			sm.set(i, theta[index++]);
		}

		sm = this.ActivationMatrix;
		numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			sm.set(i, theta[index++]);
		}
		
		sm = this.classificationMatrix;
		numElements = sm.getNumElements();
		for (int i = 0; i < numElements; i++) {
			sm.set(i, theta[index++]);
		}
		
		Iterator<SimpleMatrix> matrixIterator = wordVectors.values().iterator();
		while (matrixIterator.hasNext()) {
			SimpleMatrix matrix = matrixIterator.next();
			numElements = matrix.getNumElements();
			for (int i = 0; i < numElements; ++i) {
				sm.set(i, theta[index++]);
			}
		}
	}
}
