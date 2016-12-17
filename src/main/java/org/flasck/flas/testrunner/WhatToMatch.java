package org.flasck.flas.testrunner;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

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
			ElementWrapper elt = getElement(actual, selector);
			String classes = elt.getAttribute("class");
			TreeSet<String> classSet = new TreeSet<String>();
			if (classes != null) {
				classSet.addAll(Arrays.asList(classes.split(" ")));
				classSet.removeIf(x->x.length() == 0);
			}
			TreeSet<String> expectedSet = new TreeSet<String>(Arrays.asList(expected.split(" ")));
			expectedSet.removeIf(x->x.length() == 0);
			if (!expectedSet.equals(classSet))
				throw new NotMatched(selector, expectedSet.toString(), classSet.toString());
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