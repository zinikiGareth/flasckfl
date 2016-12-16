package org.flasck.flas.testrunner;

import java.util.List;

import com.ui4j.api.dom.Element;

public enum WhatToMatch {
	COUNT {
		@Override
		public void match(String selector, String expected, List<Element> actual) throws NotMatched {
			if (actual.size() != Integer.parseInt(expected))
				throw new NotMatched(selector, expected, Integer.toString(actual.size()));
		}
	}, ELEMENT {
		@Override
		public void match(String selector, String expected, List<Element> actual) throws NotMatched {
			if (actual.isEmpty())
				throw new NotMatched(selector, "nothing matched");
			else if (actual.size() != 1)
				throw new NotMatched(selector, actual.size() + " items matched");
			Element element = actual.get(0);
			String html = element.getOuterHTML();
			if (!expected.equals(html))
				throw new NotMatched(selector, expected, html);
		}
	}, CONTENTS {
		@Override
		public void match(String selector, String expected, List<Element> actual) throws NotMatched {
			if (actual.isEmpty())
				throw new NotMatched(selector, "nothing matched");
			else if (actual.size() != 1)
				throw new NotMatched(selector, actual.size() + " items matched");
			Element element = actual.get(0);
			String html = element.getInnerHTML();
			if (!expected.equals(html))
				throw new NotMatched(selector, expected, html);
		}
	};

	public abstract void match(String selector, String expected, List<Element> actual) throws NotMatched;
}