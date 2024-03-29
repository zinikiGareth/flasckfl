package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;

public class PendingObjectCtor extends PendingMethod {
	private final InputPosition loc;
	private final FunctionName name;

	public PendingObjectCtor(ErrorReporter errors, InputPosition loc, FunctionName name) {
		super(errors);
		this.loc = loc;
		this.name = name;
	}

	public ObjectCtor resolve(ErrorReporter errors, Repository repository, ObjectDefn od) {
		List<Pattern> patts = new ArrayList<>();
		List<Type> ts = new ArrayList<>();
		for (PendingContractArg a : args) {
			TypedPattern tp = a.resolve(errors, repository, name);
			ts.add(tp.type.namedDefn());
			patts.add(tp);
		}
		Type type;
		if (args.isEmpty())
			type = od;
		else {
			type = new Apply(ts, od);
		}
		ObjectCtor oc = new ObjectCtor(loc, od, name, patts);
		oc.bindType(type);
		oc.dontGenerate();
		repository.newObjectMethod(errors, oc);
		return oc;
	}
}
