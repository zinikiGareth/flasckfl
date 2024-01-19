package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class StateDefinition extends FieldsDefn {

	public StateDefinition(InputPosition loc) {
		this(null, loc, null, false, new ArrayList<>());
	}
	
	public StateDefinition(InputPosition loc, NameOfThing nameOfThing) {
		this(null, loc, nameOfThing);
	}
	
	public StateDefinition(InputPosition kwloc, InputPosition loc, NameOfThing nameOfThing) {
		this(kwloc, loc, nameOfThing, false, new ArrayList<>());
	}
	
	public StateDefinition(InputPosition kwloc, InputPosition loc, NameOfThing name, boolean generate, List<PolyType> polys) {
		super(kwloc, loc, FieldsType.STATE, name, false, polys);
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}

	public boolean hasMember(String text) {
		for (StructField sf : fields)
			if (sf.name.equals(text))
				return true;
		return false;
	}

	@Override
	public String asString() {
		return "STATE";
	}
}
