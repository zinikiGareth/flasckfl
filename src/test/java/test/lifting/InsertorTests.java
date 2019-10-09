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
import org.flasck.flas.lifting.MappingStore;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.repository.Traverser.VarMapping;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
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
		Traverser traverser = new Traverser(hsi);
		traverser.visitHSI(fn, new VarMapping(), slots, fn.intros());

		assertEquals(1, slots.size());
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
		Traverser traverser = new Traverser(hsi);
		traverser.visitHSI(fn, new VarMapping(), slots, fn.intros());

		assertEquals(1, slots.size());
	}
}
