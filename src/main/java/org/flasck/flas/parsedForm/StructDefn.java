package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class StructDefn {
	public final String typename;
	public final List<String> args = new ArrayList<String>();
	public final List<StructField> fields = new ArrayList<StructField>();
	public final boolean generate;

	public StructDefn(String tn, boolean generate) {
		this.typename = tn;
		this.generate = generate;
	}

	public StructDefn add(String ta) {
		args.add(ta);
		return this;
	}

	public StructDefn addField(StructField sf) {
		fields.add(sf);
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(typename);
		if (!args.isEmpty()) {
			sb.append(args);
		}
		if (!fields.isEmpty()) {
			sb.append("{");
			String sep = "";
			for (StructField f : fields) {
				sb.append(sep);
				sep = ",";
				sb.append(f.type.toString());
				sb.append(" ");
				sb.append(f.name);
			}
			sb.append("}");
		}
		return sb.toString();
	}
}
