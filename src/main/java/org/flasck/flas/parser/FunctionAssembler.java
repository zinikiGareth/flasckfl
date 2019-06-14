package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;

public class FunctionAssembler implements FunctionIntroConsumer {

	private final FunctionScopeUnitConsumer consumer;
	private FunctionIntro curr;

	public FunctionAssembler(FunctionScopeUnitConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void functionIntro(FunctionIntro curr) {
		this.curr = curr;
	}

	public void scopeDone() {
		consumer.functionDefn(new FunctionDefinition(curr.name(), curr.args.size()));
	}
}
