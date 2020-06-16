package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.FunctionDefinition;

@SuppressWarnings("serial")
public class UnboundTypeException extends RuntimeException {
	public final FunctionDefinition fn;

	public UnboundTypeException(FunctionDefinition fn) {
		super(fn.name().uniqueName());
		this.fn = fn;
	}
}
