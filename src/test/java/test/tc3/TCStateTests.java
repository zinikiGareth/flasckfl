package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.PolyInstanceMatcher;
import flas.matchers.PolyTypeMatcher;
import flas.matchers.ResolvedUTMatcher;

public class TCStateTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	FunctionDefinition fnG = new FunctionDefinition(nameG, 1);
	List<Pattern> args = new ArrayList<>();
	FunctionIntro fiF1 = new FunctionIntro(nameF, args);
	FunctionIntro fiF2 = new FunctionIntro(nameF, args);
	FunctionIntro fiG1 = new FunctionIntro(nameG, args);
	FunctionIntro fiG2 = new FunctionIntro(nameG, args);
	private FunctionGroup grp = new DependencyGroup(fnF, fnG);
	private final Repository repository = new Repository();
	CurrentTCState state = new FunctionGroupTCState(repository, grp);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);

	@Before
	public void begin() {
		LoadBuiltins.applyTo(repository);
		context.checking(new Expectations() {{
			fnF.intro(fiF1);
			fnF.intro(fiF2);
			fnG.intro(fiG1);
			fnG.intro(fiG2);
		}});
	}

	@Test
	public void aNewStateInitializesItsGroupMembersAsUTs() {
		state.requireVarConstraints(pos, nameF.uniqueName());
		state.requireVarConstraints(pos, nameG.uniqueName());
	}

	@Test
	public void aNumberConsolidatesToItself() {
		assertEquals(LoadBuiltins.number, state.consolidate(pos, Arrays.asList(LoadBuiltins.number)));
	}

	@Test
	public void twoNumbersConsolidateToNumber() {
		assertEquals(LoadBuiltins.number, state.consolidate(pos, Arrays.asList(LoadBuiltins.number, LoadBuiltins.number)));
	}
	
	@Test
	public void trueAndFalseMakeAList() {
		Type ut = state.consolidate(pos, Arrays.asList(LoadBuiltins.trueT, LoadBuiltins.falseT));
		assertTrue(ut instanceof TypeConstraintSet);
		assertEquals(LoadBuiltins.bool, ((UnifiableType) ut).resolve(errors, true));
	}
	
	@Test
	public void trueAndFalseMakeAListInTheOtherOrder() {
		Type ut = state.consolidate(pos, Arrays.asList(LoadBuiltins.falseT, LoadBuiltins.trueT));
		assertTrue(ut instanceof TypeConstraintSet);
		assertEquals(LoadBuiltins.bool, ((UnifiableType) ut).resolve(errors, true));
	}
	
	@Test
	public void youCannotMakeAUnionOfNumberAndString() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "unable to unify Number, String");
		}});
		Type ut = state.consolidate(pos, Arrays.asList(LoadBuiltins.number, LoadBuiltins.string));
		assertTrue(ut instanceof TypeConstraintSet);
		((UnifiableType) ut).resolve(errors, true);
	}
	
	@Test
	public void consolidatingErrorWithAnythingIsError() {
		assertTrue(state.consolidate(pos, Arrays.asList(new ErrorType(), LoadBuiltins.nil)) instanceof ErrorType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSimpleApplyReturnsItself() {
		assertThat(state.consolidate(pos, Arrays.asList(new Apply(LoadBuiltins.string, LoadBuiltins.number))), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void anApplyCanConsolidateTrueAndFalseToBool() {
		Type result = state.consolidate(pos, Arrays.asList(LoadBuiltins.trueT, LoadBuiltins.falseT));
		Type apply = state.consolidate(pos, Arrays.asList(new Apply(LoadBuiltins.string, result)));
		assertThat(apply, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.any(UnifiableType.class)));
		state.resolveAll(errors, true);
		assertThat(apply, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void twoApplysCanConsolidateTrueAndFalseToBool() {
		UnifiableType apply = (UnifiableType) state.consolidate(pos, Arrays.asList(new Apply(LoadBuiltins.string, LoadBuiltins.falseT), new Apply(LoadBuiltins.string, LoadBuiltins.trueT)));
		assertThat(apply, (Matcher)Matchers.any(UnifiableType.class));
		state.resolveAll(errors, false);
		state.enhanceAllMutualUTs();
		state.resolveAll(errors, true);
		assertThat(apply.resolve(errors, true), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aPolyInstanceHasItsParametersConsolidated() {
		Type result = state.consolidate(pos, Arrays.asList(LoadBuiltins.trueT, LoadBuiltins.falseT));
		PolyInstance pi = new PolyInstance(LoadBuiltins.cons, Arrays.asList(result));
		state.resolveAll(errors, true);
		assertThat(pi, (Matcher)PolyInstanceMatcher.of(LoadBuiltins.cons, ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}
	
	@Test
	public void aTCSWithNothingDoingReturnsAny() {
		Type ut = state.consolidate(pos, Arrays.asList(state.createUT(pos)));
		state.resolveAll(errors, true);
		assertThat(ut, ResolvedUTMatcher.with(LoadBuiltins.any));
	}
	
	@Test
	public void aUnionOfATypeAndAnEmptyTCSDoesTheRightThing() {
		Type c1 = state.consolidate(pos, Arrays.asList(state.createUT(pos), LoadBuiltins.number));
		state.resolveAll(errors, false);
		state.resolveAll(errors, true);
		assertThat(c1, ResolvedUTMatcher.with(LoadBuiltins.number));
	}

	@Test
	public void aUnionOfATypeAndAResolvedTCSDoesTheRightThing() {
		UnifiableType tcs = state.createUT(pos);
		tcs.isReturned(); // fake it to have been used in multiple places
		Type ct = state.consolidate(pos, Arrays.asList(tcs, LoadBuiltins.number));
		state.resolveAll(errors, false);
		state.resolveAll(errors, true);
		assertThat(ct, ResolvedUTMatcher.with(LoadBuiltins.number));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void consolidatingTwoUTsMeansTheyWillResolveToTheSamePoly() {
		UnifiableType tcs1 = state.createUT(pos);
		UnifiableType tcs2 = state.createUT(pos);
		tcs1.isReturned();
		tcs2.isUsed();
		UnifiableType ct = (UnifiableType) state.consolidate(pos, Arrays.asList(tcs1, tcs2));
		state.resolveAll(errors, false);
		state.enhanceAllMutualUTs();
		state.resolveAll(errors, true);
		assertTrue(ct.isResolved());
		assertThat(ct.resolve(errors, true), (Matcher)PolyTypeMatcher.called("A"));
	}
}
