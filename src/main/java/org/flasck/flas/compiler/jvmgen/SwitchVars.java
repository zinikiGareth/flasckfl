package org.flasck.flas.compiler.jvmgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;

public class SwitchVars {
	private final Map<Slot, IExpr> switchVars;
	private final MethodDefiner meth;
	private FunctionState state;

	public SwitchVars(FunctionState state, List<Slot> slots) {
		this.state = state;
		this.meth = state.meth;
		this.switchVars = new HashMap<>();
		int ignoreContainer = 0;
		for (Slot slot : slots) {
			ArgSlot s = (ArgSlot) slot;
			if (s.isContainer()) {
				ignoreContainer = 1;
				continue;
			}
			IExpr in = state.meth.arrayItem(J.OBJECT, state.fargs, s.argpos()-ignoreContainer);
			switchVars.put(s, in);
		}
	}

	public SwitchVars(FunctionState state, Map<Slot, IExpr> switchVars) {
		this.state = state;
		this.meth = state.meth;
		this.switchVars = new HashMap<>(switchVars);
	}
	
	public void define(Slot slot, IExpr expr) {
		switchVars.put(slot, expr);
	}

	public AVar get(JVMBlockCreator block, Slot slot) {
		IExpr e = switchVars.get(slot);
		if (e == null)
			throw new NullPointerException("No expr for slot " + slot);
		if (!(e instanceof AVar)) {
			AVar var = new Var.AVar(meth, J.OBJECT, state.nextVar("s"));
			IExpr assign = meth.assign(var, meth.callInterface(J.OBJECT, state.fcx, "head", e));
			block.add(assign);
			
			if (state.ocret() != null) {
				IExpr extract = meth.callInterface("void", state.fcx, "addAll", state.ocmsgs(), meth.as(meth.callStatic(J.RESPONSE_WITH_MESSAGES, List.class.getName(), "messages", state.fcx, var), J.OBJECT));
				IExpr reassign = meth.assign(var, meth.callStatic(J.RESPONSE_WITH_MESSAGES, Object.class.getName(), "response", state.fcx, var));
				block.add(meth.ifBoolean(meth.instanceOf(var, J.RESPONSE_WITH_MESSAGES), meth.block(extract, reassign), null));
			}
			
			e = var;
			switchVars.put(slot, e);
		}
		AVar sv = (AVar) e;
		return sv;
	}

	public SwitchVars copyMe() {
		return new SwitchVars(state, switchVars);
	}

}
