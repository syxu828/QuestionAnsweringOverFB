package RelationExtraction.CNN;
import Util.FileUtil;
import Util.NLPUtil;
import edu.stanford.nlp.util.Generics;
import org.ejml.simple.SimpleMatrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class Train {
	
	public void train(CNNModel model, List<Item> trainingItems, String paramPath) throws Exception{
		double[] sumGradSquare = new double[model.totalParamSize()];
		Arrays.fill(sumGradSquare, 0.0);
		
		List<Item> trainingItemsCopy = trainingItems;
		   
		int numBatches = trainingItemsCopy.size() / model.op.trainOption.batchSize + 1;
		System.err.println("Training on " + trainingItemsCopy.size() + " instances in " + numBatches + " batches");
		
		for(int t = 0; t < model.op.trainOption.epochs; t++)
		{
			System.err.println("Resetting adagrad weights to " + 0.0);
			sumGradSquare = new double[model.totalParamSize()];
			
			long begin = System.currentTimeMillis();
			System.err.println("======================================");
		    System.err.println("Starting epoch " + t );
		    
		    List<Item> shuffledSentences = Generics.newArrayList(trainingItemsCopy);
		    Collections.shuffle(shuffledSentences, model.rand);
		    
		    for (int batch = 0; batch < numBatches; ++batch) {
		        int startTree = batch * model.op.trainOption.batchSize;
		        int endTree = (batch + 1) * model.op.trainOption.batchSize;
		        if (endTree + model.op.trainOption.batchSize > shuffledSentences.size()) {
		          endTree = shuffledSentences.size();
		        }
		        
		        if( startTree == endTree )
		        	break;
		        
		        executeOneTrainingBatch(model, shuffledSentences.subList(startTree, endTree), sumGradSquare);
		    }
		    
			long end = System.currentTimeMillis();
			System.err.println("cost time: "+ (end-begin) + "ms");
			
			this.saveParameters(model, paramPath+"/params."+t);
		}
	}
	
	public void executeOneTrainingBatch(CNNModel model, List<Item> trainingBatch, double[] sumGradSquare)
	{
		CostAndGradient gcFunc = new CostAndGradient(model, trainingBatch);
		double[] theta = model.paramsToVector();
		
		double eps = 1e-3;
		double[] gradf = gcFunc.calculate(theta);
		
		for(int feature = 0; feature < gradf.length; feature++)
		{
			sumGradSquare[feature] = sumGradSquare[feature] + gradf[feature] * gradf[feature];
			theta[feature] = theta[feature] - (model.op.trainOption.learningRate * gradf[feature] / ( Math.sqrt(sumGradSquare[feature]) + eps ) );
//			theta[feature] = theta[feature] - model.op.trainOption.learningRate * gradf[feature];
		}
		
		model.vectorToParams(theta);
	}
	
	public List<Item> readTrainingItem(String relationPath, String inputPath) throws Exception{
		List<String> lines = FileUtil.readFile(relationPath);
		int numClasses = lines.size();
		
		NLPUtil nlpHelper = new NLPUtil("LEMMA");
		lines = FileUtil.readFile(inputPath);
		List<Item> items = new ArrayList<Item>();
		for(String line : lines){
			String[] info = line.split("\t");
			
			if( info[1].equals("-1") )
				continue;
			
			String question = nlpHelper.extractLemmaSequence(info[0]);
			Item item = new Item();
			item.goldClass = Integer.parseInt(info[1]);
			item.question = question;
			item.goldDistribution = new SimpleMatrix(numClasses, 1);
			item.goldDistribution.set(item.goldClass, 0, 1.0);
			items.add(item);
		}
		
		return items;
	}
	
	public void saveParameters(CNNModel model, String path) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		SimpleMatrix sm = model.ConvolutionMatrix;
		bw.write(sm.numRows()+"\t"+sm.numCols()+"\n");
		for(int i = 0; i < sm.numRows(); i++){
			for(int j = 0; j < sm.numCols(); j++){
				bw.write(sm.get(i, j)+"\t");
			}
			bw.write("\n");
		}
		
		sm = model.ActivationMatrix;
		bw.write(sm.numRows()+"\t"+sm.numCols()+"\n");
		for(int i = 0; i < sm.numRows(); i++){
			for(int j = 0; j < sm.numCols(); j++){
				bw.write(sm.get(i, j)+"\t");
			}
			bw.write("\n");
		}
		
		sm = model.classificationMatrix;
		bw.write(sm.numRows()+"\t"+sm.numCols()+"\n");
		for(int i = 0; i < sm.numRows(); i++){
			for(int j = 0; j < sm.numCols(); j++){
				bw.write(sm.get(i, j)+"\t");
			}
			bw.write("\n");
		}
		
		Iterator<String> it = model.wordVectors.keySet().iterator();
		while( it.hasNext() ){
			String word = it.next();
			bw.write(word+"\t");
			sm = model.wordVectors.get(word);
			for(int i = 0; i < sm.numRows(); i++)
				bw.write(sm.get(i, 0)+"\t");
			bw.write("\n");
		}
		
		bw.close();
	}
	
	public void begin(String relationPath, String dataPath, String paramPath) throws Exception{
		CNNOption option = new CNNOption();
		List<Item> trainingItems = readTrainingItem(dataPath, relationPath);
		System.err.println(trainingItems.size());
		CNNModel model = new CNNModel(option, trainingItems);
		train(model, trainingItems, paramPath);
//		model.saveParam(paramPath);
	}
	
	public void begin(List<String> dataPaths, String relationPath, String paramPath) throws Exception{
		CNNOption option = new CNNOption();
		List<Item> trainingItems = new ArrayList<Item>();
		for(String dataPath : dataPaths){
			trainingItems.addAll(readTrainingItem(relationPath,  dataPath));
		}
		System.err.println(trainingItems.size());
		CNNModel model = new CNNModel(option, trainingItems);
		train(model, trainingItems, paramPath);
//		model.saveParam(paramPath);
	}
	
	public static void main(String[] args) throws Exception{
		Train train = new Train();
		List<String> trainingPaths = new ArrayList<String>();
		trainingPaths.add("resources/RE/train.data");
		trainingPaths.add("resources/RE/dev.data");
		trainingPaths.add("resources/RE/test.data");
		train.begin(trainingPaths, "resources/RE/classLabels.txt", "resources/RE/param");
	}
}
