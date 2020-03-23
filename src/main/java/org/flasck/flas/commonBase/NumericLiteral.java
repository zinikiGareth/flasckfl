package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class NumericLiteral implements Expr {
	public final InputPosition location;
	public final String text;
	public final Integer val;

	public NumericLiteral(InputPosition loc, String text, int end) {
		this.location = loc;
		this.location.endAt(end);
		this.text = text;
		this.val = null;
	}
	
	public NumericLiteral(InputPosition location, int val) {
		this.location = location;
		this.text = null;
		this.val = val;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public Object value() {
		if (val != null)
			return val;
		else if (text.indexOf(".") != -1 || text.indexOf("e") != -1)
			return Double.parseDouble(text);
		else
			return Integer.parseInt(text);
	}

	@Override
	public String toString() {
		if (text != null)
			return text;
		else
			return Integer.toString(val);
	}
}
