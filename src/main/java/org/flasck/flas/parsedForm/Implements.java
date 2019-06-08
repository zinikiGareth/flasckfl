package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parser.ImplementationMethodConsumer;

public class Implements implements Locatable, ImplementationMethodConsumer {
	@Deprecated
	public final List<MethodCaseDefn> methods = new ArrayList<MethodCaseDefn>();
	public final InputPosition kw;
	private InputPosition location;
	private String name;
	protected NameOfThing realName;

	public Implements(InputPosition kw, InputPosition location, String name) {
		this.kw = kw;
		this.location = location;
		this.name = name;
	}

	public void setRealName(NameOfThing realName) {
		this.realName = realName;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public String name() {
		return name;
	}

	@Override
	public void addImplementationMethod(ObjectMethod method) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Deprecated
	public void addMethod(MethodCaseDefn meth) {
		methods.add(meth);
	}
	
	@Override
	public String toString() {
		return name();
	}
}
