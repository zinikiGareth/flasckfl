package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATypeReferenceParser;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAUnitTestDataParser implements TDAParsing {
	private final ErrorReporter errors;
	private final boolean atTopLevel;
	private final UnitDataNamer namer;
	private final Consumer<UnitDataDeclaration> builder;

	public TDAUnitTestDataParser(ErrorReporter errors, boolean atTopLevel, UnitDataNamer namer, Consumer<UnitDataDeclaration> builder) {
		this.errors = errors;
		this.atTopLevel = atTopLevel;
		this.namer = namer;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
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
			UnitDataDeclaration data = new UnitDataDeclaration(pos, atTopLevel, tr.get(0), fnName, null);
			builder.accept(data);
			return new TDAProcessFieldsParser(errors, data);
		}
		ExprToken send = ExprToken.from(errors, toks);
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
		UnitDataDeclaration data = new UnitDataDeclaration(pos, atTopLevel, tr.get(0), fnName, exprs.get(0));
		builder.accept(data);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
