package doc.grammar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;


public class GenerateRegressionSuite {
	final static String grammar = "/gh-grammar/grammar.xml";

	public static void main(String[] args) throws Throwable {
		File top = new File("src/regression");
		FileUtils.deleteDirectoryTree(top);
		File meta = generateInto(top);
		FileUtils.cat(meta);
	}

	public static File generateInto(File top) throws Throwable {
		JSONObject jo = new JSONObject();
		Set<String> allUsed = new TreeSet<>(new Grammar.RuleComparator());
		for (long i=21000;i<29000;i+=7) {
			final long j = i;
			SentenceProducer p = new SentenceProducer(top, grammar);
			p.sentence(i, "source-file", used -> store(jo, allUsed, "test.r" + Long.toString(j), used));
			p.sentence(i, "unit-test-file", used -> {});
		}
		File meta = new File(top, "META.json");
		FileUtils.writeFile(meta, jo.toString());

		// Assert that all the productions in the grammar are used at least once in the regression suite
		Set<String> allProds = Grammar.from(XML.fromResource(grammar)).allProductionCases();
		allProds.removeAll(allUsed);
		assertTrue("Productions not used: " + allProds, allProds.isEmpty());
		
		return meta;
	}

	private static void store(JSONObject jo, Set<String> allUsed, String key, SentenceData used) {
		try {
			JSONObject thisOne = new JSONObject();
			thisOne.put("used", used.productionsUsed);
			thisOne.put("matchers", used.matchers);
			jo.put(key, thisOne);
			allUsed.addAll(used.productionsUsed);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
