package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class StructDefn {
	public final String typename;
	public final List<String> args = new ArrayList<String>();
	public final List<StructField> fields = new ArrayList<StructField>();

	public StructDefn(String tn) {
		this.typename = tn;
	}

	public void add(String ta) {
		args.add(ta);
	}

	public void addField(StructField sf) {
		fields.add(sf);
	}
}
