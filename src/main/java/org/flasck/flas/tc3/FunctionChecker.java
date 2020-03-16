package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ExpressionChecker.GuardResult;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionChecker extends LeafAdapter implements ResultAware, TreeOrderVisitor {
	public static class ArgResult extends PosType {
		// We just can't know a position for this, because it's a slot that can map to many
		// We need to capture them later
		public ArgResult(Type t) {
			super(null, t);
		}
	}

	private final ErrorReporter errors;
	private final NestedVisitor sv;
	private final List<PosType> argTypes = new ArrayList<>();
	private final List<PosType> resultTypes = new ArrayList<>();
	private final CurrentTCState state;
	private final ObjectMethod inMeth;

	public FunctionChecker(ErrorReporter errors, NestedVisitor sv, CurrentTCState state, ObjectMethod inMeth) {
		this.errors = errors;
		this.sv = sv;
		this.state = state;
		this.inMeth = inMeth;
	}
	
	@Override
	public void argSlot(Slot s) {
		UnifiableType currentArg = state.createUT(null, "slot " + s);
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
		sv.push(new ExpressionChecker(errors, state, sv));
	}
	
	@Override
	public void visitSendMessage(SendMessage sm) {
		new MessageChecker(errors,state, sv, sm.location(), inMeth);
	}
	
	@Override
	public void visitAssignMessage(AssignMessage assign) {
		new MessageChecker(errors,state, sv, assign.location(), inMeth);
	}
	
	@Override
	public void result(Object r) {
		if (!(r instanceof PosType))
			throw new NotImplementedException("should be some kind of PosType");
		if (r instanceof ArgResult)
			argTypes.add((ArgResult)r);
		else if (r instanceof GuardResult) {
			GuardResult gr = (GuardResult)r;
			Type ret = gr.type;
			if (!LoadBuiltins.bool.incorporates(gr.location(), ret))
				errors.message(gr.location(), "guards must be booleans");
			
			// There will be an expression as well, so push another checker ...
			sv.push(new ExpressionChecker(errors, state, sv));
		} else {
			ExprResult exprResult = (ExprResult)r;
			Type ret = exprResult.type;
			if (ret instanceof UnifiableType)
				((UnifiableType)ret).isReturned(exprResult.pos);
			resultTypes.add(exprResult);
		}
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			sv.result(null);
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + fn.name().uniqueName());
		else {
			PosType c = state.consolidate(fn.location(), resultTypes);
			sv.result(buildApplyType(c.pos, c));
		}
	}
	
	@Override
	public void leaveTuple(TupleAssignment e) {
		PosType c = state.consolidate(e.location(), resultTypes);
		sv.result(buildApplyType(c.pos, c));
	}
	
	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		if (meth.messages().isEmpty())
			sv.result(null);
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + meth.name().uniqueName());
		else {
			PosType posty = state.consolidate(meth.location(), resultTypes);
			sv.result(buildApplyType(meth.location(), new PosType(posty.pos, new EnsureListMessage(meth.location(), posty.type))));
		}
	}

	private PosType buildApplyType(InputPosition pos, PosType result) {
		if (argTypes.isEmpty())
			return result;
		else {
			List<Type> args = new ArrayList<>();
			for (PosType p : argTypes)
				args.add(p.type);
			return new PosType(pos, new Apply(args, result.type));
		}
	}
}
