package test.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.lifting.FunctionGroupOrdering;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.tc3.Primitive;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.exceptions.NotImplementedException;

import flas.matchers.ExprMatcher;

public class TraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, "42", 2);
	final UnresolvedVar var = new UnresolvedVar(pos, "f");
	final UnresolvedOperator op = new UnresolvedOperator(pos, "+");
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Repository r = new Repository();
	final HSIVisitor v = context.mock(HSIVisitor.class);
	final Traverser t = new Traverser(v);
	final ErrorReporter errors = context.mock(ErrorReporter.class);

	@Test
	public void traversePrimitive() {
		Primitive p = new Primitive(pos, "Foo");
		r.addEntry(errors, p.name(), p);
		context.checking(new Expectations() {{
			oneOf(v).visitPrimitive(p);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseStructDefn() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).leaveStructDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseStructDefnWithFieldVisitsTheField() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		TypeReference tr = new TypeReference(pos, "X", new ArrayList<>());
		StructField sf = new StructField(pos, s, false, tr, "x");
		s.addField(sf);
		sf.fullName(new VarName(sf.loc,s.name, sf.name));
		r.addEntry(errors, s.name(), s);
		r.addEntry(errors, sf.name(), sf);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).visitStructField(sf);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveStructField(sf);
			oneOf(v).leaveStructDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseStructDefnWithFieldAccessorVisitsTheFieldAsAnAccessor() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		TypeReference tr = new TypeReference(pos, "X", new ArrayList<>());
		StructField sf = new StructField(pos, s, true, tr, "x");
		s.addField(sf);
		sf.fullName(new VarName(sf.loc,s.name, sf.name));
		r.addEntry(errors, s.name(), s);
		r.addEntry(errors, sf.name(), sf);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).visitStructField(sf);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveStructField(sf);
			oneOf(v).leaveStructDefn(s);
			oneOf(v).visitStructFieldAccessor(sf);
			oneOf(v).leaveStructFieldAccessor(sf);
			oneOf(v).traversalDone();
		}});
		r.traverseWithHSI(v);
	}

	@Test
	public void traverserDoesNotVisitPolyTypes() {
		PolyType pa = new PolyType(pos, new SolidName(null, "A"));
		StructDefn s = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "MyStruct"), true, Arrays.asList(pa));
		r.newStruct(errors, s);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).leaveStructDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectDefn() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
			oneOf(v).leaveObjectDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectDefnWithFieldsVisitsTheState() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition sd = new StateDefinition(pos);
		StructField sf = new StructField(pos, sd, false, LoadBuiltins.stringTR, "s");
		sd.addField(sf);
		s.defineState(sd);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
			oneOf(v).visitStateDefinition(sd);
			oneOf(v).visitStructField(sf);
			oneOf(v).visitTypeReference(LoadBuiltins.stringTR, true);
			oneOf(v).leaveStructField(sf);
			oneOf(v).leaveStateDefinition(sd);
			oneOf(v).leaveObjectDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectDefnWithInitializerFieldsVisitsTheStateEvaluatingTheInitializers() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition sd = new StateDefinition(pos);
		StringLiteral sl = new StringLiteral(pos, "hello");
		StructField sf = new StructField(pos, pos, sd, false, LoadBuiltins.stringTR, "s", sl);
		sd.addField(sf);
		s.defineState(sd);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
			oneOf(v).visitStateDefinition(sd);
			oneOf(v).visitStructField(sf);
			oneOf(v).visitTypeReference(LoadBuiltins.stringTR, true);
			oneOf(v).visitExpr(sl, 0);
			oneOf(v).visitStringLiteral(sl);
			oneOf(v).leaveStructField(sf);
			oneOf(v).leaveStateDefinition(sd);
			oneOf(v).leaveObjectDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectWithMethodsDoesNotDirectlyVisitTheMethod() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>(), null, null);
		s.methods.add(meth);
		ObjectAccessor oa = new ObjectAccessor(s, new FunctionDefinition(FunctionName.function(pos, obj, "acor"), 2, null));
		s.acors.add(oa);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
			oneOf(v).leaveObjectDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseAgentDefn() {
		AgentDefinition s = new AgentDefinition(pos, pos, new CardName(pkg, "AnAgent"));
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseAgentDefnVisitsStateIfDefined() {
		AgentDefinition s = new AgentDefinition(pos, pos, new CardName(pkg, "AnAgent"));
		StateDefinition sd = new StateDefinition(pos);
		StringLiteral sl = new StringLiteral(pos, "hello");
		StructField sf = new StructField(pos, pos, s, false, LoadBuiltins.stringTR, "s", sl);
		sd.addField(sf);
		s.defineState(sd);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).visitStateDefinition(sd);
			oneOf(v).visitStructField(sf);
			oneOf(v).visitTypeReference(LoadBuiltins.stringTR, true);
			oneOf(v).visitExpr(sl, 0);
			oneOf(v).visitStringLiteral(sl);
			oneOf(v).leaveStructField(sf);
			oneOf(v).leaveStateDefinition(sd);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traversingAgentDefnVisitsProvides() {
		CardName an = new CardName(pkg, "AnAgent");
		AgentDefinition s = new AgentDefinition(pos, pos, an);
		TypeReference fred = new TypeReference(pos, "Fred");
		Provides p = new Provides(pos, pos, s, fred, new CSName(an, "S0"));
		s.addProvidedService(p);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).visitProvides(p);
			oneOf(v).visitTypeReference(fred, true);
			oneOf(v).leaveProvides(p);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traversingAgentDefnDoesntVisitMethodsDefinedInProvidesByDefault() {
		CardName an = new CardName(pkg, "AnAgent");
		AgentDefinition s = new AgentDefinition(pos, pos, an);
		TypeReference fred = new TypeReference(pos, "Fred");
		Provides p = new Provides(pos, pos, null, fred, new CSName(an, "S0"));
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.contractMethod(pos, p.name(), "x"), new ArrayList<Pattern>(), null, null);
		p.addImplementationMethod(meth);
		s.addProvidedService(p);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).visitProvides(p);
			oneOf(v).visitTypeReference(fred, true);
			oneOf(v).leaveProvides(p);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traversingAgentDefnVisitsMethodsDefinedInProvidesIfDesired() {
		CardName an = new CardName(pkg, "AnAgent");
		AgentDefinition s = new AgentDefinition(pos, pos, an);
		TypeReference fred = new TypeReference(pos, "Fred");
		Provides p = new Provides(pos, pos, null, fred, new CSName(an, "S0"));
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.contractMethod(pos, p.name(), "x"), new ArrayList<Pattern>(), null, null);
		p.addImplementationMethod(meth);
		s.addProvidedService(p);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).visitProvides(p);
			oneOf(v).visitTypeReference(fred, true);
			oneOf(v).visitObjectMethod(meth);
			oneOf(v).leaveObjectMethod(meth);
			oneOf(v).leaveProvides(p);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverseWithImplementedMethods(v);
	}

	@Test
	public void traversingAgentDefnVisitsRequiresContract() {
		CardName an = new CardName(pkg, "AnAgent");
		AgentDefinition s = new AgentDefinition(pos, pos, an);
		TypeReference tr = new TypeReference(pos, "Svc");
		RequiresContract rc = new RequiresContract(pos, pos, s, tr, new CSName(an, "S0"), pos, "svc");
		s.addRequiredContract(rc);
		r.newRequiredContract(errors, rc);
		r.addEntry(errors, s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitAgentDefn(s);
			oneOf(v).visitRequires(rc);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveAgentDefn(s);
			oneOf(v).traversalDone();
		}});
		r.traverseWithImplementedMethods(v);
	}

	@Test
	public void traverseObjectAccessorFromTheRepository() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		FunctionDefinition acorFn = new FunctionDefinition(FunctionName.function(pos, obj, "acor"), 2, null);
		FunctionIntro fi = new FunctionIntro(FunctionName.caseName(acorFn.name(), 1), new ArrayList<>());
		acorFn.intro(fi);
		ObjectAccessor oa = new ObjectAccessor(s, acorFn);
		s.acors.add(oa);
		r.addEntry(errors, oa.name(), oa);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectAccessor(oa);
			oneOf(v).visitFunction(oa.function());
			oneOf(v).visitFunctionIntro(fi);
			oneOf(v).leaveFunctionIntro(fi);
			oneOf(v).leaveFunction(oa.function());
			oneOf(v).leaveObjectAccessor(oa);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectMethodFromTheRepository() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>(), null, null);
		s.methods.add(meth);
		r.addEntry(errors, meth.name(), meth);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectMethod(meth);
			oneOf(v).leaveObjectMethod(meth);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectMethodFromTheRepositoryEvenInGroupOrder() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>(), null, null);
		s.methods.add(meth);
		r.addEntry(errors, meth.name(), meth);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectMethod(meth);
			oneOf(v).leaveObjectMethod(meth);
			oneOf(v).traversalDone();
		}});
		r.traverseInGroups(v, new FunctionGroupOrdering(new ArrayList<>()));
	}

	@Test
	public void traverseStandaloneMethodFromTheRepository() {
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "meth"), new ArrayList<>(), null, null);
		StandaloneMethod sm = new StandaloneMethod(meth);
		r.addEntry(errors, sm.name(), sm);
		context.checking(new Expectations() {{
			oneOf(v).visitStandaloneMethod(sm);
			oneOf(v).visitObjectMethod(meth);
			oneOf(v).leaveObjectMethod(meth);
			oneOf(v).leaveStandaloneMethod(sm);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseContract() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contr"));
		r.addEntry(errors, cd.name(), cd);
		context.checking(new Expectations() {{
			oneOf(v).visitContractDecl(cd);
			oneOf(v).leaveContractDecl(cd);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseContractWithMethods() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contr"));
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cd.name(), "meth"), new ArrayList<>(), null);
		cd.addMethod(cmd);
		r.addEntry(errors, cd.name(), cd);
		context.checking(new Expectations() {{
			oneOf(v).visitContractDecl(cd);
			oneOf(v).visitContractMethod(cmd);
			oneOf(v).leaveContractMethod(cmd);
			oneOf(v).leaveContractDecl(cd);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseContractWithMethodsWithArguments() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contr"));
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cd.name(), "meth"), new ArrayList<>(), null);
		TypeReference tr = new TypeReference(pos, "HandlerType");
		cmd.args.add(new TypedPattern(pos, tr, new VarName(pos, cmd.name, "handler")));
		cd.addMethod(cmd);
		r.addEntry(errors, cd.name(), cd);
		context.checking(new Expectations() {{
			oneOf(v).visitContractDecl(cd);
			oneOf(v).visitContractMethod(cmd);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveContractMethod(cmd);
			oneOf(v).leaveContractDecl(cd);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}
	
	@Test
	public void exprDoesntVisitNull() {
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(null, 0);
		}});
		t.visitExpr(null, 0);
	}

	@Test
	public void exprVisitsString() {
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
		}});
		t.visitExpr(simpleExpr, 0);
	}

	@Test
	public void exprVisitsNumber() {
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
		}});
		t.visitExpr(number, 0);
	}

	@Test
	public void exprVisitsUnresolvedVar() {
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(var, 2);
			oneOf(v).visitUnresolvedVar(var, 2);
		}});
		t.visitExpr(var, 2);
	}

	@Test
	public void exprVisitsUnresolvedOp() {
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(op, 2);
			oneOf(v).visitUnresolvedOperator(op, 2);
		}});
		t.visitExpr(op, 2);
	}

	@Test
	public void exprVisitsMemberExpr() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(me, 0);
			oneOf(v).visitMemberExpr(me);
			oneOf(v).visitExpr(from, 0);
			oneOf(v).visitUnresolvedVar(from, 0);
			oneOf(v).leaveMemberExpr(me);
		}});
		t.visitExpr(me, 0);
	}

	@Test(expected=NotImplementedException.class)
	public void memberExprInHSIThrowsExceptionIfNotConverted() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
		}});
		t.withHSI().visitExpr(me, 0);
	}

	@Test
	public void convertedMemberExprInHSIVisitsThatInstead() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		StringLiteral sl = new StringLiteral(pos, "hello");
		me.conversion(sl);
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(sl, 0);
			oneOf(v).visitStringLiteral(sl);
		}});
		t.withHSI().visitExpr(me, 0);
	}

	@Test
	public void exprVisitsMessages() {
		Messages msgs = new Messages(pos, Arrays.asList(number, simpleExpr));
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(msgs, 0);
			oneOf(v).visitMessages(msgs);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
			oneOf(v).leaveMessages(msgs);
		}});
		t.visitExpr(msgs, 0);
	}

	@Test
	public void exprVisitsMakeSend() {
		MakeSend ms = new MakeSend(pos, FunctionName.contractMethod(pos, new SolidName(pkg, "Foo"), "f"), var, 2, null);
		context.checking(new Expectations() {{
			oneOf(v).visitMakeSend(ms);
			oneOf(v).visitExpr(ms, 0);
			oneOf(v).visitExpr(var, 0);
			oneOf(v).visitUnresolvedVar(var, 0);
			oneOf(v).leaveMakeSend(ms);
		}});
		t.visitExpr(ms, 0);
	}

	@Test
	public void exprVisitsMakeAcor() {
		MakeAcor ma = new MakeAcor(pos, FunctionName.contractMethod(pos, new SolidName(pkg, "Foo"), "f"), var, 2);
		context.checking(new Expectations() {{
			oneOf(v).visitMakeAcor(ma);
			oneOf(v).visitExpr(ma, 0);
			oneOf(v).visitExpr(var, 0);
			oneOf(v).visitUnresolvedVar(var, 0);
			oneOf(v).leaveMakeAcor(ma);
		}});
		t.visitExpr(ma, 0);
	}

	@Test
	public void exprVisitsFunctionApplication() {
		ApplyExpr ex = new ApplyExpr(pos, var, simpleExpr, number);
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(ex, 2);
			oneOf(v).visitApplyExpr(ex);
			oneOf(v).visitExpr(var, 2);
			oneOf(v).visitUnresolvedVar(var, 2);
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).leaveApplyExpr(ex);
		}});
		t.visitExpr(ex, 2);
	}

	@Test
	public void traverseUnitTest() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestPackage utp = new UnitTestPackage(pos, utfn);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		utp.testCase(utc);
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), null);
		utc.steps.add(udd);
		UnitTestAssert uta = new UnitTestAssert(null, null);
		utc.steps.add(uta);
		UnitTestExpect ute = new UnitTestExpect(new UnresolvedVar(pos, "ctr"), new UnresolvedVar(pos, "meth"), new Expr[] { new StringLiteral(pos, "hello") }, new AnonymousVar(pos));
		utc.steps.add(ute);
		r.newTestData(errors, udd);
		r.addEntry(errors, name, utp);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTestPackage(utp);
			oneOf(v).visitUnitTest(utc);
			oneOf(v).visitUnitTestStep(udd);
			oneOf(v).visitUnitDataDeclaration(udd);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveUnitDataDeclaration(udd);
			oneOf(v).visitUnitTestStep(uta);
			oneOf(v).visitUnitTestAssert(uta);
			oneOf(v).visitAssertExpr(true, null);
			oneOf(v).visitExpr(null, 0);
			oneOf(v).leaveAssertExpr(true, null);
			oneOf(v).visitAssertExpr(false, null);
			oneOf(v).visitExpr(null, 0);
			oneOf(v).leaveAssertExpr(false, null);
			oneOf(v).postUnitTestAssert(uta);
			oneOf(v).visitUnitTestStep(ute);
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("ctr")), with(0));
			oneOf(v).visitExpr(with(ExprMatcher.string("hello")), with(0));
			oneOf(v).visitStringLiteral((StringLiteral) with(ExprMatcher.string("hello")));
			oneOf(v).visitUnitTestExpect(ute);
			oneOf(v).expectHandlerNext();
			oneOf(v).visitExpr(with(any(AnonymousVar.class)), with(0));
			oneOf(v).visitAnonymousVar(with(any(AnonymousVar.class)));
			oneOf(v).leaveUnitTestExpect(ute);
			oneOf(v).leaveUnitTest(utc);
			oneOf(v).leaveUnitTestPackage(utp);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseUnitTestContract() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestPackage utp = new UnitTestPackage(pos, utfn);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		utp.testCase(utc);
		TypeReference tr = new TypeReference(pos, "SomeContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "SomeContract"));
		tr.bind(cd);
		UnresolvedVar f = new UnresolvedVar(pos, "f");
		StringLiteral hello = new StringLiteral(pos, "hello");
		ApplyExpr inv = new ApplyExpr(pos, f, hello);
		UnitTestSend uts = new UnitTestSend(new UnresolvedVar(pos, "card"), tr, inv);
		utc.steps.add(uts);
		r.addEntry(errors, name, utp);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTestPackage(utp);
			oneOf(v).visitUnitTest(utc);
			oneOf(v).visitUnitTestStep(uts);
			oneOf(v).visitUnitTestSend(uts);
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("card")), with(0));
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).visitSendMethod(cd, f);
			oneOf(v).visitExpr(hello, 0);
			oneOf(v).visitStringLiteral(hello);
			oneOf(v).leaveUnitTestSend(uts);
			oneOf(v).leaveUnitTest(utc);
			oneOf(v).leaveUnitTestPackage(utp);
			oneOf(v).traversalDone();
		}});
		r.traverseWithHSI(v);
	}

	@Test
	public void traverseTopLevelUnitTestDataDeclaration() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestPackage utp = new UnitTestPackage(pos, utfn);
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, true, tr, FunctionName.function(pos, name, "ut"), null);
		utp.data(udd);
		r.newTestData(errors, udd);
		r.addEntry(errors, name, utp);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTestPackage(utp);
			oneOf(v).visitUnitDataDeclaration(udd);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).leaveUnitDataDeclaration(udd);
			oneOf(v).leaveUnitTestPackage(utp);
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}

	@Test
	public void traverseUnitTestDataDefinitionWithFieldDeclarations() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), null);
		Assignment assign = new Assignment(new UnresolvedVar(pos, "x"), simpleExpr);
		udd.fields.add(assign);
		utc.steps.add(udd);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitDataDeclaration(udd);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).visitUnitDataField(assign);
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
			oneOf(v).leaveUnitDataField(assign);
			oneOf(v).leaveUnitDataDeclaration(udd);
		}});
		new Traverser(v).visitUnitDataDeclaration(udd);
	}

	@Test
	public void traverseUnitTestDataDefinitionWithExpr() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), simpleExpr);
		utc.steps.add(udd);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitDataDeclaration(udd);
			oneOf(v).visitTypeReference(tr, true);
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
			oneOf(v).leaveUnitDataDeclaration(udd);
		}});
		new Traverser(v).visitUnitDataDeclaration(udd);
	}

	@Test
	public void traverseUnitTestInvokeExpr() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		UnitTestInvoke uti = new UnitTestInvoke(simpleExpr);
		utc.steps.add(uti);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTest(utc);
			oneOf(v).visitUnitTestStep(uti);
			oneOf(v).visitUnitTestInvoke(uti);
			oneOf(v).visitExpr(simpleExpr, 0);
			oneOf(v).visitStringLiteral(simpleExpr);
			oneOf(v).leaveUnitTestInvoke(uti);
			oneOf(v).leaveUnitTest(utc);
		}});
		new Traverser(v).visitUnitTest(utc);
	}

	@Test
	public void traverseTuples() {
		List<LocatedName> vars = new ArrayList<LocatedName>();
		vars.add(new LocatedName(pos, "a"));
		vars.add(new LocatedName(pos, "b"));
		FunctionName fta = FunctionName.function(pos, null, "_tuple_a");
		FunctionName fa = FunctionName.function(pos, null, "a");
		UnresolvedVar expr = new UnresolvedVar(pos, "f");
		TupleAssignment ta = new TupleAssignment(vars, fta, fa, expr);
		ta.addMember(new TupleMember(pos, ta, 0, fa));
		ta.addMember(new TupleMember(pos, ta, 1, FunctionName.function(pos, null, "b")));
		r.addEntry(errors, fta, ta);
		Sequence seq = context.sequence("tuple");
		context.checking(new Expectations() {{
			oneOf(v).visitTuple(ta); inSequence(seq);
			oneOf(v).visitExpr(expr, 0); inSequence(seq);
			oneOf(v).visitUnresolvedVar(expr, 0); inSequence(seq);
			oneOf(v).tupleExprComplete(ta); inSequence(seq);
			oneOf(v).visitTupleMember(ta.members.get(0)); inSequence(seq);
			oneOf(v).leaveTupleMember(ta.members.get(0)); inSequence(seq);
			oneOf(v).visitTupleMember(ta.members.get(1)); inSequence(seq);
			oneOf(v).leaveTupleMember(ta.members.get(1)); inSequence(seq);
			oneOf(v).leaveTuple(ta); inSequence(seq);
			oneOf(v).traversalDone();
		}});
		new Traverser(v).doTraversal(r);
	}
}
