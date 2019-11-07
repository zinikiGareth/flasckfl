package org.flasck.flas.compiler.jvmgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionState {
	final MethodDefiner meth;
	final IExpr fcx;
	final Var fargs;
	private int nextVar = 1;
	private Map<String, AVar> vars = new HashMap<>();

	public FunctionState(MethodDefiner meth, IExpr fcx, Var fargs) {
		this.meth = meth;
		this.fcx = fcx;
		this.fargs = fargs;
	}

	public String nextVar(String pfx) {
		return pfx + nextVar++;
	}

	public void bindVar(List<IExpr> block, String var, Slot s, IExpr from) {
		IExpr in;
		AVar avar;
		if (s instanceof ArgSlot) {
			int k = ((ArgSlot)s).argpos();
			in = meth.arrayItem(J.OBJECT, fargs, k);
			avar = new Var.AVar(meth, J.OBJECT, "head_" + k);
		} else if (s instanceof CMSlot) {
			in = from;
			avar = new Var.AVar(meth, J.OBJECT, "var_" + s.id());
		} else
			throw new NotImplementedException();
		block.add(meth.assign(avar, meth.callStatic(J.FLEVAL, J.OBJECT, "head", fcx, in)));
		vars.put(var, avar);
	}

	public AVar boundVar(String var) {
		return vars.get(var);
	}
}
