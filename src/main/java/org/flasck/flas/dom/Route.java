package org.flasck.flas.dom;

import java.util.ArrayList;
import java.util.List;

public class Route {
	private class Element {
		private final String name;
		private final String field;

		public Element(String name, String field) {
			this.name = name;
			this.field = field;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}

	private final List<Element> routes = new ArrayList<Element>();
	
	public Route() {
	}
	
	private Route(Route r, Element and) {
		routes.addAll(r.routes);
		routes.add(and);
	}
	
	public Route extendListVar(String name) {
		return new Route(this, new Element("+" + name, "template"));
	}

	public Route extendDivMember(int pos) {
		String prefix = routes.isEmpty()?"":".";
		return new Route(this, new Element(prefix + pos, "children[" + pos + "]"));
	}

	public Route extendCase(int orc) {
		String prefix = routes.isEmpty()?"":".";
		return new Route(this, new Element(prefix + orc, "cases[" + orc + "]"));
	}

	public Route template() {
		String prefix = routes.isEmpty()?"":".";
		return new Route(this, new Element(prefix + "-", "template"));
	}

	public String name() {
		StringBuilder sb = new StringBuilder();
		for (Element e : routes)
			sb.append(e.name);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return name();
	}

	public String path(StringBuilder sb) {
		for (Element e : routes)
			sb.append("."+e.field);
		return sb.toString();
	}
}
