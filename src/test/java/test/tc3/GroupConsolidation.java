package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.ConsolidateTypes;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GroupConsolidation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final NestedVisitor nv = context.mock(NestedVisitor.class);
	private final Repository repository = new Repository();
	private final GroupChecker gc = new GroupChecker(errors, repository, nv, state);
	
	@Before
	public void init() {
		LoadBuiltins.applyTo(repository);
	}

	@Test
	public void numberIsJustNumber() {
		assertEquals(LoadBuiltins.number, gc.consolidate(LoadBuiltins.number));
	}
	
	@Test
	public void trueAndFalseMakeAList() {
		assertEquals(LoadBuiltins.bool, gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.trueT, LoadBuiltins.falseT)));
	}
	
	@Test
	public void trueAndFalseMakeAListInTheOtherOrder() {
		assertEquals(LoadBuiltins.bool, gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.falseT, LoadBuiltins.trueT)));
	}
	
	@Test
	public void youCannotMakeAUnionOfNumberAndString() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "unable to unify [Primitive[Number], Primitive[String]]");
		}});
		assertNull(gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.number, LoadBuiltins.string)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSimpleApplyReturnsItself() {
		assertThat(gc.consolidate(new Apply(LoadBuiltins.string, LoadBuiltins.number)), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void anApplyCanConsolidateTrueAndFalseToBool() {
		assertThat(gc.consolidate(new Apply(LoadBuiltins.string, new ConsolidateTypes(pos, LoadBuiltins.trueT, LoadBuiltins.falseT))), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.bool)));
	}

	@Test
	public void aTCSWithNothingDoingReturnsAny() {
		assertEquals(LoadBuiltins.any, gc.consolidate(new TypeConstraintSet(repository, state, pos, "id1")));
	}
	
}
