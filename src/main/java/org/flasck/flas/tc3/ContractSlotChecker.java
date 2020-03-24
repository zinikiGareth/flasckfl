package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
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
	private final ErrorReporter errors;
	private final NestedVisitor sv;
//	private final CurrentTCState state;
//	private final ObjectMethod inMeth;
	private final ContractMethodDecl cmd;
	private int pos;
	private Type ty;

	public ContractSlotChecker(ErrorReporter errors, NestedVisitor sv, CurrentTCState state, ObjectMethod inMeth) {
		this.errors = errors;
		this.sv = sv;
//		this.state = state;
//		this.inMeth = inMeth;
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
	public void matchType(Type want, VarName var, FunctionIntro intro) {
		NamedType defined = ((TypedPattern)cmd.args.get(pos++)).type.defn();
		if (defined != want) {
			errors.message(var.loc, "cannot bind " + var.var + " to " + ((NamedType)want).name() + " when the contract specifies " + defined.name());
			this.ty = new ErrorType();
		} else
			this.ty = want;
	}

	@Override
	public void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro) {
		if (pos > cmd.args.size())
			throw new NotImplementedException("Argument is out of range: " + pos + " " + cmd.args.size());
		if (pos == cmd.args.size()) {
			ty = cmd.handler.type.defn();
			pos++;
		} else {
			ty = ((TypedPattern)cmd.args.get(pos++)).type.defn();
		}
		vp.bindType(ty);
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
		sv.result(new ArgResult(ty));
		this.ty = null;
	}
}
