package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.ObjectElementsConsumer;

public class ObjectDefn implements ContainsScope, AsString, Locatable, ObjectElementsConsumer {
	public final List<ObjectMethod> ctors = new ArrayList<>();
	public final List<ObjectMethod> methods = new ArrayList<>();
	private final Scope innerScope;
	protected final List<PolyType> polys;
	private final Map<String, Integer> methodCases = new HashMap<>();
	protected final InputPosition location;
	private final SolidName name;
	public final InputPosition kw;
	public final boolean generate;

	public ObjectDefn(InputPosition kw, InputPosition location, SolidName tn, boolean generate, List<PolyType> polys) {
		this.kw = kw;
		this.name = tn;
		this.location = location;
		this.generate = generate;
		this.polys = polys;
		this.innerScope = new Scope(tn);
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public IScope innerScope() {
		return innerScope;
	}

	@Override
	public void defineState(int with) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	public ObjectDefn addCtor(ObjectMethod m) {
		ctors.add(m);
		return this;
	}

	public ObjectDefn addMethod(ObjectMethod m) {
		methods.add(m);
		return this;
	}

	public SolidName name() {
		return name;
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<PolyType> polys() {
		return polys;
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

	public int caseFor(String name) {
		int ret = 0;
		if (methodCases.containsKey(name))
			ret = methodCases.get(name);
		methodCases.put(name, ret+1);
		return ret;
	}
}
