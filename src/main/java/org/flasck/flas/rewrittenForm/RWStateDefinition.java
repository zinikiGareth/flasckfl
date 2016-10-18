package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

public class RWStateDefinition {
	public final List<RWStructField> fields = new ArrayList<RWStructField>();

	public void addField(RWStructField o) {
		fields.add(o);
	}

	public boolean hasMember(String text) {
		for (RWStructField sf : fields)
			if (sf.name.equals(text))
				return true;
		return false;
	}
}
