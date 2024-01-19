package org.flasck.flas.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAMultiParser implements TDAParsing {
	private final ErrorReporter errors;
	private final List<TDAParsing> parsers = new ArrayList<>();
	private TDAParsing chosen;
	private List<BiConsumer<ErrorReporter, InputPosition>> onCompleteHandlers = new ArrayList<>();

	@SafeVarargs
	public TDAMultiParser(ErrorReporter errors, TDAParserConstructor... klz) {
		this.errors = errors;
		for (TDAParserConstructor k : klz)
			parsers.add(k.construct(errors));
	}
	
	public void add(TDAParsing parser) {
		parsers.add(parser);
	}

	public void add(int pos, TDAParsing parser) {
		parsers.add(pos, parser);
	}

	// needs to return the parser that will do the children
	public TDAParsing tryParsing(Tokenizable toks) {
		ErrorMark mark = errors.mark();
		TDAParsing nested = null;
		for (TDAParsing p : parsers) {
			if (nested == null) {
				toks.reset(0);
				nested = p.tryParsing(toks);
				if (nested != null)
					chosen = p;
			} else
				p.choseOther();
		}
		if (nested != null)
			return nested;
		toks.reset(0);
		if (!mark.hasMoreNow())
			errors.message(toks, "syntax error");
		return new IgnoreNestedParser(errors);
	}

	// I added this method for testing purposes
	public boolean contains(Class<?> cls) {
		for (TDAParsing p : parsers)
			if (cls.isInstance(p))
				return true;
		return false;
	}

	public void onComplete(BiConsumer<ErrorReporter, InputPosition> handler) {
		onCompleteHandlers.add(handler);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (chosen != null)
			chosen.scopeComplete(location);
		for (BiConsumer<ErrorReporter, InputPosition> h : onCompleteHandlers)
			h.accept(errors, location);
//		if (endRuleName != null) {
//			errors.logReduction(endRuleName, parentStart, location);
//		}
	}
}
