package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final ObjectActionHandler inMeth;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor sv, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.sv = sv;
		this.inMeth = inMeth;
		sv.push(this);
		// push this for the value on the rhs
		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
	}

	// The first thing that should happen is the RHS returns a result
	@Override
	public void result(Object r) {
		if (rhsType != null)
			throw new NotImplementedException("was not expecting multiple results");
		rhsType = (ExprResult) r;
	}

	// a slot is 0-or-more MemberExprs wrapped around an UnresolvedVar.  Unpack it recursively
	@Override
	public void visitAssignSlot(Expr toSlot) {
		if (rhsType.type instanceof ErrorType)
			return;

		ExprResult container = unpackMembers(toSlot);
		if (container.type instanceof ErrorType) {
			rhsType = container;
			return;
		}
		
		if (!(container.type.incorporates(rhsType.pos, rhsType.type))) {
			if (toSlot instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) toSlot;
				errors.message(rhsType.pos, "the field " + uv.var + " is of type " + container.type.signature() + ", not " + rhsType.type.signature());
			} else {
				MemberExpr me = (MemberExpr) toSlot;
				UnresolvedVar fld = (UnresolvedVar) me.fld;
				errors.message(rhsType.pos, "the field " + fld.var + " in " + me.containerType() + " is of type " + container.type.signature() + ", not " + rhsType.type.signature());
			}
			rhsType = new ExprResult(rhsType.pos, new ErrorType());
			return;
		}
		rhsType = new ExprResult(rhsType.pos, LoadBuiltins.message);
	}
	
	private ExprResult unpackMembers(Expr toSlot) {
		InputPosition pos = toSlot.location();
		if (toSlot instanceof UnresolvedVar) {
			UnresolvedVar var = (UnresolvedVar)toSlot;
			RepositoryEntry vardefn = var.defn();
			if (vardefn instanceof StructField) {
				StructField sf = (StructField)vardefn;
				if (!(sf.container() instanceof StateDefinition)) {
					errors.message(pos, "cannot use " + var.var + " as the main slot in assignment");
					return new ExprResult(rhsType.pos, new ErrorType());
				}
//				curr = ((NamedType)sf.container()).name().uniqueName();
				return new ExprResult(var.location, sf.type());
			} else if (vardefn instanceof TypedPattern) {
//				curr = ((NamedType)container).name().uniqueName();
				return new ExprResult(var.location, ((TypedPattern)vardefn).type.defn());
			} else
				throw new NotImplementedException("cannot handle assigning to (member of) " + vardefn);
		} else if (toSlot instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) toSlot;
			ExprResult res = unpackMembers(me.from);
			Type ty = res.type;
			if (ty instanceof ErrorType)
				return res;
			UnresolvedVar v = (UnresolvedVar) me.fld;
			if (ty instanceof StructDefn) {
				StructDefn sd = (StructDefn) ty;
				StructField fld = sd.findField(v.var);
				if (fld == null) {
					errors.message(toSlot.location(), "there is no field " + v.var + " in " + sd.name().uniqueName());
					return new ExprResult(rhsType.pos, new ErrorType());
				} else if (fld == LoadBuiltins.source) {
					List<Type> sources = ((ObjectMethod)inMeth).sources();
					if (sources.size() != 1) {
						// if it's empty, I think that means the event handler is not used
						//   that SHOULD be an error
						// if there are more than one, then they all need to be: the same (?) consistent in some way (?)
						throw new NotImplementedException("we need to check the consistency of sources");
					}
					me.bindContainerType(LoadBuiltins.event); // This probably wants to be something more precise, but I think it needs more from traits
					return new ExprResult(v.location, sources.get(0));
				}
				me.bindContainerType(fld.type());
				return new ExprResult(v.location, fld.type());
			}
			else if (ty instanceof StateHolder) {
				StateHolder type = (StateHolder)ty;
				StateDefinition state = type.state();
				if (state == null) {
					errors.message(pos, type.name().uniqueName() + " does not have state");
					return new ExprResult(rhsType.pos, new ErrorType());
				}
				StructField fld = state.findField(v.var);
				if (fld == null) {
					errors.message(toSlot.location(), "there is no field " + v.var + " in " + type.name().uniqueName());
					return new ExprResult(rhsType.pos, new ErrorType());
				}
				me.bindContainerType(fld.type());
				return new ExprResult(v.location, fld.type());
			} else {
				if (v.var == null)
					throw new NotImplementedException("there is no state at the top level in: " + ty.getClass());
				if (me.from instanceof UnresolvedVar) {
					UnresolvedVar uvf = (UnresolvedVar) me.from;
					errors.message(toSlot.location(), "field " + uvf.var + " is not a container");
				} else {
					MemberExpr inner = (MemberExpr) me.from;
					UnresolvedVar uvf = (UnresolvedVar) inner.fld;
					errors.message(toSlot.location(), "field " + uvf.var + " in " + inner.containerType().signature() + " is not a container");
				}
				return new ExprResult(rhsType.pos, new ErrorType());
			}
		} else
			throw new NotImplementedException();
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
		InputPosition pos = rhsType.pos;
		sv.result(new ExprResult(pos, new EnsureListMessage(pos, rhsType.type)));
	}
}
