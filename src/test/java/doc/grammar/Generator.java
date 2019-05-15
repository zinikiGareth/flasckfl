package doc.grammar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.zinutils.utils.FileUtils;

public class Generator {
	private final File html;
	private final File srcDir;

	public Generator(File srcDir, File out) {
		this.srcDir = srcDir;
		this.html = new File(out, "grammar.html");
	}

	public void generateGrammarHTML(Grammar grammar) throws FileNotFoundException {
		// TODO: pull all of these into the XML doc as sections ...
//		FileUtils.appendToFile(new File(srcDir, "preamble.html"), html);
//		FileUtils.appendToFile(new File(srcDir, "blocking.html"), html);
//		FileUtils.appendToFile(new File(srcDir, "lexical.html"), html);
		PrintWriter str = new PrintWriter(new FileOutputStream(html));
		generateHead(grammar, str);
		includeBurble(grammar, str, "preamble");
		generateLexical(grammar, str);
		generateSummary(grammar, str);
		generateDefinitionSections(grammar, str);
		generateTail(str);
		str.close();
		FileUtils.cat(html);
	}

	private void generateHead(Grammar grammar, PrintWriter str) {
		str.println("<html>");
		str.println("<head>");
		str.println("<title>" + StringEscapeUtils.escapeHtml4(grammar.title) + "</title>");
		for (String css : grammar.cssFiles()) {
			str.println("<link type='text/css' rel='stylesheet' href='" + StringEscapeUtils.escapeHtml4(css) + "'/>");
		}
		str.println("</head>");
		str.println("<body>");
		str.println("<h1>" + StringEscapeUtils.escapeHtml4(grammar.title) + "</h1>");
	}

	private void includeBurble(Grammar grammar, PrintWriter str, String which) {
		str.print(grammar.getBurble(which));
	}

	private void generateLexical(Grammar grammar, PrintWriter str) {
		includeBurble(grammar, str, "lex");
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

	private void generateTail(PrintWriter str) {
		str.print("</body>\n");
		str.print("</html>\n");
	}
}
