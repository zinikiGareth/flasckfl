package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class StringLiteral implements Expr, Pushable {
	public final String text;
	public final InputPosition location;

	public StringLiteral(InputPosition loc, String text) {
		this.location = loc;
		this.text = text;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushString(location, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StringLiteral))
			return false;
		return text.equals(((StringLiteral)obj).text);
	}
	
	@Override
	public String toString() {
		return '"' + text + '"';
	}
}