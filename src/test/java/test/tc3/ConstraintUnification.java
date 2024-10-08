package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.StructTypeConstraints;
import org.flasck.flas.tc3.UnifiableType;
import org.flasck.flas.testsupport.matchers.PolyInstanceMatcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ConstraintUnification {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private static PackageName poly = new PackageName(true);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private CurrentTCState state = new FunctionGroupTCState(repository, new DependencyGroup());
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private final FunctionName fn = FunctionName.function(pos, null, "foo");

	@Test
	public void ifWeDontDoAnythingWeEndUpWithAny() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		assertEquals(LoadBuiltins.any, ut.resolve(tracker));
	}

	@Test
	public void oneIncoporatedByConstraintCreatesAnIdentity() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.incorporatedBy(pos, LoadBuiltins.number);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.number, ut.resolvedTo());
	}
	
	@Test
	public void ifYouAskSomethingToBeANilItWillBe() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.canBeStruct(pos, null, LoadBuiltins.nil);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.nil, ut.resolvedTo());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsAndDontConstrainItYouGetAnyAsThePoly() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.canBeStruct(pos, fn, LoadBuiltins.cons);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ut.resolve(tracker), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadNotConstrainedYouStillGetAny() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		StructTypeConstraints stc = ut.canBeStruct(pos, fn, LoadBuiltins.cons);
		PolyType pt = new PolyType(pos, new SolidName(poly, "A"));
		stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ut.resolvedTo(), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(pt)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadSpecifiedThatsTheType() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		StructTypeConstraints stc = ut.canBeStruct(pos, fn, LoadBuiltins.cons);
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.canBeStruct(pos, null, LoadBuiltins.falseT);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ut.resolve(tracker), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.falseT)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouReturnAVarThenYouGetAFreshPolyVar() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		StructTypeConstraints stc = ut.canBeStruct(pos, fn, LoadBuiltins.cons);
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.isReturned(pos);

		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		
		PolyType polyA = new PolyType(pos, new SolidName(poly, "A"));
		assertThat(f.resolvedTo(), Matchers.is(polyA));
		assertThat(ut.resolvedTo(), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(polyA)));
	}

	// TODO: head as a var leads to Cons[A] unless the var is completely unused
	// TODO: just specify the tail
	// TODO: conflict -> error
	// TODO: nil or cons[A] -> list[A]
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void trueAndFalseUnifyToBoolean() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.canBeStruct(pos, null, LoadBuiltins.trueT);
		ut.canBeStruct(pos, null, LoadBuiltins.falseT);
		context.checking(new Expectations() {{
			oneOf(repository).findUnionWith(with(tracker), with(pos), (Set) with(Matchers.containsInAnyOrder(LoadBuiltins.falseT, LoadBuiltins.trueT)), with(true)); will(returnValue(LoadBuiltins.bool));
		}});
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.bool, ut.resolvedTo());
	}
	
	@Test
	public void aSingleArgConstraintJustGivesYouThatType() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.canBeType(pos, LoadBuiltins.string);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.string, ut.resolvedTo());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void aSingleArgTypeConstraintWithAPolymorphicCtorGivesYouAnys() {
		UnifiableType ut = state.createUT(pos, "foo Cons A");
		ut.canBeType(pos, LoadBuiltins.cons);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ut.resolvedTo(), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
}

