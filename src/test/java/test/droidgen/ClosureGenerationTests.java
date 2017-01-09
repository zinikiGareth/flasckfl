package test.droidgen;

import static org.junit.Assert.*;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.droidgen.DroidClosureGenerator;
import org.flasck.flas.droidgen.J;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.hsie.VarFactory;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
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
			allowing(meth).getBCC(); will(returnValue(bcc));
			allowing(bcc).addInnerClassReference(with(any(Access.class)), with(any(String.class)), with(any(String.class)));
			allowing(meth).box(with(any(IExpr.class))); will(returnValue(expr));
		}});
	}
	
	// TODO: want to do the same thing using a return, not a closure (assuming that's possible; I think it is)
//			oneOf(meth).returnObject(expr); will(returnValue(expr));
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
		PushExternal hdc = new PushExternal(loc, hdc1);
		closure.push(loc, hdc1);
		closure.push(loc, 42);
		closure.push(loc, new StringLiteral(loc, "hello"));
		dcg.pushReturn(hdc, closure);
	}

}
