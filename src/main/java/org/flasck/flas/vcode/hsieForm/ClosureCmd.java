package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.types.Type;

public class ClosureCmd extends HSIEBlock {
	public final Var var;
	public boolean typecheckMessages;
	public Type assertType;
	public boolean justScoping = false;
	public final List<VarInSource> depends = new ArrayList<VarInSource>();

	public ClosureCmd(InputPosition loc, Var var) {
		super(loc);
		this.var = var;
	}

	@Override
	public String toString() {
		return "CLOSURE " + var + (downcastType != null?" " + downcastType:"");
	}
}
