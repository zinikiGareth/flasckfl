package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATypeReferenceParser;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAUnitTestParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitTestNamer namer;
	private final UnitTestDefinitionConsumer builder;

	public TDAUnitTestParser(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer builder) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken tok = KeywordToken.from(toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		switch (tok.text) {
		case "data": {
			List<TypeReference> tr = new ArrayList<>();
			TDATypeReferenceParser parser = new TDATypeReferenceParser(errors, x -> tr.add(x));
			if (parser.tryParsing(toks) == null) {
				// it failed
				return new IgnoreNestedParser();
			}
			ValidIdentifierToken var = VarNameToken.from(toks);
			if (var == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			FunctionName fnName = namer.dataName(var.location, var.text);
			if (!toks.hasMore()) {
				UnitDataDeclaration data = new UnitDataDeclaration(tr.get(0), fnName, null);
				builder.data(data);
				return new TDAProcessFieldsParser(errors, data);
			}
			ExprToken send = ExprToken.from(toks);
			if (send == null || !send.text.equals("<-")) {
				errors.message(toks, "expected <-");
				return new IgnoreNestedParser();
			}
			List<Expr> exprs = new ArrayList<>();
			new TDAExpressionParser(errors, x->exprs.add(x)).tryParsing(toks);
			if (exprs.isEmpty()) {
				// it failed
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			UnitDataDeclaration data = new UnitDataDeclaration(tr.get(0), fnName, exprs.get(0));
			builder.data(data);
			return new NoNestingParser(errors);
		}
		case "test": {
			final String desc = toks.remainder().trim();
			if (desc.length() == 0) {
				errors.message(toks, "test case must have a description");
				return new IgnoreNestedParser();
			}
			final UnitTestCase utc = new UnitTestCase(namer.unitTest(), desc);
			builder.testCase(utc);
			return new TestStepParser(errors, namer, utc);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
