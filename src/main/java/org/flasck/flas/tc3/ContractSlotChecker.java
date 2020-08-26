package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.ObjectActionHandler;
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
	private final CurrentTCState state;
	private final ContractMethodDecl cmd;
	private final List<Pattern> nvs;
	private int pos;
	private Type ty;

	public ContractSlotChecker(ErrorReporter errors, NestedVisitor sv, CurrentTCState state, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.sv = sv;
		this.state = state;
		this.cmd = inMeth.contractMethod();
		if (inMeth.nestedVars() != null)
			this.nvs = inMeth.nestedVars().patterns();
		else
			this.nvs = new ArrayList<>();
		this.pos = 0;
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
		if (pos < nvs.size()) {
			Pattern p = nvs.get(pos);
			if (p instanceof TypedPattern)
				ty = ((TypedPattern)p).type();
			else if (p instanceof VarPattern) {
				ty = state.createUT(p.location(), cmd.name.uniqueName() + " nv " + pos);
			} else
				throw new NotImplementedException("varInIntro for nv " + pos + ": " + p.getClass());
		} else {
			int p = pos - nvs.size();
			if (p > cmd.args.size())
				throw new NotImplementedException("Argument is out of range: " + p + " " + cmd.args.size());
			if (p == cmd.args.size() && cmd.handler == null)
				throw new NotImplementedException("There is no handler for " + cmd.name.uniqueName());
			if (p == cmd.args.size()) {
				ty = cmd.handler.type.defn();
			} else {
				ty = ((TypedPattern)cmd.args.get(p)).type.defn();
			}
			pos++;
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

	@Override
	public void patternsDone(LogicHolder fn) {
		throw new NotImplementedException();
	}
}
