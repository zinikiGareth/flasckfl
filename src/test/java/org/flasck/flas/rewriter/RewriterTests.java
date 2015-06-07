package org.flasck.flas.rewriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.FLASStory;
import org.junit.Before;
import org.junit.Test;

public class RewriterTests {
	private final Rewriter rw = new Rewriter(new ErrorResult());
	private Scope scope;
	private Scope builtinScope;
	private ScopeEntry pkgEntry;
	
	@Before
	public void setup() {
		builtinScope = FLASStory.builtinScope();
		PackageDefn pd = new PackageDefn(builtinScope, "ME");
		scope = pd.innerScope();
		pkgEntry = pd.innerScope().outerEntry;
	}

	@Test
	public void testRewritingSomethingGloballyDefined() {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		cases.add(new FunctionCaseDefn(scope, "ME.f", new ArrayList<Object>(), new UnresolvedVar("Nil")));
		FunctionDefinition fn = new FunctionDefinition("ME.f", 0, cases);
		scope.define("f", "ME.f", fn);
		rw.rewrite(pkgEntry);
		fn = (FunctionDefinition) ((PackageDefn)builtinScope.get("ME")).innerScope().get("f");
		assertEquals("ME.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof AbsoluteVar);
		assertEquals("Nil", ((AbsoluteVar)fn.cases.get(0).expr).id);
	}

	@Test
	public void testRewritingAParameter() {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(new VarPattern("x"));
		cases.add(new FunctionCaseDefn(scope, "ME.f", args, new UnresolvedVar("x")));
		FunctionDefinition fn = new FunctionDefinition("ME.f", 1, cases);
		scope.define("f", "ME.f", fn);
		rw.rewrite(pkgEntry);
		fn = (FunctionDefinition) ((PackageDefn)builtinScope.get("ME")).innerScope().get("f");
		assertEquals("ME.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)fn.cases.get(0).expr).var);
	}
	
	@Test
	public void testRewritingANestedParameter() {
		Scope innerScope;
		{
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("x"));
			cases.add(new FunctionCaseDefn(scope, "ME.f", args, new StringLiteral("x")));
			FunctionDefinition fn = new FunctionDefinition("ME.f", 1, cases);
			scope.define("f", "ME.f", fn);
			innerScope = cases.get(0).innerScope();
		}
		{
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("y"));
			cases.add(new FunctionCaseDefn(scope, "ME.f_0.g", args, new UnresolvedVar("x")));
			FunctionDefinition fn = new FunctionDefinition("ME.f_0.g", 1, cases);
			innerScope.define("g", "ME.f.g", fn);
		}
		rw.rewrite(pkgEntry);
		FunctionDefinition fn = (FunctionDefinition) ((PackageDefn)builtinScope.get("ME")).innerScope().get("f");
		FunctionDefinition g = (FunctionDefinition) fn.cases.get(0).innerScope().get("g");
		assertEquals("ME.f_0.g", g.name);
		assertTrue(g.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)g.cases.get(0).expr).var);
	}
	
	// a state var
	// a contract var
	// inside a handler
	// event handlers
}
