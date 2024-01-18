package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class CommentToken implements LoggableToken {
	private final InputPosition location;
	private final String text;

	public CommentToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "comment";
	}

	@Override
	public String text() {
		return text;
	}

}
