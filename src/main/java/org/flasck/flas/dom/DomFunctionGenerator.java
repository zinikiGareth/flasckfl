package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.dom.RenderTree.Element;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.exceptions.UtilException;

public class DomFunctionGenerator {
	private final CardDefinition card;
	private final String prefix;
	private final Map<String, FunctionDefinition> functions;
	private final Scope scope;
	private int node = 0;
	public final List<RenderTree> trees = new ArrayList<RenderTree>();

	public DomFunctionGenerator(CardDefinition card, Map<String, FunctionDefinition> functions) {
		this.card = card;
		this.prefix = card.name;
		this.functions = functions;
		this.scope = card.innerScope();
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
			
		// Create the node in the RenderTree
		RenderTree.Element rtnode = new Element("div", nextFnName());

		// Create the parts of the DOM element
		AbsoluteVar domCtor = scope.fromRoot("DOM.Element");
		AbsoluteVar nil = scope.fromRoot("Nil");
		AbsoluteVar cons = scope.fromRoot("Cons");
		AbsoluteVar tuple = scope.fromRoot("()");
		Object attrs = nil;
		Object children = nil; // I think this is just a statement about how we build our trees
		Object events = nil;

		for (Object x : tl.attrs) {
			if (x instanceof TemplateExplicitAttr) {
				TemplateExplicitAttr tea = (TemplateExplicitAttr) x;
				if (tea.type == TemplateToken.STRING)
					attrs = new ApplyExpr(cons, new ApplyExpr(tuple, new StringLiteral(tea.attr), new StringLiteral(tea.value)), attrs);
				else
					throw new UtilException("Cannot handle attribute " + tea.type);
			} else
				throw new UtilException("Attribute " + x + " of type " + x.getClass() + " is not handled");
		}
		List<String> classes = new ArrayList<String>();
		for (TemplateToken tt : tl.formats) {
			if (tt.type == TemplateToken.STRING) {
				rtnode.addClass(tt.text);
				classes.add(tt.text);
			} else
				throw new UtilException("format not handled: " + tt);
		}
		if (!classes.isEmpty())
			attrs = new ApplyExpr(cons, new ApplyExpr(tuple, new StringLiteral("class"), new StringLiteral(String.join(" ", classes))), attrs);
		
		for (EventHandler x : tl.handlers) {
			
			// TODO: check that the apply expression is one for a handler
//			card.handlers
			events = new ApplyExpr(cons, new ApplyExpr(tuple, new StringLiteral(x.action), x.expr), events);
		}
		// TODO: still need to build dependency tree
		// TODO: handle attributes (including from vars)
		// TODO: handle formats? (or just put them in the tree? because they are "common" to all classes?)
		
		// Return all the pieces together and create the actual DOM Element ctor
		return new RenderTree.ElementExpr(rtnode, new ApplyExpr(domCtor, tag, attrs, children, events));
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
