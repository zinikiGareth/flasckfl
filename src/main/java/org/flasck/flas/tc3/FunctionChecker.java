package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ExpressionChecker.GuardResult;
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
	public void varInIntro(VarName vn, VarPattern vp, FunctionIntro intro) {
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
	public void visitCase(FunctionCaseDefn fcd) {
		sv.push(new ExpressionChecker(errors, repository, state, sv));
	}
	
	@Override
	public void visitSendMessage(SendMessage sm) {
		sv.push(new ExpressionChecker(errors, repository, state, sv));
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof ArgResult)
			argTypes.add(((ArgResult)r).type);
		else if (r instanceof GuardResult) {
			GuardResult gr = (GuardResult)r;
			Type ret = gr.type;
			if (!ret.equals(LoadBuiltins.bool) && !ret.equals(LoadBuiltins.trueT) && !ret.equals(LoadBuiltins.falseT))
				errors.message(gr.location(), "guards must be booleans");
			
			// There will be an expression as well, so push another checker ...
			sv.push(new ExpressionChecker(errors, repository, state, sv));
		} else {
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
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + fn.name().uniqueName());
		else
			sv.result(buildApplyType(fn.location()));
	}
	
	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		if (meth.messages().isEmpty())
			sv.result(null);
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + meth.name().uniqueName());
		else {
			sv.result(buildApplyType(meth.location()));
		}
	}

	private Type buildApplyType(InputPosition pos) {
		Type result = consolidate(pos, resultTypes);
		if (argTypes.isEmpty())
			return result;
		else {
			return new Apply(argTypes, result);
		}
	}

	public static Type consolidate(InputPosition pos, List<Type> types) {
		if (types.size() == 1)
			return types.get(0);
		
		return new ConsolidateTypes(pos, types);
	}
}
