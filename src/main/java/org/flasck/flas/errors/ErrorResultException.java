package org.flasck.flas.errors;

@SuppressWarnings("serial")
public class ErrorResultException extends Exception {
	public final ErrorResult errors;

	public ErrorResultException(ErrorResult errors) {
		this.errors = errors;
	}

}
