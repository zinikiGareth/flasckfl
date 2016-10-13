package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class RWStateDefinition implements Serializable {
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
