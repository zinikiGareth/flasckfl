package test.droidgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.droidgen.DroidAreaGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWContentString;
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.template.TemplateTraversor;
import org.jmock.Expectations;
import org.jmock.States;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.BoolConstExpr;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.ClassConstExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.StringConstExpr;
import org.zinutils.bytecode.Var.AVar;

public class AreaGenerationTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	ByteCodeSink bcc = context.mock(ByteCodeSink.class, "bcc");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner ah = context.mock(MethodDefiner.class, "ah");
	AVar card;
	AVar parent;
	IExpr expr = context.mock(IExpr.class, "expr");

	@Before
	public void prepareTest() {
		context.checking(new Expectations() {{
//			allowing(bcc).addInnerClassReference(with(any(Access.class)), with(any(String.class)), with(any(String.class)));
//			allowing(ctor).getBCC(); will(returnValue(bcc));
//			allowing(ctor).box(with(any(IExpr.class))); will(returnValue(expr));
			allowing(ctor).nextLocal(); will(returnValue(1));
			allowing(expr).flush();
		}});
		card = new AVar(ctor, "ACard", "card");
		parent = new AVar(ctor, "ParentArea", "parent");
	}

	@Test
	public void testAddHandlersDoesNotGetCreatedOrCalledWithNoEvents() {
		context.checking(new Expectations() {{
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
		DroidAreaGenerator gen = new DroidAreaGenerator(bcc, ctor, card, parent);
		Rewriter rewriter = new Rewriter(null, null, null);
		TemplateTraversor tt = new TemplateTraversor(rewriter, null);
		CardName cn = new CardName(new PackageName("test.it"), "MyCard");
		AreaName areaName = new AreaName(cn, "B1");
		List<Object> formats = new ArrayList<>();
		FunctionName dynamicFn = null;
		RWContentString cs = new RWContentString(loc, "hello", areaName, formats, dynamicFn);
		tt.handleFormatsAndEvents(null, Arrays.asList(gen), areaName, false, cs);
		gen.done();
	}

	@Test
	public void testAnEventHandlerGetsAdded() {
		BoolConstExpr bf = new BoolConstExpr(ah, false);
		StringConstExpr sc = new StringConstExpr(ah, "click");
		ClassConstExpr cc = new ClassConstExpr(ah, "doEcho");
		context.checking(new Expectations() {{
			final States ahGen = context.states("ahGen").startsAs("none");
			oneOf(bcc).createMethod(false, J.OBJECT, "_add_handlers"); when(ahGen.is("none")); then (ahGen.is("actions")); will(returnValue(ah));
			oneOf(ah).boolConst(false);	will(returnValue(bf));
			oneOf(ah).stringConst("click");	will(returnValue(sc));
			oneOf(ah).classConst("doEcho");	will(returnValue(cc));
			oneOf(ah).callSuper(JavaType.void_.getActual(), J.AREA, "addEventHandler", bf, sc, cc); when(ahGen.is("actions")); will(returnValue(expr));
			oneOf(ah).aNull(); will(returnValue(expr));
			oneOf(ah).returnObject(expr); when(ahGen.is("actions")); then (ahGen.is("done")); will(returnValue(expr));
			oneOf(ctor).myThis(); will(new ReturnNewVar(ctor, "B1", "this"));
			oneOf(ctor).callVirtual(with(J.OBJECT), with(any(AVar.class)), with("_add_handlers"), with(new IExpr[0])); will(returnValue(expr));
			oneOf(ctor).voidExpr(expr); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
		DroidAreaGenerator gen = new DroidAreaGenerator(bcc, ctor, card, parent);
		Rewriter rewriter = new Rewriter(null, null, null);
		TemplateTraversor tt = new TemplateTraversor(rewriter, null);
		CardName cn = new CardName(new PackageName("test.it"), "MyCard");
		AreaName areaName = new AreaName(cn, "B1");
		List<Object> formats = new ArrayList<>();
		FunctionName dynamicFn = null;
		FunctionName handlerFn = FunctionName.functionInCardContext(loc, cn, "doEcho");
		Object doCall = null;
		RWContentString cs = new RWContentString(loc, "hello", areaName, formats, dynamicFn);
		cs.handlers.add(new RWEventHandler(loc, "click", doCall, handlerFn));
		tt.handleFormatsAndEvents(null, Arrays.asList(gen), areaName, false, cs);
		gen.done();
	}

}
