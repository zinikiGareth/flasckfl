package test.repository;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
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
}
