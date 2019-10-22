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
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionChecker extends LeafAdapter implements ResultAware, TreeOrderVisitor {
	public static class ArgResult {
		public final Type type;
		
		public ArgResult(Type t) {
			this.type = t;
		}
	}

	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final List<Type> argTypes = new ArrayList<>();
	private final List<Type> resultTypes = new ArrayList<>();
	private final CurrentTCState state;

	public FunctionChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
	}
	
	@Override
	public void argSlot(Slot s) {
		UnifiableType currentArg = state.createUT();
		sv.push(new SlotChecker(sv, state, currentArg));
	}

	@Override
	public void matchConstructor(StructDefn ctor) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void matchField(StructField fld) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void matchType(Type ty, VarName var, FunctionIntro intro) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void varInIntro(VarName vn, FunctionIntro intro) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void endField(StructField fld) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void endConstructor(StructDefn ctor) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void endArg(Slot s) {
		throw new NotImplementedException("This should not happen here .. just argslots");
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		sv.push(new ExpressionChecker(errors, repository, state, sv));
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof ArgResult)
			argTypes.add(((ArgResult)r).type);
		else {
			Type ret = ((ExprResult)r).type;
			if (ret instanceof UnifiableType)
				((UnifiableType)ret).isReturned();
			resultTypes.add(ret);
		}
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			sv.result(null);
		if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + fn.name().uniqueName());
		System.out.println("TC fn " + fn.name().uniqueName() + " = " + buildApplyType());
		sv.result(buildApplyType());
	}

	private Type buildApplyType() {
//		for (Type t : argTypes)
//			if (t instanceof UnifiableType)
//				((UnifiableType)t).resolve();
//		for (Type t : resultTypes)
//			if (t instanceof UnifiableType)
//				((UnifiableType)t).resolve();
		Type result = consolidate(resultTypes);
		if (argTypes.isEmpty())
			return result;
		else {
			return new Apply(argTypes, result);
		}
	}

	private Type consolidate(List<Type> types) {
		if (types.size() == 1)
			return types.get(0);
		
		return new ConsolidateTypes(types);
	}
}
