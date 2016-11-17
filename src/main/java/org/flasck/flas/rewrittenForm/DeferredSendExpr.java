package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.rewriter.Rewriter;

public class DeferredSendExpr {
	private InputPosition loc;
	public final Object sender;
	public final List<Object> args;
	public final PackageVar send;
	public final StringLiteral method;
	public final Rewriter rw;

	public DeferredSendExpr(InputPosition loc, Object sender, PackageVar send, StringLiteral method, Rewriter rw, List<Object> args) {
		this.loc = loc;
		this.sender = sender;
		this.send = send;
		this.method = method;
		this.rw = rw;
		this.args = args;
	}

	public InputPosition location() {
		return loc;
	}

}
