package doc.grammar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.RepoChecker;
import org.junit.Test;
import org.zinutils.collections.CounterSet;
import org.zinutils.utils.FileUtils;

public class RunRegressionSuite {
	final File root = new File("src/regression");

	@Test
	public void testAll() throws Throwable {
		boolean runMe = Boolean.parseBoolean(System.getProperty("doc.grammar.run"));
		if (!runMe)
			return;
		GenerateRegressionSuite.generateInto(root);
		List<File> dirs = FileUtils.findDirectoriesUnder(root);
		File meta = new File(root, "META.json");
		JSONObject jo = new JSONObject(FileUtils.readFile(meta));
		Set<String> passed = new TreeSet<>();
		Set<String> failed = new TreeSet<>();
		CounterSet<String> success = new CounterSet<>();
		CounterSet<String> failure = new CounterSet<>();
		
		String onlyRun = System.getProperty("doc.grammar.onlyrun");
		if (onlyRun != null) {
			dirs.clear();
			dirs.add(new File("test.r" + onlyRun));
		}
		
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

		if (!failed.isEmpty()) {
			for (String s : allKeys) {
				final int yes = success.getCount(s);
				final int no = failure.getCount(s);
				System.out.println(s + " " + yes + " " + no + " = " + (yes+no == 0?"--":((100*yes)/(yes+no))) + "%");
			}
			// remove the meta file so it will regenerate next time
			// this addresses a small number of issues, but it's a pain to understand what's going on if you forget
			meta.delete();
		}
		
		assertEquals("Not all regression tests were run", dirs.size(), passed.size() + failed.size());

		System.out.println("Ran " + dirs.size() + " - " + passed.size() + " passed; " + failed.size() + " failed");
		Iterator<String> it2 = failed.iterator();
		for (int i=0;it2.hasNext() && i<10;i++) {
			System.out.println("failed: " + it2.next());
		}
		
		assertTrue(failed.size() + " of " + dirs.size() + " regression tests failed", failed.isEmpty());
	}

	public boolean runCase(File f, JSONObject jo) throws JSONException {
		JSONObject ms = jo.getJSONObject("matchers");
		final File dir = FileUtils.combine(root, f);
		boolean result;
		try {
			File repoFile = File.createTempFile("repo", ".txt");
			result = !org.flasck.flas.Main.standardCompiler(null, new String[] { "--phase", "PARSING", "--dumprepo", repoFile.getPath(), dir.toString() });
			if (result) {
				result = RepoChecker.checkRepo(repoFile, asMap(ms));
			}
		} catch (Exception ex) {
			result = false;
		}
		if (!result) {
			for (File fl : FileUtils.findFilesMatching(dir, "*.fl")) {
				System.out.println(fl + ":");
				FileUtils.cat(fl);
			}
			for (File ut : FileUtils.findFilesMatching(dir, "*.ut")) {
				System.out.println(ut + ":");
				FileUtils.cat(ut);
			}
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
