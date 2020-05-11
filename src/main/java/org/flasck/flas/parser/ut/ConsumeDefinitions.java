package org.flasck.flas.parser.ut;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public class ConsumeDefinitions implements UnitTestDefinitionConsumer {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private final UnitTestPackage pkg;

	public ConsumeDefinitions(ErrorReporter errors, TopLevelDefinitionConsumer consumer, UnitTestPackage pkg) {
		this.errors = errors;
		this.consumer = consumer;
		this.pkg = pkg;
	}

	@Override
	public void testCase(UnitTestCase utc) {
		pkg.testCase(utc);
	}

	@Override
	public void data(UnitDataDeclaration data) {
		consumer.newTestData(errors, data);
		pkg.data(data);
	}
	
	@Override
	public void nestedData(UnitDataDeclaration data) {
		consumer.newTestData(errors, data);
	}

	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		consumer.newIntroduction(errors, var);
	}

	public void newHandler(ErrorReporter errors, HandlerImplements hi) {
		consumer.newHandler(errors, hi);
	}

	public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
		consumer.functionDefn(errors, func);
	}

	public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName leadName, FunctionName pkgName, Expr expr) {
		consumer.tupleDefn(errors, vars, leadName, pkgName, expr);
	}

	public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
		consumer.newStandaloneMethod(errors, meth);
	}

	public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
		consumer.newObjectMethod(errors, om);
	}

	public void argument(ErrorReporter errors, VarPattern parm) {
		consumer.argument(errors, parm);
	}

	public void argument(ErrorReporter errors, TypedPattern with) {
		consumer.argument(errors, with);
	}

	public void newAgent(ErrorReporter errors, AgentDefinition card) {
		consumer.newAgent(errors, card);
	}

	public void polytype(ErrorReporter errors, PolyType pt) {
		consumer.polytype(errors, pt);
	}

	public void newCard(ErrorReporter errors, CardDefinition card) {
		consumer.newCard(errors, card);
	}

	public void newService(ErrorReporter errors, ServiceDefinition card) {
		consumer.newService(errors, card);
	}

	public void newStruct(ErrorReporter errors, StructDefn sd) {
		consumer.newStruct(errors, sd);
	}

	public void newStructField(ErrorReporter errors, StructField sf) {
		consumer.newStructField(errors, sf);
	}

	public void newUnion(ErrorReporter errors, UnionTypeDefn with) {
		consumer.newUnion(errors, with);
	}

	public void newContract(ErrorReporter errors, ContractDecl decl) {
		consumer.newContract(errors, decl);
	}

	public void newObject(ErrorReporter errors, ObjectDefn od) {
		consumer.newObject(errors, od);
	}

	public void newTestData(ErrorReporter errors, UnitDataDeclaration data) {
		consumer.newTestData(errors, data);
	}

	public void newObjectAccessor(ErrorReporter errors, ObjectAccessor oa) {
		consumer.newObjectAccessor(errors, oa);
	}

	public void newRequiredContract(ErrorReporter errors, RequiresContract rc) {
		consumer.newRequiredContract(errors, rc);
	}

	public void newObjectContract(ErrorReporter errors, ObjectContract oc) {
		consumer.newObjectContract(errors, oc);
	}

	public void newTemplate(ErrorReporter errors, Template template) {
		consumer.newTemplate(errors, template);
	}

	public void replaceDefinition(HandlerLambda hl) {
		consumer.replaceDefinition(hl);
	}
	
}
