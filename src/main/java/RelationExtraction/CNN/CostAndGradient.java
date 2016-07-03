package RelationExtraction.CNN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ejml.simple.SimpleMatrix;

import edu.stanford.nlp.neural.NeuralUtils;
import edu.stanford.nlp.util.Generics;


public class CostAndGradient {
	public final CNNModel model;
	public final List<Item> depPaths;
	
	SimpleMatrix classificationMatrixD;
	SimpleMatrix ConvolutionMatrixD;
	SimpleMatrix ActivationMatrixD;
	Map<String, SimpleMatrix> wordVectorD;
	
	public CostAndGradient(CNNModel model, List<Item> paths)
	{
		this.model = model;
		this.depPaths = paths;
		
		ConvolutionMatrixD = new SimpleMatrix(model.ConvolutionMatrix.numRows(), model.ConvolutionMatrix.numCols());
		ActivationMatrixD = new SimpleMatrix(model.ActivationMatrix.numRows(), model.ActivationMatrix.numCols());
		
		classificationMatrixD = new SimpleMatrix(model.classificationMatrix.numRows(), model.classificationMatrix.numCols());
		wordVectorD = new HashMap<String, SimpleMatrix>();
//		for (Map.Entry<String, SimpleMatrix> entry : model.wordVectors.entrySet()) {
//	        int numRows = entry.getValue().numRows();
//	        int numCols = entry.getValue().numCols();
//	        wordVectorD.put(entry.getKey(), new SimpleMatrix(numRows, numCols));
//	    }
	}
	
	public double[] calculate(double[] theta) {
		model.vectorToParams(theta);
		List<Item> processedItems = new ArrayList<Item>();
		
		for (Item depPath : depPaths) {
			Item trainingDepPath = new Item(depPath);
			forwardPropagateItem(trainingDepPath);
			processedItems.add(trainingDepPath);
		}
		
		for (Item depPath : processedItems) {
			backpropDerivativesAndError(depPath);
		}
		
		double scale = 1.0 / this.depPaths.size();
		
		ConvolutionMatrixD = RegularizeAndScale(ConvolutionMatrixD, scale, model.ConvolutionMatrix, model.op.trainOption.regConvolutionVector);
		ActivationMatrixD = RegularizeAndScale(ActivationMatrixD, scale, model.ActivationMatrix, model.op.trainOption.regActivationVector);
		classificationMatrixD = RegularizeAndScale(classificationMatrixD, scale, model.classificationMatrix, model.op.trainOption.regClassification);
		RegularizeAndScale(wordVectorD, scale, model.wordVectors, model.op.trainOption.regWordVector);
		
		return paramsToVector();
	}
	
	SimpleMatrix RegularizeAndScale(SimpleMatrix D, double scale, SimpleMatrix M, double cost){
		SimpleMatrix regMatrix = M;
		regMatrix = new SimpleMatrix(regMatrix);
		regMatrix.insertIntoThis(0, regMatrix.numCols() - 1, new SimpleMatrix(regMatrix.numRows(), 1));
		D = D.scale(scale).plus(regMatrix.scale(cost));
		return D;
	}
	
	void RegularizeAndScale(Map<String, SimpleMatrix> D, double scale, Map<String, SimpleMatrix> M, double cost){
		for(Map.Entry<String, SimpleMatrix> entry  : M.entrySet()){
			if( !this.wordVectorD.containsKey(entry.getKey()) ){
				this.wordVectorD.put(entry.getKey(), new SimpleMatrix(entry.getValue().numRows(), entry.getValue().numCols()));
				continue;
			}
			SimpleMatrix sm = D.get(entry.getKey());
			sm = sm.scale(scale).plus(entry.getValue().scale(cost));
			D.put(entry.getKey(), sm);
		}
	}
	
	void forwardPropagateItem(Item item) {

		String[] tokens = item.question.split(" ");
		SimpleMatrix itemVector = new SimpleMatrix(model.op.windowSize * model.op.numHid + 1, tokens.length);

		for (int i = 0; i < tokens.length; i++) {
			SimpleMatrix leftWordVector = null;
			if (i == 0)
				leftWordVector = model.wordVectors.get(model.START_WORD);
			else
				leftWordVector = model.wordVectors.get(tokens[i - 1]);

			SimpleMatrix midWordVector = model.wordVectors.get(tokens[i]);

			SimpleMatrix rightWordVector = null;
			if (i == tokens.length - 1)
				rightWordVector = model.wordVectors.get(model.END_WORD);
			else
				rightWordVector = model.wordVectors.get(tokens[i + 1]);

			for (int j = 0; j < model.op.windowSize; j++) {
				SimpleMatrix curVector = null;
				if (j == 0)
					curVector = leftWordVector;
				else if (j == 1)
					curVector = midWordVector;
				else if (j == 2)
					curVector = rightWordVector;

				for (int h = 0; h < model.op.numHid; h++)
					itemVector.set(h + j * model.op.numHid, i, curVector.get(h, 0));
			}

			for (int j = 0; j < itemVector.numCols(); j++) {
				itemVector.set(itemVector.numRows() - 1, j, 1.0);
			}
		}

		item.SentenceVector = itemVector;
		SimpleMatrix convolutionedVector = model.ConvolutionMatrix.mult(itemVector);

		SimpleMatrix layer1 = getMaxAtRow(convolutionedVector);
		item.ConvolutedVector = layer1.copy();

		SimpleMatrix maxIndex = getMaxIndexAtRow(convolutionedVector);
		item.maxIndexMatrix = maxIndex;

		SimpleMatrix activatedVector = NeuralUtils.elementwiseApplyTanh(model.ActivationMatrix.mult(NeuralUtils.concatenateWithBias(layer1)));
		item.ActivatedVector = activatedVector.copy();

		SimpleMatrix temp = model.classificationMatrix.mult(NeuralUtils.concatenateWithBias(item.ActivatedVector));
		SimpleMatrix predictions = NeuralUtils.softmax(temp);

		int predicatedClass = getPredicatedClass(predictions);
		item.predicatedDis = predictions.copy();
		item.predicatedClass = predicatedClass;
	}
	
	public int getPredicatedClass(SimpleMatrix predictions)
	{
		int argmax = 0;
		for(int i = 1; i < predictions.getNumElements(); i++){
			if( predictions.get(i) > predictions.get(argmax) ){
				argmax = i;
			}
		}
		return argmax;
	}
	
	SimpleMatrix getMaxAtRow(SimpleMatrix s){
		SimpleMatrix result = new SimpleMatrix(s.numRows(), 1);
		for(int i = 0; i < s.numRows(); i++)
		{
			double max = -Double.MAX_VALUE;
			for(int j = 0; j < s.numCols(); j++){
				if( s.get(i, j) > max ){
					max = s.get(i, j);
				}
			}
			result.set(i, 0, max);
		}
		return result;
	}
	
	SimpleMatrix getMaxIndexAtRow(SimpleMatrix s){
		SimpleMatrix result = new SimpleMatrix(s.numRows(), 1);
		for(int i = 0; i < s.numRows(); i++)
		{
			double max = -Double.MAX_VALUE;
			double maxIndex = -1;
			for(int j = 0; j < s.numCols(); j++){
				if( s.get(i, j) > max ){
					max = s.get(i, j);
					maxIndex = j;
				}
			}
			result.set(i, 0, maxIndex);
		}
		return result;
	}
	
	void backpropDerivativesAndError(Item item) {
		SimpleMatrix gold = item.goldDistribution;
		SimpleMatrix predicated = item.predicatedDis;

		SimpleMatrix deltaClass = predicated.minus(gold);

		SimpleMatrix sm = deltaClass.mult(NeuralUtils.concatenateWithBias(item.ActivatedVector).transpose());

		classificationMatrixD = classificationMatrixD.plus(sm);

		SimpleMatrix deltaFromClass = model.classificationMatrix.transpose().mult(deltaClass);

		SimpleMatrix activationDerivative = NeuralUtils.elementwiseApplyTanhDerivative(item.ActivatedVector);
		deltaFromClass = deltaFromClass.extractMatrix(0, model.op.ActivationHid, 0, 1)
				.elementMult(activationDerivative);
		ActivationMatrixD = ActivationMatrixD
				.plus(deltaFromClass.mult(NeuralUtils.concatenateWithBias(item.ConvolutedVector).transpose()));

		SimpleMatrix deltaFromActivation = model.ActivationMatrix.transpose().mult(deltaFromClass);
		deltaFromActivation = deltaFromActivation.extractMatrix(0, model.op.ConvolutionHid, 0, 1);

		for (int i = 0; i < ConvolutionMatrixD.numRows(); i++) {
			int index = (int) item.maxIndexMatrix.get(i);
			SimpleMatrix wordVector = item.SentenceVector.extractMatrix(0, item.SentenceVector.numRows(), index,
					index + 1);
			SimpleMatrix deltaFromConvolution = deltaFromActivation.extractMatrix(i, i + 1, 0, 1)
					.mult(wordVector.transpose());

			for (int j = 0; j < ConvolutionMatrixD.numCols(); j++) {
				ConvolutionMatrixD.set(i, j, (ConvolutionMatrixD.get(i, j) + deltaFromConvolution.get(0, j)));
			}

			String[] tokens = item.question.split(" ");
			SimpleMatrix deltaFromWordVector = model.ConvolutionMatrix
					.extractMatrix(i, i + 1, 0, model.ConvolutionMatrix.numCols()).transpose()
					.mult(deltaFromActivation.extractMatrix(i, i + 1, 0, 1));
			String leftWord = null;
			String midWord = null;
			String rightWord = null;
			if (index == 0)
				leftWord = model.START_WORD;
			else
				leftWord = tokens[index - 1];

			midWord = tokens[index];

			if (index == tokens.length - 1)
				rightWord = model.END_WORD;
			else
				rightWord = tokens[index + 1];

			SimpleMatrix leftWordMatrixD = deltaFromWordVector.extractMatrix(0, model.op.numHid, 0, 1);
			SimpleMatrix midWordMatrixD = deltaFromWordVector.extractMatrix(model.op.numHid, 2 * model.op.numHid, 0, 1);
			SimpleMatrix rightWordMatrixD = deltaFromWordVector.extractMatrix(2 * model.op.numHid, 3 * model.op.numHid,
					0, 1);
			if( !wordVectorD.containsKey(leftWord) )
				wordVectorD.put(leftWord, leftWordMatrixD);
			else
				wordVectorD.put(leftWord, wordVectorD.get(leftWord).plus(leftWordMatrixD));
			
			if( !wordVectorD.containsKey(midWord) )
				wordVectorD.put(midWord, midWordMatrixD);
			else
				wordVectorD.put(midWord, wordVectorD.get(midWord).plus(midWordMatrixD));
			
			if( !wordVectorD.containsKey(rightWord) )
				wordVectorD.put(rightWord, rightWordMatrixD);
			else
				wordVectorD.put(rightWord, wordVectorD.get(rightWord).plus(rightWordMatrixD));
		}
	}
	
	double[] paramsToVector(){
		int totalSize = model.totalParamSize();
		double[] vector = new double[totalSize];
		int index = 0;
		SimpleMatrix sm = ConvolutionMatrixD;
		int numElements = sm.getNumElements();
		for(int i = 0; i < numElements; i++){
			vector[index++] = sm.get(i);
		}
		
		sm = ActivationMatrixD;
		numElements = sm.getNumElements();
		for(int i = 0; i < numElements; i++){
			vector[index++] = sm.get(i);
		}
		
		sm = classificationMatrixD;
		numElements = sm.getNumElements();
		for(int i = 0; i < numElements; i++){
			vector[index++] = sm.get(i);
		}
		
		Iterator<SimpleMatrix> matrixIterator = wordVectorD.values().iterator();
		while (matrixIterator.hasNext()) {
			SimpleMatrix matrix = matrixIterator.next();
			numElements = matrix.getNumElements();
			for (int i = 0; i < numElements; ++i) {
				vector[index++] = matrix.get(i);
			}
		}
		
		return vector;
	}
}
