package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.parser.TDAFunctionParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.reflection.Reflection;

public class TDAMultiParser implements TDAParsing {
	private final ErrorReporter errors;
	private final List<TDAParsing> parsers = new ArrayList<>();

	@SafeVarargs
	public TDAMultiParser(ErrorReporter errors, ParsedLineConsumer consumer, Class<? extends TDAParsing>... klz) {
		this.errors = errors;
		for (Class<? extends TDAParsing> k : klz)
			parsers.add(Reflection.create(k, errors, consumer));
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

	public static TDAParsing top(ErrorReporter errors, ParsedLineConsumer sb) {
		return new TDAMultiParser(errors, sb, TDAIntroParser.class, TDAFunctionParser.class, TDATupleDeclarationParser.class);
	}

	// I added this method for testing purposes
	public boolean contains(Class<?> cls) {
		for (TDAParsing p : parsers)
			if (cls.isInstance(p))
				return true;
		return false;
	}

}
