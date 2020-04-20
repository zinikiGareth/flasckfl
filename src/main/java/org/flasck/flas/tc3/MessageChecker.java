package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.zinutils.exceptions.NotImplementedException;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final CurrentTCState state;
	private final InputPosition pos;
	private final ObjectActionHandler inMeth;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor sv, InputPosition pos, ObjectActionHandler inMeth) {
		this.errors = errors;
		this.state = state;
		this.sv = sv;
		this.pos = pos;
		this.inMeth = inMeth;
		sv.push(this);
		sv.push(new ExpressionChecker(errors, state, sv));
	}

	@Override
	public void visitAssignSlot(List<UnresolvedVar> slots) {
		if (rhsType.type instanceof ErrorType)
			return;

		Type container;
		if (inMeth.hasObject())
			container = inMeth.getObject();
		else if (inMeth.hasImplements())
			container = inMeth.getImplements().getParent();
		else if (inMeth.isEvent())
			container = inMeth.getCard();
		else
			throw new NotImplementedException("Cannot find container in " + inMeth + " " + inMeth.getClass() + " with no object or implements");
		String curr = null;
		String var = null;
		for (int i=0;i<slots.size();i++) {
			UnresolvedVar slot = slots.get(i);
			if (container instanceof StateHolder) {
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
		check();
	}
	
	@Override
	public void result(Object r) {
		rhsType = (ExprResult) r;
	}

	private void check() {
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
