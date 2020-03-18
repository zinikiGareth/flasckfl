package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.zinutils.exceptions.NotImplementedException;

public class ContractSlotChecker extends LeafAdapter implements TreeOrderVisitor  {
	private final NestedVisitor sv;
	private final ObjectMethod inMeth;
	private final ContractMethodDecl cmd;
	private int pos;

	public ContractSlotChecker(NestedVisitor sv, ObjectMethod inMeth) {
		this.sv = sv;
		this.inMeth = inMeth;
		this.cmd = inMeth.contractMethod();
		this.pos = 0;
	}

	@Override
	public void argSlot(Slot s) {
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
		throw new NotImplementedException();
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
		sv.result(new ArgResult(((TypedPattern)cmd.args.get(pos++)).type.defn())); 
	}
}
