package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;

public class StructDefn extends FieldsDefn implements AsString, Locatable, RepositoryEntry, WithTypeSignature {
	// for tests
	public StructDefn(InputPosition location, FieldsDefn.FieldsType type, String pkg, String tn, boolean generate, PolyType... polys) {
		this(null, location, type, new SolidName(pkg == null ? null : new PackageName(pkg), tn), generate, Arrays.asList(polys));
	}
	
	// The real constructor
	public StructDefn(InputPosition kw, InputPosition location, FieldsDefn.FieldsType structType, SolidName tn, boolean generate, List<PolyType> polys) {
		super(kw, location, structType, tn, generate, polys);
		if (structType.equals(FieldsDefn.FieldsType.ENTITY))
			this.fields.add(new StructField(location, true, new TypeReference(location, "Id"), "id"));
	}

	public SolidName name() {
		return name;
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public String signature() {
		return name.uniqueName();
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
				if (f.type != null) { // wraps items do not have types
					sb.append(f.type.name());
					sb.append(" ");
				}
				sb.append(f.name);
			}
			sb.append("}");
		}
		return sb.toString();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.type + "[" + dump() +"]");
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
