package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.LocatableConsumer;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.FreeTextToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

/** This is intended to build up arbitrary numbers of lines of text
 * indented as the user wishes with the caveat that it must all be at least as indented as the first line
 * The result is then the complete text joined by spaces.
 * @author gareth
 *
 */
public class FreeTextParser implements TDAParsing {
	private final KeywordToken kw;
	private final ErrorReporter errors;
	private final LocatableConsumer<FreeTextToken> handler;
	private final FreeTextParser parent;
	private final List<FreeTextToken> buffers;
	private final LocationTracker locTracker;
	private InputPosition firstLoc;
	private InputPosition lastLoc;
	
	public FreeTextParser(KeywordToken kw, ErrorReporter errors, LocationTracker locTracker, LocatableConsumer<FreeTextToken> freeTextHandler) {
		this.kw = kw;
		this.errors = errors;
		this.handler = freeTextHandler;
		this.locTracker = locTracker;
		this.parent = null;
		this.buffers = new ArrayList<>();
		this.lastLoc = kw.location();
	}

	public FreeTextParser(KeywordToken kw, FreeTextParser parent, LocationTracker locTracker) {
		this.kw = kw;
		this.errors = parent.errors;
		this.handler = null;
		this.parent = parent;
		this.locTracker = locTracker;
		this.buffers = parent.buffers;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		if (firstLoc == null)
			firstLoc = pos;
		lastLoc = pos;
		String tok = toks.remainder();
		FreeTextToken ret = new FreeTextToken(pos, tok);
		ret.location().endAt(tok.length());
		errors.logParsingToken(ret);
		errors.logReduction("unit-match-free-text", ret.location(), ret.location().locAtEnd());
		locTracker.updateLoc(ret.location());
		this.buffers.add(ret);
		return new FreeTextParser(kw, this, locTracker);
	}

	private void seenTextAt(InputPosition later) {
		if (later != null)
			lastLoc = later;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (this.handler != null)
			this.handler.accept(lastLoc, FreeTextToken.merge(buffers));
		else if (parent != null) {
			parent.seenTextAt(lastLoc);
		}
	}
}
