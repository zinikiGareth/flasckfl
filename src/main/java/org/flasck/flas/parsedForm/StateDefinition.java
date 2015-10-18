package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class StateDefinition implements Serializable {
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
