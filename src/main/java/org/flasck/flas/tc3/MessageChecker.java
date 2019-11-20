package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class MessageChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final InputPosition pos;
	private final ObjectMethod inMeth;
	private ExprResult rhsType;

	public MessageChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor sv, InputPosition pos, ObjectMethod meth) {
		this.errors = errors;
		this.sv = sv;
		this.pos = pos;
		this.inMeth = meth;
		sv.push(this);
		sv.push(new ExpressionChecker(errors, state, sv));
	}

	@Override
	public void visitAssignSlot(List<UnresolvedVar> slots) {
		Type container = inMeth.getObject();
		String curr = null;
		String var = null;
		for (int i=0;i<slots.size();i++) {
			UnresolvedVar slot = slots.get(i);
			if (container instanceof ObjectDefn) {
				ObjectDefn type = (ObjectDefn)container;
				curr = type.name().uniqueName();
				StateDefinition state = type.state();
				if (state == null) {
					errors.message(pos, type.name().uniqueName() + " does not have state");
					return;
				}
				var = slot.var;
				StructField fld = state.findField(var);
				if (fld == null) {
					errors.message(slot.location(), "there is no field " + var + " in " + type.name().uniqueName());
					return;
				}
				container = fld.type();
			} else {
				errors.message(slot.location, "field " + var + " in " + curr + " is not a container");
				return;
			}
		}
		if (!(rhsType.type instanceof ErrorType))
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
		Type check = rhsType.type;

		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			sv.result(rhsType);
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
