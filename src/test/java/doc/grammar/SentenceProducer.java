package doc.grammar;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.Test;
import org.zinutils.xml.XML;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class SentenceProducer {

	@Test
	public void testRandomSentenceProduction() {
		Grammar g = Grammar.from(XML.fromResource("/gh-grammar/grammar.xml"));
		String top = g.top();
		ProductionVisitor visitor = new SPProductionVisitor(g, 1193L);
		visitor.referTo(top);
	}

	public class SPProductionVisitor implements ProductionVisitor {
		private final Grammar grammar;
		private final Random r;
		private int indent = 1;
		
		public SPProductionVisitor(Grammar g, long l) {
			this.grammar = g;
			this.r = new Random(l);
		}

		@Override
		public void choices(List<Definition> asList) {
			asList.get(r.nextInt(asList.size())).visit(this);
		}

		@Override
		public void zeroOrOne(Definition child) {
			boolean wanted = r.nextBoolean();
			System.out.println("Choosing " + wanted + " for optional " + child);
			if (wanted) {
				child.visit(this);
			}
		}

		@Override
		public void zeroOrMore(Definition child) {
			int cnt = r.nextInt(3);
			System.out.println("Choosing " + cnt + " for many iterations of " + child);
			for (int i=0;i<cnt;i++) {
				child.visit(this);
			}
		}

		@Override
		public void referTo(String child) {
			System.out.println("Refer to " + child);
			Production p;
			try {
				p = grammar.findRule(child);
			} catch (RuntimeException ex) {
				System.out.println(ex);
				return;
			}
			p.visit(this);
		}

		@Override
		public void token(String token) {
			final Lexer lexer = grammar.findToken(token);
			System.out.println("Want token " + token + " " + lexer);
			String t = genToken(token);
			Pattern p = Pattern.compile(lexer.pattern);
			assertTrue("generated token for " + token + " did not match pattern definition", p.matcher(t).matches());
			System.out.print(t);
		}

		@Override
		public void indent() {
			indent++;
			System.out.println("indent = " + indent);
		}

		@Override
		public void exdent() {
			indent--;
			System.out.println("indent = " + indent);
		}

		private String genToken(String token) {
			switch (token) {
			case "CCB":
				return "}";
			case "CRB":
				return ")";
			case "EQ":
				return "=";
			case "OCB":
				return "{";
			case "ORB":
				return "(";
			case "type-name":
				return "Type";
			case "var-name":
				return "f";
			default:
				throw new RuntimeException("Cannot generate a token for " + token);
			}
		}
	}
}
