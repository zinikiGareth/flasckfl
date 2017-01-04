package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.TypeWithNameAndPolys;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class RWStructDefn extends TypeWithNameAndPolys implements AsString, ExternalRef {
	public final List<RWStructField> fields = new ArrayList<RWStructField>();
	public final transient boolean generate;
	private final StructName structName;

	public RWStructDefn(InputPosition location, StructName tn, boolean generate, PolyVar... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	public RWStructDefn(InputPosition location, StructName tn, boolean generate, List<PolyVar> polys) {
		super(null, location, tn, polys);
		this.structName = tn;
		this.generate = generate;
	}

	public StructName structName() {
		return structName;
	}

	public RWStructDefn addField(RWStructField sf) {
		// TODO: validate that any poly fields here are defined in the provided list of polys
		fields.add(sf);
		return this;
	}

	public RWStructField findField(String var) {
		for (RWStructField sf : fields)
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
			for (RWStructField f : fields) {
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

	@Override
	public int compareTo(Object o) {
		return this.name().compareTo(((ExternalRef)o).uniqueName());
	}

	@Override
	public String uniqueName() {
		return this.name();
	}

	@Override
	public boolean fromHandler() {
		throw new UtilException("How would I know?");
	}

	public void visitFields(FieldVisitor visitor) {
		for (RWStructField sf : fields)
			visitor.visit(sf);
	}
}
