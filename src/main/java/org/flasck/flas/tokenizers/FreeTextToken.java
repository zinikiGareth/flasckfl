package org.flasck.flas.tokenizers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class FreeTextToken implements LoggableToken {
	private final InputPosition pos;
	private final String tok;
	public final List<FreeTextToken> original = new ArrayList<>();

	public FreeTextToken(InputPosition pos, String tok) {
		this.pos = pos;
		this.tok = tok;
		this.original.add(this);
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public String type() {
		return "FREETEXT";
	}
	
	public static FreeTextToken merge(List<FreeTextToken> buffers) {
		if (buffers.isEmpty())
			return null;

		FreeTextToken first = buffers.get(0);
		StringBuilder sb = new StringBuilder();
		for (FreeTextToken f : buffers) {
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(f.text());
		}
		FreeTextToken ret = new FreeTextToken(first.location(), sb.toString());
		ret.original.clear();
		ret.original.addAll(buffers);
		return ret;
	}

	@Override
	public String text() {
		return tok;
	}

}
