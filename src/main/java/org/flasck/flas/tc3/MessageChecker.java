package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
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
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class MessageChecker extends LeafAdapter implements ResultAware {
	public class Link {
		private final InputPosition pos;
		private final MemberExpr me;
		private final Expr toSlot;

		public Link(InputPosition pos, MemberExpr me, Expr toSlot) {
			this.pos = pos;
			this.me = me;
			this.toSlot = toSlot;
		}

		public ExprResult analyze(ExprResult ty) {
			Type rt = ty.type;
			if (rt instanceof UnifiableType)
				rt = ((UnifiableType)rt).resolvedTo();
			return analyzeContainer(toSlot, pos, me, rt);
		}
	}

	public class CheckChain extends ExprResult {
		private final UnresolvedVar var;
		private final List<Link> links = new ArrayList<>();
		public Expr finalSlot;

		public CheckChain(UnresolvedVar var, UnifiableType utf) {
			super(var.location, utf);
			this.var = var;
		}

		public void addLink(InputPosition pos, MemberExpr me, Expr toSlot) {
			links.add(new Link(pos, me, toSlot));
		}

		public void check(Type rty) {
			ErrorMark mark = errors.mark();
			ExprResult ity = checkInnermostType(var, rty);
			for (Link l : links) {
				if (mark.hasMoreNow())
					return;
				ity = l.analyze(ity);
			}
			if (mark.hasMoreNow())
				return;
			checkFinal(finalSlot, ity.type);
		}
	}

	private final ErrorReporter errors;
	private final CurrentTCState state;
	private final NestedVisitor sv;
	private final ObjectActionHandler inMeth;
	private final AssignMessage assign;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor sv, String fnCxt, ObjectActionHandler inMeth, AssignMessage assign) {
		this.errors = errors;
		this.state = state;
		this.sv = sv;
		this.inMeth = inMeth;
		this.assign = assign;
		sv.push(this);
		// push this for the value on the rhs
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
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
		if (container instanceof CheckChain) {
			((CheckChain)container).finalSlot = toSlot;
			return;
		}
		if (container.type instanceof ErrorType) {
			rhsType = container;
			return;
		}

		checkFinal(toSlot, container.type);
	}

	private void checkFinal(Expr toSlot, Type container) {
		if (container instanceof UnifiableType)
			throw new CantHappenException("this should have been resolved");
		if (assign != null && container instanceof PolyInstance) {
			PolyInstance tmp = (PolyInstance)container;
			if (tmp.struct() == LoadBuiltins.assignItem) {
				container = tmp.getPolys().get(0);
				assign.willAssignToCons();
			}
		}
		if (!(container.incorporates(rhsType.pos, rhsType.type))) {
			if (toSlot instanceof UnresolvedVar) {
				UnresolvedVar uv = (UnresolvedVar) toSlot;
				errors.message(rhsType.pos, "the field " + uv.var + " is of type " + container.signature() + ", not " + rhsType.type.signature());
			} else {
				MemberExpr me = (MemberExpr) toSlot;
				UnresolvedVar fld = (UnresolvedVar) me.fld;
				errors.message(rhsType.pos, "the field " + fld.var + " in " + me.containedType() + " is of type " + container.signature() + ", not " + rhsType.type.signature());
			}
			rhsType = new ExprResult(rhsType.pos, new ErrorType());
			return;
		}
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
				return new ExprResult(var.location, sf.type());
			} else if (vardefn instanceof TypedPattern) {
				return new ExprResult(var.location, ((TypedPattern)vardefn).type.defn());
			} else if (vardefn instanceof FunctionDefinition) {
				FunctionDefinition fd = (FunctionDefinition)vardefn;
				if (fd.hasType()) {
					Type ty = fd.type();
					return checkInnermostType(var, ty);
				} else {
					// if it doesn't have a type yet, it is presumably being checked along with us and has a UT
					UnifiableType utf = state.requireVarConstraints(var.location(), fd.name().uniqueName(), fd.name().uniqueName());
					CheckChain ret = new CheckChain(var, utf);
					utf.callOnResolved(ret::check);
					return ret;
				}
			} else
				throw new NotImplementedException("cannot handle assigning to (member of) " + vardefn);
		} else if (toSlot instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) toSlot;
			ExprResult res = unpackMembers(me.from);
			if (res instanceof CheckChain) {
				((CheckChain)res).addLink(pos, me, toSlot);
				return res;
			}
			Type ty = res.type;
			if (ty instanceof ErrorType)
				return res;
			return analyzeContainer(toSlot, pos, me, ty);
		} else
			throw new NotImplementedException();
	}

	private ExprResult checkInnermostType(UnresolvedVar var, Type ty) {
		Type rem = ty;
		if (ty instanceof Apply) {
			if (!(var.defn() instanceof FunctionDefinition)) {
				throw new HaventConsideredThisException("expecting var to be a function"); // I think this is an error
			}
			FunctionDefinition fd = (FunctionDefinition) var.defn();
			Apply app = (Apply) ty;
			StateHolder state = ((ObjectMethod)inMeth).state();
			if (!app.tys.get(0).equals((Type)state)) {
				throw new HaventConsideredThisException("expecting first arg to be the current object"); // I think this is an error
			}
			rem = app.appliedTo(state);
			if (rem instanceof UnifiableType)
				rem = ((UnifiableType)rem).resolvedTo();
			int discardNested = fd.nestedVars().size();
			if (discardNested > 0) {
				Apply da = (Apply) rem;
				rem = da.discard(discardNested);
			}
		}
		if (rem instanceof UnifiableType)
			rem = ((UnifiableType)rem).resolvedTo();
		return new ExprResult(var.location, rem);
	}

	private ExprResult analyzeContainer(Expr toSlot, InputPosition pos, MemberExpr me, Type ty) {
		UnresolvedVar v = (UnresolvedVar) me.fld;
		me.bindContainerType(ty);
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
				me.bindContainedType(LoadBuiltins.event); // This probably wants to be something more precise, but I think it needs more from traits
				return new ExprResult(v.location, sources.get(0));
			}
			me.bindContainedType(fld.type());
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
			me.bindContainedType(fld.type());
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
				errors.message(toSlot.location(), "field " + uvf.var + " in " + inner.containedType().signature() + " is not a container");
			}
			return new ExprResult(rhsType.pos, new ErrorType());
		}
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
		InputPosition pos = rhsType.pos;
		Type ty = rhsType.type;
		
		if (msg instanceof SendMessage) {
			if (!TypeHelpers.isListMessage(pos, ty)) {
				errors.message(pos, "must return a message or list of messages");
				sv.result(new ExprResult(pos, new ErrorType()));
				return;
			}
		}
		
		sv.result(new ExprResult(pos, LoadBuiltins.listMessages));
	}
}
