package org.flasck.flas.grammar;

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

import org.apache.commons.lang3.StringUtils;
import org.flasck.flas.grammar.TokenDefinition.Matcher;
import org.zinutils.utils.FileUtils;
import org.zinutils.xml.XML;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class SentenceProducer {
	public class OrState {
		public List<Integer> cnts = new ArrayList<>();

		public OrState(OrProduction op) {
			for (int i=0;i<op.size();i++)
				cnts.add(0);
		}
	}

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
		else if (top.equals("system-test-file"))
			ext = ".st";
		else
			throw new RuntimeException("Cannot generate " + top);
		final File tmp = new File(root, "r"+ Long.toString(var) + ext);
		SPProductionVisitor visitor = new SPProductionVisitor(grammar, pkg, tmp.getName(), var*100L);
		visitor.referTo(top, false);
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
		private final List<Map<String, String>> dicts = new ArrayList<>();
		
		public SPProductionVisitor(Grammar g, String pkg, String fileName, long l) {
			this.grammar = g;
			this.fileName = fileName;
			this.r = new Random(l);
			nameParts.add(new NamePart(0, pkg, UseNameForScoping.UNSCOPED));
			dicts.add(null);
			dicts.add(new TreeMap<>());
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
		public int zeroOrMore(Definition child, boolean withEOL) {
			int cnt = r.nextInt(3);
			iterateOver(child, withEOL, cnt);
			return cnt;
		}

		@Override
		public int oneOrMore(Definition child, boolean withEOL) {
			int cnt = r.nextInt(3)+1;
			iterateOver(child, withEOL, cnt);
			return cnt;
		}

		@Override
		public void exactly(int cnt, Definition child, boolean withEOL) {
			iterateOver(child, withEOL, cnt);
		}

		private void iterateOver(Definition child, boolean withEOL, int cnt) {
			OrProduction op = null;
			Object cxt = null;
			if (child instanceof RefDefinition) {
				op = ((RefDefinition)child).isOr(this);
				if (op != null)
					cxt = new OrState(op);
			}
			if (debug)
				System.out.println("Choosing " + cnt + " iterations of " + child);
			for (int i=0;i<cnt;i++) {
				if (op != null) {
					op.visitWith(cxt, this);
				} else
					visit(child);
				if (withEOL)
					token("EOL", null, UseNameForScoping.UNSCOPED, new ArrayList<>(), false, false, null, true);
			}
			if (op != null) {
				while (!op.wrapUp(cxt, this)) {
					if (withEOL)
						token("EOL", null, UseNameForScoping.UNSCOPED, new ArrayList<>(), false, false, null, true);
				}
			}
		}
		
		@Override
		public void referTo(String child, boolean resetToken) {
			if (resetToken) {
				this.dicts.get(dicts.size()-1).remove("haveLast");
				this.dicts.get(dicts.size()-1).remove("caseNumber");
			}
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
		public OrProduction isOr(String child) {
			Production p = grammar.findRule(child);
			if (p instanceof OrProduction)
				return (OrProduction) p;
			return null;
		}

		@Override
		public void choices(OrProduction prod, Object cxt, List<Definition> asList, List<Integer> probs, int maxProb, boolean repeatVarName) {
			OrState os = (OrState) cxt;
			final int ni = r.nextInt(maxProb);
			for (int i=0;i<asList.size();i++) {
				Definition cd = asList.get(i);
				RefDefinition rd = null;
				if (cd instanceof RefDefinition) 
					rd = (RefDefinition) cd;
				if (probs.get(i) > ni && (rd == null || os == null || os.cnts.get(i) < rd.getTo())) {
					final String rn = prod.ruleNumber() + "." + (i+1) + " " + prod.ruleName();
					used.add(rn);
					if (debug)
						System.out.println("Rule " + rn);
					visit(cd);
					if (os != null)
						os.cnts.set(i, os.cnts.get(i)+1);
					return;
				}
			}
		}

		@Override
		public boolean complete(OrProduction prod, Object cxt, List<Definition> choices) {
			OrState os = (OrState) cxt;
			for (int i=0;i<choices.size();i++) {
				if (!(choices.get(i) instanceof RefDefinition))
					continue;
				RefDefinition d = (RefDefinition) choices.get(i);
				if (os.cnts.get(i) < d.getFrom()) {
					final String rn = prod.ruleNumber() + "." + (i+1) + " " + prod.ruleName();
					used.add(rn);
					if (debug)
						System.out.println("Rule " + rn);
					visit(d);
					os.cnts.set(i, os.cnts.get(i)+1);
					return false;
				}
			}
			return true;
		}

		@Override
		public void token(String token, String patternMatcher, UseNameForScoping scoping, List<Matcher> matchers, boolean repeatLast, boolean saveLast, String generator, boolean space) {
			final Lexer lexer = grammar.findToken(token);
			String t;
			String haveLast = dicts.get(indent).get("haveLast");
			if (repeatLast && haveLast != null) {
				t = haveLast;
			} else {
				t = genToken(token, lexer.pattern, generator);
				if (saveLast)
					dicts.get(indent).put("haveLast", t);
			}
			if (debug)
				System.out.println("    " + t);
			if (generator == null) {
				Pattern p = Pattern.compile(lexer.pattern);
				assertTrue("generated token for " + token + "(" + t + ") did not match pattern definition (" + lexer.pattern + ")", p.matcher(t).matches());
			}
			if (token.equals("EOL"))
				haveSomething = false;
			else {
				if (!haveSomething) { // beginning of line
					for (int i=0;i<indent;i++) {
						sentence.append("\t");
					}
				} else if (space)
					sentence.append(" ");
				haveSomething = true;
			}
			sentence.append(t);
			if (patternMatcher != null) {
				replace(t, scoping);
				this.matchers.put(assembleName(t.replaceFirst("^_", ""), scoping), patternMatcher);
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
		
		@Override
		public void setDictEntry(String var, String val) {
			dicts.get(indent).put(var, val);
		}

		@Override
		public String getDictValue(String var) {
			for (int i=dicts.size()-1;i>=1;i--) {
				Map<String, String> m = dicts.get(i);
				if (m.containsKey(var)) {
					return m.get(var);
				}
			}
			return null;
		}

		@Override
		public String getTopDictValue(String var) {
			return dicts.get(dicts.size()-1).get(var);
		}

		@Override
		public void clearDictEntry(String var) {
			dicts.get(indent).remove(var);
		}

		@Override
		public void condNotEqual(String var, String ne, Definition inner) {
			String val = getDictValue(var);
			if (val != null) {
				if (!val.equals(ne)) {
					visit(inner);
				}
				return;
			}
			throw new RuntimeException("The condition var " + var + " was not set");
		}

		@Override
		public void condNotSet(String var, Definition inner) {
			Map<String, String> m = dicts.get(dicts.size()-1);
			if (!m.containsKey(var)) {
				visit(inner);
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
		
		@Override
		public void pushCaseNumber() {
			removeAbove(indent + nameNestOffset);
			String cn = getTopDictValue("caseNumber");
			if (cn == null) {
				cn = "1";
			} else {
				cn = Integer.toString(Integer.parseInt(cn)+1);
			}
			setDictEntry("caseNumber", cn);
			final NamePart finalPart = new NamePart(indent + nameNestOffset, "_" + cn, UseNameForScoping.UNSCOPED);
			nameParts.add(finalPart);
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
		public boolean indent(boolean force) {
			if (!force && (!haveSomething || indent >= 8))
				return false;
			sentence.append("\n");
			haveSomething = false;
			indent++;
			dicts.add(new TreeMap<>());
			return true;
		}

		@Override
		public void exdent() {
			removeAbove(--indent-1);
			if (haveSomething)
				sentence.append("\n");
			haveSomething = false;
			while (dicts.size() > indent+1)
				dicts.remove(dicts.size()-1);
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

		private String genToken(String token, String pattern, String generator) {
			if (generator != null)
				return generateTokenUsing(generator);
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
			case "HANDLE":
			case "PUT":
			case "SEND":
			case "SENDTO":
				return pattern;

			case "ACOR":
			case "AGENT":
			case "AJAX":
			case "ASSERT":
			case "CARD":
			case "CONFIGURE":
			case "CONTRACT":
			case "CREATE":
			case "CTOR":
			case "DATA":
			case "DEAL":
			case "ENTITY":
			case "ENVELOPE":
			case "EVENT":
			case "EXPECT":
			case "FINALLY":
			case "HANDLER":
			case "HEADER":
			case "IMPLEMENTS":
			case "INVOKE":
			case "MATCH":
			case "METHOD":
			case "OBJECT":
			case "OFFER":
			case "OPTIONAL":
			case "PROVIDES":
			case "PUMP":
			case "QUERY":
			case "REQUIRES":
			case "RESPONSES":
			case "SERVICE":
			case "SHOVE":
			case "STATE":
			case "STRUCT":
			case "STYLE":
			case "SUBSCRIBE":
			case "TEMPLATE":
			case "TEST":
			case "TEXT":
			case "UNION":
			case "WRAPS":
				return token.toLowerCase();

			case "FALSE":
			case "TRUE":
				return StringUtils.capitalize(token.toLowerCase());

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
			case "introduce-var":
				return unique(() -> "_" + randomChars(1, 8, 'a', 26));

			default:
				throw new RuntimeException("Cannot generate a token for " + token);
			}
		}

		private String generateTokenUsing(String generator) {
			switch (generator) {
			case "uri":
				return "'https://random-uri'";
			case "uri-path":
				return "'/foo'";
			default:
				throw new RuntimeException("There is no generator for " + generator);
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
