package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class StructDefn {
	public final String typename;
	public final List<String> args = new ArrayList<String>();

	public StructDefn(String tn) {
		this.typename = tn;
	}

	public void add(String ta) {
		args.add(ta);
	}
}
