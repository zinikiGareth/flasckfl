package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class JSHSIGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	private static class SwitchLevel {
		private JSExpr currentVar;
		private JSBlockCreator matchDefault;
		private JSBlockCreator elseBlock;
	}
	
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private JSBlockCreator block;
	private SwitchLevel currentLevel;
	private final Map<Slot, JSExpr> switchVars;
	private final List<SwitchLevel> switchStack = new ArrayList<>();

	public JSHSIGenerator(JSFunctionState state, NestedVisitor sv, Map<Slot, JSExpr> switchVars, Slot slot, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		currentLevel = new SwitchLevel();
		this.switchVars = switchVars;
		currentLevel.currentVar = switchVars.get(slot);
		this.block.head(currentLevel.currentVar);
		if (state != null && state.ocret() != null) {
			this.block.splitRWM(state.ocmsgs(), currentLevel.currentVar);
		}
		switchStack.add(0, currentLevel);
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		throw new NotImplementedException();
	}

	@Override
	public void switchOn(Slot slot) {
		sv.push(new JSHSIGenerator(state, sv, switchVars, slot, this.block));
	}

	@Override
	public void withConstructor(NameOfThing ctor) {
		if (currentLevel.elseBlock != null) {
//			if (!stack.isEmpty())
//				this.block.returnObject(stack.remove(0));
			this.block = currentLevel.elseBlock;
		}
		JSIfCreator ifCtor = this.block.ifCtor(currentLevel.currentVar, ctor);
		this.block = ifCtor.trueCase();
		this.currentLevel.elseBlock = ifCtor.falseCase();
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		String var = "_" + switchVars.size();
		JSVar jv = new JSVar(var);
		this.block.field(jv, switchVars.get(parent), field);
		switchVars.put(slot, jv);
	}

	// TODO: would this be better as a switch?
	@Override
	public void matchNumber(int val) {
		JSBlockCreator in = this.currentLevel.matchDefault;
		if (in == null)
			in = this.block;
		JSIfCreator ifCtor = in.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchString(String val) {
		JSBlockCreator in = this.currentLevel.matchDefault;
		if (in == null)
			in = this.block;
		JSIfCreator ifCtor = in.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchDefault() {
		if (this.currentLevel.matchDefault != null) {
			this.block = this.currentLevel.matchDefault;
		}
	}

	@Override
	public void defaultCase() {
		this.block = this.currentLevel.elseBlock;
	}

	@Override
	public void errorNoCase() {
		this.block.errorNoCase();
	}

	@Override
	public void bind(Slot slot, String var) {
		if (block == null) {
			System.out.println("block should not be null");
			return;
		}
		this.block.bindVar(slot, switchVars.get(slot), var);
	}

	@Override
	public void endSwitch() {
		switchStack.remove(0);
		if (!switchStack.isEmpty())
			currentLevel = switchStack.get(0);
		sv.result(null);
	}

	@Override
	public void startInline(FunctionIntro fi) {
		if (state != null && state.ocret() != null)
			new ObjectCtorGeneratorJS(state, sv, this.block);
		else
			sv.push(new GuardGeneratorJS(state, sv, this.block));
	}

	@Override
	public void result(Object r) {
		if (r != null)
			block.returnObject((JSExpr)r);
	}

}
