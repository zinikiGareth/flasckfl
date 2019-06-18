package test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ConsumeStructFields;
import org.flasck.flas.repository.Repository;
import org.junit.Test;

public class RepositoryTests {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");

	@Test
	public void canAddAFunctionToTheRepository() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2);
		r.functionDefn(fn);
		assertEquals(fn, r.get("test.repo.fred"));
	}

	@Test(expected=DuplicateNameException.class)
	public void cannotAddAFunctionToTheRepositoryTwice() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2);
		r.functionDefn(fn);
		r.functionDefn(fn);
	}

	@Test
	public void canAddAFunctionArgToTheRepository() {
		Repository r = new Repository();
		final FunctionName fred = FunctionName.function(pos, pkg, "fred");
		VarPattern parm = new VarPattern(pos, new VarName(pos, fred, "a"));
		r.argument(parm);
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

	@Test(expected=DuplicateNameException.class)
	public void cannotAddALeadTupleMemberToTheRepositoryTwice() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_a");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "a"));
		vars.add(new LocatedName(pos, "x"));
		r.tupleDefn(vars, exprFnName, simpleExpr);
	}

	@Test(expected=DuplicateNameException.class)
	public void cannotAddASecondaryTupleMemberToTheRepositoryTwice() {
		Repository r = new Repository();
		putATupleIntoTheRepository(r);
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_x");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "x"));
		vars.add(new LocatedName(pos, "b"));
		r.tupleDefn(vars, exprFnName, simpleExpr);
	}

	public List<LocatedName> putATupleIntoTheRepository(Repository r) {
		FunctionName exprFnName = FunctionName.function(pos, pkg, "_tuple_a");
		List<LocatedName> vars = new ArrayList<>();
		vars.add(new LocatedName(pos, "a"));
		vars.add(new LocatedName(pos, "b"));
		vars.add(new LocatedName(pos, "c"));
		// Note: simpleExpr obviously isn't a tuple expr, but it's easy to write.  To typecheck, you would need something that returns 3 elements
		r.tupleDefn(vars, exprFnName, simpleExpr);
		return vars;
	}

	@Test
	public void canAddAStandaloneMethodToTheRepository() {
		Repository r = new Repository();
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "m"), new ArrayList<>());
		StandaloneMethod meth = new StandaloneMethod(om);
		r.newStandaloneMethod(meth);
		assertEquals(meth, r.get("test.repo.m"));
	}

	@Test(expected=DuplicateNameException.class)
	public void cannotAddAStandaloneMethodToTheRepositoryTwice() {
		Repository r = new Repository();
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "m"), new ArrayList<>());
		StandaloneMethod meth = new StandaloneMethod(om);
		r.newStandaloneMethod(meth);
		r.newStandaloneMethod(meth);
	}

	@Test(expected=DuplicateNameException.class)
	public void cannotAddAStandaloneMethodToTheRepositoryIfAFunctionIsAlreadyThere() {
		Repository r = new Repository();
		FunctionDefinition fn = new FunctionDefinition(FunctionName.function(pos, pkg, "fred"), 2);
		r.functionDefn(fn);
		ObjectMethod om = new ObjectMethod(pos, FunctionName.standaloneMethod(pos, pkg, "fred"), new ArrayList<>());
		StandaloneMethod meth = new StandaloneMethod(om);
		r.newStandaloneMethod(meth);
	}

	@Test
	public void canAddAObjectDefnToTheRepository() {
		Repository r = new Repository();
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), true, new ArrayList<>());
		r.newObject(od);
		assertEquals(od, r.get("test.repo.Obj"));
	}

	@Test
	public void canAddAnObjectCtorToTheRepository() {
		Repository r = new Repository();
		final SolidName on = new SolidName(pkg, "Obj");
		ObjectCtor ctor = new ObjectCtor(pos, FunctionName.objectCtor(pos, on, "simple"), new ArrayList<>());
		r.newObjectMethod(ctor);
		assertEquals(ctor, r.get("test.repo.Obj._ctor_simple"));
	}

	@Test
	public void canAddAnObjectMethodToTheRepository() {
		Repository r = new Repository();
		final SolidName on = new SolidName(pkg, "Obj");
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, on, "doit"), new ArrayList<>());
		r.newObjectMethod(meth);
		assertEquals(meth, r.get("test.repo.Obj.doit"));
	}

	@Test
	public void canAddAStructDefnToTheRepository() {
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "StructName"), true, new ArrayList<>());
		r.newStruct(sd);
		assertEquals(sd, r.get("test.repo.StructName"));
	}

	@Test
	public void canAddAUnionDefnToTheRepository() {
		Repository r = new Repository();
		UnionTypeDefn ud = new UnionTypeDefn(pos, true, new SolidName(pkg, "MyUnion"), new ArrayList<>());
		r.newUnion(ud);
		assertEquals(ud, r.get("test.repo.MyUnion"));
	}

	@Test
	public void canAddAContractDeclToTheRepository() {
		Repository r = new Repository();
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		r.newContract(cd);
		assertEquals(cd, r.get("test.repo.Ctr"));
	}

	@Test
	public void canAddADealDefnToTheRepository() { // it's just a struct
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.DEAL, new SolidName(pkg, "MyDeal"), true, new ArrayList<>());
		r.newStruct(sd);
		assertEquals(sd, r.get("test.repo.MyDeal"));
	}

	@Test
	public void structFieldsAreAddedToTheRepository() {
		Repository r = new Repository();
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "TheStruct"), true, new ArrayList<>());
		ConsumeStructFields csf = new ConsumeStructFields(r, (loc, t) -> new VarName(loc, sd.name(), t), sd);
		r.newStruct(sd);
		final StructField sf = new StructField(pos, true, new TypeReference(pos, "A"), "x");
		csf.addField(sf);
		assertEquals(sf, r.get("test.repo.TheStruct.x"));
	}

	@Test
	public void canAddAHandlerToTheRepository() {
		Repository r = new Repository();
		HandlerImplements hi = new HandlerImplements(pos, pos, pos, new HandlerName(pkg, "X"), new TypeReference(pos, "Y"), false, new ArrayList<>());
		r.newHandler(hi);
		assertEquals(hi, r.get("test.repo.X"));
	}

	@Test(expected=DuplicateNameException.class)
	public void cannotAddAHandlerToTheRepositoryTwice() {
		Repository r = new Repository();
		HandlerImplements hi = new HandlerImplements(pos, pos, pos, new HandlerName(pkg, "X"), new TypeReference(pos, "Y"), false, new ArrayList<>());
		r.newHandler(hi);
		r.newHandler(hi);
	}

	@Test
	public void canAddAServiceToTheRepository() {
		Repository r = new Repository();
		ServiceDefinition svc = new ServiceDefinition(pos, pos, new CardName(pkg, "Foo"));
		r.newService(svc);
		assertEquals(svc, r.get("test.repo.Foo"));
	}

	@Test
	public void canAddACardDefnToTheRepository() {
		Repository r = new Repository();
		CardDefinition card = new CardDefinition(pos, pos, new CardName(pkg, "Card"));
		r.newCard(card);
		assertEquals(card, r.get("test.repo.Card"));
	}
}
