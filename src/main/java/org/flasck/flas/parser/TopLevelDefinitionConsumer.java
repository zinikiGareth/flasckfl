package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;

public interface TopLevelDefinitionConsumer extends FunctionScopeUnitConsumer {
	void newCard(CardDefinition card);
	void newService(ServiceDefinition card);
	void newStruct(StructDefn sd);
	void field(NameOfThing name, StructField sf);
	void newUnion(UnionTypeDefn with);
	void newContract(ContractDecl decl);
	void newObject(ObjectDefn od);
}
