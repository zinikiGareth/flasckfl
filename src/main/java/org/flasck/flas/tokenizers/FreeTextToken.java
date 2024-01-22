package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class FreeTextToken implements LoggableToken {
	private final InputPosition pos;
	private final String tok;

	public FreeTextToken(InputPosition pos, String tok) {
		this.pos = pos;
		this.tok = tok;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public String type() {
		return "FreeText";
	}

	@Override
	public String text() {
		return tok;
	}

}