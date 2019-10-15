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
import org.flasck.flas.repository.StackVisitor;
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
	private final StackVisitor nv = new StackVisitor();
	private final RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void aSimpleNoArgConstructorSaysThisMustBeInTheArgType() {
		UnifiableType arg = context.mock(UnifiableType.class);
		nv.push(new FunctionChecker(errors, repository, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).nextArg(); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));
		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void aConstructorCanBeConstraintedBasedOnItsFields() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		StructTypeConstraints cons = context.mock(StructTypeConstraints.class);
		UnifiableType head = context.mock(UnifiableType.class, "head");
		nv.push(new FunctionChecker(errors, repository, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).nextArg(); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(LoadBuiltins.cons); will(returnValue(cons));
			oneOf(cons).field(LoadBuiltins.cons.findField("head")); will(returnValue(head));
		}});
		nv.matchConstructor(LoadBuiltins.cons);
		nv.matchField(LoadBuiltins.cons.findField("head"));

		context.checking(new Expectations() {{
			oneOf(head).canBeStruct(LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void alternativeConstructorsCanBeOfferedForTheSameSlot() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		StructTypeConstraints cons = context.mock(StructTypeConstraints.class);
		UnifiableType head = context.mock(UnifiableType.class, "head");
		nv.push(new FunctionChecker(errors, repository, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).nextArg(); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(LoadBuiltins.cons); will(returnValue(cons));
			oneOf(cons).field(LoadBuiltins.cons.findField("head")); will(returnValue(head));
		}});
		nv.matchConstructor(LoadBuiltins.cons);
		nv.matchField(LoadBuiltins.cons.findField("head"));

		context.checking(new Expectations() {{
			oneOf(head).canBeStruct(LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

}
