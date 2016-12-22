package org.flasck.flas.testrunner;

import java.util.List;

@SuppressWarnings("serial")
public class MultiException extends RuntimeException {
	private final List<String> errors;

	public MultiException(List<String> errors) {
		this.errors = errors;
	}

	public Iterable<String> allErrors() {
		return errors;
	}

}
