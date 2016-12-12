package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public class AssertFailed extends Exception {
	public final Object expected;
	public final Object actual;

	public AssertFailed(Object expected, Object actual) {
		this.expected = expected;
		this.actual = actual;
	}

}
