package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class JSHSIGenerator extends LeafAdapter implements HSIVisitor, ResultAware {
	private static class SwitchLevel {
		private String currentVar;
		private JSBlockCreator matchDefault;
		private JSBlockCreator elseBlock;
	}
	
	private final NestedVisitor sv;
	private JSBlockCreator block;
	private SwitchLevel currentLevel;
	private final Map<Slot, String> switchVars;
	private final List<SwitchLevel> switchStack = new ArrayList<>();

	public JSHSIGenerator(NestedVisitor sv, Map<Slot, String> switchVars, Slot slot, JSBlockCreator block) {
		this.sv = sv;
		this.block = block;
		currentLevel = new SwitchLevel();
		this.switchVars = switchVars;
		currentLevel.currentVar = switchVars.get(slot);
		this.block.head(currentLevel.currentVar);
		switchStack.add(0, currentLevel);
	}
	
	@Override
	public void hsiArgs(List<Slot> slots) {
		throw new NotImplementedException();
	}

	@Override
	public void switchOn(Slot slot) {
		sv.push(new JSHSIGenerator(sv, switchVars, slot, this.block));
	}

	@Override
	public void withConstructor(String ctor) {
		if (currentLevel.elseBlock != null) {
//			if (!stack.isEmpty())
//				this.block.returnObject(stack.remove(0));
			this.block = currentLevel.elseBlock;
		}
		JSIfExpr ifCtor = this.block.ifCtor(currentLevel.currentVar, ctor);
		this.block = ifCtor.trueCase();
		this.currentLevel.elseBlock = ifCtor.falseCase();
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
		String var = "_" + switchVars.size();
		this.block.field(var, switchVars.get(parent), field);
		switchVars.put(slot, var);
	}

	// TODO: would this be better as a switch?
	@Override
	public void matchNumber(int val) {
		JSIfExpr ifCtor = this.block.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchString(String val) {
		JSIfExpr ifCtor = this.block.ifConst(currentLevel.currentVar, val);
		this.block = ifCtor.trueCase();
		this.currentLevel.matchDefault = ifCtor.falseCase();
	}

	@Override
	public void matchDefault() {
		if (this.currentLevel.matchDefault != null) {
//			if (!stack.isEmpty())
//				this.block.returnObject(this.stack.remove(0));
			this.block = this.currentLevel.matchDefault;
		}
	}

	@Override
	public void defaultCase() {
//		if (!stack.isEmpty())
//			this.block.returnObject(this.stack.remove(0));
		this.block = this.currentLevel.elseBlock;
	}

	@Override
	public void errorNoCase() {
		this.block.errorNoCase();
	}

	@Override
	public void bind(Slot slot, String var) {
		this.block.bindVar("_" + slot.id(), var);
	}

	@Override
	public void endSwitch() {
//		if (!stack.isEmpty())
//			this.block.returnObject(this.stack.remove(0));
		switchStack.remove(0);
		if (!switchStack.isEmpty())
			currentLevel = switchStack.get(0);
		sv.result(null);
	}

	@Override
	public void startInline(FunctionIntro fi) {
		sv.push(new GuardGeneratorJS(sv, this.block));
	}

	@Override
	public void result(Object r) {
		if (r != null)
			block.returnObject((JSExpr)r);
	}

}