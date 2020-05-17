package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
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
	private final CurrentTCState state;
	private final ObjectActionHandler inMeth;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor sv, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.state = state;
		this.sv = sv;
		this.inMeth = inMeth;
		sv.push(this);
		sv.push(new ExpressionChecker(errors, repository, state, sv, false));
	}

	@Override
	public void visitAssignSlot(List<UnresolvedVar> slots) {
		if (rhsType.type instanceof ErrorType)
			return;

		InputPosition pos = slots.get(0).location();
		String var = slots.get(0).var;
		Type container;
		RepositoryEntry vardefn = slots.get(0).defn();
		String curr;
		if (vardefn instanceof StructField) {
			StructField sf = (StructField)vardefn;
			if (!(sf.container() instanceof StateDefinition)) {
				errors.message(pos, "cannot use " + var + " as the main slot in assignment");
				return;
			}
			container = sf.type();
			curr = ((NamedType)sf.container()).name().uniqueName();
		} else if (vardefn instanceof TypedPattern) {
			container = ((TypedPattern)vardefn).type.defn();
			curr = ((NamedType)container).name().uniqueName();
		} else
			throw new NotImplementedException("cannot handle " + vardefn);
		boolean isEvent = inMeth.isEvent() && LoadBuiltins.event.incorporates(slots.get(0).location(), container); 
		for (int i=1;i<slots.size();i++) {
			UnresolvedVar slot = slots.get(i);
			pos = slot.location();
			// TODO: I think we also need to "remember" what we find here, because we have only resolved slot 0
			if (isEvent && i == 1) {
				if ("source".equals(slot.var)) {
					List<Type> sources = ((ObjectMethod)inMeth).sources();
					if (sources.size() != 1) {
						// if it's empty, I think that means the event handler is not used
						//   that SHOULD be an error
						// if there are more than one, then they all need to be: the same (?) consistent in some way (?)
						throw new NotImplementedException("we need to check the consistency of sources");
					}
					container = sources.get(0);
				} else 
					throw new NotImplementedException("cannot handle event var " + slot.var);
			} else if (container instanceof StructDefn) {
				StructDefn sd = (StructDefn) container;
				curr = sd.name().uniqueName();
				var = slot.var;
				StructField fld = sd.findField(var);
				if (fld == null) {
					errors.message(slot.location(), "there is no field " + var + " in " + sd.name().uniqueName());
					rhsType = new ExprResult(rhsType.pos, new ErrorType());
					return;
				}
				container = fld.type();
			}
			else if (container instanceof StateHolder) {
				StateHolder type = (StateHolder)container;
				curr = type.name().uniqueName();
				StateDefinition state = type.state();
				if (state == null) {
					errors.message(pos, type.name().uniqueName() + " does not have state");
					rhsType = new ExprResult(rhsType.pos, new ErrorType());
					return;
				}
				var = slot.var;
				StructField fld = state.findField(var);
				if (fld == null) {
					errors.message(slot.location(), "there is no field " + var + " in " + type.name().uniqueName());
					rhsType = new ExprResult(rhsType.pos, new ErrorType());
					return;
				}
				container = fld.type();
			} else {
				if (var == null)
					throw new NotImplementedException("there is no state at the top level in: " + container.getClass());
				errors.message(slot.location, "field " + var + " in " + curr + " is not a container");
				rhsType = new ExprResult(rhsType.pos, new ErrorType());
				return;
			}
		}
		
		if (!(container.incorporates(rhsType.pos, rhsType.type))) {
			errors.message(rhsType.pos, "the field " + var + " in " + curr + " is of type " + container.signature() + ", not " + rhsType.type.signature());
			rhsType = new ExprResult(rhsType.pos, new ErrorType());
			return;
		}
		rhsType = new ExprResult(rhsType.pos, LoadBuiltins.message);
	}
	
	@Override
	public void leaveMessage(ActionMessage msg) {
		InputPosition pos = null;
		if (msg != null)
			pos = msg.location();
		check(pos);
	}
	
	@Override
	public void result(Object r) {
		rhsType = (ExprResult) r;
	}

	private void check(InputPosition pos) {
		if (!state.hasGroup())
			state.resolveAll(errors, true);
		
		Type check = rhsType.type;

		// don't cascade errors
		if (check instanceof ErrorType) {
			sv.result(rhsType);
			return;
		}
		
		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			sv.result(rhsType);
			return;
		}
		
		if (check instanceof EnsureListMessage) {
			EnsureListMessage elm = (EnsureListMessage) check;
			elm.validate(errors);
			return;
		}
		
		// a poly list is fine (cons or list) as long as the type is
		if (check instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) check;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				check = pi.getPolys().get(0);
			else {
				errors.message(pos, check.signature() + " cannot be a Message");
				sv.result(new ExprResult(pos, new ErrorType()));
				return;
			}
		}
		if (LoadBuiltins.message.incorporates(pos, check)) {
			sv.result(rhsType);
			return;
		}
		errors.message(pos, rhsType.type.signature() + " cannot be a Message");
		sv.result(new ExprResult(pos, new ErrorType()));
	}

}
