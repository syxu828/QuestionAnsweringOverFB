package Util;

import java.io.File;
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;

public class Virtuoso {
	
	public static Virtuoso instance = new Virtuoso();
	public boolean useCache = true;
	Connection conn;
	String cacheFilePath = "resources/caches/virtuoso.cache";
	HashMap<String, List<String>> cache = new HashMap<String, List<String>>();
	HashMap<String, List<String>> updatedCache = new HashMap<String, List<String>>();

	public void connect()
	{
		try {
			Class.forName("virtuoso.jdbc4.Driver");
			conn = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/freebase","dba","dba");
			if( this.useCache )
				this.loadCache();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		try{
			if( conn != null ){
				 conn.close();
			}
			if( this.useCache ){
				List<String> outputs = writeCache();
				FileUtil.writeFile(outputs, cacheFilePath, true);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void loadCache(){
		List<String> lines = new ArrayList<String>();
		if( new File(cacheFilePath).exists() )
			lines = FileUtil.readFile(cacheFilePath);
		for(String line : lines){
			String[] info = line.split("\t");
			String key = info[0];
			List<String> values = new ArrayList<String>();
			for(int i = 1; i < info.length; i++)
				values.add(info[i]);
			this.cache.put(key, values);
		}
	}

	public List<String> writeCache(){
		List<String> lines = new ArrayList<String>();
		for(String key : this.updatedCache.keySet() ){
			StringBuffer buffer = new StringBuffer();
			List<String> values = this.updatedCache.get(key);
			buffer.append(key+"\t");
			for(String value : values)
				buffer.append(value+"\t");
			lines.add( buffer.toString().trim() );
		}
		return lines;
	}

	public Virtuoso(){
		try {
			this.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * An example of sparql: "sparql select * where { <fb:g.11vjx745d> ?y ?z}"
	 */
	public List<String> execute(String sparql)
	{
		if( this.cache.containsKey(sparql) ){
			return this.cache.get(sparql);
		}
		List<String> result = new ArrayList<String>();
		try{
			Statement stmt = conn.createStatement();
			boolean more = stmt.execute(sparql);
		    ResultSetMetaData data = stmt.getResultSet().getMetaData();
		    while(more)
		    {
		    	ResultSet rs = stmt.getResultSet();
				while(rs.next())
				{
					StringBuffer s = new StringBuffer();
				    for(int i = 1;i <= data.getColumnCount();i++)
				    {
				    	s.append(rs.getString(i) + "\t");
				    }
				    result.add(s.toString().trim());
				}
				more = stmt.getMoreResults();
		    }
		    stmt.close();
		    this.updatedCache.put(sparql, result);
		    return result;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public List<String> executeFuzzyQuery(String sp){
		List<String> names = new ArrayList<String>();
		String subj = sp.split("\t")[0];
		String pred = sp.split("\t")[1].trim();
		try{
			Set<String> result = new HashSet<String>();
			for(String triple : this.execute("sparql select ?x where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <"+pred.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?x .}")){
				String mid = triple;
				result.add(mid);
			}
			if( result.size() == 0 ){
				List<String> triples = this.execute("sparql select ?x ?z where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?x ?y."
						+ "?y <"+pred.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?z .}");
				if( triples.size() > 60000 )
					return names;
				String newPred = new String();
				for(String triple : triples){
					String mid = triple.split("\t")[1];
					newPred = triple.split("\t")[0].replace("http://rdf.freebase.com/ns/", "ns:")+".."+pred.replace("ns:", "");
					result.add(mid);
				}
				names.add(newPred);
			}
			else
				names.add(pred);
			
			
			result.remove(subj.replace("ns:", "http://rdf.freebase.com/ns/"));
			
			if( result.size() > 10000 )
				return names;
			for(String mid : result){
				if( !mid.startsWith("http://rdf.freebase.com/ns/") ){
					names.add("\""+URLDecoder.decode(new String(mid.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"")+"\"");
				}
				else{
					for(String triple : this.execute("sparql select * where {<"+mid+"> <http://rdf.freebase.com/ns/type.object.name> ?z .}")){
						names.add("\""+URLDecoder.decode(new String(triple.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"")+"\"");
					}
				}
			}
			return names;
			
		}catch(Exception e){
			
		}
		return names;
	}
	
	public Set<String> executeSPQuery(String sp) {
		Set<String> names = new HashSet<String>();
		try {
			Set<String> result = new HashSet<String>();
			String subj = sp.split("\t")[0];
			String pred = sp.split("\t")[1].trim();
			if (pred.indexOf("..") != -1) {
				String firstPredicate = pred.split("\\.\\.")[0];
				String secondPredicate = "ns:" + pred.split("\\.\\.")[1];
				for (String triple : this
						.execute("sparql select ?y where {<" + subj.replace("ns:", "http://rdf.freebase.com/ns/")
								+ "> <" + firstPredicate.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?x. "
								+ "?x <" + secondPredicate.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?y .}")) {
					String mid = triple;
					result.add(mid);
				}
			} else {
				for (String triple : this
						.execute("sparql select * where {<" + subj.replace("ns:", "http://rdf.freebase.com/ns/") + "> <"
								+ pred.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?z .}")) {
					String mid = triple;
					result.add(mid);
				}
			}
			result.remove(subj.replace("ns:", "http://rdf.freebase.com/ns/"));
			
			if( result.size() > 500 )
				return names;

			for (String mid : result) {
				if (!mid.startsWith("http://rdf.freebase.com/ns/")) {
					names.add("\"" + URLDecoder.decode(new String(mid.replace("%", "").getBytes("ISO8859-1")), "utf-8")
							.replace("\"", "\\\"") + "\"");
				} else {
					for (String triple : this.execute("sparql select * where {<" + mid
							+ "> <http://rdf.freebase.com/ns/type.object.name> ?z .}")) {
						names.add("\""
								+ URLDecoder.decode(new String(triple.replace("%", "").getBytes("ISO8859-1")), "utf-8")
										.replace("\"", "\\\"")
								+ "\"");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return names;
		}
	}

	public Set<String> retrieveNeighbourNodes(String sp){
		Set<String> results = new HashSet<String>();
		String[] info = sp.split("\t");
		String subj = "ns:"+info[0];
		String pred = "ns:"+info[1];
		if( pred.indexOf("..") == -1 )
			return results;
		String firstPred = pred.split("\\.\\.")[0];
		List<String> triples = this.execute("sparql select ?z where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <" + firstPred.replace("ns:", "http://rdf.freebase.com/ns/") + ">?x."
				+ "?x ?y ?z .}");
		
		for(String triple : triples){
			if( triple.equals(subj.replace("ns:", "http://rdf.freebase.com/ns/")))
				continue;
			if( !triple.startsWith("http://rdf.freebase.com/ns/") )
				results.add(triple);
			else
				results.addAll(this.retrieveNames(triple.replace("http://rdf.freebase.com/ns/", "")));
		}
		return results;
	}
	
	public Set<String> executePOQuery(String po){
		Set<String> names = new HashSet<String>();
		try{
			Set<String> result = new HashSet<String>();
			String pred = po.split("\t")[0].trim();
			String obj = po.split("\t")[1].trim();
			for (String triple : this.execute("sparql select * where {?x <" + pred.replace("ns:", "http://rdf.freebase.com/ns/") + "> <"
							+ obj.replace("ns:", "http://rdf.freebase.com/ns/") + "> .}")) {
				String mid = triple;
				result.add(mid);
			}
			
			result.remove(obj.replace("ns:", "http://rdf.freebase.com/ns/"));
			
			
			if( result.size() > 100 )
				return names;
			for(String mid : result){
				if( !mid.startsWith("http://rdf.freebase.com/ns/") ){
					names.add("\""+URLDecoder.decode(new String(mid.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"")+"\"");
				}
				else{
					for(String triple : this.execute("sparql select * where {<"+mid+"> <http://rdf.freebase.com/ns/type.object.name> ?z .}")){
						names.add("\""+URLDecoder.decode(new String(triple.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"")+"\"");
					}
				}
			}
		}catch(Exception e){
		}finally{
			return names;
		}
		
	}
	
	public Set<String> executeTriangle(String sp){
		Set<String> names = new HashSet<String>();
		try{
			Set<String> result = new HashSet<String>();
			String[] info = sp.split("\t");
			String subj = info[0];
			String pred_1 = info[1].trim();
			String pred_2 = info[2].trim();
			for (String triple : this
					.execute("sparql select ?y where {?x <" + pred_1.replace("ns:", "http://rdf.freebase.com/ns/") + "> <"
							+ subj.replace("ns:", "http://rdf.freebase.com/ns/") + "> . " + "?x <"
							+ pred_2.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?y .}")) {
				String mid = triple;
				result.add(mid);
			}

			if (result.size() > 100)
				return names;
			for (String mid : result) {
				if (!mid.startsWith("http://rdf.freebase.com/ns/")) {
					names.add("\"" + URLDecoder.decode(new String(mid.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"") + "\"");
				} else {
					for (String triple : this.execute(
							"sparql select * where {<" + mid + "> <http://rdf.freebase.com/ns/type.object.name> ?z .}")) {
						names.add("\"" + URLDecoder.decode(new String(triple.replace("%", "").getBytes("ISO8859-1")), "utf-8").replace("\"", "\\\"") + "\"");
					}
				}
			}
		}catch(Exception e){
			
		}finally{
			return names;
		}
	}
	
	public Set<String> retrieveAnswerType(String sp) throws Exception{
		Set<String> types = new HashSet<String>();
			String subj = sp.split("\t")[0];
			String pred = sp.split("\t")[1].trim();
			if( pred.indexOf("..") != -1 ){
				String firstPredicate = pred.split("\\.\\.")[0];
				String secondPredicate = "ns:"+pred.split("\\.\\.")[1];
				for(String triple : this.execute("sparql select ?x where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <"+firstPredicate.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?x. "
						+ "?x <"+secondPredicate.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?y .}" 
						+ "?y <http://rdf.freebase.com/ns/type.object.type> ?z ." )){
					types.add(triple);
				}
			}
			else{
				for(String triple : this.execute("sparql select ?y where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <"+pred.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?x ."
						+ "?x <http://rdf.freebase.com/ns/type.object.type> ?y ."
						+ "}")){
					types.add(triple);
				}
			}
		
		return types;
	}
	
	public Set<String> retrieveRelations(String subj) throws Exception{
		Set<String> relations = new HashSet<String>();
			for(String triple : this.execute("sparql select ?x ?z where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?x ?y. "
						+ "?y ?z ?m .}") ){
				triple = triple.replace("http://rdf.freebase.com/ns/", "");
				String[] info = triple.split("\t");
				relations.add("ns:"+info[0]);
				if( !info[1].equals("type.object.name") && !info[1].startsWith("user.") && !info[1].startsWith("base."))
					relations.add("ns:"+info[0]+".."+info[1]);
			}
		
		return relations;
	}
	
	public List<String> retrieveWikiIds(String mid) throws Exception{
		List<String> triples = this.execute("sparql select ?x where { <http://rdf.freebase.com/en/"+mid+"> <http://rdf.freebase.com/ns/type.namespace.keys> ?x}");
		for(int i = triples.size()-1; i >= 0; i--){
				System.err.println(triples.get(i));
		}
		return triples;
	}
	
	public List<String> retrieveNames(String mid){
		List<String> triples = this.execute("sparql select ?x where { <http://rdf.freebase.com/ns/"+mid+"> <http://rdf.freebase.com/ns/type.object.name> ?x}");
		return triples;
	}
	
	public String retrieveDescription(String mid){
		List<String> triples = this.execute("sparql select ?x where { <http://rdf.freebase.com/ns/"+mid+"> <http://rdf.freebase.com/ns/common.topic.description> ?x}");
		if( triples.size() == 0 )
			return "";
		return triples.get(0);
	}
	
	public Set<String> execute2HopQuery(String subj, String firstPred, String secondPred, String sibling){
		
		Set<String> results = new HashSet<String>();
		subj = "ns:"+subj;
		firstPred = "ns:"+firstPred;
		secondPred = "ns:"+secondPred;
		List<String> triples = this.execute("sparql select ?z ?target where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <" + firstPred.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?x. "
				+ "?x <"+secondPred.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?z."
				+ "?x ?n ?target .}");
		
		for(int i = 0; i < triples.size(); ){
			String triple = triples.get(i);
			String[] info = triple.split("\t");
			String ans = new String();
			String target = new String();
			if( info.length != 2  ){
				ans = triples.get(i);
				target = triples.get(i+1);
				i+=2;
			}
			else{
				ans = info[0];
				target = info[1];
				i++;
			}
			
			if( ans.equals(subj.replace("ns:", "http://rdf.freebase.com/ns/")) )
				continue;
			if( target.equals(sibling) )
				results.add(ans);
		}
		
		if( results.size() == 0 ){
			triples = this.execute("sparql select ?z ?target where {<"+subj.replace("ns:", "http://rdf.freebase.com/ns/")+"> <" + firstPred.replace("ns:", "http://rdf.freebase.com/ns/") + "> ?x."
					+ "?x <"+secondPred.replace("ns:", "http://rdf.freebase.com/ns/")+"> ?z. "
					+ "?x ?n ?y. "
					+ "?y <http://rdf.freebase.com/ns/type.object.name> ?target.}");
		}
		
		for(int i = 0; i < triples.size(); ){
			String triple = triples.get(i);
			String[] info = triple.split("\t");

			String ans = new String();
			String target = new String();
			if( info.length != 2  ){
				ans = triples.get(i);
				target = triples.get(i+1);
				i+=2;
			}
			else{
				ans = info[0];
				target = info[1];
				i++;
			}
			
			if( ans.equals(subj.replace("ns:", "http://rdf.freebase.com/ns/")) )
				continue;
			if( target.equals(sibling) )
				results.add(ans);
		}
		HashSet<String> names = new HashSet<String>();
		for(String result : results){
			names.addAll(this.retrieveNames(result.replace("http://rdf.freebase.com/ns/", "")));
		}
		
		return names;
	}
	
}
