package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.tokenizers.Tokenizable;

public class LastActionScopeParser implements LastOneOnlyNestedParser {
	private final ErrorReporter errors;
	private final String lastThing;
	private TDAParsing parser;
	private InputPosition lastPos;
	private boolean reportedError;
	private FunctionScopeNamer namer;
	private FunctionScopeUnitConsumer topLevel;
	private StateHolder holder;
	private LocationTracker locTracker;

	public LastActionScopeParser(ErrorReporter errors, FunctionScopeNamer namer, FunctionScopeUnitConsumer topLevel, String lastThing, StateHolder holder, LocationTracker locTracker) {
		this.errors = errors;
		this.namer = namer;
		this.topLevel = topLevel;
		this.lastThing = lastThing;
		this.holder = holder;
		this.locTracker = locTracker;
	}

	public void anotherParent() {
		if (lastPos != null && !reportedError) {
			errors.message(lastPos, "nested scope must be after last " + lastThing);
			reportedError = true;
		}
	}
	
	@Override
	public void bindLocationTracker(LocationTracker locTracker) {
		this.locTracker = locTracker;
	}

	private void createParser() {
		FunctionAssembler assembler = new FunctionAssembler(errors, topLevel, holder, locTracker);
		this.parser = ParsingPhase.functionScopeUnit(errors, namer, assembler, topLevel, holder, assembler);
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (parser == null || ((parser instanceof TDAParsingWithAction && ((TDAParsingWithAction)parser).parser == null)))
			createParser();
		lastPos = toks.realinfo();
		if (locTracker != null)
			locTracker.updateLoc(lastPos);
		return parser.tryParsing(toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (parser == null)
			createParser();
		parser.scopeComplete(location);
	}
}
