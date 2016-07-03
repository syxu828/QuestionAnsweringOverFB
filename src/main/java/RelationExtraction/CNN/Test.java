package RelationExtraction.CNN;
import Util.FileUtil;
import Util.NLPUtil;
import edu.stanford.nlp.neural.NeuralUtils;
import org.ejml.simple.SimpleMatrix;

import java.util.*;


public class Test {
	public CNNModel model;
	public NLPUtil nlpUtil = new NLPUtil("LEMMA");
	
	public boolean useCache;
	public Map<String, List<String>> cache;
	public String cacheFile;
	public int excutedTime = 0;
	
	public Test(boolean ifUseCache, String cachePath, String paramPath){
		try{
			this.useCache = ifUseCache;
			if( this.useCache ){
				cache = new HashMap<String, List<String>>();
				cacheFile = cachePath;
				this.loadCache();
			}
			CNNOption option = new CNNOption();
			this.model = new CNNModel(option, paramPath);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Test(String paramPath){
		try{
			this.useCache = false;
			CNNOption option = new CNNOption();
			this.model = new CNNModel(option, paramPath);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void loadCache(){
		List<String> lines = FileUtil.readFile(cacheFile);
		for(String line : lines){
			String[] info = line.split("\t");
			List<String> values = new ArrayList<String>();
			for(int i = 1; i < info.length; i++){
				values.add(info[i]);
			}
			cache.put(info[0], values);
		}
	}
	
	public void writeCache(){
		List<String> outputs = new ArrayList<String>();
		for(String key : cache.keySet()){
			List<String> values = cache.get(key);
			StringBuffer buffer = new StringBuffer();
			buffer.append(key+"\t");
			for(String value : values)
				buffer.append(value+"\t");
			outputs.add(buffer.toString().trim());
		}
		FileUtil.writeFile(outputs, cacheFile, false);
	}
	
	public SimpleMatrix getWordVector(String word){
		if( !model.wordVectors.containsKey(word) ){
			return model.wordVectors.get(model.UNKNOWN_WORD);
		}
		else
			return model.wordVectors.get(word); 
	}
	
	void forwardPropagateItem(Item item){
		String[] tokens = item.question.split(" ");
		SimpleMatrix itemVector = new SimpleMatrix(model.op.windowSize * model.op.numHid + 1, tokens.length);
		
		for(int i = 0; i < tokens.length; i++){
			SimpleMatrix leftWordVector = null;
			if( i == 0 )
				leftWordVector = model.wordVectors.get(model.START_WORD);
			else
				leftWordVector = model.wordVectors.get(tokens[i-1]);
			
			SimpleMatrix midWordVector = model.wordVectors.get(tokens[i]);
			
			SimpleMatrix rightWordVector = null;
			if( i == tokens.length-1 )
				rightWordVector = model.wordVectors.get(model.END_WORD);
			else
				rightWordVector = model.wordVectors.get(tokens[i+1]);
			
			if( leftWordVector == null ){
				leftWordVector = model.wordVectors.get(model.UNKNOWN_WORD);
			}
			if( midWordVector == null ){
				midWordVector = model.wordVectors.get(model.UNKNOWN_WORD);
			}
			if( rightWordVector == null ){
				rightWordVector = model.wordVectors.get(model.UNKNOWN_WORD);
			}
			
			for(int j = 0; j < model.op.windowSize; j++){
				SimpleMatrix curVector = null;
				if( j == 0 )
					curVector = leftWordVector;
				else if( j == 1 )
					curVector = midWordVector;
				else if( j == 2 )
					curVector = rightWordVector;
				
				for (int h = 0; h < model.op.numHid; h++)
					itemVector.set(h + j * model.op.numHid, i, curVector.get(h, 0));
			}
			
			for(int j = 0; j < itemVector.numCols(); j++){
				itemVector.set( itemVector.numRows()-1, j, 1.0  );
			}
		}
		
		item.SentenceVector = itemVector;
		SimpleMatrix convolutionedVector = model.ConvolutionMatrix.mult(itemVector);
		SimpleMatrix layer1 = getMaxAtRow(convolutionedVector);
		item.ConvolutedVector = layer1.copy();
		SimpleMatrix maxIndex = getMaxIndexAtRow(convolutionedVector);
		item.maxIndexMatrix = maxIndex;
		SimpleMatrix activatedVector = NeuralUtils.elementwiseApplyTanh( model.ActivationMatrix.mult(NeuralUtils.concatenateWithBias(layer1) ) );
		item.ActivatedVector = activatedVector.copy();
		item.finalFeatureVector = model.classificationMatrix.mult( NeuralUtils.concatenateWithBias(item.ActivatedVector) );
		SimpleMatrix predictions = NeuralUtils.softmax( item.finalFeatureVector );
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
	
	public List<Item> readTestItems(String dataPath){
		FileUtil util = new FileUtil();
		List<String> lines = util.readFile(dataPath);
		List<Item> items = new ArrayList<Item>();
		for(String line : lines){
			String[] info = line.split("\t");
			String question = nlpUtil.extractLemmaSequence(info[0]);
			Item item = new Item();
			item.question = nlpUtil.extractLemmaSequence(question);
			if( info.length > 1 && !info[1].equals("-1") )
				item.goldClass = Integer.parseInt(info[1]);
			items.add(item);
		}
		return items;
	}
	
	public List<String> executeOneDecode(Item item, int topK){
		if( this.useCache && this.cache.containsKey(item.question) ){
			return this.cache.get(item.question);
		}
		List<String> result = new ArrayList<String>();
		excutedTime++;
		forwardPropagateItem(item);
		SimpleMatrix sm = item.predicatedDis;
		int[] index = new int[sm.numRows()];
		
		class score_index{
			double score;
			int index;
		}
		List<score_index> indexes = new ArrayList<score_index>();
		
		for(int i = 0; i < sm.numRows(); i++){
			score_index t = new score_index();
			t.score = sm.get(i, 0);
			t.index = i;
			indexes.add(t);
		}
		
		Collections.sort(indexes, new Comparator<score_index>(){
			public int compare(score_index s1, score_index s2){
				if( s2.score > s1.score ){
					return 1;
				}else if( s2.score < s1.score ){
					return -1;
				}else
					return 0;
			}
		});
		
		if( topK == -1 ) 
			topK = index.length;
		for(int i = 0; i < indexes.size() && result.size() < topK; i++){
				result.add( indexes.get(i).index +" "+ indexes.get(i).score );
		}
		
		if( this.useCache )
			this.cache.put(item.question, result);
		return result;
	}

	public double begin(String dataPath, String outputPath, int topK) throws Exception{
		List<Item> testItems = this.readTestItems(dataPath);
		List<String> outputs = new ArrayList<String>();
		double sum = 0.0;
		double correct = 0.0;
		for(Item item : testItems){

			List<String> predicated = this.executeOneDecode(item, topK);
			for(int i = 0; i < predicated.size(); i++){
				if( predicated.get(i).split(" ")[0].equals(""+item.goldClass) ){
					correct++;
					break;
				}
			}
			
			sum++;
			StringBuffer buffer = new StringBuffer();
			for(String s : predicated){
				buffer.append(s+"\t");
			}
			outputs.add(buffer.toString());
		}
		FileUtil util = new FileUtil();
		util.writeFile(outputs, outputPath, false);
		System.err.println(correct / sum);
		return correct/sum;
	}
	
	public static void main(String[] args) throws Exception{

//		Test test = new Test("resources/RE/param/params.14");
//		test.begin("resources/RE/test.data", "resources/RE/test.predicated", 10);

		double max = 0;
		int maxIndex = 0;
		for(int i = 1; i <= 79; i++){
			System.err.println(i);
			Test test = new Test("resources/RE/param/params."+i);
			double rate = test.begin("resources/RE/test.data", "resources/RE/test.predicated", 5);
			if( rate > max )
			{
				max = rate;
				maxIndex = i;
			}
		}
		System.err.println(maxIndex);

//		test.begin("CNN/CategoryLinking/test.data", "CNN/CategoryLinking/params", "CNN/CategoryLinking/test.predicated", 10);
//		for(int i = 1; i < 9; i++){
//			System.err.println(i);
//			Test test = new Test("CNN/CategoryLinking/param/params."+i);
//			test.begin("CNN/CategoryLinking/test.data", "CNN/CategoryLinking/test.predicated", 1);
//		}
			
		
//		
	}
}
