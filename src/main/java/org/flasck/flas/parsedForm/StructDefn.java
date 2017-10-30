package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;

public class StructDefn implements AsString, Locatable {
	public enum StructType { STRUCT, ENTITY };
	public final List<StructField> fields = new ArrayList<StructField>();
	public final transient boolean generate;
	public final InputPosition kw;
	private final InputPosition location;
	public final StructType structType;
	private List<PolyType> polys;
	public final SolidName structName;

	// for tests
	public StructDefn(InputPosition location, StructType type, String pkg, String tn, boolean generate, PolyType... polys) {
		this(null, location, type, new SolidName(new PackageName(pkg), tn), generate, Arrays.asList(polys));
	}
	
	// The real constructor
	public StructDefn(InputPosition kw, InputPosition location, StructType structType, SolidName tn, boolean generate, List<PolyType> polys) {
		this.kw = kw;
		this.location = location;
		this.structType = structType;
		this.structName = tn;
		this.generate = generate;
		this.polys = polys;
		if (structType.equals(StructType.ENTITY))
			this.fields.add(new StructField(location, true, new TypeReference(location, "Id"), "id"));
	}

	public SolidName name() {
		return structName;
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
		StringBuilder sb = new StringBuilder(name().uniqueName());
		if (!polys().isEmpty()) {
			sb.append(polys());
		}
		return sb.toString();
	}

	public List<PolyType> polys() {
		return polys;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
