package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.util.List;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectDefn;

public interface JSStorage {

	JSClassCreator newClass(String pkg, String clz);

	void ensurePackageExists(String filePkg, String pkg);

	JSMethodCreator newFunction(String pkg, String cxt, boolean inPrototype, String name);
	void methodList(NameOfThing name, List<FunctionName> methods);
	void eventMap(CardName name, EventTargetZones eventMethods);

	Iterable<File> files();

	void contract(ContractDecl cd);
	void object(ObjectDefn cd);
	void handler(HandlerImplements hi);

	void complete();

	Iterable<String> packages();
}
