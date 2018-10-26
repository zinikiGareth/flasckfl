package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.parser.TDAParsing;
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

}
