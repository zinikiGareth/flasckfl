package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public class AssertFailed extends FlasTestException {
	public final Object expected;
	public final Object actual;

	public AssertFailed(Object expected, Object actual) {
		this.expected = expected;
		this.actual = actual;
	}

	public Object getExpected() {
		return expected;
	}

	public Object getActual() {
		return actual;
	}
}
