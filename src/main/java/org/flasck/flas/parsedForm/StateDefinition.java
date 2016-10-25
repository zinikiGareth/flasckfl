package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class StateDefinition implements Locatable {
	public final List<StructField> fields = new ArrayList<StructField>();
	private InputPosition loc;

	public StateDefinition(InputPosition loc) {
		this.loc = loc;
	}
	
	@Override
	public InputPosition location() {
		return loc;
	}
	
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
