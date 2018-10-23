package test.droidgen;

import java.util.ArrayList;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.jvm.J;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.IntConstExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class GenTestsForStructs {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());
	ByteCodeSink bccStruct = context.mock(ByteCodeSink.class);
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner dfe = context.mock(MethodDefiner.class, "dfe");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bccStruct).generateAssociatedSourceFile();
			allowing(expr);
			allowing(dfe).myThis(); will(new Action() {
				@Override
				public void describeTo(Description desc) {
					desc.appendText("return a new avar for this");
				}

				@Override
				public Object invoke(Invocation arg0) throws Throwable {
					return new Var.AVar(dfe, "", "");
				}
				
			});
			allowing(dfe).nextLocal(); will(returnValue(1));
		}});
	}

	@Test
	public void testNothingHappensIfWeDontWantToGenerateTheStruct() {
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Struct"), false);
		gen.visitStructDefn(sd);
	}

	@Test
	public void testVisitingAnEmptyStructDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructDFE();
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Struct"), true);
		gen.visitStructDefn(sd);
	}

	@Test
	public void testVisitingAStructDefnWithOneMemberAndNoInitGeneratesAnEmptySlot() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructEval(true);
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, J.OBJECTP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Boolean")), "f1"));
		gen.visitStructDefn(sd);
	}

	@Test
	public void testVisitingAStructDefnWithOneInitializedMemberGeneratesASlotWithTheValue() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructEval(false);
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, J.OBJECTP, "f1");
		checkAssignToField(ctor, "f1");
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, new SolidName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		gen.visitStructDefn(sd);
	}

	public void checkCreationOfStruct() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Struct"); will(returnValue(bccStruct));
			oneOf(bccStruct).superclass(J.FLAS_STRUCT);
		}});
	}

	public void checkCreationOfStructCtor() {
		context.checking(new Expectations() {{
			oneOf(ctor).nextLocal(); will(returnValue(5));
		}});
		Var cx = new AVar(ctor, J.FLEVALCONTEXT, "cx");
		context.checking(new Expectations() {{
			oneOf(bccStruct).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.FLEVALCONTEXT, "cx"); will(returnValue(cx));
			oneOf(ctor).callSuper("void", J.FLAS_STRUCT, "<init>"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	public void checkCreationOfStructEval(boolean withArg) {
		context.checking(new Expectations() {{
			oneOf(ctor).nextLocal(); will(returnValue(5));
		}});
		Var cx = new AVar(ctor, J.FLEVALCONTEXT, "cx");
		context.checking(new Expectations() {{
			oneOf(ctor).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(cx));
			oneOf(ctor).nextLocal(); will(returnValue(3));
			oneOf(ctor).nextLocal(); will(returnValue(4));
		}});
		Var av = new AVar(ctor, "[java.lang.Object", "args");
		Var ret = new AVar(ctor, "Struct", "ret");
		final IntConstExpr ice0 = new IntConstExpr(ctor, 0);
		context.checking(new Expectations() {{
			oneOf(bccStruct).createMethod(true, "Struct", "eval"); will(returnValue(ctor));
			oneOf(ctor).argument("[java.lang.Object", "args"); will(returnValue(av));
			oneOf(ctor).avar("Struct", "ret"); will(returnValue(ret));
			oneOf(ctor).makeNew("Struct", cx); will(returnValue(expr));
			oneOf(ctor).assign(ret, expr);
			if (withArg) {
				oneOf(ctor).getField(ret, "f1"); will(returnValue(expr));
				oneOf(ctor).intConst(0); will(returnValue(ice0));
				oneOf(ctor).arrayElt(av, ice0); will(returnValue(expr));
				oneOf(ctor).assign(expr, expr); will(returnValue(expr));
			}
			oneOf(ctor).returnObject(ret); will(returnValue(expr));
		}});
	}

	public void checkCreationOfStructDFE() {
		context.checking(new Expectations() {{
			oneOf(bccStruct).createMethod(false, "void", "_doFullEval"); will(returnValue(dfe));
			oneOf(dfe).argument(J.FLEVALCONTEXT, "cxt"); will(new ReturnNewVar(dfe, J.FLEVALCONTEXT, "cxt"));
			oneOf(dfe).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDefnOfField(NewMethodDefiner meth, JavaType type, String name) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
		IFieldInfo ret = context.mock(IFieldInfo.class, name);
		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
			oneOf(bccStruct).defineField(false, Access.PUBLIC, type, name); will(returnValue(ret));
			oneOf(ret).asExpr(meth); will(returnValue(fe));
			oneOf(meth).as(fe, J.OBJECT); will(returnValue(fe));
			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(any(IExpr[].class))); will(returnValue(expr));
			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}
	private void checkAssignToField(MethodDefiner meth, final String fld) {
		context.checking(new Expectations() {{
			oneOf(meth).nextLocal(); will(returnValue(8));
		}});
		Var iv = new AVar(meth, "java.lang.Object", "iv");
		context.checking(new Expectations() {{
			exactly(2).of(meth).myThis(); will(returnValue(iv));
			oneOf(meth).as(iv, J.OBJECT); will(returnValue(expr));
			oneOf(meth).as(expr, J.OBJECT); will(returnValue(expr));
			oneOf(meth).arrayOf(J.OBJECT, new ArrayList<>()); will(returnValue(expr));
			oneOf(meth).getField(iv, fld); will(returnValue(expr));
			oneOf(meth).classConst("PACKAGEFUNCTIONS$init_" + fld); will(returnValue(expr));
			oneOf(meth).callStatic(J.FLCLOSURE, J.FLCLOSURE, "obj", new IExpr[] { expr, expr, expr }); will(returnValue(expr));
			oneOf(meth).assign(expr, expr);
		}});
	}

}
