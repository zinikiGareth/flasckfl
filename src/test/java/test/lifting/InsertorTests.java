package test.lifting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
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
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.repository.Traverser.VarMapping;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class InsertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	HSIVisitor hsi = context.mock(HSIVisitor.class);

	@Test
	public void aFunctionWithNestedVarsGetsThemInHSIArgs() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, vp);
		fn.nestedVars(ms);

		assertEquals(1, fn.slots().size());
	}

	@Test
	public void aFunctionWithNestedTypedVarsGetsThemInHSIArgs() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number"), new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, tp);
		fn.nestedVars(ms);

		assertEquals(1, fn.slots().size());
	}

	@Test
	public void theSlotsWillResultInBoundVarsOnVisitHSI() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, vp);
		fn.nestedVars(ms);
		
		List<Slot> slots = fn.slots();
		context.checking(new Expectations() {{
			oneOf(hsi).bind(slots.get(0), "x");
			oneOf(hsi).startInline(fi);
			// it doesn't have an actual expression so nothing comes out - this is fine for testing and won't happen in real life
			oneOf(hsi).endInline(fi);
		}});
		Traverser traverser = new Traverser(hsi).withHSI();
		traverser.visitHSI(fn, new VarMapping(), slots, fn.intros());
	}

	@Test
	public void theSlotsWillResultInBoundTypedVarsOnVisitHSI() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number"), new VarName(pos, nameF, "x"));
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, tp);
		fn.nestedVars(ms);
		
		List<Slot> slots = fn.slots();
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(slots.get(0));
			oneOf(hsi).withConstructor("Number");
			oneOf(hsi).bind(slots.get(0), "x");
			oneOf(hsi).startInline(fi);
			// it doesn't have an actual expression so nothing comes out - this is fine for testing and won't happen in real life
			oneOf(hsi).endInline(fi);
			oneOf(hsi).defaultCase();
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		Traverser traverser = new Traverser(hsi).withHSI();
		traverser.visitHSI(fn, new VarMapping(), slots, fn.intros());
	}

	@Test
	public void middlemenAreAlsoEnhanced() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "Number"), new VarName(pos, nameF, "x"));
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
		CollectingNestedVariableReferences.assertOrder(ordering, "test.foo.f.g.h", "test.foo.f.g");
		
		List<Slot> slots = fnG.slots();
		assertEquals(1, slots.size());
		
		context.checking(new Expectations() {{
			oneOf(hsi).bind(slots.get(0), "x");
			oneOf(hsi).startInline(fiG);
			oneOf(hsi).visitExpr(xr, 0);
			oneOf(hsi).visitUnresolvedVar(xr, 0);
			oneOf(hsi).endInline(fiG);
		}});
		Traverser traverser = new Traverser(hsi).withHSI();
		traverser.visitHSI(fnG, new VarMapping(), slots, fnG.intros());
	}
}
