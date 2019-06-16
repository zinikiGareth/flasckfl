package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parser.ImplementationMethodConsumer;

public class Implements implements Locatable, ImplementationMethodConsumer {
	@Deprecated
	public final List<MethodCaseDefn> methods = new ArrayList<MethodCaseDefn>();
	public final InputPosition kw;
	private InputPosition location;
	private TypeReference implementing;

	public Implements(InputPosition kw, InputPosition location, TypeReference implementing) {
		this.kw = kw;
		this.location = location;
		this.implementing = implementing;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public TypeReference name() {
		return implementing;
	}

	@Override
	public void addImplementationMethod(ObjectMethod method) {
	}

	@Deprecated
	public void addMethod(MethodCaseDefn meth) {
		methods.add(meth);
	}
	
	@Override
	public String toString() {
		return implementing.name();
	}
}
