package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
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
import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.flasck.flas.testsupport.matchers.ApplyMatcher;
import org.flasck.flas.testsupport.matchers.PolyInstanceMatcher;
import org.flasck.flas.testsupport.matchers.PolyTypeMatcher;
import org.flasck.flas.testsupport.matchers.ResolvedUTMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TCStateTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1, null);
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	FunctionDefinition fnG = new FunctionDefinition(nameG, 1, null);
	List<Pattern> args = new ArrayList<>();
	FunctionIntro fiF1 = new FunctionIntro(nameF, args);
	FunctionIntro fiF2 = new FunctionIntro(nameF, args);
	FunctionIntro fiG1 = new FunctionIntro(nameG, args);
	FunctionIntro fiG2 = new FunctionIntro(nameG, args);
	private FunctionGroup grp = new DependencyGroup(fnF, fnG);
	private final Repository repository = new Repository();
	CurrentTCState state = new FunctionGroupTCState(repository, grp);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final LocalErrorTracker tracker = new LocalErrorTracker(errors);

	@Before
	public void begin() {
		LoadBuiltins.applyTo(tracker, repository);
		context.checking(new Expectations() {{
			fnF.intro(fiF1);
			fnF.intro(fiF2);
			fnG.intro(fiG1);
			fnG.intro(fiG2);
		}});
	}

	@Test
	public void aNewStateInitializesItsGroupMembersAsUTs() {
		state.requireVarConstraints(pos, nameF.uniqueName(), nameF.uniqueName());
		state.requireVarConstraints(pos, nameG.uniqueName(), nameG.uniqueName());
	}

	@Test
	public void aNumberConsolidatesToItself() {
		assertEquals(LoadBuiltins.number, state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.number))).type);
	}

	@Test
	public void twoNumbersConsolidateToNumber() {
		assertEquals(LoadBuiltins.number, state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.number), new PosType(pos, LoadBuiltins.number))).type);
	}
	
	@Test
	public void trueAndFalseMakeAList() {
		Type ut = state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.trueT), new PosType(pos, LoadBuiltins.falseT))).type;
		assertTrue(ut instanceof TypeConstraintSet);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.bool, ((UnifiableType) ut).resolve(tracker));
	}
	
	@Test
	public void trueAndFalseMakeAListInTheOtherOrder() {
		Type ut = state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.falseT), new PosType(pos, LoadBuiltins.trueT))).type;
		assertTrue(ut instanceof TypeConstraintSet);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertEquals(LoadBuiltins.bool, ((UnifiableType) ut).resolve(tracker));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void youCannotMakeAUnionOfNumberAndString() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(pos), with(any(Set.class)), with("cannot unify [Number, String]"));
		}});
		Type ut = state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.number), new PosType(pos, LoadBuiltins.string))).type;
		assertTrue(ut instanceof TypeConstraintSet);
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		((UnifiableType) ut).resolve(tracker);
	}
	
	@Test
	public void consolidatingErrorWithAnythingIsError() {
		assertTrue(state.consolidate(pos, Arrays.asList(new PosType(pos, new ErrorType()), new PosType(pos, LoadBuiltins.nil))).type instanceof ErrorType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSimpleApplyReturnsItself() {
		assertThat(state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.number)))).type, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void anApplyCanConsolidateTrueAndFalseToBool() {
		Type result = state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.trueT), new PosType(pos, LoadBuiltins.falseT))).type;
		Type apply = state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, result)))).type;
		assertThat(apply, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.any(UnifiableType.class)));
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(apply, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void twoApplysCanConsolidateTrueAndFalseToBool() {
		Apply apply = (Apply) state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.falseT)), new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.trueT)))).type;
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(apply, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aPolyInstanceHasItsParametersConsolidated() {
		Type result = state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.trueT), new PosType(pos, LoadBuiltins.falseT))).type;
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(result));
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(pi, (Matcher)PolyInstanceMatcher.of(LoadBuiltins.cons, ResolvedUTMatcher.with(LoadBuiltins.bool)));
	}
	
	@Test
	public void aTCSWithNothingDoingReturnsAny() {
		Type ut = state.consolidate(pos, Arrays.asList(new PosType(pos, state.createUT(pos, "unknown")))).type;
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ut, ResolvedUTMatcher.with(LoadBuiltins.any));
	}
	
	@Test
	public void aUnionOfATypeAndAnEmptyTCSDoesTheRightThing() {
		Type c1 = state.consolidate(pos, Arrays.asList(new PosType(pos, state.createUT(pos, "unknown")), new PosType(pos, LoadBuiltins.number))).type;
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(c1, ResolvedUTMatcher.with(LoadBuiltins.number));
	}

	@Test
	public void aUnionOfATypeAndAResolvedTCSDoesTheRightThing() {
		UnifiableType tcs = state.createUT(pos, "unknown");
		tcs.isReturned(pos); // fake it to have been used in multiple places
		Type ct = state.consolidate(pos, Arrays.asList(new PosType(pos, tcs), new PosType(pos, LoadBuiltins.number))).type;
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ct, ResolvedUTMatcher.with(LoadBuiltins.number));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void consolidatingTwoUTsMeansTheyWillResolveToTheSamePoly() {
		UnifiableType tcs1 = state.createUT(pos, "unknown");
		UnifiableType tcs2 = state.createUT(pos, "unknown");
		tcs1.isReturned(pos);
		tcs2.isUsed(pos);
		UnifiableType ct = (UnifiableType) state.consolidate(pos, Arrays.asList(new PosType(pos, tcs1), new PosType(pos, tcs2))).type;
		state.groupDone(tracker, new HashMap<>(), new HashMap<>());
		assertThat(ct.resolvedTo(), (Matcher)PolyTypeMatcher.called("A"));
	}
}
