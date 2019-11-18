package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.ConsolidateTypes;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.PolyTypeMatcher;

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
		assertEquals(LoadBuiltins.number, gc.consolidate(LoadBuiltins.number, true));
	}
	
	@Test
	public void trueAndFalseMakeAList() {
		assertEquals(LoadBuiltins.bool, gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.trueT, LoadBuiltins.falseT), true));
	}
	
	@Test
	public void trueAndFalseMakeAListInTheOtherOrder() {
		assertEquals(LoadBuiltins.bool, gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.falseT, LoadBuiltins.trueT), true));
	}
	
	@Test
	public void youCannotMakeAUnionOfNumberAndString() {
		context.checking(new Expectations() {{
			oneOf(state).resolveAll(true);
			oneOf(errors).message(pos, "unable to unify Number, String");
		}});
		assertNull(gc.consolidate(new ConsolidateTypes(pos, LoadBuiltins.number, LoadBuiltins.string), true));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSimpleApplyReturnsItself() {
		assertThat(gc.consolidate(new Apply(LoadBuiltins.string, LoadBuiltins.number), true), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void anApplyCanConsolidateTrueAndFalseToBool() {
		assertThat(gc.consolidate(new Apply(LoadBuiltins.string, new ConsolidateTypes(pos, LoadBuiltins.trueT, LoadBuiltins.falseT)), true), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.bool)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aPolyInstanceHasItsParametersConsolidated() {
		PolyInstance pi = new PolyInstance(LoadBuiltins.cons, Arrays.asList(new ConsolidateTypes(pos, LoadBuiltins.trueT, LoadBuiltins.falseT)));
		assertThat(gc.consolidate(pi, true), (Matcher)PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.bool)));
	}

	@Test
	public void aTCSWithNothingDoingReturnsAny() {
		assertEquals(LoadBuiltins.any, gc.consolidate(new TypeConstraintSet(repository, state, pos, "id1"), true));
	}
	
	@Test
	public void aUnionOfATypeAndAnEmptyTCSDoesTheRightThing() {
		Type c1 = gc.consolidate(new ConsolidateTypes(pos, new TypeConstraintSet(repository, state, pos, "id1"), LoadBuiltins.number), false);
		assertEquals(LoadBuiltins.number, c1);
	}

	@Test
	public void aUnionOfATypeAndAResolvedTCSDoesTheRightThing() {
		TypeConstraintSet tcs = new TypeConstraintSet(repository, state, pos, "id1");
		tcs.isReturned(); // fake it to have been used in multiple places
		ConsolidateTypes ct = new ConsolidateTypes(pos, tcs, LoadBuiltins.number);
		assertEquals(1, ct.types.size());
		assertEquals(LoadBuiltins.number, gc.consolidate(ct, false));
		assertEquals(LoadBuiltins.number, tcs.resolve());
	}

	// can this happen in real life?
	@Test
	public void consolidatingASingleUTDeliversAny() {
		TypeConstraintSet tcs1 = new TypeConstraintSet(repository, state, pos, "id1");
		ConsolidateTypes ct = new ConsolidateTypes(pos, tcs1);
		assertEquals(0, ct.types.size());
		Type beforeResolution = gc.consolidate(ct, false);
		assertNull(beforeResolution);
		assertEquals(LoadBuiltins.any, tcs1.resolve());
		assertEquals(LoadBuiltins.any, gc.consolidate(ct, true));
	}

	@Test
	public void consolidatingTwoUTsMeansTheyWillResolveToTheSamePoly() {
		TypeConstraintSet tcs1 = new TypeConstraintSet(repository, state, pos, "id1");
		TypeConstraintSet tcs2 = new TypeConstraintSet(repository, state, pos, "id1");
		ConsolidateTypes ct = new ConsolidateTypes(pos, tcs1, tcs2);
		assertEquals(0, ct.types.size());
		Type beforeResolution = gc.consolidate(ct, false);
		assertNull(beforeResolution);
		PolyType pa = new PolyType(pos, "A");
		context.checking(new Expectations() {{
			oneOf(state).nextPoly(pos); will(returnValue(pa));
		}});
		Type r1 = tcs1.resolve();
		assertEquals(pa, r1);
		assertEquals(pa, tcs2.resolve());
		assertEquals(pa, gc.consolidate(ct, true));
	}

	@Test
	public void consolidatingErrorWithAnythingIsError() {
		ConsolidateTypes ct = new ConsolidateTypes(pos, new ErrorType(), LoadBuiltins.nil);
		assertTrue(gc.consolidate(ct, true) instanceof ErrorType);
	}
}
