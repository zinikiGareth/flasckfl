package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var.AVar;

public class HSIGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	public class ConstBlock {
		private final Integer val;
		private final String str;
		private final JVMBlockCreator block;
		
		public ConstBlock(int val, JVMBlockCreator curr) {
			this.val = val;
			this.str = null;
			this.block = new JVMBlock(curr);
		}

		public ConstBlock(String val, JVMBlockCreator curr) {
			this.val = null;
			this.str = val;
			this.block = new JVMBlock(curr);
		}
	}

	public class SwitchCase {
		private final String ctor;
		private final JVMBlockCreator block;
		private final List<ConstBlock> constants = new ArrayList<>();

		public SwitchCase(String ctor, JVMBlockCreator curr) {
			this.ctor = ctor;
			this.block = new JVMBlock(curr);
		}
	}

	private final FunctionState state;
	private final StackVisitor sv;
	private final MethodDefiner meth;
	private final SwitchVars switchVars;
	private final AVar myVar;
	private final List<SwitchCase> cases = new ArrayList<>();
	private JVMBlockCreator currentBlock;

	public HSIGenerator(FunctionState state, StackVisitor sv, SwitchVars switchVars, Slot slot, JVMBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = block;
		this.meth = state.meth;
		this.switchVars = switchVars;
		myVar = switchVars.get(block, slot);
	}

	@Override
	public void hsiArgs(List<Slot> slots) {
		throw new RuntimeException("This should never be called");
	}

	@Override
	public void switchOn(Slot slot) {
		// push a nested generator for this case
		sv.push(new HSIGenerator(state, sv, switchVars.copyMe(), slot, currentBlock));
	}

	@Override
	public void withConstructor(NameOfThing ctor) {
		SwitchCase current = new SwitchCase(ctor.uniqueName(), currentBlock);
		cases.add(0, current);
		currentBlock = current.block;
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		AVar var = switchVars.get(currentBlock, parent);
		switchVars.define(slot, meth.callInterface(J.OBJECT, state.fcx, "field", var, meth.stringConst(field)));
	}

	// TODO: what does a switch look like in JVM bytecodes?  Can I be bothered?
	@Override
	public void matchNumber(int val) {
		SwitchCase current = cases.get(0);
		ConstBlock blk = new ConstBlock(val, currentBlock);
		current.constants.add(blk);
		this.currentBlock = blk.block;
	}

	@Override
	public void matchString(String val) {
		SwitchCase current = cases.get(0);
		ConstBlock blk = new ConstBlock(val, currentBlock);
		current.constants.add(blk);
		this.currentBlock = blk.block;
	}

	@Override
	public void matchDefault() {
		currentBlock = cases.get(0).block;
	}

	@Override
	public void defaultCase() {
		SwitchCase current = new SwitchCase(null, currentBlock);
		cases.add(0, current);
		currentBlock = current.block;
	}

	@Override
	public void errorNoCase() {
		currentBlock.add(meth.returnObject(meth.callStatic(J.FLERROR, J.OBJECT, "eval", state.fcx, meth.arrayOf(J.OBJECT, meth.stringConst("no matching case")))));
	}

	@Override
	public void bind(Slot slot, String var) {
		if (slot.isContainer())
			return;
		state.bindVar(currentBlock, var, slot, switchVars.copyMe().get(currentBlock, slot));
	}

	@Override
	public void endSwitch() {
		IExpr ret = null;
		for (SwitchCase c : cases) {
			IExpr blk = c.block.convert();
			blk = matchConstants(c.constants, blk);
			if (c.ctor == null)
				ret = blk;
			else
				ret = meth.ifBoolean(meth.callInterface(J.BOOLEANP.getActual(), state.fcx, "isA", myVar, meth.stringConst(c.ctor)), blk, ret);
		}
		sv.result(ret);
	}

	private IExpr matchConstants(List<ConstBlock> constants, IExpr blk) {
		for (ConstBlock b : constants) {
			IExpr tmp = b.block.convert();
			if (b.val != null)
				blk = meth.ifBoolean(meth.callInterface(J.BOOLEANP.getActual(), state.fcx, "isConst", myVar, meth.intConst(b.val)), tmp, blk);
			else
				blk = meth.ifBoolean(meth.callInterface(J.BOOLEANP.getActual(), state.fcx, "isConst", myVar, meth.stringConst(b.str)), tmp, blk);
		}
		return blk;
	}

	@Override
	public void startInline(FunctionIntro fi) {
		if (state.ocret() != null)
			new ObjectCtorGenerator(state, sv, currentBlock);
		else
			new GuardGenerator(state, sv, currentBlock);
	}

	@Override
	public void result(Object r) {
		currentBlock.add((IExpr)r);
	}

}
