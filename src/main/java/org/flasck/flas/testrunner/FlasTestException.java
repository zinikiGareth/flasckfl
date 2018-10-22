package org.flasck.flas.testrunner;

@SuppressWarnings("serial")
public abstract class FlasTestException extends Exception {

	public abstract Object getExpected();

	public abstract Object getActual();

}
