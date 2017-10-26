package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.types.TypeWithNameAndPolys;
import org.zinutils.exceptions.UtilException;

public class RWStructDefn extends TypeWithNameAndPolys implements AsString, ExternalRef {
	public final List<RWStructField> fields = new ArrayList<RWStructField>();
	public final transient boolean generate;
	private final SolidName structName;
	public final StructType ty;

	public RWStructDefn(InputPosition location, StructType ty, SolidName tn, boolean generate, PolyVar... polys) {
		this(location, ty, tn, generate, Arrays.asList(polys));
	}
	
	public RWStructDefn(InputPosition location, StructType ty, SolidName tn, boolean generate, List<PolyVar> polys) {
		super(null, location, tn, polys);
		this.structName = tn;
		this.generate = generate;
		this.ty = ty;
	}

	public SolidName structName() {
		return structName;
	}

	public SolidName myName() {
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
				sb.append(((TypeWithName)f.type).name());
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
