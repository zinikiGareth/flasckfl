package test.droidgen;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
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
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
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
		RWStructDefn sd = new RWStructDefn(loc, new SolidName(null, "Struct"), false);
		gen.visitStructDefn(sd);
	}

	// The number of assertions in here suggests that we could break the code into functions that we could test separately
	@Test
	public void testVisitingAnEmptyStructDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructDFE();
		RWStructDefn sd = new RWStructDefn(loc, new SolidName(null, "Struct"), true);
		gen.visitStructDefn(sd);
	}

	@Test
	public void testVisitingAStructDefnWithOneMemberAndNoInitGeneratesAnEmptySlot() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, J.BOOLEANP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new SolidName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Boolean")), "f1"));
		gen.visitStructDefn(sd);
	}

	@Test
	// DROID-TODO: This should generate a call to the "init_f1" function
	public void testVisitingAStructDefnWithOneInitializedMemberGeneratesASlotWithTheValue() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfStructDFE();
		checkDefnOfField(dfe, J.INTP, "f1");
		RWStructDefn sd = new RWStructDefn(loc, new SolidName(null, "Struct"), true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "Number")), "f1", FunctionName.function(loc, null, "init_f1")));
		gen.visitStructDefn(sd);
	}

	public void checkCreationOfStruct() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("Struct"); will(returnValue(bccStruct));
			oneOf(bccStruct).superclass(J.FLAS_OBJECT);
		}});
	}

	public void checkCreationOfStructCtor() {
		context.checking(new Expectations() {{
			oneOf(bccStruct).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).callSuper("void", J.FLAS_OBJECT, "<init>"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	public void checkCreationOfStructDFE() {
		context.checking(new Expectations() {{
			oneOf(bccStruct).createMethod(false, "void", "_doFullEval"); will(returnValue(dfe));
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
			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}
}
