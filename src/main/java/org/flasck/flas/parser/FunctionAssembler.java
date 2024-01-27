package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StateHolder;

public class FunctionAssembler implements FunctionIntroConsumer, LocationTracker {
	private final ErrorReporter errors;
	private final FunctionScopeUnitConsumer consumer;
	private final StateHolder holder;
	private FunctionDefinition fn;
	private FunctionIntro curr;
	private boolean broken;
	private InputPosition lastLoc = null;
	private final LocationTracker tracker;

	public FunctionAssembler(ErrorReporter errors, FunctionScopeUnitConsumer consumer, StateHolder holder, LocationTracker tracker) {
		this.errors = errors;
		this.consumer = consumer;
		this.holder = holder;
		this.tracker = tracker;
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
		lastLoc = next.location();
	}

	@Override
	public void moveOn() {
		if (fn != null) {
			reduceIntro();
			reduceFunction();
			fn = null;
		}
	}
	
	@Override
	public void updateLoc(InputPosition location) {
		if (location == null)
			return;
		if (fn != null) {
			if (location.compareTo(lastLoc) > 0)
				lastLoc = location;
		} else if (tracker != null)
			tracker.updateLoc(location);
//		System.out.println("Assembling " + fn.name() + " " + location + " => " + lastLoc);
	}
	
	private void reduceIntro() {
		if (!curr.cases().isEmpty()) {
			errors.logReduction("function-intro", curr, curr.cases().get(curr.cases().size()-1));
		}
		if (curr.location().compareTo(lastLoc) > 0)
			lastLoc = curr.location();
	}

	private void reduceFunction() {
		if (!broken && fn != null) {
//			System.out.println("reducing " + fn.name() + " with " + lastLoc);
			errors.logReduction("simple-function-case-definition", fn.location(), lastLoc);
			if (tracker != null)
				tracker.updateLoc(lastLoc);
			consumer.functionDefn(errors, fn);
		}
		fn = null;
		curr = null;
		broken = false;
	}

	@Override
	public String toString() {
		if (curr == null && fn == null)
			return "Assembler[]";
		else if (fn != null)
			return "Assembler[" + fn.name() + "]";
		else
			return "Assembler[" + curr + "]";
	}
}
