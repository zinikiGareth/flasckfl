package org.flasck.flas.grammar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.grammar.SentenceProducer.UseNameForScoping;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class Grammar {
	public final String title;
	private final LinkedHashMap<String, Section> sections;
	private final LinkedHashMap<String, Production> productions;
	private final HashMap<String, String> burbles = new HashMap<>();
	private final Set<Lexer> lexers = new TreeSet<>();
	private List<String> cssFiles = new ArrayList<>();

	private Grammar(String title) {
		this.title = title;
		sections = new LinkedHashMap<>();
		productions = new LinkedHashMap<>();
	}
	
	public static Grammar from(XML xml) {
		final XMLElement xe = xml.top();
		xe.assertTag("grammar");
		final Grammar ret = new Grammar(xe.required("title"));
		xe.attributesDone();
		ret.readCSSFiles(xe);
		ret.readBurbles(xe);
		ret.parseLexers(xe);
		ret.parseProductions(xe);
		return ret;
	}

	private void readCSSFiles(XMLElement xe) {
		List<XMLElement> css = xe.elementChildren("css");
		for (XMLElement ce : css) {
			cssFiles.add(ce.required("href"));
			ce.attributesDone();
		}
	}

	private void readBurbles(XMLElement xe) {
		List<XMLElement> burbles = xe.elementChildren("burble");
		for (XMLElement b : burbles) {
			StringBuilder sb = new StringBuilder();
			b.serializeChildrenTo(sb);
			this.burbles.put(b.get("name"), sb.toString());
		}
	}

	private void parseLexers(XMLElement xml) {
		List<XMLElement> lexers = xml.elementChildren("lex");
		for (XMLElement xe : lexers) {
			StringBuilder sb = new StringBuilder();
			xe.serializeChildrenTo(sb);
			this.lexers.add(new Lexer(xe.required("token"), xe.required("pattern"), sb.toString()));
			xe.attributesDone();
		}
	}

	public void parseProductions(XMLElement xml) {
		List<XMLElement> productions = xml.elementChildren("production");
		int ruleNumber = 1;
		for (XMLElement p : productions) {
			final String ruleName = p.get("name");

			// it should have a section
			XMLElement section = p.uniqueElement("section");
			Section s = requireSection(section);
			
			List<XMLElement> testers = p.elementChildren("tested");
			boolean needsMoreTesting = testers.isEmpty() || (testers.size() == 1 && testers.get(0).hasAttribute("have"));
			for (XMLElement t : testers) {
				t.optional("by");
				t.attributesDone();
			}
			
			List<Integer> probs = null;
			List<XMLElement> producers = p.elementChildren("producer");
			if (producers.size() > 1)
				throw new RuntimeException("Production '" + ruleName + "' had multiple 'producer' blocks");
			if (producers.size() == 1) {
				String[] shares = producers.get(0).required("shares").split(" ");
				probs = new ArrayList<Integer>();
				for (String sh : shares)
					probs.add(Integer.parseInt(sh));
			}
			// there are many possible production rules; it should have exactly one of them
			// find it by discarding all the "standard" options
			List<XMLElement> rules = new ArrayList<>();
			for (XMLElement r : p.elementChildren()) {
				if (r.hasTag("section") || r.hasTag("description") || r.hasTag("producer") || r.hasTag("tested"))
					continue;
				rules.add(r);
			}
			if (rules.size() != 1)
				throw new RuntimeException("Production '" + ruleName + "' did not have exactly one rule but " + rules.size());
			
			XMLElement rule = rules.get(0);
			// At the top level, it's either a "single" or an "or".  "Single" is boring, so is omitted and the definition immediately follows
			Production theProd;
			if (rule.hasTag("or")) {
				OrProduction orProd = handleOr(ruleNumber++, ruleName, rule);
				theProd = orProd;
				if (probs != null)
					orProd.probs(probs);
			} else {
				Definition defn = parseDefn(ruleName, rule);
				theProd = new Production(ruleNumber++, ruleName, defn);
			}
			if (this.productions.containsKey(theProd.name)) {
				throw new RuntimeException("Duplicate definition of production " + theProd.name);
			}
			if (needsMoreTesting)
				theProd.needsMoreTesting();
			this.productions.put(theProd.name, theProd);
			s.add(theProd);
		}
	}

	private Section requireSection(XMLElement xe) {
		String title = xe.required("title");
		Section ret;
		if (sections.containsKey(title)) {
			ret = sections.get(title);
			if (!xe.elementChildren("description").isEmpty())
				throw new RuntimeException("Non-first occurrence of section " + title + " had a description that would be ignored");
		} else {
			try {
				ret = new Section(title, xe.uniqueElement("description"));
				this.sections.put(title, ret);
			} catch (UtilException ex) {
				throw new RuntimeException("First occurrence of section " + title + " did not have a description");
			}
		}
		xe.attributesDone();
		return ret;
	}

	private OrProduction handleOr(int ruleNumber, String ruleName, XMLElement rule) {
		List<XMLElement> options = rule.elementChildren();
		if (options.size() < 2)
			throw new RuntimeException("Or must have at least two options");
		List<Definition> defns = new ArrayList<>();
		for (XMLElement xe : options) {
			defns.add(parseDefn(ruleName, xe));
		}
		return new OrProduction(ruleNumber, ruleName, defns);
	}
	
	private Definition parseDefn(String ruleName, XMLElement rule) {
		switch (rule.tag()) {
		case "indent":
			return handleIndent(ruleName, rule, false, true);
		case "indent-one":
			return handleIndent(ruleName, rule, true, false);
		case "indent-non-zero":
			return handleIndent(ruleName, rule, false, false);
		case "many":
			return handleMany(ruleName, rule, true);
		case "one-or-more":
			return handleMany(ruleName, rule, false);
		case "optional":
			return handleOptional(ruleName, rule);
		case "ref":
			return handleRef(ruleName, rule);
		case "seq":
			return handleSeq(ruleName, rule);
		case "token":
			return handleToken(ruleName, rule);
		case "will-name":
			return handleWillName(ruleName, rule);
		case "nested-name":
			return handleNestedName(ruleName, rule);
		case "push-part":
			return handlePushPart(ruleName, rule);
		case "can-repeat-with-case-number":
			return handleCaseNumbering(ruleName, rule);
		default:
			throw new RuntimeException("Production '" + ruleName + "' references unknown operation " + rule.tag());
		}
	}

	private IndentDefinition handleIndent(String ruleName, XMLElement rule, boolean exactlyOne, boolean allowZero) {
		Definition defn = parseDefn(ruleName, rule.uniqueElement("ref"));
		rule.attributesDone();
		return new IndentDefinition(defn, exactlyOne, allowZero);
	}

	private ManyDefinition handleMany(String ruleName, XMLElement rule, boolean allowZero) {
		Definition defn;
		if (!rule.elementChildren("ref").isEmpty())
			defn = parseDefn(ruleName, rule.uniqueElement("ref"));
		else
			defn = parseDefn(ruleName, rule.uniqueElement("token"));
		rule.attributesDone();
		return new ManyDefinition(defn, allowZero);
	}

	private Definition handleOptional(String ruleName, XMLElement rule) {
		Definition defn;
		if (!rule.elementChildren("token").isEmpty())
			defn = parseDefn(ruleName, rule.uniqueElement("token"));
		else if (!rule.elementChildren("ref").isEmpty())
			defn = parseDefn(ruleName, rule.uniqueElement("ref"));
		else if (!rule.elementChildren("or").isEmpty())
			defn = parseDefn(ruleName, rule.uniqueElement("or"));
		else
			throw new NotImplementedException("Cannot find something useful to use in optional " + rule);
		rule.attributesDone();
		return new OptionalDefinition(defn);
	}

	private Definition handleRef(String ruleName, XMLElement rule) {
		String child = rule.required("production");
		rule.attributesDone();
		return new RefDefinition(child);
	}

	private Definition handleSeq(String ruleName, XMLElement rule) {
		final SequenceDefinition ret = new SequenceDefinition();
		for (XMLElement xe : rule.elementChildren()) {
			ret.add(parseDefn(ruleName, xe));
		}
		return ret;
	}

	private TokenDefinition handleToken(String ruleName, XMLElement rule) {
		String type = rule.required("type");
		String nameAppender = rule.optional("names");
		String scope = rule.optional("scope", null);
		UseNameForScoping unfs = UseNameForScoping.UNSCOPED;
		if ("true".equals(scope))
			unfs = UseNameForScoping.USE_THIS_NAME;
		else if ("false".equals(scope))
			unfs = UseNameForScoping.USE_CURRENT_NAME;
		else if ("indent".equals(scope))
			unfs = UseNameForScoping.INDENT_THIS_ONCE;
		rule.attributesDone();
		final TokenDefinition ret = new TokenDefinition(type, nameAppender, unfs);
		List<XMLElement> matchers = rule.elementChildren("named");
		for (XMLElement xe : matchers) {
			String amendedName = xe.required("amended");
			String pattern = xe.required("pattern");
			boolean scoper = xe.optionalBoolean("scope", false);
			ret.addMatcher(amendedName, pattern, scoper?UseNameForScoping.USE_THIS_NAME:UseNameForScoping.USE_CURRENT_NAME);
			xe.attributesDone();
		}
		List<XMLElement> useMatchers = rule.elementChildren("use-name");
		for (XMLElement xe : useMatchers) {
			xe.attributesDone();
			ret.addMatcher(null, null, UseNameForScoping.UNSCOPED);
		}
		return ret;
	}

	private Definition handleWillName(String ruleName, XMLElement rule) {
		String amend = rule.optional("amended", null);
		String pattern = rule.required("pattern");
		rule.attributesDone();
		return new WillNameDefinition(amend, pattern);
	}

	private Definition handleNestedName(String ruleName, XMLElement rule) {
		int offset = rule.requiredInt("offset");
		rule.attributesDone();
		return new NestedNameDefinition(offset);
	}

	private Definition handlePushPart(String ruleName, XMLElement rule) {
		String prefix = rule.optional("prefix");
		String names = rule.optional("names");
		boolean appendFileName = rule.optionalBoolean("filename", false);
		rule.attributesDone();
		return new PushPartDefinition(prefix, names, appendFileName);
	}

	private Definition handleCaseNumbering(String ruleName, XMLElement rule) {
		rule.attributesDone();
		return new CaseNumberingDefinition();
	}

	public Iterable<Section> sections() {
		return sections.values();
	}

	public Iterable<Production> productions() {
		return productions.values();
	}

	public String top() {
		return productions.values().iterator().next().name;
	}

	public Production findRule(String name) {
		Production ret = productions.get(name);
		if (ret == null)
			throw new RuntimeException("Could not find production for " + name);
		return ret;
	}
	
	public Set<String> allProductions() {
		return productions.keySet();
	}

	public Set<String> allProductionCases() {
		Set<String> ret = new TreeSet<>(new RuleComparator());
		for (Production p : productions.values()) {
			if (p instanceof OrProduction) {
				for (int i=0;i<((OrProduction)p).size();i++)
					ret.add(p.ruleNumber() + "." + (i+1) + " " + p.ruleName());
			} else
				ret.add(p.ruleNumber() + " " + p.ruleName());
		}
		return ret;
	}

	public Set<String> allReferences() {
		Set<String> ret = new TreeSet<>();
		for (Production p : productions.values()) {
			p.collectReferences(ret);
		}
		return ret;
	}

	public Set<String> lexTokens() {
		Set<String> ret = new TreeSet<>();
		for (Lexer l : lexers) {
			ret.add(l.token);
		}
		return ret;
	}

	public Set<String> tokenUsages() {
		Set<String> ret = new TreeSet<>();
		for (Production p : productions.values()) {
			p.collectTokens(ret);
		}
		return ret;
	}

	public Set<Lexer> lexers() {
		return lexers;
	}

	public Lexer findToken(String token) {
		for (Lexer l : lexers) {
			if (l.token.equals(token))
				return l;
		}
		throw new RuntimeException("There is no token " + token);
	}

	public List<String> cssFiles() {
		return cssFiles ;
	}

	public String getBurble(String which) {
		if (!burbles.containsKey(which))
			throw new RuntimeException("There is no burble for " + which);
		return burbles.get(which);
	}

	public String substituteRuleVars(String desc) {
		StringBuilder sb = new StringBuilder(desc);
		int from = 0;
		while ((from = sb.indexOf("${", from)) != -1) {
			int to = sb.indexOf("}", from);
			if (to == -1)
				throw new RuntimeException("Mismatched rule reference" + sb.substring(from));
			String name = sb.substring(from+2, to);
			if (!productions.containsKey(name))
				throw new RuntimeException("Cannot reference ${" + name + "}");
			Iterator<String> it = productions.keySet().iterator();
			for (int i=1;it.hasNext();i++) {
				if (it.next().equals(name))
					sb.replace(from, to+1, "(" + (i) + ")");
			}
		}
		return sb.toString();
	}

	public static class RuleComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			return Float.compare(parse(o1), parse(o2));
		}

		private float parse(String o2) {
			int idx = o2.indexOf(' ');
			return Float.parseFloat(o2.substring(0, idx));
		}
	}
}
