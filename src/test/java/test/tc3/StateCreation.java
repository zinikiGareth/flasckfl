package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.SlotChecker;
import org.flasck.flas.tc3.StructTypeConstraints;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.UnifiableType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class StateCreation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final NestedVisitor nv = context.mock(NestedVisitor.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void testASimpleNoArgConstructorSaysThisMustBeInTheArgType() {
		UnifiableType arg = context.mock(UnifiableType.class);
		
		CaptureAction captureSC = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(state).nextArg(); will(returnValue(arg));
			oneOf(nv).push(with(any(SlotChecker.class))); will(captureSC);
		}});
		FunctionChecker fc = new FunctionChecker(errors, repository, nv, state);
		fc.argSlot(new ArgSlot(0, null));
		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(LoadBuiltins.nil);
		}});
		((SlotChecker) captureSC.get(0)).matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void aConstructorCanBeConstraintedBasedOnItsFields() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		StructTypeConstraints cons = context.mock(StructTypeConstraints.class);
		UnifiableType head = context.mock(UnifiableType.class, "head");
		
		CaptureAction captureSC = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(state).nextArg(); will(returnValue(arg));
			oneOf(nv).push(with(any(SlotChecker.class))); will(captureSC);
		}});
		FunctionChecker fc = new FunctionChecker(errors, repository, nv, state);
		fc.argSlot(new ArgSlot(0, null));

		CaptureAction captureFSC = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(LoadBuiltins.cons); will(returnValue(cons));
			oneOf(cons).field(LoadBuiltins.cons.findField("head")); will(returnValue(head));
			oneOf(nv).push(with(any(SlotChecker.class))); will(captureFSC);
		}});
		SlotChecker sc = (SlotChecker) captureSC.get(0);
		sc.matchConstructor(LoadBuiltins.cons);
		sc.matchField(LoadBuiltins.cons.findField("head"));

		context.checking(new Expectations() {{
			oneOf(head).canBeStruct(LoadBuiltins.nil);
		}});
		SlotChecker fsc = (SlotChecker) captureFSC.get(0);
		fsc.matchConstructor(LoadBuiltins.nil);
	}

}
