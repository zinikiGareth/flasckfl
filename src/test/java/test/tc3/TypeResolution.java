package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

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
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.PolyInstanceMatcher;
import flas.matchers.ResolvedUTMatcher;

public class TypeResolution {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final Repository repository = new Repository();
	private final FunctionGroup grp = context.mock(FunctionGroup.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	private final FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
	private CurrentTCState state;
	private GroupChecker gc;

	@Before
	public void begin() {
		LoadBuiltins.applyTo(errors, repository);
		context.checking(new Expectations() {{
			allowing(sv);
			allowing(grp).isEmpty(); will(returnValue(false));
			allowing(grp).functions(); will(returnValue(Arrays.asList(fnF)));
		}});
		state = new FunctionGroupTCState(repository, grp);
		gc = new GroupChecker(errors, sv, state);
	}

	@Test
	public void aSimplePrimitiveIsEasyToResolve() {
		gc.visitFunction(fnF);
		gc.result(new PosType(pos, LoadBuiltins.number));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void multipleIdenticalTypesAreEasilyConsolidated() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.number), new PosType(pos, LoadBuiltins.number))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void aUnionCanBeFormedFromItsComponentParts() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.falseT), new PosType(pos, LoadBuiltins.trueT))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.bool, fnF.type());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aUnionCanBeFormedFromItsComponentPolymorphicParts() {
		gc.visitFunction(fnF);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.nil), new PosType(pos, new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.any))))));
		gc.leaveFunctionGroup(null);
		assertThat(fnF.type(), PolyInstanceMatcher.of(LoadBuiltins.list, Matchers.is(LoadBuiltins.any)));
	}

	@Test
	public void becauseWeResolveAllTheTypesAUnifiableTypeCanBecomeASimplePrimitiveWhichIsEasyToResolve() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeType(pos, LoadBuiltins.number);
		gc.result(new PosType(pos, ut));
		ut.resolve(errors, true);
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void weCanObviouslyHaveAUnifiableTypeOfNumberResolveWithNumberItself() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs", "unknown");
		ut.canBeType(pos, LoadBuiltins.number);
		gc.result(state.consolidate(pos, Arrays.asList(new PosType(pos, ut), new PosType(pos, LoadBuiltins.number))));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void ifWeHaveIdentifiedAFunctionAndHaveAnApplicationOfItWeCanDeduceTheCorrectType() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "unknown"); // a function argument "f"
		UnifiableType result = utG.canBeAppliedTo(pos, Arrays.asList(new PosType(pos, LoadBuiltins.string))); // (f String) :: ?result
		result.canBeType(pos, LoadBuiltins.nil); // but also can be Nil, so (f String) :: Nil
		gc.result(new PosType(pos, result));
		gc.leaveFunctionGroup(null);
		assertThat(utG.resolve(errors, true), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), ResolvedUTMatcher.with(LoadBuiltins.nil)));
		assertEquals(LoadBuiltins.nil, fnF.type());
	}

	@Test
	public void ifWeHaveAUTInTheProcessingTypeWeConvertItToAPolyVarOnBind() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "unknown"); // the argument
		state.bindVarToUT("test.repo.x", utG);
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

	@Test
	public void ifWeHaveAHOFWithAUTInTheProcessingTypeWeConvertItToAPolyVarOnBind() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(pos, "unknown"); // a hof function argument utH->utI
		UnifiableType utH = state.createUT(pos, "unknown"); 
		UnifiableType utI = state.createUT(pos, "unknown");
		utG.canBeType(pos, new Apply(utH, utI));
		utH.canBeType(pos, utI);
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
