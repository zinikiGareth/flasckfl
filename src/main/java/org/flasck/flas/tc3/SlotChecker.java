package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.zinutils.exceptions.NotImplementedException;

public class SlotChecker extends LeafAdapter implements TreeOrderVisitor {
	private final NestedVisitor sv;
	private final CurrentTCState state;
	private final UnifiableType ty;
	private StructTypeConstraints currentStruct;

	public SlotChecker(NestedVisitor sv, CurrentTCState state, UnifiableType ty) {
		this.ty = ty;
		this.sv = sv;
		this.state = state;
	}
	
	@Override
	public void argSlot(Slot s) {
		throw new NotImplementedException("This shouldn't happen here");
	}

	@Override
	public void matchConstructor(StructDefn ctor) {
		currentStruct = ty.canBeStruct(ctor);
	}

	@Override
	public void matchField(StructField fld) {
		UnifiableType ft = currentStruct.field(state, null, fld);
		sv.push(new SlotChecker(sv, state, ft));
	}

	@Override
	public void matchType(Type ofType, VarName var, FunctionIntro intro) {
		ty.canBeType(ofType);
		// TODO: should we do something with the varname and intro?
		// I think we're supposed to bind them in a map somewhere ... write the appropriate tests ...
		// I think that goes in "state"
	}

	@Override
	public void varInIntro(VarName vn, FunctionIntro intro) {
		state.bindVarToUT(vn.uniqueName(), ty);
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
}
