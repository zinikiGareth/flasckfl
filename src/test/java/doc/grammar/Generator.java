package doc.grammar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.zinutils.utils.FileUtils;

public class Generator {
	private final File idx;
	private final File srcDir;

	public Generator(File srcDir, File out) {
		this.srcDir = srcDir;
		this.idx = new File(out, "index.html");
	}

	public void generateIndexHTML(Grammar grammar) throws FileNotFoundException {
		FileUtils.copy(new File(srcDir, "index.head.html"), idx);
		FileUtils.appendToFile(new File(srcDir, "preamble.html"), idx);
		FileUtils.appendToFile(new File(srcDir, "blocking.html"), idx);
		FileUtils.appendToFile(new File(srcDir, "lexical.html"), idx);
		PrintWriter str = new PrintWriter(new FileOutputStream(idx, true));
		generateLexical(grammar, str);
		generateSummary(grammar, str);
		generateDefinitionSections(grammar, str);
		str.close();
		FileUtils.appendToFile(new File(srcDir, "index.tail.html"), idx);
		FileUtils.cat(idx);
	}

	private void generateLexical(Grammar grammar, PrintWriter str) {
		for (Lexer l : grammar.lexers()) {
			str.print("<h3>" + StringEscapeUtils.escapeHtml4(l.token) + "</h3>");
			str.print("<span class='pattern-title'>Pattern:</span><span class='pattern'>" + StringEscapeUtils.escapeHtml4(l.pattern) + "</span>");
			str.print("<div class='lexical-description'>");
			str.print(l.desc);
			str.print("</div>");
		}
	}

	private void generateSummary(Grammar grammar, PrintWriter str) {
		str.println("<h2>Summary</h2>");
		for (Production p : grammar.productions()) {
			p.show(str);
		}
	}

	private void generateDefinitionSections(Grammar grammar, PrintWriter str) {
		str.println("<h2>Definitions</h2>");
		for (Section s : grammar.sections()) {
			str.println("<h3>" + StringEscapeUtils.escapeHtml4(s.title) + "</h3>");
			for (Production p : s.productions()) {
				p.show(str);
			}
			str.println("<div class='section-description'>");
			str.print(s.desc);
			str.println("</div>");
		}
	}

}
