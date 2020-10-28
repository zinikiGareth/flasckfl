package doc.grammar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zinutils.exceptions.CantHappenException;

public abstract class GenerateSyntaxJson {
	private final String lang;
	protected final Rule top;
	private final List<String> usedPatterns = new ArrayList<>();
	private final List<String> definedPatterns = new ArrayList<>();
	protected final JSONObject repo;

	public GenerateSyntaxJson(String lang) throws JSONException {
		this.lang = lang;
		top = new Rule("");
		repo = new JSONObject();
		JSONObject jo = top.token;
		jo.put("name", "flas-" + lang);
		jo.put("scopeName", "source.flas-" + lang);
		jo.put("patterns", new JSONArray());
		jo.put("repository", repo);
	}

	public abstract void populate() throws JSONException;
	
	public void generate() throws JSONException, FileNotFoundException {

		token(repo, "invalid-indent", "invalid.indent", "^ .*$").top(top);
		token(repo, "literate-comment", "comment.block.literate", "^[^\\s].*$").top(top);
		token(repo, "line-comment", "comment.line.double-slash", "//.*$").top(top);
		token(repo, "continuation", "meta.continuation", "^\t+( +)").top(top);
		
		populate();
		
		writeRepo();
	}

	public static void main(String[] args) throws FileNotFoundException, JSONException {
		new GenerateSyntaxJson("fl") {
			public void populate() throws JSONException {
				Rule contract = super.block("intro-contract", "meta.contract", 1, "contract", null).top(top);
				contract.pattern("simple-type");
				contract.pattern("method-decl");
				
				Rule methodDecl = super.block("method-decl", "meta.method.decl", 2, "[a-z][A-Za-z0-9]*", "entity.name.function");
				methodDecl.pattern("type-pattern");
				
				Rule card = super.block("intro-card", "meta.card", 1, "card", null).top(top);
				card.pattern("simple-type");
				card.pattern("state-defn");
				card.pattern("template-defn");
				card.pattern("implements-defn");
				card.pattern("requires-defn");
				card.pattern("explicit-method-defn");
				card.pattern("event-defn");
				card.pattern("handler-defn");
				card.pattern("function-defn");

				Rule state = super.block("state-defn", "meta.card.state", 2, "state", null);
				state.pattern("state-field-defn");
				
				Rule field = super.block("state-field-defn", "meta.card.state.field", 3, null, null);
				field.pattern("type-reference");
				field.pattern("variable-name");
				field.pattern("field-assign");
				
				Rule struct = super.block("struct-defn", "meta.struct", 1, "struct", null).top(top);
				struct.pattern("struct-field-defn");
				
				Rule entity = super.block("entity-defn", "meta.entity", 1, "entity", null).top(top);
				entity.pattern("struct-field-defn");
				
				Rule structField = super.block("struct-field-defn", "meta.card.state.field", 2, null, null);
				structField.pattern("type-reference");
				structField.pattern("variable-name");
				structField.pattern("field-assign");

				Rule fieldAssign = super.continueBlock(repo, "field-assign", "meta.expression", 3, "<-", "entity.name.function");
				fieldAssign.pattern("number");
				
				Rule template = super.block("template-defn", "meta.card.template", 2, "template", null);
				template.pattern("variable-name");
				template.pattern("number");
				template.pattern("type-reference");

				Rule implementsDefn = super.block("implements-defn", "meta.card.implements", 2, "implements", null);
				implementsDefn.pattern("type-reference");
				implementsDefn.pattern("contract-method-defn");

				Rule requiresDefn = super.block("requires-defn", "meta.card.requires", 2, "requires", null);
				requiresDefn.pattern("type-reference");
				requiresDefn.pattern("variable-name");

				Rule event = super.block("event-defn", "meta.card.event", 2, "event", null);
				event.pattern("variable-name");
				event.pattern("number");
				event.pattern("type-reference");
				event.pattern("handler-defn");

				Rule explicitMethodDefn = super.block("explicit-method-defn", "meta.method.defn", 2, "method", null);
				explicitMethodDefn.pattern("variable-name");
				explicitMethodDefn.pattern("number");
				explicitMethodDefn.pattern("type-reference");
				explicitMethodDefn.pattern("handler-defn");

				// a contract method definition is at level three and only within a contract implements
				Rule contractMethodDefn = super.block("contract-method-defn", "meta.method.defn", 3, "[a-z][A-Za-z0-9]*", "entity.name.function");
				contractMethodDefn.pattern("single-quoted-string");
				contractMethodDefn.pattern("double-quoted-string");
				contractMethodDefn.pattern("variable-name");
				contractMethodDefn.pattern("number");
				contractMethodDefn.pattern("type-reference");
				contractMethodDefn.pattern("handler-defn");

				Rule handlerDefn = super.block("handler-defn", "meta.handler.defn", -1, "handler", null).top(top);
				handlerDefn.pattern("contract-method-defn");
				handlerDefn.pattern("type-reference");
				
				Rule functionDefn = super.block("function-defn", "meta.function.defn", -1, "[a-z][A-Za-z0-9]*", "entity.name.function").top(top);
				functionDefn.pattern("function-defn");

				Rule typatt = super.token(repo, "type-pattern", "meta.pattern.type", "\\([^()]+\\)");
				typatt.pattern("type-reference");
				typatt.pattern("variable-name");
				
				super.token(repo, "number", "constant.numeric", "[0-9]+");
				super.token(repo, "single-quoted-string", "constant.string", "'[^']*'");
				super.token(repo, "double-quoted-string", "constant.string", "\"[^\"]*\"");
				super.token(repo, "simple-type", "entity.name.type", "\\b[A-Z][A-Za-z0-9]+\\b");
				super.token(repo, "type-reference", "entity.name.type", "\\b([a-z][a-zA-Z0-9]+\\.)*[A-Z][A-Za-z0-9]+\\b");
				super.token(repo, "variable-name", "variable.parameter", "\\b[a-z][a-zA-Z0-9]*\\b");
				
			}
		}.generate();
	}

	private void writeRepo() throws JSONException, FileNotFoundException {
		List<String> unused = new ArrayList<>(this.definedPatterns);
		unused.removeAll(usedPatterns);
		List<String> undefined = new ArrayList<>(this.usedPatterns);
		undefined.removeAll(definedPatterns);
		if (!unused.isEmpty() || !undefined.isEmpty()) {
			for (String s : unused) {
				System.out.println("pattern " + s + " not used");
			}
			for (String s : undefined) {
				System.out.println("pattern " + s + " used but not defined");
			}
		}
		try (PrintWriter pw = new PrintWriter(new File(new File("extension/syntax"), "flas-" + lang + ".json"))) {
			pw.print(top.token);
		}
	}

	private void addPattern(JSONArray patterns, String name) throws JSONException {
		usedPatterns.add(name);
		JSONObject patt = new JSONObject();
		patt.put("include", "#" + name);
		patterns.put(patt);
	}

	private Rule token(JSONObject repo, String name, String scope, String pattern) throws JSONException {
		if (definedPatterns.contains(name))
			throw new CantHappenException("duplication definition of rule " + name);
		definedPatterns.add(name);
		JSONObject token = new JSONObject();
		token.put("name", scope);
		token.put("match", pattern);
		repo.put(name, token);
		return new Rule(name, token);
	}

	private Rule block(String name, String scope, int ind, String kw, String kwf) throws JSONException {
		if (kwf == null)
			kwf = "keyword.intro";
		if (definedPatterns.contains(name))
			throw new CantHappenException("duplication definition of rule " + name);
		definedPatterns.add(name);
		JSONObject block = new JSONObject();
		block.put("name", scope);
		StringBuilder begin = new StringBuilder("^");
		String capture;
		if (ind == -1) {
			begin.append("(\t+)");
			capture = "2";
		} else {
			capture = "1";
			for (int i=0;i<ind;i++) {
				begin.append("\t");
			}
		}
		if (kw != null) {
			begin.append("(");
			begin.append(kw);
			begin.append(")\\b");
		} else {
			begin.append("(?!\t)");
		}
		block.put("begin", begin.toString());
		StringBuilder end;
		if (ind == -1) {
			end = new StringBuilder("(?=^\\1\\S)|(?=\t(?!\\1\\S))");
		} else {
			end = new StringBuilder("(?=^\t");
			for (int i=1;i<ind;i++)
				end.append("\t?");
			end.append("[^\t ])");
		}
		block.put("end", end.toString());
		if (kw != null) {
			JSONObject bc = new JSONObject();
			bc.put(capture, new JSONObject().put("name", kwf));
			block.put("beginCaptures", bc);
		}
		repo.put(name, block);
		Rule ret = new Rule(name, block);
		ret.pattern("invalid-indent");
		ret.pattern("literate-comment");
		ret.pattern("line-comment");
		ret.pattern("continuation");
		return ret;
	}

	private Rule continueBlock(JSONObject repo, String name, String scope, int ind, String kw, String kwf) throws JSONException {
		if (kwf == null)
			kwf = "keyword.intro";
		if (definedPatterns.contains(name))
			throw new CantHappenException("duplication definition of rule " + name);
		definedPatterns.add(name);
		JSONObject block = new JSONObject();
		block.put("name", scope);
		StringBuilder begin = new StringBuilder();
		if (kw != null) {
			begin.append("\\b(");
			begin.append(kw);
			begin.append(")\\b");
		}
		block.put("begin", begin.toString());
		StringBuilder end = new StringBuilder("(?=^\t");
		for (int i=1;i<ind;i++)
			end.append("\t?");
		end.append("[^\t ])");
		block.put("end", end.toString());
		if (kw != null) {
			JSONObject bc = new JSONObject();
			bc.put("1", new JSONObject().put("name", kwf));
			block.put("beginCaptures", bc);
		}
		repo.put(name, block);
		Rule ret = new Rule(name, block);
		ret.pattern("invalid-indent");
		ret.pattern("literate-comment");
		ret.pattern("line-comment");
		ret.pattern("continuation");
		return ret;
	}

	public class Rule {
		private final String name;
		private final JSONObject token;
		private JSONArray patts;

		public Rule(String name) {
			this.name = name;
			this.token = new JSONObject();
		}
		
		public Rule(String name, JSONObject token) {
			this.name = name;
			this.token = token;
		}

		public Rule top(Rule addTo) throws JSONException {
			addTo.pattern(name);
			return this;
		}

		public void pattern(String name) throws JSONException {
			if (patts == null) {
				patts = new JSONArray();
				token.put("patterns", patts);
			}
			addPattern(patts, name);
		}

	}

}
