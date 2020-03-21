package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parser.ImplementationMethodConsumer;
import org.flasck.flas.tc3.NamedType;

public class Implements extends ContractReferencer implements Locatable, ImplementationMethodConsumer {
	public final List<ObjectMethod> implementationMethods = new ArrayList<>();

	public Implements(InputPosition kw, InputPosition location, NamedType parent, TypeReference implementing, NameOfThing myName) {
		super(kw, location, parent, implementing, myName);
	}

	@Override
	public void addImplementationMethod(ObjectMethod method) {
		implementationMethods.add(method);
		method.bindToImplements(this);
	}
}
