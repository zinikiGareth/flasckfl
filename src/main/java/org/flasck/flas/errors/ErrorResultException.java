package org.flasck.flas.errors;

@SuppressWarnings("serial")
public class ErrorResultException extends Exception {
	public final ErrorReporter errors;

	public ErrorResultException(ErrorReporter errors) {
		this.errors = errors;
	}

}
