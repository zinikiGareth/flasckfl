package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parser.StructFieldConsumer;

public class StateDefinition extends FieldsDefn implements StructFieldConsumer {

	public StateDefinition(InputPosition loc) {
		this(loc, new ArrayList<>());
	}
	
	public StateDefinition(InputPosition loc, List<PolyType> polys) {
		super(null, loc, FieldsType.STATE, null, false, polys);
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
