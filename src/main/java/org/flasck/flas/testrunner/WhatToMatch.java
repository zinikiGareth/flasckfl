package org.flasck.flas.testrunner;

import java.util.List;

import org.zinutils.exceptions.UtilException;

public enum WhatToMatch {
	COUNT {
		@Override
		public void match(String selector, String expected, List<ElementWrapper> actual) throws NotMatched {
			if (actual.size() != Integer.parseInt(expected))
				throw new NotMatched(selector, expected, Integer.toString(actual.size()));
		}
	}, ELEMENT {
		@Override
		public void match(String selector, String expected, List<ElementWrapper> actual) throws NotMatched {
			String html = getElement(actual, selector).getElement();
			if (!expected.equals(html))
				throw new NotMatched(selector, expected, html);
		}
	}, CONTENTS {
		@Override
		public void match(String selector, String expected, List<ElementWrapper> actual) throws NotMatched {
			String html = getElement(actual, selector).getContents();
			if (!expected.equals(html))
				throw new NotMatched(selector, expected, html);
		}
	}, CLASS {
		@Override
		public void match(String selector, String expected, List<ElementWrapper> actual) throws NotMatched {
			throw new UtilException("not implemented");
		}
	};

	public abstract void match(String selector, String expected, List<ElementWrapper> actual) throws NotMatched;

	protected static ElementWrapper getElement(List<ElementWrapper> actual, String selector) throws NotMatched {
		if (actual.isEmpty())
			throw new NotMatched(selector, "nothing matched");
		else if (actual.size() != 1)
			throw new NotMatched(selector, actual.size() + " items matched");
		ElementWrapper element = actual.get(0);
		return element;
	}
}