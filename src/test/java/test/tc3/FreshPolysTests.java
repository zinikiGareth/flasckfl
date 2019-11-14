package test.tc3;

import static org.junit.Assert.*;

import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.ApplyExpressionChecker;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;

public class FreshPolysTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void weCanIntroduceANewPolyInstanceForAPolyVar() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(null, state, null);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(ut));
		}});
		Type t = aec.instantiateFreshPolys(new TreeMap<>(), new PolyType(pos, "A"));
		assertEquals(ut, t);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weCanReplaceAPolyVarInsideAnApply() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(null, state, null);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(ut));
		}});
		Type t = aec.instantiateFreshPolys(new TreeMap<>(), new Apply(new PolyType(pos, "A"), LoadBuiltins.number));
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weReplaceASinglePolyVarWithTheSameUTEachTime() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(null, state, null);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(ut));
		}});
		Type t = aec.instantiateFreshPolys(new TreeMap<>(), new Apply(new PolyType(pos, "A"), new PolyType(pos, "A")));
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut), Matchers.is(ut)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weReplaceDifferentPolyVarsWithSeparateUTs() {
		ApplyExpressionChecker aec = new ApplyExpressionChecker(null, state, null);
		UnifiableType ut1 = context.mock(UnifiableType.class, "ut1");
		UnifiableType ut2 = context.mock(UnifiableType.class, "ut2");
		context.checking(new Expectations() {{
			oneOf(state).createUT(); will(returnValue(ut1));
			oneOf(state).createUT(); will(returnValue(ut2));
		}});
		Type t = aec.instantiateFreshPolys(new TreeMap<>(), new Apply(new PolyType(pos, "A"), new PolyType(pos, "B"), new PolyType(pos, "A"), new PolyType(pos, "B")));
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut1), Matchers.is(ut2), Matchers.is(ut1), Matchers.is(ut2)));
	}
}
