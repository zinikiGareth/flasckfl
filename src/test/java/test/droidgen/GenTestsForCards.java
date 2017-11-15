package test.droidgen;

import java.util.ArrayList;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class GenTestsForCards {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());
	ByteCodeSink bccCard = context.mock(ByteCodeSink.class, "cardClass");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bccCard).generateAssociatedSourceFile();
			allowing(bccCard).getCreatedName(); will(returnValue("Card"));
			allowing(ctor).nextLocal(); will(returnValue(1));
			allowing(ctor).myThis(); will(new ReturnNewVar(ctor, "Card", "this"));
			allowing(expr);
		}});
	}

	@Test
	public void testVisitingAnEmptyCardGeneratesTheCorrectMinimumCode() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		CardGrouping sd = new CardGrouping(loc, new CardName(null, "Card"), new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true));
		gen.visitCardGrouping(sd);
	}

	@Test
	public void testVisitingACardWithOneDataMemberAndNoInitGeneratesAnEmptySlot() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
//		checkDefnOfField(J.OBJECTP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Boolean")), "f1"));
		CardGrouping card = new CardGrouping(loc, new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
	}

	@Test
	public void testVisitingACardWithOneInitializedMemberGeneratesASlotWithTheValue() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
//		checkDefnOfField(J.OBJECTP, "f1");
		checkAssignToField(ctor, "f1");
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		CardGrouping card = new CardGrouping(loc, new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
	}

	@Test
	public void testCorrectGenerationOfContractWithNoVar() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkDefnOfContract("_C0", null);
		checkRegisterOfContract("_C0", null);
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		card.contracts.add(new ContractGrouping(new SolidName(null, "CtrDecl"), new CSName(cdName, "_C0"), null));
		gen.visitCardGrouping(card);
	}

	@Test
	public void testCorrectGenerationOfContractWithVar() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkDefnOfContract("_C0", "ce");
		checkRegisterOfContract("_C0", "ce");
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		card.contracts.add(new ContractGrouping(new SolidName(null, "CtrDecl"), new CSName(cdName, "_C0"), "ce"));
		gen.visitCardGrouping(card);
	}

	@Test
	public void testCorrectGenerationOfHandler() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkDefnOfContract("ActualHandler", null);
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(loc, cdName, sd);
		HandlerName hn = new HandlerName(cdName, "ActualHandler");
		card.handlers.add(new HandlerGrouping(new HandlerName(null, "ActualHandler"), new RWHandlerImplements(loc, loc, hn, new SolidName(null, "HandlerDecl"), true, new ArrayList<>())));
		gen.visitCardGrouping(card);
	}

	
	public void checkCreationOfCard() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Card"); will(returnValue(bccCard));
			oneOf(bccCard).superclass(J.FLASCK_CARD);
			oneOf(bccCard).inheritsField(true, Access.PUBLIC, J.WRAPPER, "_wrapper");
			oneOf(bccCard).inheritsField(true, Access.PUBLIC, J.DISPLAY_ENGINE, "_display");
		}});
	}

	public void checkCreationOfCardCtor() {
		context.checking(new Expectations() {{
			oneOf(bccCard).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.CARD_DESPATCHER, "despatcher"); will(new ReturnNewVar(ctor, J.CARD_DESPATCHER, "despatcher"));
			oneOf(ctor).argument(J.DISPLAY_ENGINE, "display"); will(new ReturnNewVar(ctor, J.DISPLAY_ENGINE, "display"));
			oneOf(ctor).callSuper(with("void"), with(J.FLASCK_CARD), with("<init>"), with(aNonNull(IExpr[].class))); will(returnValue(expr));
			oneOf(ctor).callSuper("void", J.FLASCK_CARD, "ready"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDefnOfContract(String ctrName, String called) {
		context.checking(new Expectations() {{
			oneOf(bccCard).addInnerClassReference(Access.PUBLICSTATIC, "Card", ctrName);
			if (called != null)
				oneOf(bccCard).defineField(false, Access.PROTECTED, new JavaType("Card$" + ctrName), called);
		}});
	}

	private void checkRegisterOfContract(String ctrName, String called) {
		context.checking(new Expectations() {{
			oneOf(ctor).makeNew(with("Card$_C0"), with(aNonNull(Expr.class))); will(returnValue(expr));
			oneOf(ctor).stringConst("CtrDecl");
			if (called != null) {
				oneOf(ctor).getField(called); will(returnValue(expr));
				oneOf(ctor).assign(with(any(IExpr.class)), with(any(IExpr.class))); will(returnValue(expr));
				oneOf(ctor).as(expr, J.CONTRACT_IMPL); will(returnValue(expr));
			} else {
				oneOf(ctor).as(expr, J.CONTRACT_IMPL);  will(returnValue(expr));
			}
			oneOf(ctor).callVirtual(with("void"), with(aNonNull(IExpr.class)), with("registerContract"), with(aNonNull(IExpr[].class)));
		}});
	}
	private void checkAssignToField(MethodDefiner meth, final String fld) {
		context.checking(new Expectations() {{
			oneOf(meth).stringConst(fld); will(returnValue(expr));
			oneOf(meth).classConst("PACKAGEFUNCTIONS$init_" + fld); will(returnValue(expr));
			oneOf(meth).callStatic(J.FLCLOSURE, J.FLCLOSURE, "simple", expr); will(returnValue(expr));
			oneOf(meth).callVirtual(with("void"), with(any(AVar.class)), with("setVar"), with(any(IExpr[].class))); will(returnValue(expr));
		}});
	}
}
