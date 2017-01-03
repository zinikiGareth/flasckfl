package test.droidgen;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.HandlerName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
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
	DroidGenerator gen = new DroidGenerator(true, bce);
	ByteCodeSink bccHandler = context.mock(ByteCodeSink.class, "handler");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner eval = context.mock(MethodDefiner.class, "eval");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
//			allowing(bccHandler).getCreatedName(); will(returnValue("Card"));
			allowing(expr);
			allowing(ctor).nextLocal(); will(returnValue(1));
			allowing(eval).nextLocal(); will(returnValue(1));
			allowing(eval).aNull(); will(returnValue(expr));
		}});
	}

	@Test
	public void testNothingHappensIfWeDontTurnOnGeneration() {
		DroidGenerator gen = new DroidGenerator(false, bce);
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, new HandlerName(new CardName(null, "Card"), "MyHandler"), new StructName(null, "Callback"), true, new ArrayList<>());
		gen.visitHandlerImpl(hi);
	}

	@Test
	public void testVisitingAMinimalInCardHandlerDefnGeneratesTheCorrectMinimumCode() {
		String container = "Card";
		checkCreationOfNestedClass(container, true);
		checkCreationOfImplCtor(true);
		checkCreationOfEvalMethod(container, true);
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, new HandlerName(new CardName(null, container), "MyHandler"), new StructName(null, "Callback"), true, new ArrayList<>());
		gen.visitHandlerImpl(hi);
	}

	@Test
	public void testVisitingAMinimalNotInCardHandlerDefnGeneratesTheCorrectMinimumCode() {
		String container = "test.foo";
		checkCreationOfNestedClass(container, false);
		checkCreationOfImplCtor(false);
		checkCreationOfEvalMethod(container, false);
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, new HandlerName(new PackageName(container), "MyHandler"), new StructName(null, "Callback"), false, new ArrayList<>());
		gen.visitHandlerImpl(hi);
	}

	@Test
	public void testInCardLambdasInHandlerGeneratesSuitableVars() {
		String container = "Card";
		checkCreationOfNestedClass(container, true);
		checkCreationOfImplCtor(true);
		checkCreationOfEvalMethod(container, true);
		checkProcessingOfLambda("x");
		HandlerName hn = new HandlerName(new CardName(null, container), "MyHandler");
		ArrayList<HandlerLambda> lambdas = new ArrayList<>();
		lambdas.add(new HandlerLambda(loc, hn.uniqueName(), new RWStructDefn(loc, new StructName(null, "Foo"), true), "x"));
		RWHandlerImplements hi = new RWHandlerImplements(loc, loc, hn, new StructName(null, "Callback"), true, lambdas);
		gen.visitHandlerImpl(hi);
	}

	public void checkCreationOfNestedClass(String container, boolean insideCard) {
		context.checking(new Expectations() {{
			oneOf(bce).newClass(container + "$MyHandler"); will(returnValue(bccHandler));
			oneOf(bccHandler).superclass("Callback");
			if (insideCard) {
				oneOf(bccHandler).defineField(false, Access.PRIVATE, new JavaType("Card"), "_card");
			}
			// It seems a little odd that this is generated at all, but if we're not in a card?
			oneOf(bccHandler).addInnerClassReference(Access.PUBLICSTATIC, container, "MyHandler");
		}});
	}

	public void checkCreationOfImplCtor(boolean inCard) {
		context.checking(new Expectations() {{
			oneOf(bccHandler).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			if (inCard) {
				oneOf(ctor).argument(J.OBJECT, "card"); will(new ReturnNewVar(ctor, J.OBJECT, "card"));
			}
			oneOf(ctor).callSuper("void", "Callback", "<init>"); will(returnValue(expr));
			if (inCard) {
				oneOf(ctor).callStatic(with("org.flasck.android.FLEval"), with(J.OBJECT), with("full"), with(any(Expr[].class)));
				oneOf(ctor).castTo(null, "Card");
				oneOf(ctor).assign(with(aNull(FieldExpr.class)), with(aNull(IExpr.class)));
			}
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	public void checkCreationOfEvalMethod(String container, boolean inCard) {
		context.checking(new Expectations() {{
			oneOf(bccHandler).createMethod(true, J.OBJECT, "eval"); will(returnValue(eval));
			if (inCard) {
				oneOf(eval).argument(J.OBJECT, "card"); will(new ReturnNewVar(ctor, J.OBJECT, "card"));
			}
			oneOf(eval).argument("[" + J.OBJECT, "args"); will(new ReturnNewVar(eval, "[" + J.OBJECT, "args"));
			oneOf(eval).arraylen(with(any(Expr.class)));
			oneOf(eval).intConst(0); will(returnValue(new IntConstExpr(eval, 0)));
			oneOf(eval).classConst(container + "$MyHandler"); will(returnValue(new ClassConstExpr(eval, container + "$MyHandler")));
			oneOf(eval).makeNew(with(J.FLCURRY), with(any(IExpr[].class)));
			oneOf(eval).returnObject(with(any(IExpr.class)));
			oneOf(eval).makeNew(with(container + "$MyHandler"), with(any(IExpr[].class)));
			oneOf(eval).returnObject(with(any(IExpr.class)));
			oneOf(eval).ifOp(with(162), with(aNull(Expr.class)), with(any(Expr.class)), with(aNull(Expr.class)), with(aNull(Expr.class))); will(returnValue(expr));
		}});
	}

	public void checkProcessingOfLambda(String called) {
		context.checking(new Expectations() {{
			oneOf(bccHandler).defineField(false, Access.PRIVATE, JavaType.object_, called);
			oneOf(ctor).argument(J.OBJECT, called); will(new ReturnNewVar(ctor, J.OBJECT, called));
			oneOf(ctor).callStatic(with("org.flasck.android.FLEval"), with(J.OBJECT), with("head"), with(any(Expr[].class)));
			oneOf(ctor).assign(with(aNull(FieldExpr.class)), with(aNull(IExpr.class)));
			oneOf(eval).arrayElt(with(aNonNull(Expr.class)), with(aNonNull(IntConstExpr.class)));
			oneOf(eval).intConst(1); will(returnValue(new IntConstExpr(eval, 1)));
		}});
	}
}
