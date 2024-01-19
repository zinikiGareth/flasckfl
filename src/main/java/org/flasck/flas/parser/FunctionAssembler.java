package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StateHolder;

public class FunctionAssembler implements FunctionIntroConsumer {
	private final ErrorReporter errors;
	private final FunctionScopeUnitConsumer consumer;
	private final StateHolder holder;
	private FunctionDefinition fn;
	private FunctionIntro curr;
	private boolean broken;

	public FunctionAssembler(ErrorReporter errors, FunctionScopeUnitConsumer consumer, StateHolder holder) {
		this.errors = errors;
		this.consumer = consumer;
		this.holder = holder;
	}

	@Override
	public int nextCaseNumber(FunctionName fname) {
		if (fn != null && fn.name().uniqueName().equals(fname.uniqueName()))
			return fn.intros().size() + 1;
		else
			return 1;
	}
	
	@Override
	public void functionIntro(FunctionIntro next) {
		FunctionName fname = (FunctionName) next.name().inContext;
		if (curr != null)
			reduceIntro();
		if (fn != null && !fname.equals(fn.name())) {
			reduceFunction();
		}
		if (fn == null) {
			fn = new FunctionDefinition(fname, next.args.size(), holder);
		} else if (fn.argCount() != next.args.size()) {
			errors.message(next.location, "inconsistent number of formal parameters");
			broken = true;
			return;
		}
		fn.intro(next);
		curr = next;
	}

	@Override
	public void moveOn() {
		if (fn != null) {
			reduceIntro();
			reduceFunction();
		}
	}
	
	private void reduceIntro() {
		if (!curr.cases().isEmpty())
			errors.logReduction("function-intro", curr, curr.cases().get(curr.cases().size()-1));
	}

	private void reduceFunction() {
		if (!broken && fn != null) {
			consumer.functionDefn(errors, fn);
			errors.logReduction("function-from-intros", fn, fn.intros().get(fn.intros().size()-1));
		}
		fn = null;
		curr = null;
		broken = false;
	}
}
