package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;

@FunctionalInterface
public interface FunctionDefnConsumer {
	void functionDefn(ErrorReporter errors, FunctionDefinition func);
}
