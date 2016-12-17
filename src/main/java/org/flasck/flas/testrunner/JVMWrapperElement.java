package org.flasck.flas.testrunner;

import org.jsoup.nodes.Element;

public class JVMWrapperElement implements ElementWrapper {
	private final Element elt;

	public JVMWrapperElement(Element elt) {
		this.elt = elt;
	}

	@Override
	public String getContents() {
		return elt.html();
	}

	@Override
	public String getElement() {
		return elt.outerHtml();
	}

	@Override
	public String getAttribute(String name) {
		return elt.attr(name);
	}
}
