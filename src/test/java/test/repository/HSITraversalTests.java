package test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class HSITraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final NumericLiteral number = new NumericLiteral(pos, "42", 2);
	final HSIVisitor v = context.mock(HSIVisitor.class);
	final FunctionName fname = FunctionName.function(pos, pkg, "f");
	final Traverser t = new Traverser(v).withHSI();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSimpleVarBindsItAndThenJustEvalsTheExpression() {
		FunctionDefinition fn = new FunctionDefinition(fname, 1);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		VarName vx = new VarName(pos, fname, "x");
		VarPattern vp = new VarPattern(pos, vx);
		CaptureAction slots = new CaptureAction(null);
		CaptureAction boundSlot = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn);
			oneOf(v).hsiArgs(with(any(List.class))); will(slots);
			oneOf(v).bind(with(any(Slot.class)), with("x")); will(boundSlot);
			oneOf(v).startInline(fi);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).endInline(fi);
			oneOf(v).leaveFunction(fn);
		}});
		HSIArgsTree tree = new HSIArgsTree(1);
		tree.consider(fi);
		tree.get(0).addVar(vp, fi);
		fn.bindHsi(tree);
		
		fi.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi);
		
		t.visitFunction(fn);
		context.assertIsSatisfied();
		
		Object allSlots = slots.get(0);
		assertTrue(allSlots instanceof List);
		assertEquals(boundSlot.get(0), ((List)allSlots).get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aConstantConstructorForcesATypeErrorCase() {
		FunctionDefinition fn = new FunctionDefinition(fname, 1);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		fi.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi);
		
		CaptureAction slots = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn);
			oneOf(v).hsiArgs(with(any(List.class))); will(slots);
			oneOf(v).switchOn(with(SlotMatcher.from(slots, 0)));
			oneOf(v).withConstructor("Nil");
			oneOf(v).startInline(fi);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).endInline(fi);
			oneOf(v).defaultCase();
			oneOf(v).errorNoCase();
			oneOf(v).endSwitch();
			oneOf(v).leaveFunction(fn);
		}});
		HSIArgsTree tree = new HSIArgsTree(1);
		tree.consider(fi);
		tree.get(0).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(tree);
		
		t.visitFunction(fn);
		
		Object allSlots = slots.get(0);
		assertTrue(allSlots instanceof List);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aTypeAnnotationBehavesMuchLikeAConstructorWithoutArgs() {
		FunctionDefinition fn = new FunctionDefinition(fname, 1);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		fi.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi);
		
		CaptureAction slots = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn);
			oneOf(v).hsiArgs(with(any(List.class))); will(slots);
			oneOf(v).switchOn(with(SlotMatcher.from(slots, 0)));
			oneOf(v).withConstructor("Number");
			oneOf(v).bind(with(SlotMatcher.from(slots, 0)), with("x"));
			oneOf(v).startInline(fi);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).endInline(fi);
			oneOf(v).defaultCase();
			oneOf(v).errorNoCase();
			oneOf(v).endSwitch();
			oneOf(v).leaveFunction(fn);
		}});
		HSIArgsTree tree = new HSIArgsTree(1);
		tree.consider(fi);
		tree.get(0).addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, fname, "x")), fi);
		fn.bindHsi(tree);
		
		t.visitFunction(fn);
		
		Object allSlots = slots.get(0);
		assertTrue(allSlots instanceof List);
	}
}
