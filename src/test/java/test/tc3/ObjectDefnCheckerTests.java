package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ObjectDefnChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ObjectDefnCheckerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private String fnCxt = "test.repo.f";
	
	@Test
	public void anObjectWithNoFieldsIsJustWavedOnThrough() {
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ObjectDefnChecker.class)));
			oneOf(sv).result(null);
		}});
		ObjectDefnChecker tc = new ObjectDefnChecker(errors, repository, sv, fnCxt, new ArrayList<>(), false);
		tc.leaveObjectDefn(od);
	}

	@Test
	public void aFieldWithNoInitIsJustWavedOnThrough() {
		SolidName on = new SolidName(pkg, "Obj");
		ObjectDefn od = new ObjectDefn(pos, pos, on, false, new ArrayList<>());
		StructField sf = new StructField(pos, null, false, true, LoadBuiltins.stringTR, "x");
		sf.fullName(new VarName(pos, on, "x"));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ObjectDefnChecker.class)));
			oneOf(sv).result(null);
		}});
		ObjectDefnChecker tc = new ObjectDefnChecker(errors, repository, sv, fnCxt, new ArrayList<>(), false);
		tc.visitStructField(sf);
		tc.leaveStructField(sf);
		tc.leaveObjectDefn(od);
	}

	@Test
	public void aFieldWithAMatchingInitPushesAnExprCheckerButIsFine() {
		SolidName on = new SolidName(pkg, "Obj");
		ObjectDefn od = new ObjectDefn(pos, pos, on, false, new ArrayList<>());
		StringLiteral sl = new StringLiteral(pos, "hello");
		StructField sf = new StructField(pos, pos, null, false, true, LoadBuiltins.stringTR, "x", sl);
		sf.fullName(new VarName(pos, on, "x"));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ObjectDefnChecker.class)));
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(sv).result(null);
		}});
		ObjectDefnChecker tc = new ObjectDefnChecker(errors, repository, sv, fnCxt, new ArrayList<>(), false);
		tc.visitStructField(sf);
		tc.visitExpr(sl, 0);
		tc.result(new ExprResult(pos, LoadBuiltins.string));
		tc.leaveStructField(sf);
		tc.leaveObjectDefn(od);
	}

	@Test
	public void aFieldWithAIncorrectInitRaisesAnError() {
		SolidName on = new SolidName(pkg, "Obj");
		ObjectDefn od = new ObjectDefn(pos, pos, on, false, new ArrayList<>());
		StringLiteral sl = new StringLiteral(pos, "hello");
		StructField sf = new StructField(pos, pos, null, false, true, LoadBuiltins.numberTR, "x", sl);
		sf.fullName(new VarName(pos, on, "x"));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ObjectDefnChecker.class)));
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(errors).message(pos, "cannot initialize x from String");
			oneOf(sv).result(null);
		}});
		ObjectDefnChecker tc = new ObjectDefnChecker(errors, repository, sv, fnCxt, new ArrayList<>(), false);
		tc.visitStructField(sf);
		tc.visitExpr(sl, 0);
		tc.result(new ExprResult(pos, LoadBuiltins.string));
		tc.leaveStructField(sf);
		tc.leaveObjectDefn(od);
	}
}
