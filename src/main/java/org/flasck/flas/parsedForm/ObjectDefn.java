package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectDefn implements ContainsScope, AsString, Locatable, ObjectElementsConsumer, RepositoryEntry, NamedType {
	private StateDefinition state;
	private final List<Template> templates = new ArrayList<>();
	public final List<ObjectCtor> ctors = new ArrayList<>();
	public final List<ObjectAccessor> acors = new ArrayList<>();
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
	public ObjectDefn defineState(StateDefinition state) {
		if (this.state != null) {
			throw new RuntimeException("Fix this case with a test");
		}
		this.state = state;
		return this;
	}

	@Override
	public ObjectElementsConsumer addTemplate(Template template) {
		this.templates.add(template);
		return this;
	}

	@Override
	public ObjectDefn addConstructor(ObjectCtor ctor) {
		ctors.add(ctor);
		return this;
	}

	public ObjectDefn addAccessor(ObjectAccessor m) {
		acors.add(m);
		return this;
	}

	public void addMethod(ObjectMethod m) {
		methods.add(m);
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
	
	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "ObjectDefinition[" + asString() + "]";
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
}
