package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class MakeAcor implements Expr, WithTypeSignature {
	private final InputPosition pos;
	public final FunctionName acorMeth;
	public final Expr obj;
	public final int nargs;

	public MakeAcor(InputPosition pos, FunctionName acorMeth, Expr obj, int nargs) {
		this.pos = pos;
		this.acorMeth = acorMeth;
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
		return "MakeAcor[" + acorMeth.uniqueName() + "(" + obj + ", " + nargs + ")]";
	}
}
