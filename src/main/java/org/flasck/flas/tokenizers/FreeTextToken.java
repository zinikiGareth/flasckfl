package org.flasck.flas.tokenizers;

import java.util.List;

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
	
	public static FreeTextToken merge(List<FreeTextToken> buffers) {
		FreeTextToken first = buffers.get(0);
		StringBuilder sb = new StringBuilder();
		for (FreeTextToken f : buffers) {
			if (!sb.isEmpty())
				sb.append(" ");
			sb.append(f.text());
		}
		return new FreeTextToken(first.location(), sb.toString());
	}

	@Override
	public String text() {
		return tok;
	}

}
