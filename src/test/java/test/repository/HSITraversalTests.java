package test.repository;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIPatternTree;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class HSITraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
//	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, "42", 2);
//	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final HSIVisitor v = context.mock(HSIVisitor.class);
//	final TypeReference list = new TypeReference(pos, "List");
//	private FunctionName fnName = FunctionName.function(pos, new PackageName("test.golden"), "f");
//	final VarPattern vp = new VarPattern(pos, new VarName(pos, fnName, "v"));
//	final TypedPattern tp = new TypedPattern(pos, list, new VarName(pos, null, "x"));
//	final ConstructorMatch cm = new ConstructorMatch(pos, "Nil");
	final FunctionName fname = FunctionName.function(pos, pkg, "f");
	final Traverser t = new Traverser(v);

	@Test
	public void aSimpleVarBindsItAndThenJustEvalsTheExpression() {
		FunctionDefinition fn = new FunctionDefinition(fname, 1);
		FunctionIntro fi = new FunctionIntro(fname, new ArrayList<>());
		VarName vx = new VarName(pos, fname, "x");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn);
			oneOf(v).hsiArgs(with(any(Slot.class)));
			oneOf(v).startInline(fi);
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).endInline(fi);
			oneOf(v).leaveFunction(fn);
		}});
		HSIPatternTree tree = new HSIPatternTree(1);
		tree.get(0).addVar(vx);
		fn.bindHsi(tree);
		
		fi.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi);
		
		t.visitFunction(fn);
	}
}
