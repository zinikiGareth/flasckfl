package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;

public interface TopLevelDefinitionConsumer extends FunctionScopeUnitConsumer {
	SolidName qualifyName(String base);
	CardName cardName(String name);
	HandlerName handlerName(String base);

	void newCard(CardDefinition card);
	void newService(ServiceDefinition card);
	void newStruct(StructDefn sd);
	void newUnion(UnionTypeDefn with);
	void newContract(ContractDecl decl);
	void newObject(ObjectDefn od);
}
