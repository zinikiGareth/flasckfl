package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
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
	public void matchType(Type ty, VarName var, FunctionIntro intro) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void varInIntro(VarPattern vp, FunctionIntro intro) {
		// TODO Auto-generated method stub
		
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
		sv.result(null);
	}
}
