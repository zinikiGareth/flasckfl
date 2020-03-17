package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;

import jdk.nashorn.internal.ir.annotations.Ignore;

public class StructDefn extends FieldsDefn implements AsString, Locatable, RepositoryEntry, WithTypeSignature, NamedType, AccessorHolder {
	public static Comparator<StructDefn> nameComparator = new Comparator<StructDefn>() {
		@Override
		public int compare(StructDefn l, StructDefn r) {
			return l.name().uniqueName().compareTo(r.name().uniqueName());
		}
	};

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
		return fields.size() - type.ignoreCount();
	}

	@Override
	public Type get(int pos) {
		if (pos == argCount())
			return this;
		return (Type)fields.get(pos + type.ignoreCount()).type.defn();
	}

	@Override
	public Type type() {
		return this;
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

	@Override
	public FieldAccessor getAccessor(String called) {
		for (StructField sf : fields) {
			if (sf.accessor && sf.name.equals(called))
				return sf;
		}
		return null;
	}

	public List<PolyType> polys() {
		return polys;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return other == this;
	}

	public PolyType findPoly(TypeReference ft) {
		if (polys == null)
			return null;
		for (PolyType pt : polys)
			if (pt.shortName().equals(ft.name()))
				return pt;
		return null;
	}

	public void completePolyNames() {
		for (PolyType pa : polys)
			pa.containedIn(name);
	}
}
