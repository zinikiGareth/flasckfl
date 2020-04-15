package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public interface TopLevelDefinitionConsumer extends FunctionScopeUnitConsumer, IntroductionConsumer {
	void newAgent(AgentDefinition card);
	void newCard(CardDefinition card);
	void newService(ServiceDefinition card);
	void newStruct(StructDefn sd);
	void newStructField(StructField sf);
	void newUnion(UnionTypeDefn with);
	void newContract(ContractDecl decl);
	void newObject(ObjectDefn od);
	void newTestData(UnitDataDeclaration data);
	void newObjectAccessor(ObjectAccessor oa);
	void newRequiredContract(RequiresContract rc);
	void newObjectContract(ObjectContract oc);
	void replaceDefinition(HandlerLambda hl);
}
