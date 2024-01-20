package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.tokenizers.Tokenizable;

public class LastActionScopeParser implements LastOneOnlyNestedParser {
	private final ErrorReporter errors;
	private final String lastThing;
	private final TDAParsing parser;
	private InputPosition from;
	private InputPosition lastPos;
	private boolean reportedError;

	public LastActionScopeParser(ErrorReporter errors, FunctionScopeNamer namer, FunctionScopeUnitConsumer topLevel, String lastThing, StateHolder holder) {
		this.errors = errors;
		this.lastThing = lastThing;
		FunctionIntroConsumer assembler = new FunctionAssembler(errors, topLevel, holder);
		this.parser = ParsingPhase.functionScopeUnit(errors, namer, assembler, topLevel, holder);
	}

	public void anotherParent() {
		if (lastPos != null && !reportedError) {
			errors.message(lastPos, "nested scope must be after last " + lastThing);
			reportedError = true;
		}
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		lastPos = toks.realinfo();
		if (from == null)
			lastPos = from;
		return parser.tryParsing(toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		parser.scopeComplete(location);
		if (from != null)
			errors.logReduction("inner-block", from, lastPos);
	}

}
