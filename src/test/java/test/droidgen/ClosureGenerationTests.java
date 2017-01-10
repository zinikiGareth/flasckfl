package test.droidgen;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.droidgen.DroidClosureGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.hsie.VarFactory;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.FieldExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IntConstExpr;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.MethodDefiner;

public class ClosureGenerationTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
	ByteCodeSink bcc = context.mock(ByteCodeSink.class, "bcc");
	MethodDefiner meth = context.mock(MethodDefiner.class, "meth");
	IExpr expr = context.mock(IExpr.class);

	@Before
	public void prepareTest() {
		context.checking(new Expectations() {{
			allowing(bcc).addInnerClassReference(with(any(Access.class)), with(any(String.class)), with(any(String.class)));
			allowing(meth).getBCC(); will(returnValue(bcc));
			allowing(meth).box(with(any(IExpr.class))); will(returnValue(expr));
			allowing(meth).nextLocal(); will(returnValue(1));
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
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		HSIEBlock closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "tuple"), BuiltinOperation.TUPLE);
		closure.push(loc, hdc1);
		closure.push(loc, 42);
		closure.push(loc, new StringLiteral(loc, "hello"));
		dcg.pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
	}

	// NOT CLEAR: if this is a real case - doesn't it need arguments?  Would they be closures, or would it have to be a closure itself
	@Test
	public void testReturningATuple() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.FLEVAL + "$Tuple"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "tuple"), BuiltinOperation.TUPLE);
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null);
	}

	// NOT CLEAR: if this is the correct generated code or not for this case
	// This is based on the golden test typeof, and the question is "what happens if you return a type, specifically Number"?
	@Test
	public void testTypeOfPrimitiveReturnsAClass() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst(J.BUILTINPKG + ".Number"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		SolidName number = new SolidName(null, "Number");
		PackageVar hdc1 = new PackageVar(loc, number, new PrimitiveType(loc, number));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null);
	}

	// NOT CLEAR: if this is the correct generated code or not for this case
	// This is based on the golden test typeof, and the question is "what happens if you return a type, specifically a Card"?
	@Test
	public void testTypeOfACardClassReturnsTheClassCard() {
		context.checking(new Expectations() {{
			oneOf(meth).classConst("test.golden$MyCard"); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardGrouping(cn, null));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null);
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
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		HSIEBlock closure = form.createClosure(loc);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		HandlerName hn = new HandlerName(cn, "Handler");
		PackageVar hdc1 = new PackageVar(loc, hn, new ObjectReference(loc, cn, hn));
		closure.push(loc, hdc1);
		closure.push(loc, new StringLiteral(loc, "hello"));
		dcg.pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
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
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		HSIEBlock closure = form.createClosure(loc);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		CardFunction cf = new CardFunction(loc, cn, "eventHandler");
		PackageVar hdc1 = new PackageVar(loc, FunctionName.function(loc, new PackageName("FLEval"), "curry"), cf);
		PackageVar hdc2 = new PackageVar(loc, FunctionName.functionInCardContext(loc, cn, cf.function), cf);
		closure.push(loc, hdc1);
		closure.push(loc, hdc2);
		closure.push(loc, 2);
		closure.push(loc, new StringLiteral(loc, "hello"));
		dcg.pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
	}

	// Card function that returns a card member
	@Test
	public void testReturningACardMemberFromAMethodOnTheCard() {
		context.checking(new Expectations() {{
			oneOf(meth).myThis(); will(new ReturnNewVar(meth, "fred", "bar"));
			oneOf(meth).getField(with(any(IExpr.class)), with("var")); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.CARD, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardMember(loc, cn, "var", new PrimitiveType(loc, new SolidName(null, "String"))));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null);
	}

	// Function on a Card associate that returns a card member
	@Test
	public void testReturningACardMemberFromAMethodOnACardAssociate() {
		context.checking(new Expectations() {{
			oneOf(meth).getField("_card"); will(returnValue(new FieldExpr(meth, expr, "fred", "bar", "_card")));
			oneOf(meth).getField(with(any(IExpr.class)), with("var")); will(returnValue(expr));
			oneOf(meth).returnObject(expr); will(returnValue(expr));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.AREA, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		CardName cn = new CardName(new PackageName("test.golden"), "MyCard");
		PackageVar hdc1 = new PackageVar(loc, cn, new CardMember(loc, cn, "var", new PrimitiveType(loc, new SolidName(null, "String"))));
		PushExternal hdc = new PushExternal(loc, hdc1);
		dcg.pushReturn(hdc, null);
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
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		HSIEBlock closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 1, false));
		closure.push(loc, hdc1);
		closure.push(loc, new StringLiteral(loc, "hello"));
		IExpr out = dcg.pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
		assertEquals(result, out);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanReturnAClosureForAFunctionCallWithNoArgsInAClosure() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic("test.golden.PACKAGEFUNCTIONS$callMe", J.OBJECT, "eval", expr); will(returnValue(expr));
			oneOf(meth).makeNew(with(J.FLCLOSURE), with(any(IExpr[].class))); will(returnValue(result));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		HSIEBlock closure = form.createClosure(loc);
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 0, false));
		closure.push(loc, hdc1);
		IExpr out = dcg.pushReturn((PushReturn) closure.nestedCommands().get(0), closure);
		assertEquals(result, out);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanReturnAFunctionCallWithNoArgsDirectlyFromAFunction() {
		IExpr result = context.mock(IExpr.class, "result");
		context.checking(new Expectations() {{
			allowing(meth).arrayOf(with(J.OBJECT), with(any(List.class))); will(returnValue(expr));
			oneOf(meth).callStatic("test.golden.PACKAGEFUNCTIONS$callMe", J.OBJECT, "eval", expr); will(returnValue(result));
			oneOf(meth).returnObject(result); will(returnValue(result));
		}});
		VarFactory vf = new VarFactory();
		HSIEForm form = new HSIEForm(loc, FunctionName.function(loc, null, "testfn"), 0, CodeType.FUNCTION, null, vf);
		DroidClosureGenerator dcg = new DroidClosureGenerator(form, meth, null);
		FunctionName fn = FunctionName.function(loc, new PackageName("test.golden"), "callMe");
		PackageVar hdc1 = new PackageVar(loc, fn, new RWFunctionDefinition(fn, 0, false));
		PushReturn pr = new PushExternal(loc, hdc1);
		IExpr out = dcg.pushReturn(pr, null);
		assertEquals(result, out);
	}
}
