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

public class HSIGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public class ConstBlock {
		private final Integer val;
		private final String str;
		private final List<IExpr> block = new ArrayList<>();
		
		public ConstBlock(int val) {
			this.val = val;
			this.str = null;
		}

		public ConstBlock(String val) {
			this.val = null;
			this.str = val;
		}
	}

	public class SwitchCase {
		private final String ctor;
		private final List<IExpr> block = new ArrayList<>();
		private final List<ConstBlock> constants = new ArrayList<>();

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
	private List<IExpr> currentBlock;

	public HSIGenerator(FunctionState state, StackVisitor sv, Map<Slot, IExpr> switchVars, Slot slot, List<IExpr> blk) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = blk;
		this.meth = state.meth;
		this.switchVars = new HashMap<>(switchVars);
		myVar = getSwitchVar(slot);
	}

	@Override
	public void hsiArgs(List<Slot> slots) {
		throw new RuntimeException("This should never be called");
	}

	@Override
	public void switchOn(Slot slot) {
		// push a nested generator for this case
		sv.push(new HSIGenerator(state, sv, switchVars, slot, currentBlock));
	}

	@Override
	public void withConstructor(String ctor) {
		SwitchCase current = new SwitchCase(ctor);
		cases.add(0, current);
		currentBlock = current.block;
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		AVar var = getSwitchVar(parent);
		switchVars.put(slot, meth.callStatic(J.FLEVAL, J.OBJECT, "field", state.fcx, var, meth.stringConst(field)));
	}

	// TODO: what does a switch look like in JVM bytecodes?  Can I be bothered?
	@Override
	public void matchNumber(int val) {
		SwitchCase current = cases.get(0);
		ConstBlock blk = new ConstBlock(val);
		current.constants.add(blk);
		this.currentBlock = blk.block;
	}

	@Override
	public void matchString(String val) {
		SwitchCase current = cases.get(0);
		ConstBlock blk = new ConstBlock(val);
		current.constants.add(blk);
		this.currentBlock = blk.block;
	}

	@Override
	public void matchDefault() {
		currentBlock = cases.get(0).block;
	}

	@Override
	public void defaultCase() {
		SwitchCase current = new SwitchCase(null);
		cases.add(0, current);
		currentBlock = current.block;
	}

	@Override
	public void errorNoCase() {
		currentBlock.add(meth.returnObject(meth.callStatic(J.ERROR, J.OBJECT, "eval", state.fcx, meth.arrayOf(J.OBJECT, meth.stringConst("no such case")))));
	}

	@Override
	public void bind(Slot slot, String var) {
		state.bindVar(currentBlock, var, slot);
	}

	@Override
	public void endSwitch() {
		IExpr ret = null;
		for (SwitchCase c : cases) {
			IExpr blk = JVMGenerator.makeBlock(meth, c.block);
			blk = matchConstants(c.constants, blk);
			if (c.ctor == null)
				ret = blk;
			else
				ret = meth.ifBoolean(meth.callStatic(J.FLEVAL, JavaType.boolean_, "isA", state.fcx, myVar, meth.stringConst(c.ctor)), blk, ret);
		}
		sv.result(ret);
	}

	private IExpr matchConstants(List<ConstBlock> constants, IExpr blk) {
		for (ConstBlock b : constants) {
			IExpr tmp = JVMGenerator.makeBlock(meth, b.block);
			if (b.val != null)
				blk = meth.ifBoolean(meth.callStatic(J.FLEVAL, JavaType.boolean_, "isConst", state.fcx, myVar, meth.intConst(b.val)), tmp, blk);
			else
				blk = meth.ifBoolean(meth.callStatic(J.FLEVAL, JavaType.boolean_, "isConst", state.fcx, myVar, meth.stringConst(b.str)), tmp, blk);
		}
		return blk;
	}

	@Override
	public void startInline(FunctionIntro fi) {
		sv.push(new ExprGenerator(state, sv, currentBlock));
	}

	@Override
	public void result(Object r) {
		currentBlock.add((IExpr)r);
	}

	private AVar getSwitchVar(Slot slot) {
		IExpr e = switchVars.get(slot);
		if (e == null)
			throw new NullPointerException("No expr for slot " + slot);
		if (!(e instanceof AVar)) {
			AVar var = new Var.AVar(meth, J.OBJECT, state.nextVar("s"));
			IExpr assign = meth.assign(var, meth.callStatic(J.FLEVAL, J.OBJECT, "head", state.fcx, e));
			currentBlock.add(assign);
			e = var;
			switchVars.put(slot, e);
		}
		AVar sv = (AVar) e;
		return sv;
	}

}
