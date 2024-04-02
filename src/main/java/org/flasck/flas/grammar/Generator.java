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
		str.println("<div class='whole-window'>");
		generateHamburger(grammar, str);
		str.println("<div class='grammar-content'>");
		generateLexical(grammar, str);
		generateSummary(grammar, str);
		generateDefinitionSections(grammar, str);
		str.println("</div>");
		str.println("</div>");
		generateTail(str);
		str.close();
	}

	private void generateHead(Grammar grammar, PrintWriter str) {
		str.println("<!DOCTYPE html>");
		str.println("<html>");
		str.println("<head>");
		str.println("<title>" + StringEscapeUtils.escapeHtml4(grammar.title) + "</title>");
		for (CSSFile css : grammar.cssFiles()) {
			String mtype = "";
			if (css.media != null)
				mtype = " media='" + css.media + "'";
			str.println("<link type='text/css' rel='stylesheet' href='" + StringEscapeUtils.escapeHtml4(css.href) + "'" + mtype + ">");
		}
		for (String js : grammar.jsFiles()) {
			str.println("<script type='text/javascript' src='" + StringEscapeUtils.escapeHtml4(js) + "'></script>");
		}
		str.println("</head>");
		str.println("<body>");
//		str.println("<h1>" + StringEscapeUtils.escapeHtml4(grammar.title) + "</h1>");
	}

	private void generateHamburger(Grammar grammar, PrintWriter str) {
		str.println("<div class='hamburger-div'>");
		str.println("<div class='hamburger-icon'></div>");
		str.println("</div>");
		str.println("<div class='hamburger-menu'>");
		for (Section s : grammar.sections()) {
			str.println("<div class='hamburger-section-link' data-grammar-section='" + s.title + "'>");
			str.println(s.title);
			str.println("</div>");
		}
		str.println("</div>");
	}

	private void generateLexical(Grammar grammar, PrintWriter str) {
		str.println("<div class='grammar-section' data-grammar-section='lexical-issues'>");
		for (Lexer l : grammar.lexers()) {
			str.print("<h3>" + StringEscapeUtils.escapeHtml4(l.token) + "</h3>");
			str.print("<span class='pattern-title'>Pattern:</span><span class='pattern'>" + StringEscapeUtils.escapeHtml4(l.pattern) + "</span>");
			str.print("<div class='lexical-description'>");
			str.print(l.desc);
			str.print("</div>");
		}
		str.println("</div>");
	}

	private void generateSummary(Grammar grammar, PrintWriter str) {
		str.println("<div class='grammar-section' data-grammar-section='grammar-summary'>");
		for (Production p : grammar.productions()) {
			p.show(str, false);
		}
		str.println("</div>");
	}

	private void generateDefinitionSections(Grammar grammar, PrintWriter str) {
		for (Section s : grammar.sections()) {
			str.println("<div class='grammar-section' data-grammar-section='" + s.title +"'>");
			str.println("<h3>" + StringEscapeUtils.escapeHtml4(s.title) + "</h3>");
			str.println("<div class='section-description'>");
			str.print(grammar.substituteRuleVars(s.desc));
			str.println("</div>");
			for (Production p : s.productions()) {
				p.show(str, true);
			}
			str.println("</div>");
		}
	}

	private void generateTail(PrintWriter str) {
		str.print("</body>\n");
		str.print("</html>\n");
	}
}
