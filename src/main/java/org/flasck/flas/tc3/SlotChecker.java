package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.zinutils.exceptions.NotImplementedException;

public class SlotChecker extends LeafAdapter implements TreeOrderVisitor {
	private final NestedVisitor sv;
	private final FunctionName fnName;
	private final CurrentTCState state;
	private final UnifiableType ty;
	private StructTypeConstraints currentStruct;

	public SlotChecker(NestedVisitor sv, FunctionName fnName, CurrentTCState state, UnifiableType ty) {
		this.fnName = fnName;
		this.ty = ty;
		this.sv = sv;
		this.state = state;
	}
	
	@Override
	public void argSlot(ArgSlot s) {
		throw new NotImplementedException("This shouldn't happen here");
	}

	@Override
	public void matchConstructor(StructDefn ctor) {
		currentStruct = ty.canBeStruct(null, fnName, ctor);
	}

	@Override
	public void matchField(StructField fld) {
		UnifiableType ft = currentStruct.field(state, null, fld);
		sv.push(new SlotChecker(sv, fnName, state, ft));
	}

	@Override
	public void matchType(Type ofType, VarName var, FunctionIntro intro) {
		ty.canBeType(var == null ? null : var.loc, ofType);
		if (var != null)
			state.bindVarToUT(var.uniqueName(), ty);
	}

	@Override
	public void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro) {
		state.bindVarToUT(vn.uniqueName(), ty);
		if (vp != null)
			state.bindVarPatternToUT(vp, ty);
	}

	@Override
	public void endField(StructField fld) {
		sv.result(null);
	}

	@Override
	public void endConstructor(StructDefn ctor) {
	}

	@Override
	public void endArg(Slot s) {
		sv.result(new ArgResult(ty));
	}

	@Override
	public void patternsDone(LogicHolder fn) {
		throw new NotImplementedException();
	}
}
