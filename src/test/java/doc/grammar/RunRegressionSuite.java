package doc.grammar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.collections.CounterSet;
import org.zinutils.utils.FileUtils;

public class RunRegressionSuite {
	final File root = new File("src/regression");

	@Test
	public void testAll() throws Throwable {
		if (!root.exists()) {
			GenerateRegressionSuite.generateInto(root);
		}
		List<File> dirs = FileUtils.findDirectoriesUnder(root);
		JSONObject jo = new JSONObject(FileUtils.readFile(new File(root, "META.json")));
		Set<String> passed = new TreeSet<>();
		Set<String> failed = new TreeSet<>();
		CounterSet<String> success = new CounterSet<>();
		CounterSet<String> failure = new CounterSet<>();
		for (File f : dirs) {
			boolean result = runCase(f, jo.getJSONObject(f.getName()));
			CounterSet<String> mycase;
			if (result) {
				passed.add(f.getName());
				mycase = success;
			} else {
				failed.add(f.getName());
				mycase = failure;
			}
			JSONObject thisCase = jo.getJSONObject(f.getName());
			JSONArray rules = thisCase.getJSONArray("used");
			for (int i=0;i<rules.length();i++) {
				mycase.add(rules.getString(i));
			}
		}
		Set<String> allKeys = new TreeSet<>(new Grammar.RuleComparator());
		@SuppressWarnings("unchecked")
		Iterator<String> it = jo.keys();
		while (it.hasNext()) {
			JSONObject obj = jo.getJSONObject(it.next());
			JSONArray tmp = obj.getJSONArray("used");
			for (int i=0;i<tmp.length();i++)
				allKeys.add(tmp.getString(i));
		}

		for (String s : allKeys) {
			final int yes = success.getCount(s);
			final int no = failure.getCount(s);
			System.out.println(s + " " + yes + " " + no + " = " + (yes+no == 0?"--":((100*yes)/(yes+no))) + "%");
		}
		
		assertEquals("Not all regression tests were run", dirs.size(), passed.size() + failed.size());

		System.out.println("Ran " + dirs.size() + " - " + passed.size() + " passed; " + failed.size() + " failed");
		
//		assertTrue(failed.size() + " regression tests failed", failed.isEmpty());
	}

	@Test
	@Ignore
	public void testOne() throws Exception {
		JSONObject jo = new JSONObject(FileUtils.readFile(new File(root, "META.json")));
		final String name = "test.r21014";
		runCase(new File(name), jo.getJSONObject(name));
	}
	
	public boolean runCase(File f, JSONObject jo) throws JSONException {
		JSONObject ms = jo.getJSONObject("matchers");
		final File dir = FileUtils.combine(root, f);
		boolean result;
		try {
			File repoFile = File.createTempFile("repo", ".txt");
			result = !org.flasck.flas.Main.noExit(new String[] { "--phase", "PARSING", "--dumprepo", repoFile.getPath(), dir.toString() });
			if (result) {
				result = RepoChecker.checkRepo(repoFile, asMap(ms));
			}
		} catch (Exception ex) {
			result = false;
		}
		if (!result) {
			for (File fl : FileUtils.findFilesMatching(dir, "*.fl"))
				FileUtils.cat(fl);
			for (File ut : FileUtils.findFilesMatching(dir, "*.ut"))
				FileUtils.cat(ut);
		}
		return result;
	}

	private Map<String, String> asMap(JSONObject ms) throws JSONException {
		@SuppressWarnings("unchecked")
		Iterator<String> it = ms.keys();
		Map<String, String> ret = new TreeMap<>();
		while (it.hasNext()) {
			String s = it.next();
			ret.put(s, ms.getString(s));
		}
		return ret;
	}
}
