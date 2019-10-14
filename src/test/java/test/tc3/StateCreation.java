package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.TypeChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StateCreation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final NestedVisitor nv = context.mock(NestedVisitor.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);

	@Before
	public void always() {
		context.checking(new Expectations() {{
			allowing(nv).push(with(any(TypeChecker.class)));
		}});
	}
	@Test
	public void testASimpleNoArgConstructorSaysThisMustBeInTheArgType() {
		context.checking(new Expectations() {{
			oneOf(repository).get("Nil"); will(returnValue(LoadBuiltins.nil));
			oneOf(state).argType(LoadBuiltins.nil);
		}});
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		HSIArgsTree hat = new HSIArgsTree(1);
		hat.consider(fi);
		hat.get(0).requireCM(LoadBuiltins.nil);
		fn.bindHsi(hat);
		
		GroupChecker tc = new GroupChecker(errors, repository, nv, state);
		tc.visitFunction(fn);
		tc.visitPatterns(hat);
	}

}
