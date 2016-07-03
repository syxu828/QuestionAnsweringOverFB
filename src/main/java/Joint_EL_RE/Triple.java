package Joint_EL_RE;

import java.util.HashSet;
import java.util.Set;

public class Triple
{
	public String question;
	public String subj;
	public String subjSurface;
	public double subjScore;
	public String pred;
	public double predScore;
	public Set<String> answers = new HashSet<String>();
	public String answerType;
	public double[] featureVector;
	public double sumScore;
}