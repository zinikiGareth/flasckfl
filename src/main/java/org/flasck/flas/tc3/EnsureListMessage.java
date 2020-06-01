package org.flasck.flas.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LoadBuiltins;
import org.zinutils.exceptions.NotImplementedException;

public class EnsureListMessage implements Type {
	public final static Type listMessages = new PolyInstance(LoadBuiltins.pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
	private final InputPosition pos;
	private Type ret;
	
	public EnsureListMessage(InputPosition pos, Type ret) {
		this.pos = pos;
		this.ret = ret;
	}
	
	public Type checking() {
		return ret;
	}

	public Type validate(ErrorReporter errors) {
		if (validate(errors, pos, ret))
			return listMessages;
		else
			return new ErrorType();
	}

	public static boolean validate(ErrorReporter errors, InputPosition pos, Type check) {
		// don't cascade errors
		if (check instanceof ErrorType) {
			return false;
		}

		if (check instanceof UnifiableType) {
			return ((UnifiableType) check).mustBeMessage(errors);
		}
		
		// an empty list is fine
		if (check == LoadBuiltins.nil) {
			return true;
		}
		
		if (check instanceof EnsureListMessage) {
			EnsureListMessage elm = (EnsureListMessage) check;
			return elm.validate(errors) == listMessages;
		}
		
		// a poly list is fine (cons or list) as long as the type is some kind of Message
		if (check instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) check;
			NamedType nt = pi.struct();
			if (nt == LoadBuiltins.cons || nt == LoadBuiltins.list)
				check = pi.getPolys().get(0);
			else {
				errors.message(pos, check.signature() + " cannot be a Message");
				return false;
			}
		}
		if (LoadBuiltins.message.incorporates(pos, check)) {
			return true;
		}
		errors.message(pos, check.signature() + " cannot be a Message");
		return false;
	}

	@Override
	public String signature() {
		return "List[Message]";
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		if (pos == 0)
			return listMessages;
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return listMessages.incorporates(pos, other);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EnsureListMessage;
	}
	
	@Override
	public String toString() {
		return pos + " " + listMessages;
	}
}
