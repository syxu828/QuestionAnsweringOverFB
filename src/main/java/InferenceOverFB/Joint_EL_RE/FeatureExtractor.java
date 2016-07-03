package InferenceOverFB.Joint_EL_RE;

import Util.FileUtil;
import Util.NLPUtil;
import Util.Virtuoso;

import java.util.*;

public class FeatureExtractor {
	public HashMap<String, Integer> two_grams_regressionModel;
	public HashMap<String, String> expectedTypeForRel;

	public static FeatureExtractor instance;

	static {
		instance = new FeatureExtractor();
	}

	public FeatureExtractor(){
		loadScheme();
	}

	public void loadScheme(){
		expectedTypeForRel = new HashMap<String, String>();
		List<String> lines = FileUtil.readFile("resources/Freebase/freebase.schema");
		for(String line : lines){
			String[] info = line.substring(0, line.length()-1).split("\t");
			String subj = info[0];
			String obj = info[2];
			String pred = info[1];
			if( pred.equals("ns:type.property.expected_type") ){
				expectedTypeForRel.put(subj, obj);
			}
		}
	}

	public double[] extractFeatureVector(Triple triple){

		String question = triple.question;

		StringBuffer featureBuffer = new StringBuffer();

		/*
		 * Feature #1: the entity linking score
		 */
		featureBuffer.append(triple.subjScore+" ");

		/*
		 * the edit distance between the mention and the entity name
		 */
//		List<String> names = instance.retrieveNames(triple.subj);
//		double minDis = triple.subjSurface.length();
//		for(String name : names){
//			int dis = extractor.distance(name, triple.subjSurface);
//			if( dis < minDis ){
//				minDis = dis;
//			}
//		}
//		featureBuffer.append(minDis+" ");

		/*
		 * Feature #2: the overlap token number between the entity name and the question
		*/
		List<String> names = Virtuoso.instance.retrieveNames(triple.subj);
		int overLapped = 0;
		for(String name : names){
			int overlap = overlapped(name, question);
			if( overlap > overLapped )
				overLapped = overlap;
		}
		featureBuffer.append(overLapped+" ");

		/*
		 * Feature #3: the description clue feature
		 */
		String description = Virtuoso.instance.retrieveDescription(triple.subj);
		int overlap = overlapped(question, description);
		featureBuffer.append(overlap+" ");

		/*
		 * Feature #4: the relation linking score
		 */
		featureBuffer.append(triple.predScore+" ");

		/*
		 * TODO:the tf-idf score of the predicated relation
		 */

		/*
		 * Feature #5: the coherence between the question word and the relation
		 */
		String rel = triple.pred;
		if( rel.indexOf("..") != -1 ){
			rel = rel.split("\\.\\.")[1];
		}
		String expectedAnsType = expectedTypeForRel.get("ns:"+rel).replace("ns:", "");
		featureBuffer.append(frequency_qw_rel(question, expectedAnsType)+" ");

		/*
		 * Feature #6: the regression score between the question and the relation
		 */

		String predString = rel;
		if( predString.indexOf("..") != -1 ){
			predString = predString.split("\\.\\.")[1];
		}
		String[] info = predString.split("\\.");
		String predToken = info[info.length-1];
		String[] questionTokens = NLPUtil.lemma_instance.extractLemmaSequence(question).split(" ");
		double regression_score = computeRegressionScore(questionTokens, predToken);
		featureBuffer.append(regression_score+" ");

		/*
		 * the answer size
		 */
//		featureBuffer.append(triple.answers.size()+" ");

		/*
		 * the answer type coherence in relation and type indicators
		 */
//        String tempSurface = this.question.replace(triple.subjSurface, "").replace("  ", " ").trim();
//        List<String> categories = this.categorySolver.solve(tempSurface);
//        double prob = 0.0;
//        for (String category : categories) {
//            info = category.split(" ");
//            if (info[0].equals(expectedAnsType)) {
//                prob = Double.parseDouble(info[1]);
//                break;
//            }
//        }
//        featureBuffer.append(prob + " ");

		/*
		 * if
		 */

		/*
		 * summerize
		 */
		info = featureBuffer.toString().trim().split(" ");
		double vector[] = new double[info.length];
		for(int i = 0; i < info.length; i++)
			vector[i] = Double.parseDouble(info[i]);
		return vector;
	}
	
	public int distance(String p, String q){
		p = p.toLowerCase().replace(" ", "");
		q = q.toLowerCase().replace(" ", "");
		
		int pLen = p.length();
		int qLen = q.length();
		int[][] minDis = new int[pLen][qLen];
		if( p.charAt(0) == q.charAt(0) )
			minDis[0][0] = 0;
		else
			minDis[0][0] = 1;
		for(int i = 0; i < pLen; i++){
			for(int j = 1; j < qLen; j++){
				if( p.charAt(i) == q.charAt(j) ){
					if( i == 0 ){
						minDis[i][j] = j;
					}
					else
						minDis[i][j] = minDis[i-1][j-1];
				}
				else{
					int min_1 = 0;
					if( i == 0 )
						min_1 = j+1;
					else
						min_1 = minDis[i-1][j]+1;
					int min_2 = minDis[i][j-1]+1;
					if( min_1 <= min_2 )
						minDis[i][j] = min_1;
					else
						minDis[i][j] = min_2;
				}
			}
		}
		return minDis[pLen-1][qLen-1];
	}
	
	public double frequency_qw_rel(String question, String expectedType){
		String[] questionWords = {"what", "when", "what", "where", "which", "how", "who"};
		int minIndex = question.split(" ").length;
		String word = new String();
		for(int i = 0; i < questionWords.length; i++){
			int index = Arrays.asList(question.replace("'s", " 's").split(" ")).indexOf(questionWords[i]);
			if( index != -1 && index < minIndex ){
				minIndex = index;
				word = questionWords[i];
			}
		}
		if( word.length() == 0 )
			return 0.0;
		FileUtil util = new FileUtil();
		List<String> lines = util.readFile("resources/Freebase/qw_rel.cooccur");
		String key = word+" "+expectedType;
		for(String line : lines){
			if( line.startsWith(key) ){
				return Double.parseDouble(line.split("\t")[1]);
			}
		}
		return 0.0;
	}
	
	public int overlapped(String p, String q){
		
		String[] stopwords = {"is","are","which","where","who","what","how","of","in","to","for","i","the","does",
				"were","was","did"};
		
		String[] pTokens = p.toLowerCase().split(" ");
		List<String> qTokens = Arrays.asList(q.toLowerCase().split(" "));
		Set<String> overlap = new HashSet<String>();
		for(int i = 0; i < pTokens.length; i++){
			if( qTokens.contains(pTokens[i]) && !Arrays.asList(stopwords).contains(pTokens[i])){
				overlap.add(pTokens[i]);
			}
		}
		return overlap.size();
	}

	//this function is used to compute the regression score of question token and predicate token
	public double computeRegressionScore(String[] questionTokens, String predToken){
		if( two_grams_regressionModel == null ){
			two_grams_regressionModel = new HashMap<String, Integer>();
			List<String> two_grams = FileUtil.readFile("resources/RegressionModel/2grams.txt");
			int index = 0;
			for(String gram : two_grams){
				two_grams_regressionModel.put(gram, index++);
			}
		}
		double score = 0.0;
		
		Set<Integer> indexes = new HashSet<Integer>();
		for(String questionToken : questionTokens){
			String two_gram = questionToken+" "+predToken;
			if( two_grams_regressionModel.containsKey(two_gram) ){
				indexes.add(two_grams_regressionModel.get(two_gram));
			}
		}
		
		List<Integer> indexList = new ArrayList(indexes);
		Collections.sort(indexList, new Comparator<Integer>(){
			public int compare(Integer i1, Integer i2){
				if( i1 > i2 )
					return 1;
				else if( i2 > i1 )
					return -1;
				else
					return 0;
			}
		});
		
		List<String> outputs = new ArrayList<String>();
		StringBuffer featureVector = new StringBuffer("0 ");
		for(Integer index : indexList)
			featureVector.append(index+":1 ");
		outputs.add(featureVector.toString().trim());
		FileUtil.writeFile(outputs, "resources/tool/libsvm-3.20/fuzzyRE/MyData.data.ordered", false);
			
		Process process;
		String[] rank_cmd = { "resources/tool/libsvm-3.20/svm-predict",
				"resources/tool/libsvm-3.20/fuzzyRE/MyData.data.ordered",
				"resources/tool/libsvm-3.20/fuzzyRE/svm.model",
				"resources/tool/libsvm-3.20/fuzzyRE/predicted" };
		
		try {  
            Runtime runtime = Runtime.getRuntime();  
            process = runtime.exec(rank_cmd);
            int exitVal = process.waitFor();
            if( exitVal == 0 ){
            	List<String> lines = FileUtil.readFile("resources/tool/libsvm-3.20/fuzzyRE/predicted");
				if( lines.size() > 1 )
            		return Double.parseDouble(lines.get(0));
				else
					return score;
            }
        }catch(Exception e){
        	e.printStackTrace();
        }
		
		return score;
	}
	
}
