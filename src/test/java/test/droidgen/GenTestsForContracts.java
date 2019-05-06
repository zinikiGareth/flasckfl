package test.droidgen;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
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
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;

public class GenTestsForContracts {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	DroidGenerator gen = new DroidGenerator(bce, new DroidBuilder());
	ByteCodeSink bccHandler = context.mock(ByteCodeSink.class, "Handler");
	ByteCodeSink bccService = context.mock(ByteCodeSink.class, "Service");
	ByteCodeSink bccImpl = context.mock(ByteCodeSink.class, "Impl");
	MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
	MethodDefiner dfe = context.mock(MethodDefiner.class, "dfe");
	MethodDefiner hMeth = context.mock(MethodDefiner.class, "hMeth");
	MethodDefiner iMeth = context.mock(MethodDefiner.class, "iMeth");
	MethodDefiner uMeth = context.mock(MethodDefiner.class, "uMeth");
	FunctionType sendReturnType = Type.function(loc, new PrimitiveType(loc, new SolidName(null, "Send")));
	RWTypedPattern stringArg = new RWTypedPattern(loc, new PrimitiveType(loc, new SolidName(null, "String")), loc, new VarName(loc, null, "s"));
	
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void allowAnythingToHappenToExprsWeDontCareAbout() {
		context.checking(new Expectations() {{
			allowing(bce).newClass("ContDecl");
			allowing(bccImpl).generateAssociatedSourceFile();
			allowing(bccHandler).generateAssociatedSourceFile();
			allowing(bccService).generateAssociatedSourceFile();
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
			allowing(ctor).nextLocal(); will(returnValue(1));
			allowing(dfe).nextLocal(); will(returnValue(1));
		}});
	}

	@Test
	public void testVisitingAnEmptyContractDefnGeneratesTheCorrectMinimumCode() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		gen.visitContractDecl(cd);
	}

	@Test
	public void testVisitingAContractDefnWithADownMethodGeneratesTheCorrectMemberDeclInTheCardImpl() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		checkDeclOfMethod("fred");
		context.checking(new Expectations() {{
			oneOf(hMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(iMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(iMeth).argument(J.OBJECT, "arg0");
			oneOf(hMeth).argument(J.OBJECT, "arg0");
		}});
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, ContractMethodDir.DOWN, FunctionName.function(loc, null, "fred"), new ArrayList<>(), sendReturnType, null));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAContractDefnWithADownMethodCanHaveAnArgument() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		checkDeclOfMethod("fred");
		context.checking(new Expectations() {{
			oneOf(hMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(hMeth).argument(J.OBJECT, "s");
			oneOf(iMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(iMeth).argument(J.OBJECT, "s");
			oneOf(hMeth).argument(J.OBJECT, "arg1");
			oneOf(iMeth).argument(J.OBJECT, "arg1");
		}});
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, ContractMethodDir.DOWN, FunctionName.function(loc, null, "fred"), Arrays.asList(stringArg), sendReturnType, null));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAnUpMethodInAContractDefnIsIgnoredInTheCardImplButPresentInTheServiceIntf() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		checkIntfDeclOfMethod("fred");
		context.checking(new Expectations() {{
			oneOf(uMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(uMeth).argument(J.OBJECT, "arg0");
		}});
		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, ContractMethodDir.UP, FunctionName.function(loc, null, "fred"), new ArrayList<>(), sendReturnType, null));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAContractDefnWithAnUpMethodCanHaveAnArgument() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		checkIntfDeclOfMethod("fred");
		context.checking(new Expectations() {{
			oneOf(uMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(uMeth).argument(J.OBJECT, "arg0");
			oneOf(uMeth).argument(J.OBJECT, "arg1");
		}});

		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		cd.addMethod(new RWContractMethodDecl(loc, true, ContractMethodDir.UP, FunctionName.function(loc, null, "fred"), Arrays.asList((Object)null), sendReturnType, null));
		gen.visitContractDecl(cd);
	}

	@Test
	public void testAServiceInterfaceCanTakeAHandlerAsItsArgumentAndItHasASensibleType() {
		checkCreationOfStruct();
		checkCreationOfStructCtor();
		checkCreationOfHandlerIntf();
		checkCreationOfServiceIntf();
		checkIntfDeclOfMethod("callMeBack");
		context.checking(new Expectations() {{
			oneOf(uMeth).argument(J.FLEVALCONTEXT, "from");
			oneOf(uMeth).argument(J.OBJECT, "h");
			oneOf(uMeth).argument(J.OBJECT, "arg1");
		}});

		RWContractDecl cd = new RWContractDecl(loc, loc, new SolidName(null, "ContDecl"), true);
		RWContractDecl hdlrType = new RWContractDecl(loc, loc, new SolidName(new PackageName("test"), "MyHandler"), true);
		RWTypedPattern handlerArg = new RWTypedPattern(loc, hdlrType, loc, new VarName(loc, null, "h"));
		cd.addMethod(new RWContractMethodDecl(loc, true, ContractMethodDir.UP, FunctionName.function(loc, null, "callMeBack"), Arrays.asList(handlerArg), sendReturnType, null));
		gen.visitContractDecl(cd);
	}

	public void checkCreationOfStruct() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("ContDecl$Impl"); will(returnValue(bccImpl));
			oneOf(bccImpl).superclass(J.CONTRACT_IMPL);
			oneOf(bccImpl).implementsInterface("ContDecl$Down");
			oneOf(bccImpl).makeAbstract();
		}});
	}

	public void checkCreationOfHandlerIntf() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("ContDecl$Down"); will(returnValue(bccHandler));
			oneOf(bccHandler).makeInterface();
			oneOf(bccHandler).addInnerClassReference(Access.PUBLICSTATICINTERFACE, "", "Down");
			oneOf(bccHandler).implementsInterface("org.ziniki.ziwsh.DownContract");
		}});
	}

	public void checkCreationOfServiceIntf() {
		context.checking(new Expectations() {{
			oneOf(bce).newClass("ContDecl$Up"); will(returnValue(bccService));
			oneOf(bccService).makeInterface();
			oneOf(bccService).addInnerClassReference(Access.PUBLICSTATICINTERFACE, "", "Up");
			oneOf(bccService).implementsInterface("org.ziniki.ziwsh.UpContract");
		}});
	}

	public void checkCreationOfStructCtor() {
		AVar dvar = new Var.AVar(ctor, J.IDESPATCHER, "despatcher");
		context.checking(new Expectations() {{
			oneOf(bccImpl).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.IDESPATCHER, "despatcher"); will(returnValue(dvar));
			oneOf(ctor).callSuper("void", J.CONTRACT_IMPL, "<init>", dvar); will(returnValue(expr));
			oneOf(ctor).returnVoid(); will(returnValue(expr));
		}});
	}

	private void checkDeclOfMethod(String name) {
		context.checking(new Expectations() {{
			oneOf(bccHandler).createMethod(false, J.OBJECT, name); will(returnValue(hMeth));
			oneOf(bccImpl).createMethod(false, J.OBJECT, name); will(returnValue(iMeth));
		}});
	}

	private void checkIntfDeclOfMethod(String name) {
		context.checking(new Expectations() {{
			oneOf(bccService).createMethod(false, J.OBJECT, name); will(returnValue(uMeth));
		}});
	}
}
