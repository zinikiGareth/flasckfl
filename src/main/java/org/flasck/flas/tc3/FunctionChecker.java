package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class FunctionChecker extends LeafAdapter implements ResultAware, TreeOrderVisitor {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final List<Type> resultTypes = new ArrayList<>();
	private final CurrentTCState state;
	private UnifiableType currentArg;
	private StructTypeConstraints currentStruct;

	public FunctionChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
	}
	
	@Override
	public void argSlot(Slot s) {
		currentArg = state.nextArg();
	}

	@Override
	public void matchConstructor(StructDefn ctor) {
		currentStruct = currentArg.canBeStruct(ctor);
	}

	@Override
	public void matchField(StructField fld) {
		// TODO Auto-generated method stub
		
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
	public void visitFunctionIntro(FunctionIntro fi) {
		sv.push(new ExpressionChecker(errors, repository, state, sv));
	}
	
	@Override
	public void result(Object r) {
		resultTypes.add((Type) r);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			sv.result(null);
		if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + fn.name().uniqueName());
		System.out.println("TC fn " + fn.name().uniqueName() + " = " + consolidateType());
		sv.result(consolidateType());
	}

	private Type consolidateType() {
		// TODO: this actually needs to consolidate the types ...
		return resultTypes.get(0);
	}
}
