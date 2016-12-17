package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class SendExpr implements Expr {
	private InputPosition loc;
	public final Object sender;
	public final List<Object> args;
	public final StringLiteral method;

	public SendExpr(InputPosition loc, Object sender, StringLiteral method, List<Object> args) {
		this.loc = loc;
		this.sender = sender;
		this.method = method;
		this.args = args;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public String toString() {
		return "(#send " + sender + "." + method + args + ")";
	}
}
