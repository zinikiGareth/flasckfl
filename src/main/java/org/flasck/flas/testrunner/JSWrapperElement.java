package org.flasck.flas.testrunner;

import com.ui4j.api.dom.Element;

public class JSWrapperElement implements ElementWrapper {

	private final Element elt;

	public JSWrapperElement(Element elt) {
		this.elt = elt;
	}

	@Override
	public String getContents() {
		return elt.getInnerHTML();
	}

	@Override
	public String getElement() {
		return elt.getOuterHTML();
	}

}
