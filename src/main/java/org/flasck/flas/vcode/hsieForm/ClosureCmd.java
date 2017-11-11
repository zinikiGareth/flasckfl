package org.flasck.flas.vcode.hsieForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.types.Type;
import org.zinutils.bytecode.IExpr;

public class ClosureCmd extends HSIEBlock implements ClosureGenerator {
	public final Var var;
	public boolean typecheckMessages;
	public boolean checkSend;
	public Type assertType;
	public final List<VarInSource> depends = new ArrayList<VarInSource>();

	public ClosureCmd(InputPosition loc, Var var) {
		super(loc);
		this.var = var;
	}

	@Override
	public boolean justScoping() {
		return false;
	}
	
	@Override
	public Object arguments(ExprHandler h, int from) {
		// Process all the arguments
		h.beginClosure();
		for (int i=from;i<nestedCommands().size();i++) {
			PushReturn c = (PushReturn) nestedCommands().get(i);
			h.visit(c);
		}
		return (IExpr) h.endClosure();
	}

	@Override
	public List<VarInSource> dependencies() {
		return depends;
	}

	@Override
	public String toString() {
		return "CLOSURE " + var + leftpad((justScoping()?"!":"") + (typecheckMessages?"M":"") + (checkSend?"S":"")) + (downcastType != null?" >" + downcastType:"") + (assertType != null? " #" + assertType:"");
	}

	private String leftpad(String string) {
		if (string.isEmpty())
			return string;
		return " " + string;
	}
}
