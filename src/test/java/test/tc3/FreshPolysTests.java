package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.UnifiableType;
import org.flasck.flas.testsupport.matchers.ApplyMatcher;
import org.flasck.flas.testsupport.matchers.PolyInstanceMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FreshPolysTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private static PackageName poly = new PackageName(true);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void weCanIntroduceANewPolyInstanceForAPolyVar() {
		UnifiableType ut = context.mock(UnifiableType.class);
		PolyType pa = new PolyType(pos, new SolidName(poly, "A"));
		context.checking(new Expectations() {{
			allowing(state).hasPoly(pa);
			oneOf(state).createUT(null, "instantiating map.A"); will(returnValue(ut));
		}});
		Type t = TypeChecker.instantiateFreshPolys(new UnresolvedVar(pos, "map"), state, new TreeMap<>(), new PosType(pos, pa), false).type;
		assertEquals(ut, t);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weCanReplaceAPolyVarInsideAnApply() {
		UnifiableType ut = context.mock(UnifiableType.class);
		PolyType pa = new PolyType(pos, new SolidName(poly, "A"));
		context.checking(new Expectations() {{
			allowing(state).hasPoly(pa);
			oneOf(state).createUT(null, "instantiating map.A"); will(returnValue(ut));
		}});
		Type t = TypeChecker.instantiateFreshPolys(new UnresolvedVar(pos, "map"), state, new TreeMap<>(), new PosType(pos, new Apply(pa, LoadBuiltins.number)), false).type;
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut), Matchers.is(LoadBuiltins.number)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weReplaceASinglePolyVarWithTheSameUTEachTime() {
		UnifiableType ut = context.mock(UnifiableType.class);
		PolyType pa = new PolyType(pos, new SolidName(poly, "A"));
		context.checking(new Expectations() {{
			allowing(state).hasPoly(pa);
			oneOf(state).createUT(null, "instantiating map.A"); will(returnValue(ut));
		}});
		Type t = TypeChecker.instantiateFreshPolys(new UnresolvedVar(pos, "map"), state, new TreeMap<>(), new PosType(pos, new Apply(pa, pa)), false).type;
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut), Matchers.is(ut)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weReplaceDifferentPolyVarsWithSeparateUTs() {
		UnifiableType ut1 = context.mock(UnifiableType.class, "ut1");
		UnifiableType ut2 = context.mock(UnifiableType.class, "ut2");
		PolyType pa = new PolyType(pos, new SolidName(poly, "A"));
		PolyType pb = new PolyType(pos, new SolidName(poly, "B"));
		context.checking(new Expectations() {{
			allowing(state).hasPoly(pa);
			allowing(state).hasPoly(pb);
			oneOf(state).createUT(null, "instantiating map.A"); will(returnValue(ut1));
			oneOf(state).createUT(null, "instantiating map.B"); will(returnValue(ut2));
		}});
		Type t = TypeChecker.instantiateFreshPolys(new UnresolvedVar(pos, "map"), state, new TreeMap<>(), new PosType(pos, new Apply(pa, pb, pa, pb)), false).type;
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut1), Matchers.is(ut2), Matchers.is(ut1), Matchers.is(ut2)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void weCanReplaceAPolyVarInsideAnStructDefn() {
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			allowing(state).hasPoly(LoadBuiltins.cons.polys().get(0));
			allowing(state).hasPoly(LoadBuiltins.list.polys().get(0));
			oneOf(state).createUT(null, "instantiating map.A"); will(returnValue(ut));
		}});
		Type t = TypeChecker.instantiateFreshPolys(new UnresolvedVar(pos, "map"), state, new TreeMap<>(), new PosType(pos, LoadBuiltins.cons), false).type;
		assertThat(t, (Matcher)ApplyMatcher.type(Matchers.is(ut), PolyInstanceMatcher.of(LoadBuiltins.list, Matchers.is(ut)), PolyInstanceMatcher.of(LoadBuiltins.cons, Matchers.is(ut))));
	}
}
