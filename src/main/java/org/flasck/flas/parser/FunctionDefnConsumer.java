package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.FunctionDefinition;

@FunctionalInterface
public interface FunctionDefnConsumer {
	void functionDefn(FunctionDefinition func);

}
