package doc.grammar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.SentenceData;
import org.flasck.flas.grammar.SentenceProducer;
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
		File meta = new File(top, "META.json");
		if (meta.exists()) {
			URL gm = XML.class.getResource(grammar);
			File gf = new File(gm.getFile());
			if (gf.lastModified() < meta.lastModified()) {
				System.out.println("not regenerating grammar.xml because it has not changed");
				System.out.println("remove " + meta + " to force regeneration");
				return meta;
			}
		}
		
		JSONObject jo = new JSONObject();
		Set<String> allUsed = new TreeSet<>(new Grammar.RuleComparator());
		for (long i=21000;i<29000;i+=7) {
			final long j = i;
			SentenceProducer p = new SentenceProducer(top, grammar);
			Consumer<SentenceData> store = used -> store(allUsed, jo, "test.r" + Long.toString(j), used);
			p.sentence(i, "source-file", store);
			p.sentence(i, "unit-test-file", store);
			p.sentence(i, "system-test-file", store);
		}

		// Assert that all the productions in the grammar are used at least once in the regression suite
		Set<String> allProds = Grammar.from(XML.fromResource(grammar)).allProductionCases();
		removeOnesWeKnowWeDontTestYet(allProds);
		allProds.removeAll(allUsed);
		assertTrue("Productions not used: " + allProds, allProds.isEmpty());
		
		FileUtils.writeFile(meta, jo.toString());
		return meta;
	}

	private static void removeOnesWeKnowWeDontTestYet(Set<String> allProds) {
		int removed = 0;
		Iterator<String> it = allProds.iterator();
		while (it.hasNext()) {
			String prod = it.next();
			if (prod.endsWith(" file") ||
				prod.endsWith(" assembly-file") ||
				prod.endsWith(" assembly-unit") ||
				prod.endsWith(" protocol-test-file") ||
				prod.endsWith(" protocol-test-unit")) {
				it.remove();
				removed++;
			}
		}
		assertEquals(9, removed);
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
