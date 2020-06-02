package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.FunctionDefinition;

@SuppressWarnings("serial")
public class UnboundTypeException extends RuntimeException {
	public final FunctionDefinition fn;

	public UnboundTypeException(FunctionDefinition fn) {
		this.fn = fn;
	}
}
