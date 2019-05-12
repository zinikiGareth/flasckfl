package doc.grammar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

// Can we do anything to make this a "real" testcase, i.e. that the parser matches the spec?
// e.g. generate random valid documents & check they pass?
// mutate them to invalid documents & check they error?
public class GenerateGrammarDoc {
	final File srcDir = new File("src/test/resources/gh-grammar");

	@Test
	public void generateAllTheGrammarPages() throws FileNotFoundException {
		String gd = System.getenv("FLAS_GRAMMAR_DIR");
		if (gd == null) {
			System.out.println("There is no env var FLAS_GRAMMAR_DIR to store the output");
			return;
		}
		File out = new File(gd);
		for (File f : FileUtils.findFilesMatching(srcDir, "*.css")) {
			FileUtils.copy(f, out);
		}
		XML grammarAsXML = XML.fromFile(new File(srcDir, "grammar.xml"));
		Grammar grammar = Grammar.from(grammarAsXML);
		Generator gen = new Generator(srcDir, out);
		gen.generateIndexHTML(grammar);
		checkTokens(grammar);
		checkProductions(grammar);
	}

	public void checkTokens(Grammar grammar) {
		Set<String> prods = grammar.lexTokens();
		Set<String> refs = grammar.tokenUsages();
		Set<String> unused = new TreeSet<>(prods);
		unused.remove(grammar.top());
		unused.removeAll(refs);
		refs.removeAll(prods);
		System.out.println("unused tokens: " + unused);
		System.out.println("undefined tokens: " + refs);
		assertEquals(new TreeSet<>(), unused);
		assertEquals(new TreeSet<>(), refs);
	}

	public void checkProductions(Grammar grammar) {
		Set<String> prods = grammar.allProductions();
		Set<String> refs = grammar.allReferences();
		Set<String> unused = new TreeSet<>(prods);
		unused.remove(grammar.top());
		unused.removeAll(refs);
		refs.removeAll(prods);
		System.out.println("unused: " + unused);
		System.out.println("undefined (" + refs.size() + "): " + refs);
		assertEquals(new TreeSet<>(), unused);
//		assertEquals(new TreeSet<>(), refs);
	}
}