package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.FunctionIntroConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMultiParser implements TDAParsing {
	private final ErrorReporter errors;
	private final List<TDAParsing> parsers = new ArrayList<>();

	@SafeVarargs
	public TDAMultiParser(ErrorReporter errors, TDAParserConstructor... klz) {
		this.errors = errors;
		for (TDAParserConstructor k : klz)
			parsers.add(k.construct(errors));
	}

	// needs to return the parser that will do the children
	public TDAParsing tryParsing(Tokenizable toks) {
		for (TDAParsing p : parsers) {
			toks.reset(0);
			final TDAParsing nested = p.tryParsing(toks);
			if (nested != null)
				return nested;
		}
		toks.reset(0);
		errors.message(toks, "syntax error");
		return null;
	}

	public static TDAParsing topLevelUnit(ErrorReporter errors, FunctionNameProvider namer, TopLevelDefinitionConsumer sb) {
		return new TDAMultiParser(errors, TDAIntroParser.constructor(sb), TDAFunctionParser.constructor(namer, sb, sb), TDATupleDeclarationParser.constructor(namer, sb, sb));
	}
	
	public static TDAParsing functionScopeUnit(ErrorReporter errors, FunctionNameProvider namer, FunctionIntroConsumer sb, FunctionScopeUnitConsumer topLevel) {
		// TODO: this should include some of the intro stuff, specifically handlers and standalone methods ...
		return new TDAMultiParser(errors, /*TDAIntroParser.constructor(sb), */TDAFunctionParser.constructor(namer, sb, topLevel), TDATupleDeclarationParser.constructor(namer, sb, topLevel));
	}

	// I added this method for testing purposes
	public boolean contains(Class<?> cls) {
		for (TDAParsing p : parsers)
			if (cls.isInstance(p))
				return true;
		return false;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
