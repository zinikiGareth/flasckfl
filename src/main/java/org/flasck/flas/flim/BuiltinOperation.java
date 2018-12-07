package org.flasck.flas.flim;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.vcode.hsieForm.PushBuiltin;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class BuiltinOperation implements Locatable, Pushable {
	public static final BuiltinOperation OCTOR = new BuiltinOperation("octor", null);
	public static final BuiltinOperation FIELD = new BuiltinOperation("field", null);
	public static final BuiltinOperation TUPLE = new BuiltinOperation("tuple", null);
	public static final BuiltinOperation IDEM = new BuiltinOperation("idem", null);
	
	public final String opName;
	private final InputPosition location;

	private BuiltinOperation(String which, InputPosition location) {
		this.opName = which;
		this.location = location;
	}
	
	public boolean isField() {
		return opName.equals("field");
	}

	public Object at(InputPosition location) {
		return new BuiltinOperation(opName, location);
	}

	@Override
	public InputPosition location() {
		if (location == null)
			throw new NullPointerException("There is no location");
		return location;
	}

	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushBuiltin(location, this);
	}
	
	@Override
	public String toString() {
		return "#" +  opName;
	}
}
