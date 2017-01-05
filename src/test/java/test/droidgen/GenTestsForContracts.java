package test.droidgen;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.types.Type;
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
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class GenTestsForContracts {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(true, bce);
	ByteCodeSink bccContract = context.mock(ByteCodeSink.class);
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner dfe = context.mock(MethodDefiner.class, "dfe");
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
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
	public void testNothingHappensIfWeDontTurnOnGeneration() {
		DroidGenerator gen = new DroidGenerator(false, bce);
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		gen.visitContractDecl(cd);
	}

	@Test
	public void testVisitingAnEmptyContractDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		gen.visitContractDecl(cd);
	}

	@Test
	public void testVisitingAContractDefnWithADownMethodGeneratesTheCorrectMemberDecl() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkDeclOfMethod("fred");
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, "down", FunctionName.function(loc, null, "fred"), new ArrayList<>(), Type.function(loc, new RWStructDefn(loc, new SolidName(null, "Send"), false))));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAContractDefnWithADownMethodCanHaveAnArgument() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkDeclOfMethod("fred");
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, "down", FunctionName.function(loc, null, "fred"), Arrays.asList((Object)null), Type.function(loc, new RWStructDefn(loc, new SolidName(null, "Send"), false))));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAnUpMethodInAContractDefnIsIgnored() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, "up", FunctionName.function(loc, null, "fred"), new ArrayList<>(), Type.function(loc, new RWStructDefn(loc, new SolidName(null, "Send"), false))));
		gen.visitContractDecl(cd);
	}

	public void checkCreationOfStruct() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("ContDecl"); will(returnValue(bccContract));
			oneOf(bccContract).superclass(J.CONTRACT_IMPL);
			oneOf(bccContract).makeAbstract();
		}});
	}

	public void checkCreationOfStructCtor() {
		context.checking(new Expectations() {{
			oneOf(bccContract).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).callSuper("void", J.CONTRACT_IMPL, "<init>"); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDeclOfMethod(String name) {
		// I expect this will eventually need to be more public, eg. stored in a map or something
//		IFieldInfo ret = context.mock(IFieldInfo.class, name);
//		FieldExpr fe = new FieldExpr(meth, null, null, "", name);
		context.checking(new Expectations() {{
//			oneOf(bccContract).defineField(false, Access.PUBLIC, J.OBJECT, name); will(returnValue(ret));
			oneOf(bccContract).createMethod(false, J.OBJECT, name); // will(returnValue(ret));
//			oneOf(ret).asExpr(meth); will(returnValue(fe));
//			oneOf(meth).callVirtual(with(J.OBJECT), with(aNonNull(FieldExpr.class)), with("_fullOf"), with(new Expr[] { fe })); will(returnValue(expr));
//			oneOf(meth).assign(fe, expr); will(returnValue(expr));
		}});
	}
}
