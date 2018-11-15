package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;

public class ObjectDefn implements ContainsScope, AsString, Locatable {
	public StateDefinition state;
	public final List<ObjectMethod> ctors = new ArrayList<>();
	public final List<ObjectMethod> methods = new ArrayList<>();
	public final transient boolean generate;
	private final Scope innerScope;
	public final InputPosition kw;
	private final InputPosition location;
	private final SolidName name;
	private final List<PolyType> polys;
	private final Map<String, Integer> methodCases = new HashMap<>();

	public ObjectDefn(InputPosition kw, InputPosition location, SolidName tn, boolean generate, List<PolyType> polys) {
		this.kw = kw;
		this.location = location;
		this.name = tn;
		this.generate = generate;
		this.polys = polys;
		this.innerScope = new Scope(tn);
	}

	@Override
	public IScope innerScope() {
		return innerScope;
	}

	public SolidName name() {
		return name;
	}

	public ObjectDefn addCtor(ObjectMethod m) {
		ctors.add(m);
		return this;
	}

	public ObjectDefn addMethod(ObjectMethod m) {
		methods.add(m);
		return this;
	}

	@Override
	public String toString() {
		return asString();
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public String asString() {
		StringBuilder sb = new StringBuilder(name().uniqueName());
		if (hasPolys()) {
			sb.append("[");
			String sep = "";
			for (int i=0;i<polys().size();i++) {
				sb.append(sep);
				sb.append(polys().get(i));
				sep = ",";
			}
			sb.append("]");
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

	public int caseFor(String name) {
		int ret = 0;
		if (methodCases.containsKey(name))
			ret = methodCases.get(name);
		methodCases.put(name, ret+1);
		return ret;
	}
}
