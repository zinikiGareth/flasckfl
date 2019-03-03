package org.flasck.flas.parser;

import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAFunctionParser implements TDAParsing, ScopeReceiver {
	private final ErrorReporter errors;
	private final ParsedLineConsumer consumer;
	private FunctionParser delegate;

	public TDAFunctionParser(ErrorReporter errors, ParsedLineConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
		consumer.scopeTo(this);
	}
	
	@Override
	public void provideScope(Scope scope) {
		State state = new State(scope, scope.scopeName.uniqueName());
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
			else if (o instanceof FunctionIntro)
				consumer.functionIntro((FunctionIntro)o);
			else if (o instanceof FunctionCaseDefn)
				consumer.functionCase((FunctionCaseDefn)o);
			else
				throw new RuntimeException("Cannot handle " + o.getClass());

			// This should, of course, be a "ScopedParser" of some description,
			// probably a rename of IntroParser
			return new NoNestingParser(errors);
		} else {
			throw new NotImplementedException();
		}
	}

}
