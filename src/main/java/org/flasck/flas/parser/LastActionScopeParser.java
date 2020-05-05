package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;

public class LastActionScopeParser implements LastOneOnlyNestedParser {
	private final ErrorReporter errors;
	private final String lastThing;
	private final TDAParsing parser;
	private Tokenizable seenSomething;
	private boolean reportedError;

	public LastActionScopeParser(ErrorReporter errors, FunctionScopeNamer namer, FunctionScopeUnitConsumer topLevel, String lastThing, boolean stateAvailable) {
		this.errors = errors;
		this.lastThing = lastThing;
		FunctionIntroConsumer assembler = new FunctionAssembler(errors, topLevel, stateAvailable);
		this.parser = TDAMultiParser.functionScopeUnit(errors, namer, assembler, topLevel, stateAvailable);
	}

	public void anotherParent() {
		if (seenSomething != null && !reportedError) {
			errors.message(seenSomething, "nested scope must be after last " + lastThing);
			reportedError = true;
		}
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		seenSomething = toks;
		return parser.tryParsing(toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
