package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class MakeSend implements Expr, WithTypeSignature {
	private final InputPosition pos;
	public final FunctionName sendMeth;
	public final Expr obj;
	public final int nargs;
	public Expr handler;
	public Expr handlerName;

	public MakeSend(InputPosition pos, FunctionName sendMeth, Expr obj, int nargs) {
		this.pos = pos;
		this.sendMeth = sendMeth;
		this.obj = obj;
		this.nargs = nargs;
	}

	@Override
	public InputPosition location() {
		return pos;
	}

	@Override
	public NameOfThing name() {
		throw new NotImplementedException();
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		return nargs;
	}

	@Override
	public Type type() {
		throw new NotImplementedException();
	}
	
	@Override
	public String toString() {
		return "MakeSend[" + sendMeth.uniqueName() + "(" + obj + ", " + nargs + ")" + (handlerName != null ? " => " + handlerName.toString() : "") + "]";
	}
}
