package org.flasck.flas.grammar;

import java.io.File;

import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

public class GrammarSupport {
	private static File srcDir = new File("src/test/resources/gh-grammar");
	
	static {
		String gr = System.getenv("GRAMMAR_ROOT");
		if (gr == null)
			gr = System.getProperty("org.ziniki.grammar_root");
		if (gr != null)
			srcDir = FileUtils.combine(new File(gr), srcDir);
	}
	
	public static File srcDir() {
		return srcDir;
	}

	public static Grammar loadGrammar() {
		XML grammarAsXML = XML.fromFile(new File(srcDir, "grammar.xml"));
		Grammar grammar = Grammar.from(grammarAsXML);
		return grammar;
	}
}
