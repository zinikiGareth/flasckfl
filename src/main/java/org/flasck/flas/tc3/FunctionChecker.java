package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TypedPattern;
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
	private final ObjectActionHandler inMeth;
	private final ContractSlotChecker csc;

	public FunctionChecker(ErrorReporter errors, NestedVisitor sv, CurrentTCState state, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.sv = sv;
		this.state = state;
		this.inMeth = inMeth;
		if (inMeth != null && inMeth.contractMethod() != null) {
			csc = new ContractSlotChecker(errors, sv, state, inMeth);
		} else
			csc = null;
		sv.push(this);
	}
	
	@Override
	public void visitHandlerLambda(HandlerLambda hl) {
		Pattern patt = hl.patt;
		if (patt instanceof VarPattern) {
			VarPattern vp = (VarPattern) patt;
			UnifiableType lt = state.createUT(null, "hl " + vp.var);
			state.bindVarToUT(vp.name().uniqueName(), lt);
			state.bindVarPatternToUT(vp, lt);
		} else if (patt instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) patt;
			UnifiableType lt = state.createUT(null, "hl " + tp.var);
			lt.canBeType(tp.var.loc, tp.type.defn());
			state.bindVarToUT(tp.name().uniqueName(), lt);
		} else
			throw new NotImplementedException("not supported as lambda: " + patt.getClass());
	}
	
	@Override
	public void argSlot(Slot s) {
		if (inMeth != null && inMeth.contractMethod() != null) {
			// handle contract methods where the types are already prescribed
			sv.push(csc);
		} else {
			UnifiableType currentArg = state.createUT(null, "slot " + s);
			sv.push(new SlotChecker(sv, state, currentArg));
		}
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
			sv.result(new PosType(meth.location(), LoadBuiltins.nil));
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + meth.name().uniqueName());
		else {
			PosType posty = state.consolidate(meth.location(), resultTypes);
			sv.result(buildApplyType(meth.location(), new PosType(posty.pos, new EnsureListMessage(meth.location(), posty.type))));
		}
	}

	@Override
	public void leaveObjectCtor(ObjectCtor ctor) {
		if (ctor.messages().isEmpty())
			sv.result(buildApplyType(ctor.location(), new PosType(ctor.location(), ctor.getObject())));
		else if (resultTypes.isEmpty())
			throw new RuntimeException("No types inferred for " + ctor.name().uniqueName());
		else {
			PosType posty = state.consolidate(ctor.location(), resultTypes);
			sv.result(buildApplyType(ctor.location(), new PosType(posty.pos, new EnsureListMessage(ctor.location(), posty.type))));
		}
	}

	public void leaveObjectActionHandler(ObjectActionHandler meth) {
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
