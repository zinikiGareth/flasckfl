package test.lifting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.TreeSet;

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
import org.flasck.flas.parsedForm.LogicHolder;
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

import flas.matchers.PatternMatcher;

public class InsertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null, null);
	PackageName pkg = new PackageName("test.foo");
	HSIVisitor hsi = context.mock(HSIVisitor.class);
	private final FunctionIntro intro = null;

	@Test
	public void aFunctionWithNestedVarsGetsThemWhenTraversingPatterns() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0, null);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore(nameF);
		ms.recordNestedVar(fi, null, vp);
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
		FunctionDefinition fn = new FunctionDefinition(nameG, 0, null);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore(nameF);
		ms.recordNestedVar(fi, null, tp);
		fn.nestedVars(ms);

		Traverser t = new Traverser(hsi).withNestedPatterns();
		t.rememberCaller(fn);
		context.checking(new Expectations() {{
			oneOf(hsi).visitPattern(tp, true);
			oneOf(hsi).visitTypedPattern(tp, true);
			oneOf(hsi).visitTypeReference(tp.type, true, -1);
			oneOf(hsi).visitPatternVar(pos, tp.var.var);
			oneOf(hsi).leavePattern(tp, true);
		}});
		t.visitPatterns(fi);
	}

	@Test
	public void middlemenAreAlsoEnhanced() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		FunctionDefinition fnF = new FunctionDefinition(nameF, 1, null);
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fnG = new FunctionDefinition(nameG, 0, null);
		FunctionName nameH = FunctionName.function(pos, nameG, "h");
		FunctionDefinition fnH = new FunctionDefinition(nameH, 0, null);

		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, nameF, "x"));
		tp.isDefinedBy(fnF);

		UnresolvedVar gr = new UnresolvedVar(pos, "g");
		gr.bind(fnG);
		FunctionIntro fiF = new FunctionIntro(nameF, new ArrayList<>());
		fiF.cases().add(new FunctionCaseDefn(intro, null, gr));
		fnF.intro(fiF);

		UnresolvedVar hr = new UnresolvedVar(pos, "h");
		hr.bind(fnH);
		FunctionIntro fiG = new FunctionIntro(nameG, new ArrayList<>());
		fiG.cases().add(new FunctionCaseDefn(intro, null, hr));
		fnG.intro(fiG);

		UnresolvedVar xr = new UnresolvedVar(pos, "x");
		xr.bind(tp);

		FunctionIntro fiH = new FunctionIntro(nameH, new ArrayList<>());
		fiG.cases().add(new FunctionCaseDefn(intro, null, xr));
		fnH.intro(fiH);

		RepositoryLifter lifter = new RepositoryLifter();
		lifter.visitFunction(fnF);
		lifter.visitUnresolvedVar(gr, 0);
		lifter.leaveFunction(fnF);
		lifter.visitFunction(fnG);
		lifter.visitFunctionIntro(fiG);
		lifter.visitUnresolvedVar(hr, 0);
		lifter.leaveFunction(fnG);
		lifter.visitFunction(fnH);
		lifter.visitFunctionIntro(fiH);
		lifter.visitUnresolvedVar(xr, 0);
		lifter.leaveFunction(fnH);
		lifter.enhanceAll();
		lifter.refhandlers();
		FunctionGroupOrdering ordering = lifter.resolve(new TreeSet<>());
		assertOrder(ordering, "test.foo.f//test.foo.f.g//test.foo.f.g.h");
		
		Traverser t = new Traverser(hsi).withNestedPatterns();
		t.rememberCaller(fnG);
		context.checking(new Expectations() {{
			oneOf(hsi).visitPattern(with(PatternMatcher.var("test.foo.f.x")), with(true));
			oneOf(hsi).visitVarPattern((VarPattern) with(PatternMatcher.var("test.foo.f.x")), with(true));
			oneOf(hsi).visitPatternVar(pos, xr.var);
			oneOf(hsi).leavePattern(with(PatternMatcher.var("test.foo.f.x")), with(true));
		}});
		t.visitPatterns(fiG);
	}
	
	public static void assertOrder(FunctionGroups ordering, String... groups) {
		assertEquals(ordering.toString(), groups.length, ordering.size());
		int i = 0;
		for (FunctionGroup g : ordering) {
			assertEquals(groups[i++], assembleGroup(g));
		}
	}

	private static String assembleGroup(FunctionGroup grp) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (LogicHolder f : grp.functions()) {
			sb.append(sep);
			sb.append(f.name().uniqueName());
			sep = "//";
		}
		return sb.toString();
	}

}
