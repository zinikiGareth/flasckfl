package test.droidgen;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.Type;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class GenTestsForCards {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(null, true, bce);
	ByteCodeSink bccCard = context.mock(ByteCodeSink.class, "cardClass");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner onCreate = context.mock(MethodDefiner.class, "onCreate");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bccCard).getCreatedName(); will(returnValue("Card"));
			allowing(onCreate).myThis(); will(new ReturnNewVar(onCreate, "Card", "this"));
			allowing(expr);
			allowing(onCreate).nextLocal(); will(returnValue(1));
		}});
	}

	@Test
	public void testNothingHappensIfWeDontTurnOnGeneration() {
		DroidGenerator gen = new DroidGenerator(null, false, bce);
		CardGrouping sd = new CardGrouping(new CardName(null, "Card"), new RWStructDefn(loc, new StructName(null, "Card"), true));
		gen.visitCardGrouping(sd);
	}

	@Test
	public void testVisitingAnEmptyStructDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfCardOnCreate();
		CardGrouping sd = new CardGrouping(new CardName(null, "Card"), new RWStructDefn(loc, new StructName(null, "Card"), true));
		gen.visitCardGrouping(sd);
	}

	@Test
	public void testVisitingAStructDefnWithOneMemberAndNoInitGeneratesAnEmptySlot() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfCardOnCreate();
		checkDefnOfField(J.BOOLEANP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, Type.primitive(loc, new StructName(null, "Boolean")), "f1"));
		CardGrouping card = new CardGrouping(new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
	}

	@Test
	// DROID-TODO: This should generate a call to the "init_f1" function
	public void testVisitingAStructDefnWithOneInitializedMemberGeneratesASlotWithTheValue() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfCardOnCreate();
		checkDefnOfField(J.INTP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Card"), true);
		sd.addField(new RWStructField(loc, false, Type.primitive(loc, new StructName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		CardGrouping card = new CardGrouping(new CardName(null, "Card"), sd);
		gen.visitCardGrouping(card);
	}

	@Test
	public void testCorrectGenerationOfContractWithNoVar() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfCardOnCreate();
		checkDefnOfContract("_C0", null);
		checkRegisterOfContract("_C0", null);
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Card"), true);
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(cdName, sd);
		card.contracts.add(new ContractGrouping("CtrDecl", new CSName(cdName, "_C0"), null));
		gen.visitCardGrouping(card);
	}

	@Test
	public void testCorrectGenerationOfContractWithVar() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfCardOnCreate();
		checkDefnOfContract("_C0", "ce");
		checkRegisterOfContract("_C0", "ce");
//		checkDefnOfField(J.BOOLEANP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Card"), true);
//		sd.addField(new RWStructField(loc, false, Type.primitive(loc, new StructName(null, "Boolean")), "f1"));
		CardName cdName = new CardName(null, "Card");
		CardGrouping card = new CardGrouping(cdName, sd);
		card.contracts.add(new ContractGrouping("CtrDecl", new CSName(cdName, "_C0"), "ce"));
		gen.visitCardGrouping(card);
	}

	// testCorrectGenerationOfHandler
	
	public void checkCreationOfCard() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Card"); will(returnValue(bccCard));
			oneOf(bccCard).superclass(J.FLASCK_ACTIVITY);
			oneOf(bccCard).inheritsField(false, Access.PUBLIC, J.WRAPPER, "_wrapper");
		}});
	}

	public void checkCreationOfCardCtor() {
		context.checking(new Expectations() {{
			oneOf(bccCard).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).callSuper("void", J.FLASCK_ACTIVITY, "<init>"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	public void checkCreationOfCardOnCreate() {
		context.checking(new Expectations() {{
			oneOf(bccCard).createMethod(false, "void", "onCreate"); will(returnValue(onCreate));
			oneOf(onCreate).argument("android.os.Bundle", "savedState"); will(new ReturnNewVar(onCreate, "Bundle", "savedState"));
			oneOf(onCreate).setAccess(Access.PROTECTED);
			oneOf(onCreate).callSuper(with("void"), with(J.FLASCK_ACTIVITY), with("onCreate"), with(any(IExpr[].class))); will(returnValue(expr));
			oneOf(onCreate).callSuper("void", J.FLASCK_ACTIVITY, "ready"); will(returnValue(expr));
			oneOf(onCreate).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDefnOfField(JavaType type, String name) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
		IFieldInfo ret = context.mock(IFieldInfo.class, name);
//		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
			oneOf(bccCard).defineField(false, Access.PROTECTED, type, name); will(returnValue(ret));
//			oneOf(ret).asExpr(meth); will(returnValue(fe));
//			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
//			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}

	private void checkDefnOfContract(String ctrName, String called) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
//		IFieldInfo ret = context.mock(IFieldInfo.class, name);
//		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
			oneOf(bccCard).addInnerClassReference(Access.PUBLICSTATIC, "Card", ctrName);
			if (called != null)
				oneOf(bccCard).defineField(false, Access.PROTECTED, new JavaType("Card$" + ctrName), called);
//			oneOf(ret).asExpr(meth); will(returnValue(fe));
//			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
//			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}

	private void checkRegisterOfContract(String ctrName, String called) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
//		IFieldInfo ret = context.mock(IFieldInfo.class, name);
//		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
			oneOf(onCreate).makeNew(with("Card$_C0"), with(aNonNull(Expr.class))); will(returnValue(expr));
			oneOf(onCreate).stringConst("CtrDecl");
			if (called != null) {
				oneOf(onCreate).getField(called);
				oneOf(onCreate).assign(with(aNull(FieldExpr.class)), with(any(IExpr.class)));
				oneOf(onCreate).as(null, J.CONTRACT_IMPL);
			} else
				oneOf(onCreate).as(expr, J.CONTRACT_IMPL);
			oneOf(onCreate).callVirtual(with("void"), with(aNonNull(IExpr.class)), with("registerContract"), with(aNonNull(IExpr[].class)));
//			oneOf(ret).asExpr(meth); will(returnValue(fe));
//			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
//			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}
}
