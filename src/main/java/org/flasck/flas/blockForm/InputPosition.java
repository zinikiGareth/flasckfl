package org.flasck.flas.blockForm;

public class InputPosition {
	private final int lineNo;
	private final int off;
	public final String text;

	public InputPosition(int lineNo, int off, String text) {
		this.lineNo = lineNo;
		this.off = off;
		this.text = text;
	}

	@Override
	public String toString() {
		return lineNo +":" + off;
	}
}
