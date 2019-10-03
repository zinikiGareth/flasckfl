package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.NotImplementedException;

public class HSIGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public class SwitchCase {
		private final String ctor;
		private final List<IExpr> block = new ArrayList<>();

		public SwitchCase(String ctor) {
			this.ctor = ctor;
		}
	}

	private final FunctionState state;
	private final StackVisitor sv;
	private final MethodDefiner meth;
	private final Map<Slot, IExpr> switchVars;
	private final AVar myVar;
	private final List<SwitchCase> cases = new ArrayList<>();
	private SwitchCase current = null;

	public HSIGenerator(FunctionState state, StackVisitor sv, Map<Slot, IExpr> switchVars, Slot slot) {
		this.state = state;
		this.sv = sv;
		this.meth = state.meth;
		this.switchVars = new HashMap<>(switchVars);
		myVar = getSwitchVar(slot);
	}

	@Override
	public boolean isHsi() {
		return true;
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		throw new RuntimeException("This should never be called");
	}

	@Override
	public void switchOn(Slot slot) {
		// push a nested generator for this case
		sv.push(new HSIGenerator(state, sv, switchVars, slot));
	}

	@Override
	public void withConstructor(String ctor) {
		current = new SwitchCase(ctor);
		cases.add(0, current);
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		AVar var = getSwitchVar(parent);
		switchVars.put(slot, meth.callStatic(J.FLEVAL, J.OBJECT, "field", state.fcx, var, meth.stringConst(field)));
	}

	@Override
	public void defaultCase() {
		current = new SwitchCase(null);
		cases.add(0, current);
	}

	@Override
	public void errorNoCase() {
		current = new SwitchCase(null);
		cases.add(0, current);
		current.block.add(meth.returnObject(meth.callStatic(J.ERROR, J.OBJECT, "eval", state.fcx, meth.arrayOf(J.OBJECT, meth.stringConst("no such case")))));
	}

	@Override
	public void bind(Slot slot, String var) {
	}

	@Override
	public void endSwitch() {
		IExpr ret = null;
		for (SwitchCase c : cases) {
			IExpr blk;
			if (c.block.isEmpty())
				throw new NotImplementedException("there must be at least one statement in a block");
			else if (c.block.size() == 1)
				blk = c.block.get(0);
			else
				blk = meth.block(c.block.toArray(new IExpr[c.block.size()]));
			if (c.ctor == null)
				ret = blk;
			else
				ret = meth.ifBoolean(meth.callStatic(J.FLEVAL, JavaType.boolean_, "isA", state.fcx, myVar, meth.stringConst(c.ctor)), blk, ret);
		}
		sv.result(ret);
	}
	
	@Override
	public void startInline(FunctionIntro fi) {
		sv.push(new ExprGenerator(state, sv));
	}

	@Override
	public void result(Object r) {
		current.block.add((IExpr)r);
	}

	private AVar getSwitchVar(Slot slot) {
		IExpr e = switchVars.get(slot);
		if (e == null)
			throw new NullPointerException("No expr for slot " + slot);
		if (!(e instanceof AVar)) {
			AVar var = new Var.AVar(meth, J.OBJECT, state.nextVar("s"));
			meth.assign(var, meth.callStatic(J.FLEVAL, J.OBJECT, "head", state.fcx, e));
			e = var;
			switchVars.put(slot, e);
		}
		AVar sv = (AVar) e;
		return sv;
	}

}
