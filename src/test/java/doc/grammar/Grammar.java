package doc.grammar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XML;
import org.zinutils.xml.XMLElement;

public class Grammar {
	private final LinkedHashMap<String, Section> sections;
	private final LinkedHashMap<String, Production> productions;
	private final Set<Lexer> lexers = new TreeSet<>();

	private Grammar() {
		sections = new LinkedHashMap<>();
		productions = new LinkedHashMap<>();
	}
	
	public static Grammar from(XML xml) {
		final Grammar ret = new Grammar();
		xml.top().assertTag("grammar");
		ret.parseLexers(xml);
		ret.parseProductions(xml);
		return ret;
	}

	private void parseLexers(XML xml) {
		List<XMLElement> lexers = xml.top().elementChildren("lex");
		for (XMLElement xe : lexers) {
			StringBuilder sb = new StringBuilder();
			xe.serializeChildrenTo(sb);
			this.lexers.add(new Lexer(xe.required("token"), xe.required("pattern"), sb.toString()));
			xe.attributesDone();
		}
	}

	public void parseProductions(XML xml) {
		List<XMLElement> productions = xml.top().elementChildren("production");
		int ruleNumber = 1;
		for (XMLElement p : productions) {
			final String ruleName = p.get("name");

			// it should have a section
			XMLElement section = p.uniqueElement("section");
			Section s = requireSection(section);
			
			// there are many possible production rules; it should have exactly one of them
			// find it by discarding all the "standard" options
			List<XMLElement> rules = new ArrayList<>();
			for (XMLElement r : p.elementChildren()) {
				if (r.hasTag("section") || r.hasTag("description"))
					continue;
				rules.add(r);
			}
			if (rules.size() != 1)
				throw new RuntimeException("Production '" + ruleName + "' did not have exactly one rule but " + rules.size());
			
			XMLElement rule = rules.get(0);
			// At the top level, it's either a "single" or an "or".  "Single" is boring, so is omitted and the definition immediately follows
			Production theProd;
			if (rule.hasTag("or")) {
				theProd = handleOr(ruleNumber++, ruleName, rule);
			} else {
				Definition defn = parseDefn(ruleName, rule);
				theProd = new Production(ruleNumber++, ruleName, defn);
			}
			if (this.productions.containsKey(theProd.name)) {
				throw new RuntimeException("Duplicate definition of production " + theProd.name);
			}
			this.productions.put(theProd.name, theProd);
			s.add(theProd);
		}
	}

	private Section requireSection(XMLElement xe) {
		String title = xe.required("title");
		Section ret;
		if (sections.containsKey(title))
			ret = sections.get(title);
		else {
			try {
				ret = new Section(title, xe.uniqueElement("description"));
				this.sections.put(title, ret);
			} catch (UtilException ex) {
				throw new RuntimeException("First occurrence of section " + title + " did not have a description");
			}
		}
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
			return handleIndent(ruleName, rule);
		case "many":
			return handleMany(ruleName, rule);
		case "optional":
			return handleOptional(ruleName, rule);
		case "ref":
			return handleRef(ruleName, rule);
		case "seq":
			return handleSeq(ruleName, rule);
		case "token":
			return handleToken(ruleName, rule);
		default:
			throw new RuntimeException("Production '" + ruleName + "' references unknown production rule " + rule.tag());
		}
	}

	private IndentDefinition handleIndent(String ruleName, XMLElement rule) {
		Definition defn = parseDefn(ruleName, rule.uniqueElement("ref"));
		rule.attributesDone();
		return new IndentDefinition(defn);
	}

	private ManyDefinition handleMany(String ruleName, XMLElement rule) {
		Definition defn = parseDefn(ruleName, rule.uniqueElement("ref"));
		rule.attributesDone();
		return new ManyDefinition(defn);
	}

	private Definition handleOptional(String ruleName, XMLElement rule) {
		Definition defn;
		if (!rule.elementChildren("token").isEmpty())
			defn = parseDefn(ruleName, rule.uniqueElement("token"));
		else
			defn = parseDefn(ruleName, rule.uniqueElement("ref"));
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
		rule.attributesDone();
		return new TokenDefinition(type);
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
}
