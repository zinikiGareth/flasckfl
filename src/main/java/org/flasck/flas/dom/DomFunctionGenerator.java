package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.dom.RenderTree.Element;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.exceptions.UtilException;

public class DomFunctionGenerator {
	private final String prefix;
	private final Map<String, FunctionDefinition> functions;
	private final Scope scope;
//	private final StateDefinition state;
	private int node = 0;
	public final List<RenderTree> trees = new ArrayList<RenderTree>();

	public DomFunctionGenerator(String prefix, Map<String, FunctionDefinition> functions, Scope scope, StateDefinition state) {
		this.prefix = prefix;
		this.functions = functions;
		this.scope = scope;
//		this.state = state;
	}

	// Steps:
	//  we need to break down the overall thing into lines & (content) nodes
	//  we need to generate appropriate functions
	//  we need to create the maps of overall tree and dependencies

	public void generateTree(TemplateLine template) {
		RenderTree rt = new RenderTree(prefix, "template", (Element) generate(template));
		trees.add(rt);
	}

	public Object generate(TemplateLine template) {
		Object ret = generateOne(template);
		for (TemplateLine tl : template.nested)
			((Element)ret).addChildren(generate(tl));
		return ret;
	}
	
	// I think there should be a name-generator in this class
	// We should then iterate over the "contents" array (if not a div/list)
	public Object generateOne(TemplateLine tl) {
		if (tl.contents.isEmpty()) {
			RenderTree.ElementExpr pair = div(tl);
			function(pair.element.fn, pair.expr);
			return pair.element;
		} else {
			List<Element> ret = new ArrayList<Element>();
			for (Object x : tl.contents) {
				if (x instanceof TemplateToken) {
					TemplateToken tt = (TemplateToken)x;
					// This is the case for a simple (variable) content item
					// TODO: it goes with a "type: content" entry in the tree
					if (tt.type == TemplateToken.IDENTIFIER) {
						// TODO: distinguish between state vars and functions to call
						// TODO: check that functions are defined on the card and not global
						String fn = nextFnName();
						function(fn, new CardMember(prefix, tt.text));
						ret.add(new Element("content", fn));
					} else if (tt.type == TemplateToken.STRING) {
						String fn = nextFnName();
						function(fn, new StringLiteral(tt.text));
						ret.add(new Element("content", fn));
					} else if (tt.type == TemplateToken.DIV) {
						RenderTree.ElementExpr pair = div(tl);
						function(pair.element.fn, pair.expr);
						return pair.element; // there can only be one item in this case
					} else
						throw new UtilException("template token case not handled: " + tt.type);
				} else if (x instanceof ApplyExpr) {
					// in this case, this is an expression which should return an HTML structure or text value
					// anyway, it can be directly inserted into the DOM
					// But, it is effectively curried on the card, so lift that
					String fn = nextFnName();
					function(fn, x);
					ret.add(new Element("content", fn));
				} else
					throw new UtilException("Non TT not handled: " + x.getClass());
			}
			return ret;
		}
	}

	private RenderTree.ElementExpr div(TemplateLine tl) {
		Object tag;
		if (tl.customTagVar != null)
			tag = new UnresolvedVar(tl.customTagVar);
		else {
			if (tl.customTag != null)
				tag = new StringLiteral(tl.customTag);
			else
				tag = new StringLiteral("div");
		}
			
		Element elt = new Element("div", nextFnName());
		for (TemplateToken tt : tl.formats) {
			if (tt.type == TemplateToken.STRING) {
				elt.addClass(tt.text);
			}
		}
		// TODO: handle attributes (including from vars)
		// TODO: handle formats? (or just put them in the tree? because they are "common" to all classes?)
		// TODO: generate tree state
		return new RenderTree.ElementExpr(elt, new ApplyExpr(scope.fromRoot("DOM.Element"), tag, scope.fromRoot("Nil"), scope.fromRoot("Nil"), scope.fromRoot("Nil")));
	}

	private String nextFnName() {
		return prefix+"._templateNode_"+(++node);
	}

	private void function(String name, Object expr) {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		List<Object> args = new ArrayList<Object>();
		cases.add(new FunctionCaseDefn(scope, name, args, expr));
		functions.put(name, new FunctionDefinition(Type.CARD, name, 0, cases));
	}
}
