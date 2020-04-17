package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.FunctionName;
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
	public int nextCaseNumber(FunctionName fname) {
		if (curr != null && curr.name().uniqueName().equals(fname.uniqueName()))
			return curr.intros().size() + 1;
		else
			return 1;
	}
	
	@Override
	public void functionIntro(FunctionIntro next) {
		FunctionName fname = (FunctionName) next.name().inContext;
		if (curr == null || !fname.equals(curr.name())) {
			if (curr != null)
				consumer.functionDefn(errors, curr);
			curr = new FunctionDefinition(fname, next.args.size());
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
			consumer.functionDefn(errors, curr);
		curr = null;
		broken = false;
	}
}
