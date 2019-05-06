package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAObjectElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ObjectElementsConsumer builder;

	public TDAObjectElementsParser(ErrorReporter errors, ObjectElementsConsumer od) {
		this.errors = errors;
		this.builder = od;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "state": {
			if (toks.hasMore()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			StateDefinition state = new StateDefinition(toks.realinfo());
			builder.defineState(state);
			return new TDAStructFieldParser(errors, state);
		}
		case "ctor": {
			ValidIdentifierToken var = VarNameToken.from(toks);
			List<Pattern> args = new ArrayList<>();
			TDAPatternParser pp = new TDAPatternParser(errors, p -> {
				args.add(p);
			});
			while (pp.tryParsing(toks) != null)
				;
			if (toks.hasMore()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			ObjectCtor ctor = new ObjectCtor(var.location, var.text, args);
			builder.addConstructor(ctor);
			return new TDAMethodMessageParser();
		}
		case "acor": {
			FunctionIntroConsumer consumer = new FunctionIntroConsumer() {
				@Override
				public FunctionName functionName(InputPosition location, String base) {
					return FunctionName.objectMethod(location, builder.name(), base);
				}
				
				@Override
				public void functionIntro(FunctionIntro o) {
					throw new org.zinutils.exceptions.NotImplementedException();
				}
				
				@Override
				public void functionCase(FunctionCaseDefn o) {
					builder.addAccessor(new ObjectAccessor());
				}
			};
			TDAFunctionParser fcp = new TDAFunctionParser(errors, consumer);
			return fcp.tryParsing(toks);
		}
		default: {
			errors.message(toks, "'" + kw.text + "' is not a valid object keyword");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
