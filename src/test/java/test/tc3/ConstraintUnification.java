package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.UnionTypeDefn.Unifier;
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

import flas.matchers.PolyInstanceMatcher;

public class ConstraintUnification {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);

	@Test
	public void ifWeDontDoAnythingWeEndUpWithAny() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		assertEquals(LoadBuiltins.any, ut.resolve(errors, true));
	}

	@Test
	public void oneIncoporatedByConstraintCreatesAnIdentity() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.incorporatedBy(pos, LoadBuiltins.number);
		Type ty = ut.resolve(errors, true);
		assertEquals(LoadBuiltins.number, ty);
	}
	
	@Test
	public void ifYouAskSomethingToBeANilItWillBe() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeStruct(pos, LoadBuiltins.nil);
		Type ty = ut.resolve(errors, true);
		assertEquals(LoadBuiltins.nil, ty);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsAndDontConstrainItYouGetAnyAsThePoly() {
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "poly var A"); will(returnValue(new TypeConstraintSet(repository, state, pos, "A", "poly var A")));
		}});
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeStruct(pos, LoadBuiltins.cons);
		assertThat(ut.resolve(errors, true), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadNotConstrainedYouStillGetAny() {
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "poly var A"); will(returnValue(new TypeConstraintSet(repository, state, pos, "A", "poly var A")));
		}});
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		StructTypeConstraints stc = ut.canBeStruct(pos, LoadBuiltins.cons);
		PolyType pt = new PolyType(pos, new SolidName(null, "A"));
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "field head"); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld", "field head")));
			oneOf(state).nextPoly(pos); will(returnValue(pt));
		}});
		stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		assertThat(ut.resolve(errors, true), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(pt)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouAskSomethingToBeAConsWithHeadSpecifiedThatsTheType() {
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "poly var A"); will(returnValue(new TypeConstraintSet(repository, state, pos, "A", "poly var A")));
		}});
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		StructTypeConstraints stc = ut.canBeStruct(pos, LoadBuiltins.cons);
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "field head"); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld", "field head")));
		}});
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.canBeStruct(pos, LoadBuiltins.falseT);
		assertThat(ut.resolve(errors, true), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.falseT)));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void ifYouReturnAVarThenYouGetAFreshPolyVar() {
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "poly var A"); will(returnValue(new TypeConstraintSet(repository, state, pos, "A", "poly var A")));
		}});
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		StructTypeConstraints stc = ut.canBeStruct(pos, LoadBuiltins.cons);
		context.checking(new Expectations() {{
			oneOf(state).createUT(pos, "field head"); will(returnValue(new TypeConstraintSet(repository, state, pos, "fld", "field head")));
		}});
		UnifiableType f = stc.field(state, pos, LoadBuiltins.cons.findField("head"));
		f.isReturned(pos);

		PolyType polyA = new PolyType(pos, new SolidName(null, "A"));
		context.checking(new Expectations() {{
			oneOf(state).nextPoly(pos); will(returnValue(polyA));
		}});
		
		assertThat(f.resolve(errors, true), Matchers.is(polyA));
		assertThat(ut.resolve(errors, true), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(polyA)));
	}

	// TODO: head as a var leads to Cons[A] unless the var is completely unused
	// TODO: just specify the tail
	// TODO: conflict -> error
	// TODO: nil or cons[A] -> list[A]
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void trueAndFalseUnifyToBoolean() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeStruct(pos, LoadBuiltins.trueT);
		ut.canBeStruct(pos, LoadBuiltins.falseT);
		context.checking(new Expectations() {{
			oneOf(repository).findUnionWith((Set) with(Matchers.containsInAnyOrder(LoadBuiltins.falseT, LoadBuiltins.trueT)), with(any(Unifier.class))); will(returnValue(LoadBuiltins.bool));
		}});
		Type ty = ut.resolve(errors, true);
		assertEquals(LoadBuiltins.bool, ty);
	}
	
	@Test
	public void aSingleArgConstraintJustGivesYouThatType() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeType(pos, LoadBuiltins.string);
		Type ty = ut.resolve(errors, true);
		assertEquals(LoadBuiltins.string, ty);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void aSingleArgTypeConstraintWithAPolymorphicCtorGivesYouAnys() {
		UnifiableType ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeType(pos, LoadBuiltins.cons);
		Type ty = ut.resolve(errors, true);
		assertThat(ty, PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(LoadBuiltins.any)));
	}
}

