package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ExpressionChecker.GuardResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionChecker extends LeafAdapter implements ResultAware, TreeOrderVisitor {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	public static class ArgResult extends PosType {
		// We just can't know a position for this, because it's a slot that can map to many
		// We need to capture them later
		public ArgResult(Type t) {
			super(null, t);
		}
	}

	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final List<PosType> argTypes = new ArrayList<>();
	private final List<PosType> resultTypes = new ArrayList<>();
	private final CurrentTCState state;
	private final ObjectActionHandler inMeth;
	private final ContractSlotChecker csc;
	private FunctionName name;

	public FunctionChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, FunctionName name, CurrentTCState state, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.name = name;
		this.state = state;
		this.inMeth = inMeth;
		if (inMeth != null && inMeth.contractMethod() != null) {
			csc = new ContractSlotChecker(errors, sv, state, inMeth);
		} else
			csc = null;
		sv.push(this);
		Type t = state.getMember(name);
		if (t != null && t instanceof Apply) {
			List<Type> att = ((Apply)t).tys;
			List<Type> sl = att.subList(0, att.size()-1);
			for (Type ti : sl)
				argTypes.add(new PosType(null, ti));
		}
	}
	
	@Override
	public void visitEventSource(Template t) {
		if (t.nestingChain() == null)
			((ObjectMethod)inMeth).bindEventSource(inMeth.getCard());
		else
			((ObjectMethod)inMeth).bindEventSource(t.nestingChain().iterator().next().type());
	}
	
	@Override
	public void visitHandlerLambda(HandlerLambda hl) {
		Pattern patt = hl.patt;
		if (patt instanceof VarPattern) {
			if (hl.usedBy.contains(this.name)) {
				VarPattern vp = (VarPattern) patt;
				UnifiableType lt = state.createUT(null, "hl " + vp.var);
				state.bindVarToUT(name.uniqueName(), vp.name().uniqueName(), lt);
				if (hl.isNested)
					hl.unifiableType(lt);
				state.bindVarPatternToUT(vp, lt);
			}
		} else if (patt instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) patt;
			UnifiableType lt = state.createUT(null, "hl " + tp.var);
			lt.canBeType(tp.var.loc, tp.type.defn());
			state.bindVarToUT(hl.name().uniqueName(), tp.name().uniqueName(), lt);
		} else
			throw new NotImplementedException("not supported as lambda: " + patt.getClass());
	}
	
	@Override
	public void argSlot(ArgSlot s) {
		if (inMeth != null && inMeth.contractMethod() != null) {
			// handle contract methods where the types are already prescribed
			sv.push(csc);
		} else if (s.isContainer()) {
			new ContainerChecker(sv, s.containerType());
		} else {
			UnifiableType currentArg = state.createUT(null, name.uniqueName() + " slot " + s);
			sv.push(new SlotChecker(sv, name, state, currentArg));
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
	public void patternsDone(LogicHolder fn) {
		if (fn instanceof ObjectCtor && !((ObjectCtor)fn).generate)
			return;
		if (fn instanceof ObjectMethod && !((ObjectMethod)fn).generate)
			return;
		List<Type> ats = new ArrayList<>();
		for (PosType ar : argTypes)
			ats.add(ar.type);
		state.recordMember(fn.name(), ats);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		name = fi.name();
	}
	
	@Override
	public void visitCase(FunctionCaseDefn fcd) {
		sv.push(new ExpressionChecker(errors, repository, state, sv, name.uniqueName(), false));
	}
	
	@Override
	public void visitGuardedMessage(GuardedMessages gm) {
		new GuardedMessagesChecker(errors, repository, state, sv, name.uniqueName(), inMeth);
	}
	
	@Override
	public void visitSendMessage(SendMessage sm) {
		new MessageChecker(errors,repository, state, sv, name.uniqueName(), inMeth, null);
	}
	
	@Override
	public void visitAssignMessage(AssignMessage assign) {
		new MessageChecker(errors,repository, state, sv, name.uniqueName(), inMeth, assign);
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
			sv.push(new ExpressionChecker(errors, repository, state, sv, name.uniqueName(), false));
		} else {
			ExprResult exprResult = (ExprResult)r;
			InputPosition pos = exprResult.pos;
			Type ret = exprResult.type;
			markUsed(pos, ret);
			resultTypes.add(exprResult);
		}
	}

	private void markUsed(InputPosition pos, Type ret) {
		if (ret instanceof UnifiableType) {
			((UnifiableType)ret).isReturned(pos);
		} else if (ret instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ret;
			for (Type ty : pi.polys())
				markUsed(pos, ty);
		}
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			sv.result(null);
		else if (resultTypes.isEmpty()) {
			// this will be the case when we are just processing arguments
			sv.result(null);
		} else {
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
		if (!meth.generate) {
			sv.result(null);
			return;
		}
		if (!meth.hasMessages())
			sv.result(buildApplyType(meth.location(), new PosType(meth.location(), LoadBuiltins.nil)));
		else if (resultTypes.isEmpty()) {
			// this will be the case when we are just processing arguments
			sv.result(null);
		} else {
			PosType posty = state.consolidate(meth.location(), resultTypes);
			sv.result(buildApplyType(meth.location(), new PosType(posty.pos, new EnsureListMessage(meth.location(), posty.type))));
		}
	}

	@Override
	public void leaveObjectCtor(ObjectCtor ctor) {
		if (!ctor.generate) {
			sv.result(null);
		} else if (ctor.hasMessages() && resultTypes.isEmpty()) {
			// this will be the case when we are just processing arguments
			sv.result(null);
		} else {
			// Note that this is its declared type.  It carries messages behind the scenes
			// 	The reaason for this trickery is that constructors are special and we want to have natural looking syntax
			// I'm not sure if there is a "purely functional syntax" for object construction at the moment - create it if/when it is needed
			sv.result(buildApplyType(ctor.location(), new PosType(ctor.location(), ctor.getObject())));
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
