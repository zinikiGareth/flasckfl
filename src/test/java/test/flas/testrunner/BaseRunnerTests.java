package test.flas.testrunner;

import java.io.IOException;
import java.util.ArrayList;

import org.flasck.flas.Main;
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
import org.flasck.flas.testrunner.TestRunner;
import org.flasck.flas.types.Type;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;

public abstract class BaseRunnerTests {
	static final int X_VALUE = 420;
	static final int X_OTHER_VALUE = 520;
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null, null);
	TypeChecker2 tc = new TypeChecker2(errors, rw);
	// TODO: defining bce here feels out of place and should be in the JVMRunnerTest
	// But it is "part of" the CompileResult.  Why?
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	CompileResult prior;
	FLASCompiler sc = new FLASCompiler();
	TestRunner runner;
	Scope mainScope = Scope.topScope("test.golden");
	Scope testScope;
	
	@Before
	public void setup() {
		Main.setLogLevels();
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

	protected abstract void prepareRunner() throws IOException, ErrorResultException;
	
	protected FunctionCaseDefn function(String name, Object expr) {
		FunctionCaseDefn defn = new FunctionCaseDefn(FunctionName.function(loc, new PackageName("test.golden.script"), name), new ArrayList<>(), expr);
		defn.provideCaseName(0);
		return defn;
	}
}
