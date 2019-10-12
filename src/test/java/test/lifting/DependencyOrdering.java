package test.lifting;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
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

	private FunctionDefinition quick(String name, FunctionDefinition... deps) {
		FunctionDefinition fn = function(name);
		visit(fn, deps);
		return fn;
	}

	private FunctionDefinition function(String name) {
		FunctionName fname = FunctionName.function(pos, pkg, name);
		FunctionDefinition fn = new FunctionDefinition(fname, 0);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		fn.intro(fi);
		return fn;
	}

	private void visit(FunctionDefinition fn, FunctionDefinition... deps) {
		lifter.visitFunction(fn);
		lifter.visitFunctionIntro(fn.intros().get(0));
		for (FunctionDefinition d : deps) {
			UnresolvedVar ref = new UnresolvedVar(pos, d.name().name);
			ref.bind(d);
			lifter.visitUnresolvedVar(ref, 0);
		}
		lifter.leaveFunction(fn);
	}

	private void assertOrder(String... fns) {
		List<FunctionGroup> ordering = lifter.resolve();
		CollectingNestedVariableReferences.assertOrder(ordering, fns);
	}
}
