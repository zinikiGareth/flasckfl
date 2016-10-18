package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.collections.CollectionUtils;

public class StructDefn implements AsString, Locatable {
	public final List<StructField> fields = new ArrayList<StructField>();
	public final transient boolean generate;
	private final InputPosition location;
	private String name;
	private List<TypeReference> polys;

	// for tests
	public StructDefn(InputPosition location, String tn, boolean generate, TypeReference... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	// The real constructor
	public StructDefn(InputPosition location, String tn, boolean generate, List<TypeReference> polys) {
		this.location = location;
		this.name = tn;
		this.generate = generate;
		this.polys = polys;
	}

	public String name() {
		return name;
	}

	public StructDefn addField(StructField sf) {
		// TODO: validate that any poly fields here are defined in the provided list of polys
		fields.add(sf);
		return this;
	}

	public StructField findField(String var) {
		for (StructField sf : fields)
			if (sf.name.equals(var))
				return sf;
		return null;
	}

	public String toString() {
		return asString();
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder(asString());
		if (!fields.isEmpty()) {
			sb.append("{");
			String sep = "";
			for (StructField f : fields) {
				sb.append(sep);
				sep = ",";
				sb.append(f.type.name());
				sb.append(" ");
				sb.append(f.name);
			}
			sb.append("}");
		}
		return sb.toString();
	}

	public String asString() {
		StringBuilder sb = new StringBuilder(name());
		if (!polys().isEmpty()) {
			sb.append(polys());
		}
		return sb.toString();
	}

	public List<TypeReference> polys() {
		return polys;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
