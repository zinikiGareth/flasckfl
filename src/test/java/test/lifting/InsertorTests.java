package test.lifting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.lifting.FunctionGroupOrdering;
import org.flasck.flas.lifting.MappingStore;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.PatternMatcher;

public class InsertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	HSIVisitor hsi = context.mock(HSIVisitor.class);

	@Test
	public void aFunctionWithNestedVarsGetsThemWhenTraversingPatterns() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, vp);
		fn.nestedVars(ms);

		Traverser t = new Traverser(hsi).withNestedPatterns();
		t.rememberCaller(fn);
		context.checking(new Expectations() {{
			oneOf(hsi).visitPattern(vp, true);
			oneOf(hsi).visitVarPattern(vp, true);
			oneOf(hsi).visitPatternVar(pos, vp.var);
			oneOf(hsi).leavePattern(vp, true);
		}});
		t.visitPatterns(fi);
	}

	@Test
	public void aFunctionWithNestedTypedVarsGetsThemWhenTraversingPatterns() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, tp);
		fn.nestedVars(ms);

		Traverser t = new Traverser(hsi).withNestedPatterns();
		t.rememberCaller(fn);
		context.checking(new Expectations() {{
			oneOf(hsi).visitPattern(tp, true);
			oneOf(hsi).visitTypedPattern(tp, true);
			oneOf(hsi).visitTypeReference(tp.type);
			oneOf(hsi).visitPatternVar(pos, tp.var.var);
			oneOf(hsi).leavePattern(tp, true);
		}});
		t.visitPatterns(fi);
	}

	@Test
	public void middlemenAreAlsoEnhanced() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fnG = new FunctionDefinition(nameG, 0);
		FunctionIntro fiG = new FunctionIntro(nameG, new ArrayList<>());
		UnresolvedVar xr = new UnresolvedVar(pos, "x");
		xr.bind(tp);
		fiG.cases().add(new FunctionCaseDefn(null, xr));
		fnG.intro(fiG);
		FunctionName nameH = FunctionName.function(pos, nameG, "h");
		FunctionDefinition fnH = new FunctionDefinition(nameH, 0);
		FunctionIntro fiH = new FunctionIntro(nameH, new ArrayList<>());
		fnH.intro(fiH);

		UnresolvedVar hr = new UnresolvedVar(pos, "h");
		hr.bind(fnH);
		
		RepositoryLifter lifter = new RepositoryLifter();
		lifter.visitFunction(fnG);
		lifter.visitFunctionIntro(fiG);
		lifter.visitUnresolvedVar(hr, 0);
		lifter.leaveFunction(fnG);
		lifter.visitFunction(fnH);
		lifter.visitFunctionIntro(fiH);
		lifter.visitUnresolvedVar(xr, 0);
		lifter.leaveFunction(fnH);
		FunctionGroupOrdering ordering = lifter.resolve();
		assertOrder(ordering, "test.foo.f.g.h", "test.foo.f.g");
		
		Traverser t = new Traverser(hsi).withNestedPatterns();
		t.rememberCaller(fnG);
		context.checking(new Expectations() {{
			oneOf(hsi).visitPattern(with(PatternMatcher.var("test.foo.f.x")), with(true));
			oneOf(hsi).visitVarPattern((VarPattern) with(PatternMatcher.var("test.foo.f.x")), with(true));
//			oneOf(hsi).visitVarPattern(vp, true);
			oneOf(hsi).visitPatternVar(pos, xr.var);
			oneOf(hsi).leavePattern(with(PatternMatcher.var("test.foo.f.x")), with(true));
		}});
		t.visitPatterns(fiG);
	}
	
	public static void assertOrder(FunctionGroups ordering, String... fns) {
		assertEquals(ordering.toString(), fns.length, ordering.size());
		int i = 0;
		for (FunctionGroup g : ordering) {
			assertEquals(fns[i++], assembleGroup(g));
		}
	}

	private static String assembleGroup(FunctionGroup grp) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (FunctionDefinition f : grp.functions()) {
			sb.append(sep);
			sb.append(f.name().uniqueName());
			sep = "//";
		}
		return sb.toString();
	}

}
