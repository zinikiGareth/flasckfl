package org.flasck.flas.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LoadBuiltins;
import org.zinutils.exceptions.NotImplementedException;

public class EnsureListMessage implements Type {
	private final static Type lm = new PolyInstance(LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
	private final InputPosition pos;
	private Type ret;
	
	public EnsureListMessage(InputPosition pos, Type ret) {
		this.pos = pos;
		this.ret = ret;
	}

	public void validate(ErrorReporter errors) {
		if (ret instanceof UnifiableType)
			ret = ((UnifiableType) ret).resolve(errors, true);
		if (ret instanceof ErrorType)
			return; // message has already been displayed
		if (!LoadBuiltins.message.incorporates(pos, ret)) {
			// If this happens, I think this should spit out an error, but I think it should have been caught already 
			throw new NotImplementedException();
		}
	}
	
	public Type listMessages() {
		return lm;
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
			return lm;
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return lm.incorporates(pos, other);
	}

}
