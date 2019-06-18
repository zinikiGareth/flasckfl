package doc.grammar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONArray;
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
			p.sentence(i, "source-file", used -> store(allUsed, jo, "test.r" + Long.toString(j), used));
			p.sentence(i, "unit-test-file", used -> store(allUsed, jo, "test.r" + Long.toString(j), used));
		}
		File meta = new File(top, "META.json");
		FileUtils.writeFile(meta, jo.toString());

		// Assert that all the productions in the grammar are used at least once in the regression suite
		Set<String> allProds = Grammar.from(XML.fromResource(grammar)).allProductionCases();
		allProds.removeAll(allUsed);
		assertTrue("Productions not used: " + allProds, allProds.isEmpty());
		
		return meta;
	}

	private static void store(Set<String> allUsed, JSONObject jo, String key, SentenceData used) {
		try {
			allUsed.addAll(used.productionsUsed);

			TreeSet<String> now = new TreeSet<>();
			TreeMap<String, String> nowms = new TreeMap<>();
			if (jo.has(key)) {
				JSONObject curr = jo.getJSONObject(key);
				addAll(now, curr.getJSONArray("used"));
				putAll(nowms, curr.getJSONObject("matchers"));
			}

			now.addAll(used.productionsUsed);
			nowms.putAll(used.matchers);
			JSONObject thisOne = new JSONObject();
			thisOne.put("used", now);
			thisOne.put("matchers", nowms);
			jo.put(key, thisOne);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static void addAll(TreeSet<String> now, JSONArray ja) throws JSONException {
		for (int i=0;i<ja.length();i++)
			now.add(ja.getString(i));
	}

	private static void putAll(TreeMap<String, String> nowms, JSONObject jo) throws JSONException {
		@SuppressWarnings("rawtypes")
		Iterator it = jo.keys();
		while (it.hasNext()) {
			String s = (String) it.next();
			nowms.put(s, jo.getString(s));
		}
	}
}
