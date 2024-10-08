package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StateHolder;

public class FunctionAssembler extends BlockLocationTracker implements FunctionIntroConsumer {
	private final FunctionScopeUnitConsumer consumer;
	private final StateHolder holder;
	private FunctionDefinition fn;
	private FunctionIntro curr;
	private boolean broken;
	private boolean hasGuards;

	public FunctionAssembler(ErrorReporter errors, FunctionScopeUnitConsumer consumer, StateHolder holder, LocationTracker tracker) {
		super(errors, tracker);
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
			reduceCurr();
		if (fn != null && !fname.equals(fn.name())) {
			reduceFunction();
		}
		if (fn == null) {
			fn = new FunctionDefinition(fname, next.args.size(), holder);
		} else if (fn.argCount() != next.args.size()) {
			errors.message(next.location, "inconsistent number of formal parameters");
			broken = true;
			
			// 2024-06-18 To avoid cascades, define the function anyway; if you don't every use shows up as an "undefined function" error,
			// swamping the actual error message
			consumer.functionDefn(errors, fn);
			
			return;
		}
		fn.intro(next);
		curr = next;
	}

	@Override
	public void hasGuards(boolean hasGuards) {
		this.hasGuards = hasGuards;;
	}

	@Override
	public void done() {
		if (curr != null) {
			reduceCurr();
			curr = null;
		}
	}

	@Override
	public void moveOn() {
		if (curr != null) {
			reduceCurr();
			curr = null;
		}
		
		if (fn != null) {
			reduceFunction();
			fn = null;
		}
	}
		
	private void reduceCurr() {
		if (curr.cases().size() == 1) {
			// it's either the very simple one-line case
			if (!hasGuards) {
				errors.logReduction("simple-function-case-definition-intro", curr.location(), curr.cases().get(0).expr.location());
			} else {
				InputPosition to = curr.location;
				if (!curr.args.isEmpty())
					to = curr.args.get(curr.args.size()-1).location();
				// or it's the degenerate case
				if (curr.cases().get(0).guard == null)
					errors.logReduction("degenerate-guarded-function-case-definition-intro", curr.location(), to);
				else
					errors.logReduction("guarded-function-case-definition-intro", curr.location(), to);
			}
		} else {
			InputPosition to = curr.location;
			if (!curr.args.isEmpty())
				to = curr.args.get(curr.args.size()-1).location();
			// it must have multiple guards, each of which should have been reduced
			if (!curr.cases().isEmpty()) {
//				FunctionCaseDefn lastCase = curr.cases().get(curr.cases().size()-1);
				errors.logReduction("guarded-function-case-definition-intro", curr.location(), to);
			} // the alternative is that it has an error ...
		}
	}

	private void reduceFunction() {
		if (!broken && fn != null) {
//			System.out.println("reducing " + fn.name() + " with " + lastLoc);
//			errors.logReduction("function-case-definition", fn.location(), lastLoc);
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
