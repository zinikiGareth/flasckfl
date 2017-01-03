package test.droidgen;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.HandlerName;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.ClassConstExpr;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IntConstExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;

public class GenTestsForHandlers {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(null, true, bce);
	ByteCodeSink bccHandler = context.mock(ByteCodeSink.class, "handler");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner eval = context.mock(MethodDefiner.class, "eval");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bccHandler).getCreatedName(); will(returnValue("Card"));
			allowing(expr);
			allowing(ctor).nextLocal(); will(returnValue(1));
			allowing(eval).nextLocal(); will(returnValue(1));
		}});
	}

	@Test
	public void testNothingHappensIfWeDontTurnOnGeneration() {
		DroidGenerator gen = new DroidGenerator(null, false, bce);
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, new HandlerName(new CardName(null, "Card"), "MyHandler"), new StructName(null, "Callback"), true, new ArrayList<>());
		gen.visitHandlerImpl(hi);
	}

	@Test
	public void testVisitingAMinimalInCardHandlerDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfNestedClass();
		checkCreationOfImplCtor();
		checkCreationOfEvalMethod();
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, new HandlerName(new CardName(null, "Card"), "MyHandler"), new StructName(null, "Callback"), true, new ArrayList<>());
		gen.visitHandlerImpl(hi);
	}

	public void checkCreationOfNestedClass() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Card$MyHandler"); will(returnValue(bccHandler));
			oneOf(bccHandler).superclass("Callback");
			oneOf(bccHandler).defineField(false, Access.PRIVATE, new JavaType("Card"), "_card");
			oneOf(bccHandler).addInnerClassReference(Access.PUBLICSTATIC, "Card", "MyHandler");
		}});
	}

	public void checkCreationOfImplCtor() {
		context.checking(new Expectations() {{
			oneOf(bccHandler).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.OBJECT, "card"); will(new ReturnNewVar(ctor, J.OBJECT, "card"));
			oneOf(ctor).callSuper("void", "Callback", "<init>"); will(returnValue(expr));
			oneOf(ctor).callStatic(with("org.flasck.android.FLEval"), with(J.OBJECT), with("full"), with(any(Expr[].class)));
			oneOf(ctor).castTo(null, "Card");
			oneOf(ctor).assign(with(aNull(FieldExpr.class)), with(aNull(IExpr.class)));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	public void checkCreationOfEvalMethod() {
		context.checking(new Expectations() {{
			oneOf(bccHandler).createMethod(true, J.OBJECT, "eval"); will(returnValue(eval));
			oneOf(eval).argument(J.OBJECT, "card"); will(new ReturnNewVar(ctor, J.OBJECT, "card"));
			oneOf(eval).argument("[" + J.OBJECT, "args"); will(new ReturnNewVar(eval, "[" + J.OBJECT, "args"));
			oneOf(eval).arraylen(with(any(Expr.class)));
			oneOf(eval).intConst(0); will(returnValue(new IntConstExpr(eval, 0)));
			oneOf(eval).classConst("Card$MyHandler"); will(returnValue(new ClassConstExpr(eval, "Card$MyHandler")));
			oneOf(eval).makeNew(with(J.FLCURRY), with(any(IExpr[].class)));
			oneOf(eval).returnObject(with(any(IExpr.class)));
			oneOf(eval).makeNew(with("Card$MyHandler"), with(any(IExpr[].class)));
			oneOf(eval).returnObject(with(any(IExpr.class)));
			oneOf(eval).ifOp(with(162), with(aNull(Expr.class)), with(any(Expr.class)), with(aNull(Expr.class)), with(aNull(Expr.class))); will(returnValue(expr));
		}});
	}
}
