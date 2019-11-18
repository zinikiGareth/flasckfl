package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.ConsolidateTypes;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.GroupChecker;
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
import flas.matchers.PolyTypeMatcher;

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
		LoadBuiltins.applyTo(repository);
		context.checking(new Expectations() {{
			allowing(sv);
			allowing(grp).functions(); will(returnValue(Arrays.asList(fnF)));
		}});
		state = new FunctionGroupTCState(repository, grp);
		gc = new GroupChecker(errors, repository, sv, state);
	}

	@Test
	public void aSimplePrimitiveIsEasyToResolve() {
		gc.visitFunction(fnF);
		gc.result(LoadBuiltins.number);
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void multipleIdenticalTypesAreEasilyConsolidated() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(pos, Arrays.asList(LoadBuiltins.number, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void aUnionCanBeFormedFromItsComponentParts() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(pos, Arrays.asList(LoadBuiltins.falseT, LoadBuiltins.trueT)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.bool, fnF.type());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aUnionCanBeFormedFromItsComponentPolymorphicParts() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(pos, Arrays.asList(LoadBuiltins.nil, new PolyInstance(LoadBuiltins.cons, Arrays.asList(LoadBuiltins.any)))));
		gc.leaveFunctionGroup(null);
		assertThat(fnF.type(), PolyTypeMatcher.of(LoadBuiltins.list, Matchers.is(LoadBuiltins.any)));
	}

	@Test
	public void becauseWeResolveAllTheTypesAUnifiableTypeCanBecomeASimplePrimitiveWhichIsEasyToResolve() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.number);
		gc.result(ut);
		ut.resolve(true);
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void weCanObviouslyHaveAUnifiableTypeOfNumberResolveWithNumberItself() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.number);
		gc.result(new ConsolidateTypes(pos, Arrays.asList(ut, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void ifWeHaveIdentifiedAFunctionAndHaveAnApplicationOfItWeCanDeduceTheCorrectType() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(); // a function argument "f"
		UnifiableType result = utG.canBeAppliedTo(Arrays.asList(LoadBuiltins.string)); // (f String) :: ?result
		result.canBeType(LoadBuiltins.nil); // but also can be Nil, so (f String) :: Nil
		gc.result(result);
		gc.leaveFunctionGroup(null);
		assertThat(utG.resolve(), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.nil)));
		assertEquals(LoadBuiltins.nil, fnF.type());
	}

	@Test
	public void ifWeHaveAUTInTheProcessingTypeWeConvertItToAPolyVarOnBind() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(); // the argument
		state.bindVarToUT("test.repo.x", utG);
		utG.isReturned();
		gc.result(new Apply(utG, utG));
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
		UnifiableType utG = state.createUT(); // a hof function argument utH->utI
		UnifiableType utH = state.createUT(); 
		UnifiableType utI = state.createUT();
		utG.canBeType(new Apply(utH, utI));
		utH.canBeType(utI);
		utG.isReturned();
		gc.result(new Apply(utG, LoadBuiltins.number));
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
