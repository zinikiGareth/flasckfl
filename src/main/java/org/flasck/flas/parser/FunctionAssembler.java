package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;

public class FunctionAssembler implements FunctionIntroConsumer {
	private final ErrorReporter errors;
	private final FunctionScopeUnitConsumer consumer;
	private FunctionDefinition curr;
	private boolean broken;

	public FunctionAssembler(ErrorReporter errors, FunctionScopeUnitConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public void functionIntro(FunctionIntro next) {
		if (curr == null || !next.name().equals(curr.name())) {
			if (curr != null)
				consumer.functionDefn(curr);
			curr = new FunctionDefinition(next.name(), next.args.size());
			broken = false;
		}
		if (curr.argCount() != next.args.size()) {
			errors.message(next.location, "inconsistent number of formal parameters");
			broken = true;
			return;
		}
		curr.intro(next);
	}

	@Override
	public void moveOn() {
		if (!broken && curr != null)
			consumer.functionDefn(curr);
		curr = null;
		broken = false;
	}
}
