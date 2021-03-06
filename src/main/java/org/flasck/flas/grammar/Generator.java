package org.flasck.flas.grammar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringEscapeUtils;

public class Generator {
	private final File html;

	public Generator(File out) {
		this.html = new File(out, "grammar.html");
	}

	public void generateGrammarHTML(Grammar grammar) throws FileNotFoundException {
		PrintWriter str = new PrintWriter(new FileOutputStream(html));
		generateHead(grammar, str);
		includeBurble(grammar, str, "preamble");
		generateLexical(grammar, str);
		generateSummary(grammar, str);
		generateDefinitionSections(grammar, str);
		generateTail(str);
		str.close();
	}

	private void generateHead(Grammar grammar, PrintWriter str) {
		str.println("<html>");
		str.println("<head>");
		str.println("<title>" + StringEscapeUtils.escapeHtml4(grammar.title) + "</title>");
		for (String css : grammar.cssFiles()) {
			str.println("<link type='text/css' rel='stylesheet' href='" + StringEscapeUtils.escapeHtml4(css) + "'>");
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
			str.print(grammar.substituteRuleVars(s.desc));
			str.println("</div>");
		}
	}

	private void generateTail(PrintWriter str) {
		str.print("</body>\n");
		str.print("</html>\n");
	}
}
