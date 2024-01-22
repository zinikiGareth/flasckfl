package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATypeReferenceParser;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAUnitTestDataParser implements TDAParsing {
	private final ErrorReporter errors;
	private final boolean atTopLevel;
	private final KeywordToken kw;
	private final UnitDataNamer namer;
	private final Consumer<UnitDataDeclaration> builder;
	private final FunctionScopeUnitConsumer topLevel;
	private final LocationTracker locTracker;

	public TDAUnitTestDataParser(ErrorReporter errors, boolean atTopLevel, KeywordToken kw, UnitDataNamer namer, Consumer<UnitDataDeclaration> builder, FunctionScopeUnitConsumer topLevel, LocationTracker locTracker) {
		this.errors = errors;
		this.atTopLevel = atTopLevel;
		this.kw = kw;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
		this.locTracker = locTracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		List<TypeReference> tr = new ArrayList<>();
		TDATypeReferenceParser parser = new TDATypeReferenceParser(errors, namer, x -> tr.add(x), topLevel);
		if (parser.tryParsing(toks) == null) {
			// it failed
			return new IgnoreNestedParser(errors);
		}
		ValidIdentifierToken var = VarNameToken.from(errors, toks);
		if (var == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		FunctionName fnName = namer.dataName(var.location, var.text);
		if (!toks.hasMoreContent(errors)) {
			errors.logReduction("test-data-declaration", kw, var);
			if (locTracker != null)
				locTracker.updateLoc(kw.location);
			UnitDataDeclaration data = new UnitDataDeclaration(pos, atTopLevel, tr.get(0), fnName, null);
			builder.accept(data);
			return new TDAUTDataProcessFieldsParser(errors, data);
		}
		ExprToken send = ExprToken.from(errors, toks);
		if (send == null || !send.text.equals("<-")) {
			errors.message(toks, "expected <-");
			return new IgnoreNestedParser(errors);
		}
		List<Expr> exprs = new ArrayList<>();
		new TDAExpressionParser(errors, x->exprs.add(x)).tryParsing(toks);
		if (exprs.isEmpty()) {
			// it failed
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("test-data-declaration-with-init", kw, exprs.get(0));
		if (locTracker != null)
			locTracker.updateLoc(kw.location);
		UnitDataDeclaration data = new UnitDataDeclaration(pos, atTopLevel, tr.get(0), fnName, exprs.get(0));
		builder.accept(data);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
