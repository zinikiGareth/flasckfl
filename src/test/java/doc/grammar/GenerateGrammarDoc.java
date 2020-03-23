package doc.grammar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

public class GenerateGrammarDoc {
	final File srcDir = new File("src/test/resources/gh-grammar");

	@Test
	public void generateAllTheGrammarPages() throws FileNotFoundException {
		String gd = System.getenv("FLAS_GRAMMAR_DIR");
		if (gd == null)
			gd = System.getProperty("org.ziniki.flas_grammar_dir");
		if (gd == null) {
			System.out.println("There is no env var FLAS_GRAMMAR_DIR or -Dorg.ziniki.flas_grammar_dir to store the output");
			return;
		}
		File out = new File(gd);
		for (File f : FileUtils.findFilesMatching(srcDir, "*.css")) {
			FileUtils.copy(f, out);
		}
		XML grammarAsXML = XML.fromFile(new File(srcDir, "grammar.xml"));
		Grammar grammar = Grammar.from(grammarAsXML);
		Generator gen = new Generator(out);
		gen.generateGrammarHTML(grammar);
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
		assertEquals("unused tokens", new TreeSet<>(), unused);
		assertEquals("undefined tokens", new TreeSet<>(), refs);
	}

	public void checkProductions(Grammar grammar) {
		Set<String> prods = grammar.allProductions();
		Set<String> refs = grammar.allReferences();
		Set<String> unused = new TreeSet<>(prods);
		unused.remove(grammar.top());
		unused.removeAll(refs);
		refs.removeAll(prods);
		assertEquals("unused productions", new TreeSet<>(), unused);
		assertEquals("undefined productions", new TreeSet<>(), refs);
	}
}
