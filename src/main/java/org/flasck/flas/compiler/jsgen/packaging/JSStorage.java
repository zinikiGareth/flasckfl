package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.st.SystemTest;

public interface JSStorage {

	JSClassCreator newClass(String pkg, NameOfThing clz);
	JSClassCreator newSystemTest(SystemTest st);

	void ensurePackageExists(String filePkg, String pkg);

	JSMethodCreator newFunction(NameOfThing fnName, String pkg, NameOfThing cxt, boolean inPrototype, String name);
	void methodList(NameOfThing name, List<FunctionName> methods);
	void eventMap(NameOfThing name, EventTargetZones eventMethods);

	Iterable<File> files();
	File fileFor(String s);

	void contract(ContractDecl cd);
	void object(ObjectDefn cd);
	void handler(HandlerImplements hi);

	void complete();

	Iterable<String> packages();


}
