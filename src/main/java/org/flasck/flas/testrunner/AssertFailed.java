package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public class AssertFailed extends FlasTestException {
	public final Object expected;
	public final Object actual;
	public final AssertFailed cause;

	public AssertFailed(Object expected, Object actual) {
		this(expected, actual, null);
	}

	public AssertFailed(Object expected, Object actual, AssertFailed ex) {
		this.expected = expected;
		this.actual = actual;
		this.cause = ex;
	}

	public Object getExpected() {
		return expected;
	}

	public Object getActual() {
		return actual;
	}
}
