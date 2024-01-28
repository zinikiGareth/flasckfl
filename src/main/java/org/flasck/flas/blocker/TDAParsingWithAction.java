package org.flasck.flas.blocker;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAParsingWithAction implements TDAParsing {
	public final TDAParsing parser;
	public final Runnable afterParsing;

	public TDAParsingWithAction(TDAParsing parser, Runnable afterParsing) {
		this.parser = parser;
		this.afterParsing = afterParsing;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return parser.tryParsing(toks);
	}

	public static boolean is(TDAParsing parser, Class<? extends TDAParsing> cls) {
		if (cls.isInstance(parser))
			return true;
		else if (parser instanceof TDAParsingWithAction)
			return is(((TDAParsingWithAction)parser).parser, cls);
		else
			return false;
	}
	
	public static void invokeAction(TDAParsing parser) {
		if (parser instanceof TDAParsingWithAction)
			((TDAParsingWithAction)parser).afterParsing.run();
	}

	@Override
	public void scopeComplete(InputPosition location) {
		parser.scopeComplete(location);
	}
}
