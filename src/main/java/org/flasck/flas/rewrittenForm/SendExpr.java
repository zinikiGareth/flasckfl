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
	public final Object handler;

	public SendExpr(InputPosition loc, Object sender, StringLiteral method, List<Object> args, Object handler) {
		this.loc = loc;
		this.sender = sender;
		this.method = method;
		this.args = args;
		if (handler instanceof RWTypedPattern)
			throw new RuntimeException("Not a good idea");
		this.handler = handler;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public String toString() {
		return "(#send " + sender + "." + method + args + "=>" + handler + ")";
	}
}
