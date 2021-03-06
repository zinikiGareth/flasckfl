package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.SlotChecker;
import org.flasck.flas.tc3.UnifiableType;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ArgumentChecking {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private CurrentTCState state = context.mock(CurrentTCState.class);
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private UnifiableType ut = context.mock(UnifiableType.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private FunctionName fn = FunctionName.function(pos, null, "f");
	private String fnCxt = "f";
	private SlotChecker tc = new SlotChecker(nv, fn, state, ut);

	@Test
	public void aNoArgConstructorIsHandled() {
		context.checking(new Expectations() {{
			oneOf(ut).canBeStruct(null, fn, LoadBuiltins.nil);
		}});
		tc.matchConstructor(LoadBuiltins.nil);
	}

	@Test
	public void aMultiArgConstructorIsHandled() {
		context.checking(new Expectations() {{
			oneOf(ut).canBeStruct(null, fn, LoadBuiltins.cons);
		}});
		tc.matchConstructor(LoadBuiltins.cons);
	}

	@Test
	public void aFieldEntersANewLevel() {
		context.checking(new Expectations() {{
			oneOf(ut).canBeStruct(null, fn, LoadBuiltins.cons);
			oneOf(nv).push(with(any(SlotChecker.class)));
		}});
		tc.matchConstructor(LoadBuiltins.cons);
		tc.matchField(LoadBuiltins.cons.findField("head"));
	}

	@Test
	public void aTypeConstraintIsHandled() {
		context.checking(new Expectations() {{
			oneOf(ut).canBeType(pos, LoadBuiltins.string);
			oneOf(state).bindVarToUT(fnCxt, "f.x", ut);
			oneOf(state).recordPolys(LoadBuiltins.string);
		}});
		tc.matchType(LoadBuiltins.string, new VarName(pos, fn, "x"), null);
	}
}
