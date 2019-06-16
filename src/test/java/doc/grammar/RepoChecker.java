package doc.grammar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zinutils.utils.FileUtils;

public class RepoChecker {

	public static boolean checkRepo(File repoFile, Map<String, String> ms) throws IOException, FileNotFoundException {
		boolean ret = true;
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(repoFile))) {
			String s;
			while ((s = lnr.readLine()) != null) {
				if (s.length() == 0 || Character.isWhitespace(s.charAt(0)))
					continue;
				int idx = s.indexOf("=");
				if (idx == -1) {
					System.out.println("Repo entry does not have =: " + s);
					ret = false;
					continue;
				}
				String name = s.substring(0, idx).trim();
				int id2 = name.lastIndexOf(".");
				String finalS = s.substring(id2+1, idx).trim();
				String defn = s.substring(idx+1).trim();
				if (!ms.containsKey(name)) {
					System.out.println("There is no matcher for: " + name);
					ret = false;
					continue;
				}
				final String pattS = ms.remove(name).replace("${name}", name).replace("${final}", finalS);
				Pattern patt = Pattern.compile(pattS);
				Matcher m = patt.matcher(defn);
				if (!m.find()) {
					System.out.println("Var '" + name + "' has defn '" + defn + "' which does not match pattern: " + patt);
					ret = false;
					continue;
				}
//				System.out.println("Fine: " + name + " " + defn + " (" + patt + ")");
			}
		}
		if (!ms.isEmpty()) {
			System.out.println("Names not found: " + ms.keySet());
			ret = false;
		}
		if (!ret) {
			System.out.println("------ " + repoFile);
			FileUtils.cat(repoFile);
			System.out.println("------");
		}
		return ret;
	}

}
