package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class TestDescriptionToken implements LoggableToken {
	private final InputPosition pos;
	private final String desc;

	public TestDescriptionToken(InputPosition pos, String desc) {
		this.pos = pos;
		this.desc = desc;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public String type() {
		return "TESTDESCRIPTION";
	}

	@Override
	public String text() {
		return desc;
	}

}
