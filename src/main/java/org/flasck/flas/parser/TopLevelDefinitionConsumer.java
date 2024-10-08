package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.RepositoryEntry;

public interface TopLevelDefinitionConsumer extends FunctionScopeUnitConsumer, IntroductionConsumer, ContractConsumer {
	void newAgent(ErrorReporter errors, AgentDefinition card);
	void newCard(ErrorReporter errors, CardDefinition card);
	void newService(ErrorReporter errors, ServiceDefinition card);
	void newStruct(ErrorReporter errors, StructDefn sd);
	void newStructField(ErrorReporter errors, StructField sf);
	void newUnion(ErrorReporter errors, UnionTypeDefn with);
	void newContract(ErrorReporter errors, ContractDecl decl);
	void newObject(ErrorReporter errors, ObjectDefn od);
	void newTestData(ErrorReporter errors, UnitDataDeclaration data);
	void newObjectAccessor(ErrorReporter errors, ObjectAccessor oa);
	void newRequiredContract(ErrorReporter errors, RequiresContract rc);
	void newProvidesServiceWithName(ErrorReporter errors, Provides cs);
	void newContractImpl(ErrorReporter errors, ImplementsContract ci);
	void newObjectContract(ErrorReporter errors, ObjectContract oc);
	void newTemplate(ErrorReporter errors, Template template);
	void replaceDefinition(HandlerLambda hl);
	void addEntry(ErrorReporter errors, NameOfThing name, RepositoryEntry entry);
}
