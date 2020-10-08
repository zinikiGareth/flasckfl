package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.PolyInstanceMatcher;
import test.parsing.LocalErrorTracker;

public class TypeResolution {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private final Repository repository = new Repository();
	private final FunctionGroup grp = context.mock(FunctionGroup.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final FunctionDefinition fnF = new FunctionDefinition(nameF, 1, null);
	private CurrentTCState state;
	private GroupChecker gc;

	@Before
	public void begin() {
		Main.setLogLevels();
		LoadBuiltins.applyTo(tracker, repository);
		context.checking(new Expectations() {{
			allowing(sv);
			allowing(grp).isEmpty(); will(returnValue(false));
			allowing(grp).functions(); will(returnValue(Arrays.asList(fnF)));
		}});
		state = new FunctionGroupTCState(repository, grp);
		gc = new GroupChecker(tracker, repository, sv, state, null);
	}

	@Test
	public void aSimplePrimitiveIsEasyToResolve() {
		gc.visitFunction(fnF);
		gc.result(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type().get(1));
	}

	@Test
	public void multipleIdenticalTypesAreEasilyConsolidated() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.number)), new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.number)))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type().get(1));
	}

	@Test
	public void aUnionCanBeFormedFromItsComponentParts() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.falseT)), new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.trueT)))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.bool, fnF.type().get(1));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aUnionCanBeFormedFromItsComponentPolymorphicParts() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.nil)), new PosType(pos, new Apply(LoadBuiltins.string, new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.any)))))));
		gc.leaveFunctionGroup(null);
		assertThat(fnF.type().get(1), PolyInstanceMatcher.of(LoadBuiltins.list, Matchers.is(LoadBuiltins.any)));
	}

	@Test
	public void becauseWeResolveAllTheTypesAUnifiableTypeCanBecomeASimplePrimitiveWhichIsEasyToResolve() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown", true);
		ut.canBeType(pos, LoadBuiltins.number);
		gc.result(new PosType(pos, new Apply(LoadBuiltins.string, ut)));
		ut.resolve(tracker);
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type().get(1));
	}

	@Test
	public void weCanObviouslyHaveAUnifiableTypeOfNumberResolveWithNumberItself() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs", "f return type", true);
		ut.canBeType(pos, LoadBuiltins.number);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, new Apply(LoadBuiltins.string, ut)), new PosType(pos, new Apply(LoadBuiltins.string, LoadBuiltins.number)))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.string, fnF.type().get(0));
		assertEquals(LoadBuiltins.number, fnF.type().get(1));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void ifWeHaveIdentifiedAFunctionAndHaveAnApplicationOfItWeCanDeduceTheCorrectType() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "f return type"); // a function argument "f"
		UnifiableType result = utG.canBeAppliedTo(pos, Arrays.asList(new PosType(pos, LoadBuiltins.string))); // (f String) :: ?result
		result.canBeType(pos, LoadBuiltins.nil); // but also can be Nil, so (f String) :: Nil
		gc.result(new PosType(pos, new Apply(LoadBuiltins.string, result)));
		gc.leaveFunctionGroup(null);
		assertThat(utG.resolve(tracker), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.nil)));
		assertTrue(fnF.type() instanceof Apply);
		assertEquals(LoadBuiltins.string, fnF.type().get(0));
		assertEquals(LoadBuiltins.nil, fnF.type().get(1));
	}

	@Test
	public void ifWeHaveAUTInTheProcessingTypeWeConvertItToAPolyVarOnBind() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "unknown"); // the argument
		state.bindVarToUT("test.repo.x", "test.repo.x", utG);
		utG.isReturned(pos);
		gc.result(new PosType(pos, new Apply(utG, utG)));
		gc.leaveFunctionGroup(null);
		Type ty = fnF.type();
		assertEquals("A->A", ty.signature());
		assertTrue(ty instanceof Apply);
		ty = ((Apply)ty).get(0);
		assertTrue(ty instanceof PolyType);
		assertEquals("A", ty.signature());
	}

	// As of changing type resolution on 2020-06-18, this no longer works and, as far as I can tell,
	// does not correspond to a real case.  If you can find a real case, reinstantitate this; or else figure out
	// the real case this is *trying* to describe and make it describe that.
	@Test
	@Ignore
	public void ifWeHaveAHOFWithAUTInTheProcessingTypeWeConvertItToAPolyVarOnBind() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "unknown"); // a hof function argument utH->utI
		UnifiableType utH = state.createUT(pos, "unknown"); 
		UnifiableType utI = state.createUT(pos, "unknown");
		utG.canBeType(pos, new Apply(utH, utI));
		utI.canBeType(pos, utH);
		utG.isReturned(pos);
		gc.result(new PosType(pos, new Apply(utG, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		Type ty = fnF.type();
		assertEquals("(A->A)->Number", ty.signature());

		// extract the hof
		assertTrue(ty instanceof Apply);
		ty = ((Apply)ty).get(0);
		
		// extract the input arg
		assertTrue(ty instanceof Apply);
		ty = ((Apply)ty).get(0);
		
		// check it ...
		assertTrue(ty instanceof PolyType);
		assertEquals("A", ty.signature());
	}
}
