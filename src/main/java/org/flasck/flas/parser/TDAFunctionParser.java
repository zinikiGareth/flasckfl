package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
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
	public TDAParsing tryParsing(Tokenizable line) {
		if (FLASCompiler.backwardCompatibilityMode) {
			Object o = delegate.tryParsing(line);
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
			ExprToken t = ExprToken.from(line);
			if (t == null || t.type != ExprToken.IDENTIFIER)
				return null;
			final FunctionName fname = consumer.functionName(t.location, t.text);
			
			List<Object> args = null;
			ExprToken tok;
			while ((tok = ExprToken.from(line)) != null && !tok.text.equals("=")) {
				
			}
			if (tok == null) {
				consumer.functionIntro(new FunctionIntro(fname, args));
				return new TDAFunctionCaseParser(errors, consumer);
			}
			if (!line.hasMore()) {
				errors.message(line, "function definition requires expression");
				return null;
			}
			new TDAExpressionParser(errors, e -> {
				consumer.functionCase(new FunctionCaseDefn(fname, args, e));
			}).tryParsing(line);

			// TODO: I don't think this should be quite top - it should allow "as many" intro things (which? not card, but some others such as handler are good to have)
			return TDAMultiParser.top(errors, consumer);
		}
	}

}
