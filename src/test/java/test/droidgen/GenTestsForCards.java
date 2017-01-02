package test.droidgen;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.Var;
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
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.JavaInfo.Access;

public class GenTestsForCards {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(null, true, bce);
	ByteCodeSink bccCard = context.mock(ByteCodeSink.class);
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner onCreate = context.mock(MethodDefiner.class, "onCreate");
	MethodDefiner dfe = context.mock(MethodDefiner.class, "dfe");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(expr);
			allowing(dfe).myThis(); will(new ReturnNewVar(dfe, "MyClass", "this"));
			allowing(dfe).nextLocal(); will(returnValue(1));
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
//		checkCreationOfStructDFE();
		CardGrouping sd = new CardGrouping(new CardName(null, "Card"), new RWStructDefn(loc, new StructName(null, "Card"), true));
		gen.visitCardGrouping(sd);
	}

//	@Test
	public void testVisitingAStructDefnWithOneMemberAndNoInitGeneratesAnEmptySlot() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, Type.primitive(loc, new StructName(null, "Boolean")), "f1"));
		gen.visitStructDefn(sd);
	}

//	@Test
	// DROID-TODO: This should generate a call to the "init_f1" function
	public void testVisitingAStructDefnWithOneInitializedMemberGeneratesASlotWithTheValue() {
		checkCreationOfCard();
		checkCreationOfCardCtor();
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new StructName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, Type.primitive(loc, new StructName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		gen.visitStructDefn(sd);
	}

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

	public void checkCreationOfStructDFE() {
		context.checking(new Expectations() {{
			oneOf(bccCard).createMethod(false, "void", "_doFullEval"); will(returnValue(dfe));
			oneOf(dfe).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDefnOfField(NewMethodDefiner meth, String name) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
		IFieldInfo ret = context.mock(IFieldInfo.class, name);
		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
			oneOf(bccCard).defineField(false, Access.PUBLIC, J.OBJECT, name); will(returnValue(ret));
			oneOf(ret).asExpr(meth); will(returnValue(fe));
			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}
}
