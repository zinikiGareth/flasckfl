package test.droidgen;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.BooleanLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.droidgen.IMethodGenerationContext;
import org.flasck.flas.droidgen.VarHolder;
import org.flasck.flas.hsie.PushArgumentTraverser;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.BoolConstExpr;
import org.zinutils.bytecode.ClassConstExpr;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IntConstExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.MethodInvocation;
import org.zinutils.bytecode.StringConstExpr;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.UtilException;

@Ignore
public class AppendTestsForFunctionsLookingAtFunctionPosition {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	MethodDefiner meth = context.mock(MethodDefiner.class);
	private HSIEForm form = null;
	IMethodGenerationContext cxt = context.mock(IMethodGenerationContext.class);
	private VarHolder vh;
	PushArgumentTraverser<IExpr> dpa;
	private FunctionName funcName = FunctionName.function(loc, null, "func");
	@SuppressWarnings("unchecked")
	private OutputHandler<IExpr> op = context.mock(OutputHandler.class);
	
	@Before
	public void setup() {
		 vh = new VarHolder();
		 context.checking(new Expectations() {{
			allowing(cxt).getVarHolder(); will(returnValue(vh));
			allowing(cxt).getMethod(); will(returnValue(meth));
		 }});
		 dpa = new PushArgumentTraverser<>(form, cxt);
	}
	@Test
	public void testWeCanPushABoolean() {
		BoolConstExpr bce = new BoolConstExpr(meth, true);
		context.checking(new Expectations() {{
			oneOf(meth).boolConst(true); will(returnValue(bce));
			oneOf(op).result(bce);
		}});
		dpa.visit(new PushBool(loc, new BooleanLiteral(loc, true)), op);
	}

	@Test
	public void testWeCanPushAnInt() {
		IntConstExpr bce = new IntConstExpr(meth, 12);
		MethodInvocation mi = new MethodInvocation(meth, null, null, null, null, null, null);
		context.checking(new Expectations() {{
			oneOf(meth).intConst(12); will(returnValue(bce));
			oneOf(meth).callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", bce); will(returnValue(mi));
			oneOf(op).result(mi);
		}});
		dpa.visit(new PushInt(loc, 12), op);
	}

	@Test
	public void testWeCanPushAString() {
		StringConstExpr sce = new StringConstExpr(meth, "hello");
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(sce));
			oneOf(op).result(sce);
		}});
		dpa.visit(new PushString(loc, new StringLiteral(loc, "hello")), op);
	}

	@Test
	public void testWeCanPushAnExternalRepresentingAType() {
		ClassConstExpr cce = new ClassConstExpr(meth, "demo.ziniki.Account");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("demo.ziniki.Account"); will(returnValue(cce));
			oneOf(op).result(cce);
		}});
		SolidName sn = new SolidName(new PackageName("demo.ziniki"), "Account");
		RWStructDefn sd = new RWStructDefn(loc, FieldsDefn.FieldsType.STRUCT, sn, true);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "String")), "id"));
		dpa.visitExternal(new PackageVar(loc, sn, sd), op);
	}

	@Test
	public void testWeCanPushAVariable() {
		Var var = new Var(1);
		context.checking(new Expectations() {{
			allowing(meth).nextLocal();
		}});
		AVar av = new AVar(meth, "12", "x");
		vh.put(var, av);
		context.checking(new Expectations() {{
			oneOf(op).result(av);
		}});
		dpa.visit(new PushVar(loc, new VarInSource(var, loc, "x"), null), op);
	}

	@Test(expected=UtilException.class)
	public void testWeCannotPushAVariableWeHaventDefinedYet() {
		Var var = new Var(1);
		dpa.visit(new PushVar(loc, new VarInSource(var, loc, "x"), null), op);
	}

	@Test
	public void testWeCanPushATemplateListVariable() {
		FieldExpr fe1 = new FieldExpr(meth, null, "SomeClass", "", "_src_my_var");
		FieldExpr fe2 = new FieldExpr(meth, null, "SourceClass", "", "my_var");
		context.checking(new Expectations() {{
			oneOf(meth).getField("_src_my_var"); will(returnValue(fe1));
			oneOf(meth).getField(fe1, "my_var"); will(returnValue(fe2));
			oneOf(op).result(fe2);
		}});
		IterVar iterVar = new IterVar(loc, new CardName(new PackageName("test"), "Card"), "my_var");
		dpa.visit(new PushTLV(loc, new TemplateListVar(loc, funcName, iterVar)), op);
	}

	@Test
	public void testWeCanPushAReferenceToTheCardStateFromTheClassItself() {
		context.checking(new Expectations() {{
			allowing(meth).nextLocal();
		}});
		AVar av = new AVar(meth, "12", "x");
		context.checking(new Expectations() {{
			oneOf(meth).myThis(); will(returnValue(av));
			oneOf(op).result(av);
		}});
		dpa.visit(new PushCSR(loc, new CardStateRef(loc, false)), op);
	}

	@Test
	public void testWeCanPushAReferenceToTheCardMemberFromAHangerOn() {
		FieldExpr fe1 = new FieldExpr(meth, null, "HandlerClass", "", "_card");
		context.checking(new Expectations() {{
			oneOf(meth).getField("_card"); will(returnValue(fe1));
			oneOf(op).result(fe1);
		}});
		dpa.visit(new PushCSR(loc, new CardStateRef(loc, true)), op);
	}


}
