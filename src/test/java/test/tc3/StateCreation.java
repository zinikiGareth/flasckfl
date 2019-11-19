package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.StructTypeConstraints;
import org.flasck.flas.tc3.UnifiableType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class StateCreation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final StackVisitor nv = new StackVisitor();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void aSimpleNoArgConstructorSaysThisMustBeInTheArgType() {
		UnifiableType arg = context.mock(UnifiableType.class);
		nv.push(new FunctionChecker(errors, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "unknown"); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));
		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(null, LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void aConstructorCanBeConstraintedBasedOnItsFields() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		StructTypeConstraints cons = context.mock(StructTypeConstraints.class);
		UnifiableType head = context.mock(UnifiableType.class, "head");
		nv.push(new FunctionChecker(errors, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "unknown"); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(null, LoadBuiltins.cons); will(returnValue(cons));
			oneOf(cons).field(state, null, LoadBuiltins.cons.findField("head")); will(returnValue(head));
		}});
		nv.matchConstructor(LoadBuiltins.cons);
		nv.matchField(LoadBuiltins.cons.findField("head"));

		context.checking(new Expectations() {{
			oneOf(head).canBeStruct(null, LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void alternativeConstructorsCanBeOfferedForTheSameSlot() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		nv.push(new FunctionChecker(errors, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "unknown"); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(null, LoadBuiltins.trueT);
		}});
		nv.matchConstructor(LoadBuiltins.trueT);

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(null, LoadBuiltins.falseT);
		}});
		nv.matchConstructor(LoadBuiltins.falseT);
	}

	@Test
	@Ignore
	public void alternativeConstructorsCanBeOfferedForTheSameSlotAfterNesting() {
		UnifiableType arg = context.mock(UnifiableType.class, "arg");
		StructTypeConstraints cons = context.mock(StructTypeConstraints.class);
		UnifiableType head = context.mock(UnifiableType.class, "head");
		nv.push(new FunctionChecker(errors, nv, state));
		
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "unknown"); will(returnValue(arg));
		}});
		nv.argSlot(new ArgSlot(0, null));

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(pos, LoadBuiltins.cons); will(returnValue(cons));
			oneOf(cons).field(state, null, LoadBuiltins.cons.findField("head")); will(returnValue(head));
		}});
		nv.matchConstructor(LoadBuiltins.cons);
		nv.matchField(LoadBuiltins.cons.findField("head"));

		context.checking(new Expectations() {{
			oneOf(head).canBeStruct(pos, LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);

		context.checking(new Expectations() {{
			oneOf(arg).canBeStruct(pos, LoadBuiltins.nil);
		}});
		nv.matchConstructor(LoadBuiltins.nil);
	}

}
