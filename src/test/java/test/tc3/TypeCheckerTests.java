package test.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.ObjectDefnChecker;
import org.flasck.flas.tc3.TypeChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class TypeCheckerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1, false);
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	FunctionDefinition fnG = new FunctionDefinition(nameG, 1, false);
	List<Pattern> args = new ArrayList<>();
	FunctionIntro fiF1 = new FunctionIntro(nameF, args);
	FunctionIntro fiF2 = new FunctionIntro(nameF, args);
	FunctionIntro fiG1 = new FunctionIntro(nameG, args);
	FunctionIntro fiG2 = new FunctionIntro(nameG, args);
	private FunctionGroup grp = new DependencyGroup(fnF, fnG);

	@Before
	public void begin() {
		context.checking(new Expectations() {{
			fnF.intro(fiF1);
			fnF.intro(fiF2);
			fnG.intro(fiG1);
			fnG.intro(fiG2);
		}});
	}
	
	@Test
	public void visitObjectDefnPushesANewChecker() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(TypeChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, sv);
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ObjectDefnChecker.class)));
		}});
		tc.visitObjectDefn(od);
	}

	@Test
	public void visitGroupIntroducesUTsForItsFunctions() {
		CaptureAction gc = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(TypeChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, sv);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(GroupChecker.class))); will(gc);
		}});
		tc.visitFunctionGroup(grp);
		CurrentTCState state = ((GroupChecker)gc.get(0)).testsWantToCheckState();
		state.requireVarConstraints(pos, nameF.uniqueName());
		state.requireVarConstraints(pos, nameG.uniqueName());
	}
}
