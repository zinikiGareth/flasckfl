package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.dom.RenderTree.Element;
import org.flasck.flas.dom.UpdateTree.Update;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class DomFunctionGenerator {
	private final ErrorResult errors;
	public final String prefix;
	private final Map<String, FunctionDefinition> functions;
	private final Scope scope;
	private int node = 0;
	public final List<RenderTree> trees = new ArrayList<RenderTree>();
	public final ListMap<String, Update> updates = new ListMap<String, Update>(new StringComparator());

	public DomFunctionGenerator(ErrorResult errors, Template template, Map<String, FunctionDefinition> functions) {
		this.errors = errors;
		this.prefix = template.prefix/* + "." + template.name*/;
		this.functions = functions;
		this.scope = template.scope;
	}

	// Steps:
	//  we need to break down the overall thing into lines & (content) nodes
	//  we need to generate appropriate functions
	//  we need to create the maps of overall tree and dependencies

	@SuppressWarnings("unchecked")
	public void generateTree(TemplateLine template) {
		Object genned = generate(template, new Route());
		if (genned instanceof Element) {
			RenderTree rt = new RenderTree(prefix, "template", (Element) genned);
			trees.add(rt);
		} else if (genned instanceof List) {
			for (Object o : (List<Object>)genned) {
				RenderTree rt = new RenderTree(prefix, "template", (Element) o);
				trees.add(rt);
			}
		}
		System.out.println("updates = " + updates);
	}

	public Object generate(TemplateLine template, Route route) {
		Object ret = generateOne(template, route);
		if (template instanceof TemplateList) {
			TemplateList tl = (TemplateList)template;
			route = route.extendListVar(((TemplateListVar)tl.iterVar).name);
			((Element)ret).addChildren(generate(tl.template, route));
		} else if (template instanceof TemplateDiv) {
			int pos = 0;
			Element elt = (Element)ret;
			TemplateDiv td = (TemplateDiv) template;
			for (TemplateLine tl : td.nested) {
				Route troute = route.extendDivMember(pos++);
				System.out.println("route = " + route + "; " + elt.route);
				elt.addChildren(generate(tl, troute));
			}
		} else if (template instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) template;
			Element elt = (Element)ret;
			// Build up an if/else tree in functions
			// Put all of these in the tree as nodes
			if (tc.cases.isEmpty()) {
				errors.message(tc.loc, "template cases must have at least one clause");
				return null;
			}
			int orc = 0;
			for (TemplateOr c : tc.cases) {
				Route myroute = route.extendCase(orc++);
				List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
				List<Object> args = new ArrayList<Object>();
				Object expr;
				String csfn = nextFnName();
				Element e;
				List<CardMember> dependsOn = new ArrayList<CardMember>();
				traverseForMembers(dependsOn, c.cond);
				if (tc.switchOn != null) {
					args.add(new VarPattern("_x"));
					expr = new ApplyExpr(scope.fromRoot("=="), new LocalVar(csfn+"_0", "_x"), c.cond);
					e = new Element("case", null, csfn, null, myroute);
					elt.children.add(e);
					for (CardMember cm : dependsOn)
						addUpdate(cm.var, route, "renderChildren"); // I think this is right: we want to update the parent of a switch but only update the children
				} else {
					expr = c.cond;
					e = new Element("cond", null, csfn, null, myroute);
					for (CardMember cm : dependsOn)
						addUpdate(cm.var, route, "renderChildren"); // I think this is right: we want to update the parent of a switch but only update the children
				}
				cases.add(new FunctionCaseDefn(scope, csfn, args, expr));
				functions.put(csfn, new FunctionDefinition(Type.CARD, csfn, args.size(), cases));
				e.addChildren(generate(c.template, myroute.template()));
			}
		}
		return ret;
	}
	
	// I think there should be a name-generator in this class
	// We should then iterate over the "contents" array (if not a div/list)
	public Object generateOne(TemplateLine tl, Route route) {
		if (tl == null)
			throw new UtilException("tl == null");
		if (tl instanceof TemplateDiv) {
			RenderTree.ElementExpr pair = div((TemplateDiv) tl, route);
			function(pair.element.fn, pair.expr);
			return pair.element;
		} else if (tl instanceof ContentString) {
			String fn = nextFnName();
			function(fn, new StringLiteral(((ContentString)tl).text));
			return new Element("content", fn, null, null, route);
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) tl;
			String fn = nextFnName();
			function(fn, ce.expr);
			List<CardMember> dependsOn = new ArrayList<CardMember>();
			traverseForMembers(dependsOn, ce.expr);
			for (CardMember cm : dependsOn)
				addUpdate(cm.var, route, "render");
			return new Element("content", fn, null, null, route);
		} else if (tl instanceof CardMember || tl instanceof ApplyExpr || tl instanceof TemplateListVar) {
			// in this case, this is an expression which should return an HTML structure or text value
			// anyway, it can be directly inserted into the DOM
			// But, it is effectively curried on the card, so lift that
			String fn = nextFnName();
			function(fn, tl);
			List<CardMember> dependsOn = new ArrayList<CardMember>();
			traverseForMembers(dependsOn, tl);
			for (CardMember cm : dependsOn)
				addUpdate(cm.var, route, "render");
			return new Element("content", fn, null, null, route);
		} else if (tl instanceof CardReference) {
			CardReference cr = (CardReference) tl;
			AbsoluteVar domCtor = scope.fromRoot("DOM.Element");
			AbsoluteVar nil = scope.fromRoot("Nil");
			AbsoluteVar create = scope.fromRoot("CreateCard");
			String fn = nextFnName();
			ApplyExpr into = new ApplyExpr(domCtor, new StringLiteral("div"), nil, nil, nil);
			// TODO: somebody, somewhere, needs to make sure that this card is only created once, even if it is reused/hidden etc.
			// If we view "CreateCard" as being "pineal" (i.e. just a thought) and we have some sort of ID 
			// associated with it, the runtime can keep track.  I think that is probably easiest
			ApplyExpr cc = new ApplyExpr(create, cr.explicitCard, into, nil);
			function(fn, cc);
			return new Element("card", fn, null, null, route);
		} else if (tl instanceof TemplateList) {
			TemplateList list = (TemplateList) tl;
			String fn = nextFnName();
			StringLiteral tag = new StringLiteral("ul");
			AbsoluteVar domCtor = scope.fromRoot("DOM.Element");
			AbsoluteVar nil = scope.fromRoot("Nil");
			Object attrs = nil;
			Object children = nil; // I think this is just a statement about how we build our trees
			Object events = nil;
			function(fn, new ApplyExpr(domCtor, tag, attrs, children, events));
			String val = nextFnName();
			function(val, list.listVar);
			String var = ((TemplateListVar)list.iterVar).name;
			Element elt = new Element("list", fn, val, var, route);
			List<CardMember> dependsOn = new ArrayList<CardMember>();
			traverseForMembers(dependsOn, list.listVar);
			for (CardMember cm : dependsOn)
				addUpdate(cm.var, route.extendListVar(var), "render");
			return elt;
		} else if (tl instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) tl;
			Element elt = null;
			if (tc.switchOn != null) {
				String swfn = nextFnName();
				elt = new Element("switch", null, swfn, null, route);
//					ret.add(elt);
				function(swfn, tc.switchOn);
				List<CardMember> dependsOn = new ArrayList<CardMember>();
				traverseForMembers(dependsOn, tc.switchOn);
				for (CardMember cm : dependsOn)
					addUpdate(cm.var, route, "renderChildren");
				return elt;
			} else
				throw new UtilException("You need to deal with this case, whatever it is");
//			return elt;
//			throw new UtilException("Can't handle TemplateCases yet");
		} else if (tl instanceof D3Invoke) {
			System.out.println("D3 Invoke");
			D3Invoke d3i = (D3Invoke) tl;
			String mainName = nextFnName();

			ListMap<String, Object> byKey = new ListMap<String, Object>(new StringComparator());
			for (D3PatternBlock p : d3i.d3.patterns) {
				for (Entry<String, D3Section> s : p.sections.entrySet()) {
					String fnName = nextFnName();
					
					function(fnName, ItemExpr.from(ExprToken.from(new Tokenizable("'hello, " + s.getKey() + " " + p.pattern + "'"))));
					byKey.add(s.getKey(), new FunctionLiteral(fnName));
				}
			}
			Object o = scope.fromRoot("NilMap");
			AbsoluteVar assoc = scope.fromRoot("Assoc");
			AbsoluteVar cons = scope.fromRoot("Cons");
			for (Entry<String, List<Object>> k : byKey.entrySet()) {
				Object list = scope.fromRoot("Nil");
				for (Object v : k.getValue())
					list = new ApplyExpr(cons, v, list);
				o = new ApplyExpr(assoc, new StringLiteral(k.getKey()), list, o);
			}
			function(mainName, o);
			return new Element("d3", mainName, null, null, route);
		} else
			throw new UtilException("Non TT not handled: " + tl.getClass());
	}

	private RenderTree.ElementExpr div(TemplateDiv tl, Route route) {
		Object tag;
		if (tl.customTagVar != null)
			tag = new UnresolvedVar(null, tl.customTagVar);
		else {
			if (tl.customTag != null)
				tag = new StringLiteral(tl.customTag);
			else
				tag = new StringLiteral("div");
		}
			
		// Create the node in the RenderTree
		RenderTree.Element rtnode = new Element("div", nextFnName(), null, null, route);

		// Create the parts of the DOM element
		AbsoluteVar domCtor = scope.fromRoot("DOM.Element");
		AbsoluteVar nil = scope.fromRoot("Nil");
		AbsoluteVar cons = scope.fromRoot("Cons");
		AbsoluteVar join = scope.fromRoot("join");
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
					throw new UtilException("Cannot handle attribute " + tea);
			} else
				throw new UtilException("Attribute " + x + " of type " + x.getClass() + " is not handled");
		}
		List<String> classLiterals = new ArrayList<String>();
		ApplyExpr computed = null;
//		System.out.println(tl.formats);
		for (Object f : tl.formats) {
			if (f instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken) f;
				if (tt.type == TemplateToken.STRING) {
					rtnode.addClass(tt.text);
					classLiterals.add(tt.text);
				} else
					throw new UtilException("format not handled: " + tt);
			} else if (f instanceof ApplyExpr) {
				List<CardMember> dependsOn = new ArrayList<CardMember>();
				traverseForMembers(dependsOn, f);
				for (CardMember cm : dependsOn)
					addUpdate(cm.var, route, "attrs"); // Just update the attrs
				if (computed == null)
					computed = new ApplyExpr(nil);
//				String ename = nextFnName();
//				function(ename, f);
//				rtnode.addClassExpr(ename);
				computed = new ApplyExpr(cons, f, computed);
			} else
				throw new UtilException("Cannot handle format " + f + " of class " + f.getClass());
		}
		if (!classLiterals.isEmpty() || computed != null) {
			StringLiteral lits = new StringLiteral(String.join(" ", classLiterals));
			if (computed != null)
				computed = new ApplyExpr(join, new ApplyExpr(cons, lits, computed), new StringLiteral(" "));
			attrs = new ApplyExpr(cons, new ApplyExpr(tuple, new StringLiteral("class"), computed != null?computed:lits), attrs);
		}
		
		for (EventHandler x : tl.handlers) {
			// TODO: check that the apply expression is one for a handler
//			card.handlers
			events = new ApplyExpr(cons, new ApplyExpr(tuple, new StringLiteral(x.action), x.expr), events);
		}
		// TODO: still need to build dependency tree
		
		// Return all the pieces together and create the actual DOM Element ctor
		return new RenderTree.ElementExpr(rtnode, new ApplyExpr(domCtor, tag, attrs, children, events));
	}

	private void traverseForMembers(List<CardMember> dependsOn, Object f) {
		if (f == null || f instanceof StringLiteral || f instanceof NumericLiteral || f instanceof LocalVar || f instanceof AbsoluteVar || f instanceof TemplateListVar)
			return;
		if (f instanceof CardMember)
			dependsOn.add((CardMember) f);
		else if (f instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) f;
			traverseForMembers(dependsOn, ae.fn);
			for (Object arg : ae.args)
				traverseForMembers(dependsOn, arg);
		} else if (f instanceof CardFunction) {
			CardFunction cf = (CardFunction) f;
			if (this.prefix.equals(cf.clzName)) {
				FunctionDefinition fd = functions.get(cf.uniqueName());
				for (FunctionCaseDefn fcd : fd.cases) {
					traverseForMembers(dependsOn, fcd.expr);
				}
			} else
				throw new UtilException("Can we handle this case?");
		} else
			throw new UtilException("Case not handled: " + f + " "+ (f!=null?f.getClass():""));
	}

	private void addUpdate(String dependsOn, Route route, String updateType) {
		updates.add(dependsOn, new Update(route, updateType));
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
