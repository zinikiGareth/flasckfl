package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class StateDefinition {
	public final List<StructField> fields = new ArrayList<StructField>();

	public void addField(StructField o) {
		fields.add(o);
	}

	public boolean hasMember(String text) {
		for (StructField sf : fields)
			if (sf.name.equals(text))
				return true;
		return false;
	}
}
