package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.PushDouble;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class NumericLiteral implements Expr, Pushable {
	public final InputPosition location;
	public final String text;
	private final Integer val;

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
	
	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		if (val != null)
			return new PushInt(location, val);
		else if (text.indexOf(".") != -1 || text.indexOf("e") != -1)
			return new PushDouble(location, Double.parseDouble(text));
		else
			return new PushInt(location, Integer.parseInt(text));
	}

	@Override
	public String toString() {
		return text;
	}
}
