package org.flasck.flas.parser;

import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAFunctionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TopLevelDefnConsumer consumer;
	private final FunctionParser delegate;

	public TDAFunctionParser(ErrorReporter errors, TopLevelDefnConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
		State state = new State(null, null);
		delegate = new FunctionParser(state);
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (FLASCompiler.backwardCompatibilityMode) {
			Object o = delegate.tryParsing(toks);
			if (o == null)
				return null;
			else if (o instanceof ErrorReporter)
				errors.merge((ErrorReporter)o);
			else
				consumer.functionCase((FunctionCaseDefn)o);

			// This should, of course, be a "ScopedParser" of some description,
			// probably a rename of IntroParser
			return new NoNestingParser(errors);
		} else {
			throw new NotImplementedException();
		}
	}

}
