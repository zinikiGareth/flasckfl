package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public class NotMatched extends Exception {
	private final String selector;
	private final String failure;
	private final String expected;
	private final String actual;

	public NotMatched(String selector, String failure) {
		this.selector = selector;
		this.failure = failure;
		this.expected = null;
		this.actual = null;
	}

	public NotMatched(String selector, String expected, String actual) {
		this.selector = selector;
		this.expected = expected;
		this.actual = actual;
		this.failure = null;
	}

	@Override
	public String toString() {
		if (failure != null)
			return "Matcher failed on '" + selector + "': " + failure;
		else
			return "Matcher '" + selector + "' failed: expected '" + expected + "' but was '" + actual + "'"; 
	}
}
