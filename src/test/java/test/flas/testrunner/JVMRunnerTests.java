package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.AssertFailed;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.flas.types.Type;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

public class JVMRunnerTests {
	private static final int X_VALUE = 420;
	private static final int X_OTHER_VALUE = 520;
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null, null);
	TypeChecker2 tc = new TypeChecker2(errors, rw);
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	CompileResult prior;
	FLASCompiler sc = new FLASCompiler();
	JVMRunner runner;
	Scope mainScope = Scope.topScope("test.golden");
	private Scope testScope;
	
	@Before
	public void setup() {
		mainScope.define("x", "test.golden.x", null);
		tc.define("test.golden.x", Type.function(loc, Type.builtin(loc, "Number")));
		prior = new CompileResult("test.golden", mainScope, bce, tc);
		testScope = Scope.topScope("test.golden.script");
	}
	
	@Test
	public void testAssertDoesNotThrowIfXDoesIndeedEqualX() throws Exception {
		testScope.define("expr1", "test.golden.script.expr1", function("expr1", new UnresolvedVar(loc, "x")));
		testScope.define("value1", "test.golden.script.value1", function("value1", new NumericLiteral(loc, Integer.toString(X_VALUE), -1)));
		prepareRunner();
		runner.assertCorrectValue(1);
	}

	@Test(expected=AssertFailed.class)
	public void testAssertThrowsIfXIsNotThePrescribedValue() throws Exception {
		testScope.define("expr1", "test.golden.script.expr1", function("expr1", new UnresolvedVar(loc, "x")));
		testScope.define("value1", "test.golden.script.value1", function("value1", new NumericLiteral(loc, Integer.toString(X_OTHER_VALUE), -1)));
		prepareRunner();
		runner.assertCorrectValue(1);
	}

	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.golden.script", prior, testScope);
		runner = new JVMRunner(prior);
		runner.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		runner.prepareScript(sc, testScope);
	}
	
	protected FunctionCaseDefn function(String name, Object expr) {
		FunctionCaseDefn defn = new FunctionCaseDefn(FunctionName.function(loc, new PackageName("test.golden.script"), name), new ArrayList<>(), expr);
		defn.provideCaseName(0);
		return defn;
	}

	@Before
	public void defineSupportingFunctions() {
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS$x");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.golden.PACKAGEFUNCTIONS", "java.lang.Object", "x")).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS$id");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar args = ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.golden.PACKAGEFUNCTIONS", "java.lang.Object", "id", meth.arrayElt(args.getVar(), meth.intConst(0)))).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS");
			{
				GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "x");
				ga.returns("java.lang.Object");
				MethodDefiner meth = ga.done();
				meth.returnObject(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(X_VALUE))).flush();
			}
			{
				GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "id");
				PendingVar val = ga.argument("java.lang.Object", "val");
				ga.returns("java.lang.Object");
				MethodDefiner meth = ga.done();
				meth.returnObject(val.getVar()).flush();
			}
		}
		System.out.println(bce.all());
	}
}
