package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.StructTypeConstraints;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ConstraintUnification {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);

	@Test
	public void ifWeDontDoAnythingWeEndUpWithAny() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		assertEquals(LoadBuiltins.any, ut.resolve());
	}

	@Test
	public void oneIncoporatedByConstraintCreatesAnIdentity() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.incorporatedBy(pos, LoadBuiltins.number);
		Type ty = ut.resolve();
		assertEquals(LoadBuiltins.number, ty);
	}
	
	@Test
	public void ifYouAskSomethingToBeANilItWillBe() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeStruct(LoadBuiltins.nil);
		Type ty = ut.resolve();
		assertEquals(LoadBuiltins.nil, ty);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsAndDontConstrainItYouGetAnyAsThePoly() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeStruct(LoadBuiltins.cons);
		assertThat(ut.resolve(), PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadNotConstrainedYouStillGetAny() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		StructTypeConstraints stc = ut.canBeStruct(LoadBuiltins.cons);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld")));
		}});
		stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		assertThat(ut.resolve(), PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadSpecifiedThatsTheType() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		StructTypeConstraints stc = ut.canBeStruct(LoadBuiltins.cons);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld")));
		}});
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.canBeStruct(LoadBuiltins.falseT);
		assertThat(ut.resolve(), PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.falseT)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouUseAVarThenYouGetAFreshPolyVar() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		StructTypeConstraints stc = ut.canBeStruct(LoadBuiltins.cons);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld")));
		}});
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.isReturned();

		PolyType polyA = new PolyType(pos, "A");
		context.checking(new Expectations() {{
			oneOf(state).nextPoly(pos); will(returnValue(polyA));
		}});
		
		assertThat(f.resolve(), Matchers.is(polyA));
		assertThat(ut.resolve(), PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(polyA)));
	}

	// TODO: head as a var leads to Cons[A] unless the var is completely unused
	// TODO: just specify the tail
	// TODO: conflict -> error
	// TODO: nil or cons[A] -> list[A]
	
	@SuppressWarnings("unchecked")
	@Test
	public void trueAndFalseUnifyToBoolean() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeStruct(LoadBuiltins.trueT);
		ut.canBeStruct(LoadBuiltins.falseT);
		context.checking(new Expectations() {{
			oneOf(repository).findUnionWith((Set<Type>) with(Matchers.containsInAnyOrder(LoadBuiltins.falseT, LoadBuiltins.trueT))); will(returnValue(LoadBuiltins.bool));
		}});
		Type ty = ut.resolve();
		assertEquals(LoadBuiltins.bool, ty);
	}
	
	@Test
	public void aSingleArgConstraintJustGivesYouThatType() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.string);
		Type ty = ut.resolve();
		assertEquals(LoadBuiltins.string, ty);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void aSingleArgTypeConstraintWithAPolymorphicCtorGivesYouAnys() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.cons);
		Type ty = ut.resolve();
		assertThat(ty, PolyTypeMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
}

