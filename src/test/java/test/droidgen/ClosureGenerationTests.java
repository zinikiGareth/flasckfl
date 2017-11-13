package test.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.droidgen.VarHolder;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.hsie.ClosureTraverser;
import org.flasck.flas.hsie.NextVarFactory;
import org.flasck.flas.hsie.VarFactory;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.jvm.J;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IntConstExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;

@Ignore
public class ClosureGenerationTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	@SuppressWarnings("unchecked")
	GenerationContext<IExpr> genCxt = context.mock(GenerationContext.class);
	ByteCodeSink bcc = context.mock(ByteCodeSink.class, "bcc");
	MethodDefiner meth = context.mock(MethodDefiner.class, "meth");
	Var cxt;
	IExpr expr = context.mock(IExpr.class, "expr");
	@SuppressWarnings("unchecked")
	OutputHandler<IExpr> op = context.mock(OutputHandler.class);

	@Before
	public void prepareTest() {
		context.checking(new Expectations() {{
			allowing(bcc).addInnerClassReference(with(any(Access.class)), with(any(String.class)), with(any(String.class)));
			allowing(meth).getBCC(); will(returnValue(bcc));
			allowing(meth).box(with(any(IExpr.class))); will(returnValue(expr));
			allowing(meth).nextLocal(); will(returnValue(1));
		}});
		cxt = new Var.AVar(meth, "Object", "cxt");
		context.checking(new Expectations() {{
			allowing(genCxt).getMethod(); will(returnValue(meth));
			allowing(genCxt).getCxtArg(); will(returnValue(cxt));
		}});
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testATupleProducesAClosureCallingFLEvalDOTTuple() {
		context.checking(new Expectations() {{
			oneOf(meth).intConst(42); will(returnValue(new IntConstExpr(meth, 42)));
			oneOf(meth).callStatic(with(J.INTEGER), with(J.INTEGER), with("valueOf"), with(any(IExpr[].class))); will(returnValue(expr));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).classConst(J.FLEVAL + "$Tuple"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(J.FLCLOSURE, expr, expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		ClosureCmd closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "tuple"), BuiltinOperation.TUPLE);
		closure.push(loc, hdc1, null);
		closure.push(loc, new NumericLiteral(loc, 42), null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}

	// NOT CLEAR: if this is a real case - doesn't it need arguments?  Would they be closures, or would it have to be a closure itself
	@Test
	public void testReturningATuple() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.FLEVAL + "$Tuple"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "tuple"), BuiltinOperation.TUPLE);
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null, op);
	}

	// NOT CLEAR: if this is the correct generated code or not for this case
	// This is based on the golden test typeof, and the question is "what happens if you return a type, specifically Number"?
	@Test
	public void testTypeOfPrimitiveReturnsAClass() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.BUILTINPKG + ".Number"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		SolidName number = new SolidName(null, "Number");
		PackageVar hdc1 = new PackageVar(loc, number, new PrimitiveType(loc, number));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null, op);
	}

	// NOT CLEAR: if this is the correct generated code or not for this case
	// This is based on the golden test typeof, and the question is "what happens if you return a type, specifically a Card"?
	@Test
	public void testTypeOfACardClassReturnsTheClassCard() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.golden$MyCard"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardGrouping(loc, cn, null));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null, op);
	}

	// This case is where we create an instance of a handler within a card to pass to a method
	// I think (looking at the code for ObjectReference) that there is another case, untested in either golden or unit tests
	@SuppressWarnings("unchecked")
	@Test
	public void testAnObjectReferenceCanFormAClosureForANestedClass() {
		context.checking(new Expectations() {{
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).classConst("test.golden.MyCard$Handler"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(J.FLCLOSURE, expr, expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		ClosureCmd closure = form.createClosure(loc);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		HandlerName hn = new HandlerName(cn, "Handler");
		PackageVar hdc1 = new PackageVar(loc, hn, new ObjectReference(loc, hn));
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}

	// There are probably a lot of cases where we call methods on cards from other methods on cards,
	// but a typical example is calling an event handling method from an event handler
	// Because of that, we end up with a curried function call
	@SuppressWarnings("unchecked")
	@Test
	public void testEventHandlersCanCallEventMethods() {
		IntConstExpr ice = new IntConstExpr(meth, 2);
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.golden.MyCard$eventHandler"); will(returnValue(expr));
			oneOf(meth).intConst(2); will(returnValue(ice));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(J.FLCURRY, expr, ice, expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		ClosureCmd closure = form.createClosure(loc);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		CardFunction cf = new CardFunction(loc, cn, "eventHandler");
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "curry"), cf);
		PackageVar hdc2 = new PackageVar(loc, FunctionName.functionInCardContext(loc, cn, cf.function), cf);
		closure.push(loc, hdc1, null);
		closure.push(loc, hdc2, null);
		closure.push(loc, new NumericLiteral(loc, 2), null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}

	// Card function that returns a card member
	@Test
	public void testReturningACardMemberFromAMethodOnTheCard() {
		context.checking(new Expectations() {{
			oneOf(meth).myThis(); will(new ReturnNewVar(meth, "fred", "bar"));
			oneOf(meth).stringConst("var");
			oneOf(meth).callVirtual(with(J.OBJECT), with(any(AVar.class)), with("getVar"), (IExpr[]) with(any(Object[].class))); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.CARD, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardMember(loc, cn, "var", new PrimitiveType(loc, new SolidName(null, "String"))));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null, op);
	}

	// Function on a Card associate that returns a card member
	@Test
	public void testReturningACardMemberFromAMethodOnACardAssociate() {
		context.checking(new Expectations() {{
			oneOf(meth).getField("_card"); will(returnValue(new FieldExpr(meth, expr, "fred", "bar", "_card")));
			oneOf(meth).stringConst("var");
			oneOf(meth).callVirtual(with(J.OBJECT), with(any(FieldExpr.class)), with("getVar"), (IExpr[]) with(any(Object[].class))); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.AREA, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardMember(loc, cn, "var", new PrimitiveType(loc, new SolidName(null, "String"))));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null, op);
	}
	
	// 3 function cases: closure w args/no args  & no closure w/no args
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanReturnAClosureForAFunctionCallWithArgsInAClosure() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.golden.PACKAGEFUNCTIONS$callMe"); will(returnValue(expr));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(any(IExpr[].class))); will(returnValue(result));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
			oneOf(op).result(result);
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		ClosureCmd closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 1, false));
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanReturnAClosureForAFunctionCallWithNoArgsInAClosure() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic("test.golden.PACKAGEFUNCTIONS$callMe", J.OBJECT, "eval", cxt, expr); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(any(IExpr[].class))); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		ClosureCmd closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 0, false));
		closure.push(loc, hdc1, null);
		dcg.closure(closure, op);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanReturnAFunctionCallWithNoArgsDirectlyFromAFunction() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic("test.golden.PACKAGEFUNCTIONS$callMe", J.OBJECT, "eval", cxt, expr); will(returnValue(result));
			oneOf(meth).returnObject(result); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 0, false));
		PushReturn pr = new PushExternal(loc, hdc1);
		dcg.pushReturn(pr, null, op);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanCreateAStructDirectFromAFunction() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic(J.BUILTINPKG+".Nil", J.OBJECT, "eval", cxt, expr); will(returnValue(result));
			oneOf(meth).returnObject(result); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		SolidName fn = new SolidName(null, "Nil");
		PackageVar hdc1 = new PackageVar(loc, fn, new RWStructDefn(loc, StructType.STRUCT, fn, false));
		PushReturn pr = new PushExternal(loc, hdc1);
		dcg.pushReturn(pr, null, op);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanCreateAClosureForAStructCreationWithArgs() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.BUILTINPKG + ".Cons"); will(returnValue(expr));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(any(IExpr[].class))); will(returnValue(result));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
			oneOf(op).result(result);
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		SolidName fn = new SolidName(null, "Cons");
		ClosureCmd closure = form.createClosure(loc);
		RWStructDefn sd = new RWStructDefn(loc, StructType.STRUCT, fn, false);
		sd.addField(new RWStructField(loc, false, new PrimitiveType(loc, new SolidName(null, "String")), "head"));
		PackageVar hdc1 = new PackageVar(loc, fn, sd);
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanCreateAnObjectWithoutArgsDirectFromAFunction() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic(J.BUILTINPKG+".Croset", J.OBJECT, "eval", cxt, expr); will(returnValue(result));
			oneOf(meth).returnObject(result); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		SolidName fn = new SolidName(null, "Croset");
		PackageVar hdc1 = new PackageVar(loc, fn, new RWObjectDefn(loc, fn, false));
		PushReturn pr = new PushExternal(loc, hdc1);
		dcg.pushReturn(pr, null, op);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanCreateAnObjectWithArgsUsingAClosure() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.BUILTINPKG + ".Croset"); will(returnValue(expr));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			oneOf(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(any(IExpr[].class))); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		SolidName fn = new SolidName(null, "Croset");
		ClosureCmd closure = form.createClosure(loc);
		RWObjectDefn od = new RWObjectDefn(loc, fn, false);
		od.constructorArg(loc, new PrimitiveType(loc, new SolidName(null, "String")), "init");
		PackageVar hdc1 = new PackageVar(loc, fn, od);
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}


	// In the context of a handler, it is possible that one of the "lambda arguments" could be a function (or method)
	// It would then be reasonable to call/invoke this in the process of handling a request
	// Currently, the parser does not allow lambdas of "function" type to be passed in, but the same effect *can* happen by using a suitable scoped var (which is turned into a lambda during lifting)
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanCallAFunctionWhichWasGivenToUsAsALambda() {
		IExpr fn = context.mock(IExpr.class, "fn");
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).getField("length"); will(returnValue(fn));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			exactly(1).of(meth).arrayOf(with(J.OBJECT), (List<IExpr>) with(Matchers.contains(expr))); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(new IExpr[] { fn, expr })); will(returnValue(result));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
			oneOf(op).result(result);
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		HandlerName hn = new HandlerName(new PackageName("test.golden"), "MyHC");
		ClosureCmd closure = form.createClosure(loc);
		HandlerLambda hl = new HandlerLambda(loc, hn, FunctionType.function(loc, new PrimitiveType(loc, new SolidName(null, "String")), new PrimitiveType(loc, new SolidName(null, "Number"))), "length");
		PackageVar hdc1 = new PackageVar(loc, hn, hl);
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}

	@Test
	public void testWeCanReturnAValueWhichWasGivenToUsAsALambda() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).getField("str"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(result));
		}});
		VarFactory vf = new NextVarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		VarHolder vh = new VarHolder(form, new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
			oneOf(op).result(result);
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		HandlerName hn = new HandlerName(new PackageName("test.golden"), "MyHC");
		HandlerLambda hl = new HandlerLambda(loc, hn, new PrimitiveType(loc, new SolidName(null, "String")), "str");
		PackageVar hdc1 = new PackageVar(loc, hn, hl);
		dcg.pushReturn(new PushExternal(loc, hdc1), null, op);
	}

	// There are a number of cases with regards to scoped vars, and I'm not sure I've covered all of them
	// Specifically, there are cases where they are used in nested functions, and cases where they're used in the defining function
	// The defining function also has the ability to use them in setting up "scoping closures" which need special treatment
	// These cases are the ones that drive the behaviour we currently need for golden tests
	
	// In a nested function, check that the use of a scoped var maps to the input pending var
	@Test
	public void testAScopedVarResolvesToAVar() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).argument(J.STRING, "x"); will(new ReturnNewVar(meth, J.STRING, "x"));
			oneOf(meth).returnObject(with(any(AVar.class))); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		FunctionName fn = FunctionName.function(loc, null, "testfn");
		HSIEForm form = new HSIEForm(loc, fn, 0, CodeType.FUNCTION, null, vf);
		VarName hn = new VarName(loc, new PackageName("test.golden"), "x");
		ScopedVar sv = new ScopedVar(loc, hn, new PrimitiveType(loc, new SolidName(null, "String")), fn);
		form.scoped.add(sv);
		List<PendingVar> pvs = new ArrayList<>();
		pvs.add(new PendingVar(JavaType.string, "x", 0).apply(meth));
		VarHolder vh = new VarHolder(form, pvs);
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		PackageVar hdc1 = new PackageVar(loc, hn, sv);
		dcg.pushReturn(new PushExternal(loc, hdc1), null, op);
	}

	// When used in defining a scoping closure, make sure that it is just the object itself that's used
	@SuppressWarnings("unchecked")
	@Test
	public void testAScopedVarInAScopingClosureResolvesToTheName() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			oneOf(meth).argument(J.STRING, "x"); will(new ReturnNewVar(meth, J.STRING, "x"));
			oneOf(meth).classConst("test.golden$x"); will(returnValue(expr));
			oneOf(meth).stringConst("hello"); will(returnValue(expr));
			exactly(1).of(meth).arrayOf(with(J.OBJECT), (List<IExpr>) with(Matchers.contains(expr))); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(new IExpr[] { expr, expr })); will(returnValue(result));
			oneOf(op).result(result);
		}});
		VarFactory vf = new NextVarFactory();
		FunctionName fn = FunctionName.function(loc, null, "testfn");
		HSIEForm form = new HSIEForm(loc, fn, 0, CodeType.FUNCTION, null, vf);
		VarName hn = new VarName(loc, new PackageName("test.golden"), "x");
		ScopedVar sv = new ScopedVar(loc, hn, new PrimitiveType(loc, new SolidName(null, "String")), fn);
		form.scopedDefinitions.add(sv);
		List<PendingVar> pvs = new ArrayList<>();
		pvs.add(new PendingVar(JavaType.string, "x", 0).apply(meth));
		VarHolder vh = new VarHolder(form, pvs);
		context.checking(new Expectations() {{
			allowing(genCxt).getVarHolder(); will(returnValue(vh));
		}});
		ClosureTraverser<IExpr> dcg = new ClosureTraverser<>(form, genCxt);
		ClosureCmd closure = form.createScopingClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, hn, sv);
		closure.push(loc, hdc1, null);
		closure.push(loc, new StringLiteral(loc, "hello"), null);
		dcg.closure(closure, op);
	}
}
