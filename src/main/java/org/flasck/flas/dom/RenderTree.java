package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;

public class RenderTree {
	public static class ElementExpr {
		public final Element element;
		public final ApplyExpr expr;

		public ElementExpr(Element tree, ApplyExpr expr) {
			this.element = tree;
			this.expr = expr;
		}
	}

	public static class Element {
		public final String type;
		public final String fn;
		public final String val;
		public final String route;
		public final List<Element> children = new ArrayList<Element>();
		public final List<String> classes = new ArrayList<String>();
		public final List<String> clsexprs = new ArrayList<String>();
		
		public Element(String t, String f, String val, String route) {
			type = t;
			fn = f;
			this.val = val;
			this.route = route.length() == 0 ? "" : route.substring(1);
		}
		
		@SuppressWarnings("unchecked")
		public void addChildren(Object obj) {
			if (obj instanceof Element)
				children.add((Element)obj);
			else
				children.addAll((Collection<Element>)obj);
		}

		public void addClass(String clz) {
			classes.add(clz);
		}

		public void addClassExpr(String ename) {
			clsexprs.add(ename);
		}
	}

	public final String card;
	public final String template;
	public final Element ret;

	public RenderTree(String card, String template, Element ret) {
		this.card = card;
		this.template = template;
		this.ret = ret;
	}
}
