package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.TypeWithMethods;

public class RWObjectDefn extends RWStructDefn implements TypeWithMethods, AsString, Locatable {
	public final List<RWObjectMethod> ctors = new ArrayList<RWObjectMethod>();
	public final List<RWObjectMethod> methods = new ArrayList<RWObjectMethod>();

	public RWObjectDefn(InputPosition location, SolidName tn, boolean generate, PolyVar... polys) {
		this(location, tn, generate, Arrays.asList(polys));
	}
	
	public RWObjectDefn(InputPosition location, SolidName tn, boolean generate, List<PolyVar> polys) {
		super(location, FieldsType.OBJECT, tn, generate, polys);
	}

	public String uniqueName() {
		return name();
	}

	public SolidName myName() {
		return (SolidName) this.getTypeName();
	}

	@Override
	public boolean hasMethod(String named) {
		for (RWObjectMethod m : methods)
			if (m.name.name.equals(named))
				return true;
		return false;
	}

	public RWObjectDefn addConstructor(RWObjectMethod ctor) {
		ctors.add(ctor);
		return this;
	}

	public RWObjectMethod getConstructor(String meth) {
		String ctor = "_ctor_" + meth;
		for (RWObjectMethod m : ctors)
			if (m.name.name.equals(ctor))
				return m;
		return null;
	}

	public RWObjectDefn addMethod(RWObjectMethod m) {
		methods.add(m);
		return this;
	}

	public RWMethodDefinition getMethod(FunctionName named) {
		for (RWObjectMethod m : methods)
			if (m.name.name.equals(named.name))
				return m.defn;
		return null;
	}

	public FunctionType getMethodType(String named) {
		for (RWObjectMethod m : methods)
			if (m.name.name.equals(named))
				return m.type;
		return null;
	}

	@Override
	public String toString() {
		return asString();
	}

	public String asString() {
		StringBuilder sb = new StringBuilder(name());
		if (hasPolys()) {
			sb.append("[");
			String sep = "";
			for (int i=0;i<polys().size();i++) {
				sb.append(sep);
				sb.append(poly(i));
				sep = ",";
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
