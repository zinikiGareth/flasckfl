package doc.grammar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class SentenceProducer {
	private final Grammar grammar;
	private final File td;

	public SentenceProducer(File td, String grammar) {
		this.td = td;
		this.grammar = Grammar.from(XML.fromResource(grammar));
	}
	
	public File sentence(long var, Consumer<Set<String>> sendUsed) throws Throwable {
		String top = grammar.top();
		SPProductionVisitor visitor = new SPProductionVisitor(grammar, var*100L);
		visitor.referTo(top);
		sendUsed.accept(visitor.used);
		final File root = new File(td, "test." + var);
		FileUtils.assertDirectory(root);
		final File tmp = new File(root, Long.toString(var) + ".fl");
		FileUtils.writeFile(tmp, visitor.sentence.toString());
		return tmp;
	}

	public class SPProductionVisitor implements ProductionVisitor {
		private StringBuilder sentence = new StringBuilder();
		private final Grammar grammar;
		private final Random r;
		private int indent = 1;
		private boolean haveSomething;
		private Set<String> used = new TreeSet<>();
		
		public SPProductionVisitor(Grammar g, long l) {
			this.grammar = g;
			this.r = new Random(l);
		}

		@Override
		public void zeroOrOne(Definition child) {
			boolean wanted = r.nextBoolean();
//			System.out.println("Choosing " + wanted + " for optional " + child);
			if (wanted) {
				child.visit(this);
			}
		}

		@Override
		public void zeroOrMore(Definition child) {
			int cnt = r.nextInt(3);
			// System.out.println("Choosing " + cnt + " iterations of " + child);
			for (int i=0;i<cnt;i++) {
				child.visit(this);
			}
		}

		@Override
		public void referTo(String child) {
			Production p;
			try {
				p = grammar.findRule(child);
				if (!(p instanceof OrProduction)) {
					final String rn = p.ruleNumber() + " " + p.ruleName();
					used.add(rn);
//					System.out.println("Rule " + rn);
				}
			} catch (RuntimeException ex) {
				System.out.println(ex);
				return;
			}
			p.visit(this);
		}

		@Override
		public void choices(OrProduction prod, List<Definition> asList, List<Integer> probs, int maxProb) {
			final int ni = r.nextInt(maxProb);
			for (int i=0;i<asList.size();i++) {
				if (probs.get(i) > ni) {
					final String rn = prod.ruleNumber() + "." + (i+1) + " " + prod.ruleName();
					used.add(rn);
//					System.out.println("Rule " + rn);
					asList.get(i).visit(this);
					return;
				}
			}
		}

		@Override
		public void token(String token) {
			final Lexer lexer = grammar.findToken(token);
			String t = genToken(token, lexer.pattern);
			Pattern p = Pattern.compile(lexer.pattern);
			assertTrue("generated token for " + token + "(" + t + ") did not match pattern definition (" + lexer.pattern + ")", p.matcher(t).matches());
			if (token.equals("EOL"))
				haveSomething = false;
			else {
				if (!haveSomething) { // beginning of line
					for (int i=0;i<indent;i++) {
						sentence.append("\t");
					}
				} else
					sentence.append(" ");
				haveSomething = true;
			}
			sentence.append(t);
		}

		@Override
		public boolean indent() {
			if (!haveSomething || indent >= 8)
				return false;
			sentence.append("\n");
			haveSomething = false;
			indent++;
			return true;
		}

		@Override
		public void exdent() {
			indent--;
			if (haveSomething)
				sentence.append("\n");
			haveSomething = false;
		}

		private String genToken(String token, String pattern) {
			switch (token) {
			case "APPLY":
				return ".";
			case "CCB":
				return "}";
			case "CRB":
				return ")";
			case "CSB":
				return "]";
			case "EOL":
				return "\n";
			case "EQ":
				return "=";
			case "GUARD":
			case "OR":
				return "|";
			case "OCB":
				return "{";
			case "ORB":
				return "(";
			case "OSB":
				return "[";
			
			case "COLON":
			case "COMMA":
			case "PUT":
			case "SEND":
			case "SENDTO":
				return pattern;

			case "ACOR":
			case "CARD":
			case "CONTRACT":
			case "CTOR":
			case "ENTITY":
			case "ENVELOPE":
			case "EVENT":
			case "FALSE":
			case "HANDLER":
			case "IMPLEMENTS":
			case "METHOD":
			case "OBJECT":
			case "OPTIONAL":
			case "SERVICE":
			case "STATE":
			case "STRUCT":
			case "TEMPLATE":
			case "TRUE":
			case "UNION":
			case "WRAPS":
				return token.toLowerCase();

			case "BINOP":
				return oneOf("+", "-", "*", "/"); // TODO: more operators
			case "DIR":
				return r.nextBoolean()?"up":"down";
			case "NUMBER":
				return randomChars(1, 1, '1', 9) + randomChars(0, 3, '0', 10);
			case "STRING":
				return "'" + randomChars(10, 20, '!', 90).replaceAll("'", "_") + "'";
			case "UNOP":
				return "-"; // are there more?  ~ maybe?  ! maybe?
			case "poly-var":
				return randomChars(1, 1, 'A', 26);
			case "type-name":
				return randomChars(1, 1, 'A', 26) + randomChars(2, 8, 'a', 26);
			case "event-name":
			case "template-name":
			case "var-name":
				return randomChars(1, 8, 'a', 26);

			default:
				throw new RuntimeException("Cannot generate a token for " + token);
			}
		}

		private String oneOf(String... strings) {
			int k = r.nextInt(strings.length);
			return strings[k];
		}

		private String randomChars(int min, int max, char fst, int range) {
			int len = min;
			if (max > min)
				len += r.nextInt(max-min);
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<len;i++)
				sb.append((char)(fst + r.nextInt(range)));
			return sb.toString();
		}
	}
}
