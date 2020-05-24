package test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn.Unifier;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.ConsumeStructFields;
import org.flasck.flas.parser.SimpleVarNamer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class RepositoryTests {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final ErrorReporter errors = context.mock(ErrorReporter.class);
	final Unifier unifier = context.mock(Unifier.class);

	@Test
	public void canAddAFunctionToTheRepository() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2, false);
		r.functionDefn(errors, fn);
		assertEquals(fn, r.get("test.repo.fred"));
	}

	@Test
	public void cannotAddAFunctionToTheRepositoryTwice() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2, false);
		r.functionDefn(errors, fn);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.fred is defined multiple times: " + pos);
		}});
		r.functionDefn(errors, fn);
	}

	@Test
	public void canAddAFunctionArgToTheRepository() {
		Repository r = new Repository();
		final FunctionName fred = FunctionName.function(pos, pkg, "fred");
		VarPattern parm = new VarPattern(pos, new VarName(pos, fred, "a"));
		r.argument(errors, parm);
		assertEquals(parm, r.get("test.repo.fred.a"));
	}

	@Test
	public void canAddAllOfATupleToTheRepository() {
		Repository r = new Repository();
		List<LocatedName> vars = putATupleIntoTheRepository(r);
		final TupleAssignment ta = r.get("test.repo._tuple_a");
		assertNotNull(ta);
		assertEquals(vars, ta.vars);
		assertEquals(simpleExpr, ta.expr);
	}

	@Test
	public void addingATupleToTheRepositoryAddsTheFirstElement() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		final TupleAssignment ta = r.get("test.repo._tuple_a");
		final TupleMember tm = r.get("test.repo.a");
		assertNotNull(tm);
		assertEquals(ta, tm.ta);
		assertEquals(0, tm.which);
		assertEquals("test.repo.a", tm.name().uniqueName());
	}

	@Test
	public void addingATupleToTheRepositoryAddsTheFinalElement() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		final TupleAssignment ta = r.get("test.repo._tuple_a");
		final TupleMember tm = r.get("test.repo.c");
		assertNotNull(tm);
		assertEquals(ta, tm.ta);
		assertEquals(2, tm.which);
		assertEquals("test.repo.c", tm.name().uniqueName());
	}

	@Test
	public void cannotAddALeadTupleMemberToTheRepositoryTwice() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_a");
		FunctionName pkgName = FunctionName.function(pos, pkg, "a");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "a"));
		vars.add(new LocatedName(pos, "x"));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.a is defined multiple times: " + pos);
		}});
		r.tupleDefn(errors, vars, exprFnName, pkgName, simpleExpr);
	}

	@Test
	public void cannotAddASecondaryTupleMemberToTheRepositoryTwice() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_x");
		FunctionName pkgName = FunctionName.function(pos, pkg, "x");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "x"));
		vars.add(new LocatedName(pos, "b"));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.b is defined multiple times: " + pos);
		}});
		r.tupleDefn(errors, vars, exprFnName, pkgName, simpleExpr);
	}

	public List<LocatedName> putATupleIntoTheRepository(Repository r) {
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_a");
		FunctionName pkgName = FunctionName.function(pos, pkg, "a");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "a"));
		vars.add(new LocatedName(pos, "b"));
		vars.add(new LocatedName(pos, "c"));
		// Note: simpleExpr obviously isn't a tuple expr, but it's easy to write.  To typecheck, you would need something that returns 3 elements
		r.tupleDefn(errors, vars, exprFnName, pkgName, simpleExpr);
		return vars;
	}

	@Test
	public void canAddAStandaloneMethodToTheRepository() {
		Repository r = new Repository();
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "m"), new ArrayList<>(), null);
		StandaloneMethod meth = new StandaloneMethod(om);
		r.newStandaloneMethod(errors, meth);
		assertEquals(meth, r.get("test.repo.m"));
	}

	@Test
	public void cannotAddAStandaloneMethodToTheRepositoryTwice() {
		Repository r = new Repository();
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "m"), new ArrayList<>(), null);
		StandaloneMethod meth = new StandaloneMethod(om);
		r.newStandaloneMethod(errors, meth);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.m is defined multiple times: " + pos);
		}});
		r.newStandaloneMethod(errors, meth);
	}

	@Test
	public void cannotAddAStandaloneMethodToTheRepositoryIfAFunctionIsAlreadyThere() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2, false);
		r.functionDefn(errors, fn);
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "fred"), new ArrayList<>(), null);
		StandaloneMethod meth = new StandaloneMethod(om);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.fred is defined multiple times: " + pos);
		}});
		r.newStandaloneMethod(errors, meth);
	}

	@Test
	public void canAddAObjectDefnToTheRepository() {
		Repository r = new Repository();
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), true, new ArrayList<>());
		r.newObject(errors, od);
		assertEquals(od, r.get("test.repo.Obj"));
	}

	@Test
	public void aPolyObjectDefnAddsItsVars() {
		Repository r = new Repository();
		SolidName sn = new SolidName(pkg, "Obj");
		PolyType pa = new PolyType(pos, new SolidName(sn, "A"));
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, Arrays.asList(pa));
		r.newObject(errors, od);
		assertEquals(pa, r.get("test.repo.Obj.A"));
	}


	@Test
	public void canAddAnObjectAcorToTheRepository() {
		Repository r = new Repository();
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), true, new ArrayList<>());
		ObjectAccessor oa = new ObjectAccessor(od, new FunctionDefinition(FunctionName.function(pos, od.name(), "acor"), 2, false));
		r.newObjectAccessor(errors, oa);
		assertEquals(oa, r.get("test.repo.Obj.acor"));
	}

	@Test
	public void canAddAnObjectCtorToTheRepository() {
		Repository r = new Repository();
		final SolidName on = new SolidName(pkg, "Obj");
		ObjectCtor ctor = new ObjectCtor(pos, null, FunctionName.objectCtor(pos, on, "simple"), new ArrayList<>());
		r.newObjectMethod(errors, ctor);
		assertEquals(ctor, r.get("test.repo.Obj._ctor_simple"));
	}

	@Test
	public void canAddAnObjectMethodToTheRepository() {
		Repository r = new Repository();
		final SolidName on = new SolidName(pkg, "Obj");
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, on, "doit"), new ArrayList<>(), null);
		r.newObjectMethod(errors, meth);
		assertEquals(meth, r.get("test.repo.Obj.doit"));
	}

	@Test
	public void canAddAStructDefnToTheRepository() {
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "StructName"), true, new ArrayList<>());
		r.newStruct(errors, sd);
		assertEquals(sd, r.get("test.repo.StructName"));
	}

	@Test
	public void aPolyStructDefnAddsItsVars() {
		Repository r = new Repository();
		SolidName sn = new SolidName(pkg, "StructName");
		PolyType pa = new PolyType(pos, new SolidName(sn, "A"));
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, Arrays.asList(pa));
		r.newStruct(errors, sd);
		assertEquals(pa, r.get("test.repo.StructName.A"));
	}

	@Test
	public void canAddAUnionDefnToTheRepository() {
		Repository r = new Repository();
		UnionTypeDefn ud = new UnionTypeDefn(pos, true, new SolidName(pkg, "MyUnion"), new ArrayList<>());
		r.newUnion(errors, ud);
		assertEquals(ud, r.get("test.repo.MyUnion"));
	}

	@Test
	public void canAddAContractDeclToTheRepository() {
		Repository r = new Repository();
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		r.newContract(errors, cd);
		assertEquals(cd, r.get("test.repo.Ctr"));
	}

	@Test
	public void canAddADealDefnToTheRepository() { // it's just a struct
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.DEAL, new SolidName(pkg, "MyDeal"), true, new ArrayList<>());
		r.newStruct(errors, sd);
		assertEquals(sd, r.get("test.repo.MyDeal"));
	}

	@Test
	public void structFieldsAreGivenNamesAndAddedToTheRepo() {
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "TheStruct"), true, new ArrayList<>());
		ConsumeStructFields csf = new ConsumeStructFields(errors, r, new SimpleVarNamer(sd.name()), sd);
		r.newStruct(errors, sd);
		final StructField sf = new StructField(pos, sd, true, new TypeReference(pos, "A"), "x");
		csf.addField(sf);
		assertEquals("test.repo.TheStruct.x", sf.name().uniqueName());
		assertEquals(sf, r.get("test.repo.TheStruct.x"));
	}

	@Test
	public void canAddAHandlerToTheRepository() {
		Repository r = new Repository();
		HandlerImplements hi = new HandlerImplements(pos, pos, pos, null, new HandlerName(pkg, "X"), new TypeReference(pos, "Y"), false, new ArrayList<>());
		r.newHandler(errors, hi);
		assertEquals(hi, r.get("test.repo.X"));
	}

	@Test
	public void cannotAddAHandlerToTheRepositoryTwice() {
		Repository r = new Repository();
		HandlerImplements hi = new HandlerImplements(pos, pos, pos, null, new HandlerName(pkg, "X"), new TypeReference(pos, "Y"), false, new ArrayList<>());
		r.newHandler(errors, hi);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.X is defined multiple times: " + pos);
		}});
		r.newHandler(errors, hi);
	}

	@Test
	public void canAddAServiceToTheRepository() {
		Repository r = new Repository();
		ServiceDefinition svc = new ServiceDefinition(pos, pos, new CardName(pkg, "Foo"));
		r.newService(errors, svc);
		assertEquals(svc, r.get("test.repo.Foo"));
	}

	@Test
	public void canAddACardDefnToTheRepository() {
		Repository r = new Repository();
		CardDefinition card = new CardDefinition(pos, pos, new CardName(pkg, "Card"));
		r.newCard(errors, card);
		assertEquals(card, r.get("test.repo.Card"));
	}
	
	@Test
	public void canAddAUTPackageToTheRepository()  {
		Repository r = new Repository();
		UnitTestFileName utfn = new UnitTestFileName(pkg, "_ut_file");
		UnitTestPackage utp = new UnitTestPackage(pos, utfn);
		r.unitTestPackage(errors, utp);
		assertEquals(utp, r.get("test.repo._ut_file"));
	}
	
	@Test
	public void canFindAUnionInTheRepository() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(LoadBuiltins.trueT);
		ms.add(LoadBuiltins.falseT);
		UnionTypeDefn b = (UnionTypeDefn) r.findUnionWith(ms, unifier);
		assertEquals(LoadBuiltins.bool, b);
	}
	
	@Test
	public void anyIsAllowedWhenFindingAUnionInTheRepositoryIfItIsOneOfTheInputs() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(LoadBuiltins.number);
		ms.add(LoadBuiltins.any);
		Type t = r.findUnionWith(ms, unifier);
		assertEquals(LoadBuiltins.any, t);
	}
	
	@Test
	public void inOrderToMatchAUnionMustContainAllTheThings() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(LoadBuiltins.trueT);
		ms.add(LoadBuiltins.falseT);
		ms.add(LoadBuiltins.nil);
		assertNull(r.findUnionWith(ms, unifier));
	}
	
	@Test
	public void inOrderToMatchAUnionMustNotContainMoreThings() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(LoadBuiltins.trueT);
		assertEquals(LoadBuiltins.trueT, r.findUnionWith(ms, unifier ));
	}

	@Test
	public void allTheThingsMustBeTheRightThings() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(LoadBuiltins.trueT);
		ms.add(LoadBuiltins.nil);
		assertNull(r.findUnionWith(ms, unifier));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void unionsCanBeFormedOfPolyInstances() {
		Repository r = new Repository();
		LoadBuiltins.applyTo(errors, r);
		Set<Type> ms = new HashSet<>();
		ms.add(new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.bool)));
		ms.add(LoadBuiltins.nil);
		context.checking(new Expectations() {{
			oneOf(unifier).unify(with(any(Set.class))); will(returnValue(LoadBuiltins.bool));
		}});
		Type u = r.findUnionWith(ms, unifier);
		assertNotNull(u);
		assertTrue(u instanceof PolyInstance);
		PolyInstance pi = (PolyInstance) u;
		assertEquals(1, pi.getPolys().size());
		assertEquals(LoadBuiltins.bool, pi.getPolys().get(0));
	}
}
