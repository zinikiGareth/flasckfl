package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

/** This is intended to build up arbitrary numbers of lines of text
 * indented as the user wishes with the caveat that it must all be at least as indented as the first line
 * The result is then the complete text joined by spaces.
 * @author gareth
 *
 */
public class FreeTextParser implements TDAParsing {
	private final ErrorReporter errors;
	private final List<String> buffers;
	private final Consumer<String> handler;
	
	public FreeTextParser(ErrorReporter errors, Consumer<String> freeTextHandler) {
		this.errors = errors;
		this.handler = freeTextHandler;
		this.buffers = new ArrayList<>();
	}

	public FreeTextParser(FreeTextParser parent) {
		this.errors = parent.errors;
		this.handler = null;
		this.buffers = parent.buffers;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		this.buffers.add(toks.remainder());
		return new FreeTextParser(this);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (this.handler != null)
			this.handler.accept(String.join(" ", buffers));
	}

}
