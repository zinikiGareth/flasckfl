package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.zinutils.exceptions.NotImplementedException;

public class ContainerChecker extends LeafAdapter implements TreeOrderVisitor {

	private final NestedVisitor sv;
	private final NamedType ty;
	private Object location;

	public ContainerChecker(NestedVisitor sv, NamedType ty) {
		this.sv = sv;
		this.ty = ty;
		sv.push(this);
	}

	@Override
	public void argSlot(ArgSlot s) {
		throw new NotImplementedException();
	}

	@Override
	public void matchConstructor(StructDefn ctor) {
		throw new NotImplementedException();
	}

	@Override
	public void matchField(StructField fld) {
		throw new NotImplementedException();
	}

	@Override
	public void matchType(Type ty, VarName var, FunctionIntro intro) {
		throw new NotImplementedException();
	}

	@Override
	public void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro) {
	}

	@Override
	public void endField(StructField fld) {
		throw new NotImplementedException();
	}

	@Override
	public void endConstructor(StructDefn ctor) {
		throw new NotImplementedException();
	}

	@Override
	public void endArg(Slot s) {
		sv.result(new FunctionChecker.ArgResult(ty));
	}
}
