package test.lifting;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.lifting.FunctionGroupOrdering;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class DependencyOrdering {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	RepositoryLifter lifter = new RepositoryLifter();

	@Test
	public void aSingleFunctionIsObviouslyFirst() {
		quick("f");

		assertOrder("test.foo.f");
	}

	@Test
	public void methodsAreConsideredToo() {
		FunctionName fname = FunctionName.standaloneMethod(pos, pkg, "m");
		StandaloneMethod meth = new StandaloneMethod(new ObjectMethod(pos, fname, new ArrayList<>()));
		lifter.visitStandaloneMethod(meth);
		lifter.leaveStandaloneMethod(meth);
		
		assertOrder("test.foo.m");
	}

	@Test
	public void builtinsAreOKButJustIgnored() {
		quick("f", LoadBuiltins.length);
				
		assertOrder("test.foo.f");
	}

	@Test
	public void withNoDependenciesFunctionsAreInAlphaOrder() {
		quick("f");
		quick("g");
				
		assertOrder("test.foo.f", "test.foo.g");
	}

	@Test
	public void fDependsOnGSoComesAfterIt() {
		FunctionDefinition fnG = quick("g");
		quick("f", fnG);

		assertOrder("test.foo.g", "test.foo.f");
	}

	@Test
	public void aFunctionReferencingAMethodComesAfterIt() {
		FunctionName fname = FunctionName.standaloneMethod(pos, pkg, "m");
		StandaloneMethod fn = new StandaloneMethod(new ObjectMethod(pos, fname, new ArrayList<>()));
		lifter.visitStandaloneMethod(fn);
		lifter.leaveStandaloneMethod(fn);
		
		quick("f", fn);
		
		assertOrder("test.foo.m", "test.foo.f");
	}

	@Test
	public void transitiveDependenciesCanBePlacedInOrder() {
		FunctionDefinition fnG = quick("g");
		FunctionDefinition fnH = quick("h", fnG);
		quick("f", fnH);

		assertOrder("test.foo.g", "test.foo.h", "test.foo.f");
	}

	@Test
	public void mutualRecursionMakesAGroup() {
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		visit(fnF, fnG);
		visit(fnG, fnF);

		assertOrder("test.foo.f//test.foo.g");
	}

	@Test
	public void mutualRecursionCanGetMessy() { // f -> g; g -> f, h; h -> g: all one group
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		FunctionDefinition fnH = function("h");
		visit(fnF, fnG);
		visit(fnG, fnF, fnH);
		visit(fnH, fnG);

		assertOrder("test.foo.f//test.foo.g//test.foo.h");
	}

	@Test
	public void circularRecursionIsHandled() { // f -> g; g -> h; h -> f: all one group
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		FunctionDefinition fnH = function("h");
		visit(fnF, fnG);
		visit(fnG, fnH);
		visit(fnH, fnF);

		assertOrder("test.foo.f//test.foo.g//test.foo.h");
	}

	@Test
	public void circularRecursionCanAlsoDependOnOthers() { // f -> g; g -> h, k; h -> f: all one group; but k is easy
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		FunctionDefinition fnH = function("h");
		FunctionDefinition fnK = quick("k");
		visit(fnF, fnG);
		visit(fnG, fnH, fnK);
		visit(fnH, fnF);

		assertOrder("test.foo.k", "test.foo.f//test.foo.g//test.foo.h");
	}

	@Test
	public void aFunctionDependingOnACycleComesAtTheEndByItself() { // f -> g; g <-> h
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		FunctionDefinition fnH = function("h");
		visit(fnF, fnG);
		visit(fnG, fnH);
		visit(fnH, fnG);

		assertOrder("test.foo.g//test.foo.h", "test.foo.f");
	}

	@Test
	public void twoCyclesCanBeHandledEvenWithComplexConnections() { // f -> g, l; g -> h, k; h -> f, k, l: all one group; k <-> l
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function("g");
		FunctionDefinition fnH = function("h");
		FunctionDefinition fnK = function("k");
		FunctionDefinition fnL = function("l");
		visit(fnF, fnG, fnL);
		visit(fnG, fnH, fnK);
		visit(fnH, fnF, fnK, fnL);
		visit(fnK, fnL);
		visit(fnL, fnK);

		assertOrder("test.foo.k//test.foo.l", "test.foo.f//test.foo.g//test.foo.h");
	}

	@Test
	public void aFunctionDependingOnANestedVariableIsInTheSameGroupAsTheParentFunction() { // f x -> g; g -> x
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function(fnF.name(), "g");
		visit(fnF, fnG);

		lifter.visitFunction(fnG);
		lifter.visitFunctionIntro(fnG.intros().get(0));
		VarPattern vp = new VarPattern(pos, new VarName(pos, fnF.name(), "x"));
		vp.isDefinedBy(fnF);
		UnresolvedVar ref = new UnresolvedVar(pos, vp.name().var);
		ref.bind(vp);
		lifter.visitUnresolvedVar(ref, 0);
		lifter.leaveFunction(fnG);

		assertOrder("test.foo.f//test.foo.f.g");
	}

	@Test
	public void aPairOfNestedFunctionsWhereOneDoesNotDependOnANestedVariableWillComeInTwoGroups() { // f x -> g; g -> x; h
		FunctionDefinition fnF = function("f");
		FunctionDefinition fnG = function(fnF.name(), "g");
		FunctionDefinition fnH = function(fnF.name(), "h");
		visit(fnF, fnG);

		lifter.visitFunction(fnG);
		lifter.visitFunctionIntro(fnG.intros().get(0));
		VarPattern vp = new VarPattern(pos, new VarName(pos, fnF.name(), "x"));
		vp.isDefinedBy(fnF);
		UnresolvedVar ref = new UnresolvedVar(pos, vp.name().var);
		ref.bind(vp);
		lifter.visitUnresolvedVar(ref, 0);
		lifter.leaveFunction(fnG);

		visit(fnH);

		assertOrder("test.foo.f.h", "test.foo.f//test.foo.f.g");
	}

	@Test
	public void somethingWithTuples() {
		FunctionDefinition f = quick("f");
		UnresolvedVar uvf = new UnresolvedVar(pos, "f");
		uvf.bind(f);
		TupleAssignment ta = new TupleAssignment(Arrays.asList(new LocatedName(pos, "a"), new LocatedName(pos, "b")), FunctionName.function(pos, pkg, "_tuple_a"), FunctionName.function(pos, pkg, "a"), uvf);
		TupleMember ma = new TupleMember(pos, ta, 0, FunctionName.function(pos, pkg, "a"));
		TupleMember mb = new TupleMember(pos, ta, 1, FunctionName.function(pos, pkg, "b"));
		ta.addMember(ma);
		ta.addMember(mb);
		lifter.visitTuple(ta);
		lifter.visitUnresolvedVar((UnresolvedVar) ta.expr, 0);
		lifter.leaveTuple(ta);
		assertOrder("test.foo.f", "test.foo._tuple_a", "test.foo.a", "test.foo.b");
	}
	
	private FunctionDefinition quick(String name, RepositoryEntry... deps) {
		FunctionDefinition fn = function(name);
		visit(fn, deps);
		return fn;
	}

	private FunctionDefinition function(String name) {
		NameOfThing scope = pkg;
		return function(scope, name);
	}

	private FunctionDefinition function(NameOfThing scope, String name) {
		FunctionName fname = FunctionName.function(pos, scope, name);
		FunctionDefinition fn = new FunctionDefinition(fname, 0);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		fn.intro(fi);
		return fn;
	}

	private void visit(FunctionDefinition fn, RepositoryEntry... deps) {
		lifter.visitFunction(fn);
		lifter.visitFunctionIntro(fn.intros().get(0));
		for (RepositoryEntry d : deps) {
			UnresolvedVar ref = new UnresolvedVar(pos, ((FunctionName)d.name()).name);
			ref.bind(d);
			lifter.visitUnresolvedVar(ref, 0);
		}
		lifter.leaveFunction(fn);
	}

	private void assertOrder(String... fns) {
		FunctionGroupOrdering ordering = lifter.resolve();
		InsertorTests.assertOrder(ordering, fns);
	}
}
