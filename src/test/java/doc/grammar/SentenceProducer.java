package doc.grammar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

import doc.grammar.TokenDefinition.Matcher;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class SentenceProducer {
	public enum UseNameForScoping {
		USE_THIS_NAME,
		USE_CURRENT_NAME,
		INDENT_THIS_ONCE,
		UNSCOPED
	}

	private boolean debug = false;
	private final Grammar grammar;
	private final File td;

	public SentenceProducer(File td, String grammar) {
		this.td = td;
		this.grammar = Grammar.from(XML.fromResource(grammar));
	}
	
	public void sentence(long var, String top, Consumer<SentenceData> sendUsed) throws Throwable {
		final String pkg = "test.r" + var;
		final File root = new File(td, pkg);
		FileUtils.assertDirectory(root);
		String ext;
		if (top.equals("source-file"))
			ext = ".fl";
		else if (top.equals("unit-test-file"))
			ext = ".ut";
		else
			throw new RuntimeException("Cannot generate " + top);
		final File tmp = new File(root, "r"+ Long.toString(var) + ext);
		SPProductionVisitor visitor = new SPProductionVisitor(grammar, pkg, tmp.getName(), var*100L);
		visitor.referTo(top);
		FileUtils.writeFile(tmp, visitor.sentence.toString());
		sendUsed.accept(new SentenceData(visitor.used, visitor.matchers, tmp));
	}

	public class NamePart {
		public NamePart(int indent, String token, UseNameForScoping scoping) {
			indentLevel = indent;
			name = token;
			this.scoping = scoping;
		}
		
		private final int indentLevel;
		private final String name;
		private final UseNameForScoping scoping;
		public int serviceNamer;
		
		@Override
		public String toString() {
			return indentLevel + ":" + name + "." + scoping;
		}
	}

	public class SPProductionVisitor implements ProductionVisitor {
		private StringBuilder sentence = new StringBuilder();
		private final Grammar grammar;
		private final String fileName;
		private final Random r;
		private int indent = 1;
		private boolean haveSomething;
		private Set<String> used = new TreeSet<>();
		private List<NamePart> nameParts = new ArrayList<>();
		private Map<String, String> matchers = new TreeMap<>();
		private String futureAmended;
		private String futurePattern;
		private Set<String> tokens = new HashSet<>();
		private int nameNestOffset;
		
		public SPProductionVisitor(Grammar g, String pkg, String fileName, long l) {
			this.grammar = g;
			this.fileName = fileName;
			this.r = new Random(l);
			nameParts.add(new NamePart(0, pkg, UseNameForScoping.UNSCOPED));
		}

		@Override
		public void visit(Definition child) {
			child.visit(this);
		}
		
		@Override
		public void zeroOrOne(Definition child) {
			boolean wanted = r.nextBoolean();
			if (debug)
				System.out.println("Choosing " + wanted + " for optional " + child);
			if (wanted) {
				visit(child);
			}
		}

		@Override
		public void zeroOrMore(Definition child, boolean withEOL) {
			int cnt = r.nextInt(3);
			if (debug)
				System.out.println("Choosing " + cnt + " iterations of " + child);
			for (int i=0;i<cnt;i++) {
				visit(child);
				if (withEOL)
					token("EOL", null, UseNameForScoping.UNSCOPED, new ArrayList<>());
			}
		}
		
		@Override
		public void oneOrMore(Definition child, boolean withEOL) {
			int cnt = r.nextInt(3)+1;
			if (debug)
				System.out.println("Choosing " + cnt + " iterations of " + child);
			for (int i=0;i<cnt;i++) {
				visit(child);
				if (withEOL)
					token("EOL", null, UseNameForScoping.UNSCOPED, new ArrayList<>());
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
					if (debug)
						System.out.println("Rule " + rn);
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
					if (debug)
						System.out.println("Rule " + rn);
					visit(asList.get(i));
					return;
				}
			}
		}

		@Override
		public void token(String token, String patternMatcher, UseNameForScoping scoping, List<Matcher> matchers) {
			final Lexer lexer = grammar.findToken(token);
			String t = genToken(token, lexer.pattern);
			if (debug)
				System.out.println("    " + t);
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
			if (patternMatcher != null) {
				replace(t, scoping);
				this.matchers.put(assembleName(t, scoping), patternMatcher);
			}
			for (Matcher m : matchers) {
				String patt = m.pattern;
				String doAmend = t;
				if (patt == null) {
					if (futurePattern == null)
						throw new RuntimeException("Cannot use pattern because it has not been set");
					patt = futurePattern;
					futurePattern = null;
					if (futureAmended != null)
						doAmend = futureAmended.replace("${final}", t);
				}
				if (m.amendedName != null)
					doAmend = m.amendedName.replace("${final}", t);
				replace(doAmend, m.scoper);
				this.matchers.put(assembleName(doAmend, scoping), patt);
			}
		}

		private void replace(String t, UseNameForScoping scoping) {
			NamePart np = null;
			for (NamePart p : nameParts)
				if (p.indentLevel == indent + nameNestOffset)
					np = p;
			if (np != null && (scoping != UseNameForScoping.USE_CURRENT_NAME && scoping != UseNameForScoping.INDENT_THIS_ONCE))
				removeAbove(indent + nameNestOffset -1);
			if (np == null || (scoping != UseNameForScoping.USE_CURRENT_NAME && scoping != UseNameForScoping.INDENT_THIS_ONCE))
				nameParts.add(new NamePart(indent + nameNestOffset, t, scoping));
		}

		@Override
		public void futurePattern(String amended, String pattern) {
			if (futurePattern != null)
				throw new RuntimeException("Cannot set next pattern without using previous pattern");
			futureAmended = amended;
			futurePattern = pattern;
		}

		@Override
		public void nestName(int offset) {
			this.nameNestOffset = offset;
		}

		@Override
		public void pushPart(String prefix, String names, boolean appendFileName) {
			NamePart np = null;
			for (NamePart p : nameParts)
				if (p.indentLevel == indent + nameNestOffset -1)
					np = p;
			removeAbove(indent + nameNestOffset -1);
			if (appendFileName) {
				if (nameParts.size() != 1)
					throw new RuntimeException("Should only append file name at top level");
				int idx = fileName.lastIndexOf('.');
				String ext = fileName.substring(idx+1);
				String fn = fileName.substring(0, idx).replace('.', '_');
				nameParts.add(new NamePart(indent + nameNestOffset-1, "_" + ext + "_" + fn, UseNameForScoping.UNSCOPED));
			}
			if (prefix != null) {
				final NamePart finalPart = new NamePart(indent + nameNestOffset, "_" + prefix + (np.serviceNamer++), UseNameForScoping.UNSCOPED);
				nameParts.add(finalPart);
				if (names != null)
					this.matchers.put(assembleName(finalPart.name, UseNameForScoping.UNSCOPED), names);
			}
		}

		private String assembleName(String desiredName, UseNameForScoping scoping) {
			StringBuilder sb = new StringBuilder();
			int drop = scoping == UseNameForScoping.INDENT_THIS_ONCE?0:1;
			for (int i=0;i<nameParts.size()-drop;i++) {
				NamePart np = nameParts.get(i);
				sb.append(np.name);
				sb.append(".");
			}
			sb.append(desiredName);
			return sb.toString();
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
			int above = indent;
			removeAbove(above);
			if (haveSomething)
				sentence.append("\n");
			haveSomething = false;
		}

		public void removeAbove(int above) {
			for (int i=0;i<nameParts.size();) {
				NamePart part = nameParts.get(i);
				if (part.indentLevel > above)
					nameParts.remove(i);
				else
					i++;
			}
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
			case "ASSERT":
			case "CARD":
			case "CONTRACT":
			case "CTOR":
			case "DATA":
			case "DEAL":
			case "ENTITY":
			case "ENVELOPE":
			case "EVENT":
			case "EXPECT":
			case "FALSE":
			case "HANDLER":
			case "IMPLEMENTS":
			case "INVOKE":
			case "MATCH":
			case "METHOD":
			case "OBJECT":
			case "OFFER":
			case "OPTIONAL":
			case "PROVIDES":
			case "SERVICE":
			case "STATE":
			case "STRUCT":
			case "STYLE":
			case "TEMPLATE":
			case "TEST":
			case "TEXT":
			case "TRUE":
			case "UNION":
			case "WRAPS":
				return token.toLowerCase();

			case "BINOP":
				return oneOf("+", "-", "*", "/"); // TODO: more operators
			case "DOSEND":
				return "send";
			case "NUMBER":
				return randomChars(1, 1, '1', 9) + randomChars(0, 3, '0', 10);
			case "STRING":
				return "'" + randomChars(10, 20, '!', 90).replaceAll("'", "_") + "'";
			case "DOCWORD":
				return randomChars(5, 10, 'a', 26);
			case "UNOP":
				return "-"; // are there more?  ~ maybe?  ! maybe?
			case "poly-var":
				return unique(() -> randomChars(1, 1, 'A', 26));
			case "type-name":
				return unique(() -> randomChars(1, 1, 'A', 26) + randomChars(2, 8, 'a', 26));
			case "event-name":
			case "template-name":
			case "var-name":
				return unique(() -> randomChars(1, 8, 'a', 26));

			default:
				throw new RuntimeException("Cannot generate a token for " + token);
			}
		}

		private String unique(Supplier<String> supplier) {
			for (int j=0;j<100;j++) {
				String tok = supplier.get();
				if (!tokens.contains(tok)) {
					tokens.add(tok);
					return tok;
				}
			}
			throw new RuntimeException("Could not find unique token among " + tokens);
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

	public void debugMode() {
		this.debug = true;
	}
}
