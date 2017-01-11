package org.flasck.flas.testrunner;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.zinutils.exceptions.NotImplementedException;

public abstract class HTMLMatcher {
	public static class Count extends HTMLMatcher {
		private int matchCount;

		public Count(String text) {
			this.matchCount = Integer.parseInt(text);
		}

		@Override
		public void match(String selector, List<ElementWrapper> actual) throws NotMatched {
			if (actual.size() != matchCount)
				throw new NotMatched(selector, Integer.toString(matchCount), Integer.toString(actual.size()));
		}
	}

	public static class Element extends HTMLMatcher {
		private TextMatcher expected;

		public Element(String text) {
			this.expected = stringOrRegexp(text);
		}

		@Override
		public void match(String selector, List<ElementWrapper> actual) throws NotMatched {
			String html = getElement(actual, selector).getElement();
			if (!expected.matches(html))
				throw new NotMatched(selector, expected.getDescription(), html);
		}
	}

	public static class Contents extends HTMLMatcher {
		private TextMatcher expected;

		public Contents(String text) {
			this.expected = stringOrRegexp(text);
		}

		@Override
		public void match(String selector, List<ElementWrapper> actual) throws NotMatched {
			String html = getElement(actual, selector).getContents();
			if (!expected.matches(html))
				throw new NotMatched(selector, expected.getDescription(), html);
		}
	}

	public static class Class extends HTMLMatcher {
		private TreeSet<String> expectedSet;

		public Class(String text) {
			expectedSet = new TreeSet<String>(Arrays.asList(text.split(" ")));
			expectedSet.removeIf(x->x.length() == 0);
		}

		@Override
		public void match(String selector, List<ElementWrapper> actual) throws NotMatched {
			ElementWrapper elt = getElement(actual, selector);
			String classes = elt.getAttribute("class");
			TreeSet<String> classSet = new TreeSet<String>();
			if (classes != null) {
				classSet.addAll(Arrays.asList(classes.split(" ")));
				classSet.removeIf(x->x.length() == 0);
			}
			if (!expectedSet.equals(classSet))
				throw new NotMatched(selector, expectedSet.toString(), classSet.toString());
		}
	}

	public abstract void match(String selector, List<ElementWrapper> actual) throws NotMatched;

	public TextMatcher stringOrRegexp(String text) {
		if (text.startsWith("/") && text.endsWith("/"))
			return new TextMatcher.Regexp(text.substring(1, text.length()-1));
		else if (text.startsWith("\"") && text.endsWith("\"") || text.startsWith("'") && text.endsWith("'"))
			return new TextMatcher.Exact(text.substring(1, text.length()-1));
		else
			return new TextMatcher.Exact(text);
	}

	protected ElementWrapper getElement(List<ElementWrapper> actual, String selector) throws NotMatched {
		if (actual.isEmpty())
			throw new NotMatched(selector, "nothing matched");
		else if (actual.size() != 1)
			throw new NotMatched(selector, actual.size() + " items matched");
		ElementWrapper element = actual.get(0);
		return element;
	}
}