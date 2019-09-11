package test.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class PatternTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, 42);
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Visitor v = context.mock(Visitor.class);
	final TypeReference list = new TypeReference(pos, "List");
	private FunctionName fnName = FunctionName.function(pos, new PackageName("test.golden"), "f");
	final VarPattern vp = new VarPattern(pos, new VarName(pos, fnName, "v"));
	final TypedPattern tp = new TypedPattern(pos, list, pos, "x");
	final ConstructorMatch cm = new ConstructorMatch(pos, "Nil");

	@Test
	public void simpleVarPattern() {
		context.checking(new Expectations() {{
			oneOf(v).visitPattern(vp);
			oneOf(v).visitVarPattern(vp);
			oneOf(v).visitPatternVar(pos, "v");
			oneOf(v).leavePattern(vp);
		}});
		new Traverser(v).visitPattern(vp);
	}

	@Test
	public void simpleTypedPattern() {
		context.checking(new Expectations() {{
			oneOf(v).visitPattern(tp);
			oneOf(v).visitTypedPattern(tp);
			oneOf(v).visitTypeReference(list);
			oneOf(v).visitPatternVar(pos, "x");
			oneOf(v).leavePattern(tp);
		}});
		new Traverser(v).visitPattern(tp);
	}

	@Test
	public void simpleConstructorMatch() {
		context.checking(new Expectations() {{
			oneOf(v).visitPattern(cm);
			oneOf(v).visitConstructorMatch(cm);
			oneOf(v).leaveConstructorMatch(cm);
			oneOf(v).leavePattern(cm);
		}});
		new Traverser(v).visitPattern(cm);
	}
}
