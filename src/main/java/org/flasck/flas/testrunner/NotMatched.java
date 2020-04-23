package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public class NotMatched extends FlasTestException {
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

	public String getExpected() {
		return expected;
	}

	public String getActual() {
		return actual;
	}

	@Override
	public String getMessage() {
		if (failure != null)
			return "Matcher failed on '" + selector + "': " + failure;
		else {
			String s = selector != null ? " '" + selector + "'" : "";
			return "Matcher" + s + " failed: expected <<" + expected + ">> but was '" + actual + "'"; 
		}
	}
}
