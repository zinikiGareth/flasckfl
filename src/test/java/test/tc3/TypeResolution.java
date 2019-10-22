package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.ConsolidateTypes;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TypeResolution {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final Repository repository = new Repository();
	private CurrentTCState state = new FunctionGroupTCState(repository);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final GroupChecker gc = new GroupChecker(errors, repository, sv, state);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1);

	@Before
	public void begin() {
		LoadBuiltins.applyTo(repository);
		context.checking(new Expectations() {{
			allowing(sv);
		}});
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
		gc.result(new ConsolidateTypes(Arrays.asList(LoadBuiltins.number, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void aUnionCanBeFormedFromItsComponentParts() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(Arrays.asList(LoadBuiltins.falseT, LoadBuiltins.trueT)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.bool, fnF.type());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aUnionCanBeFormedFromItsComponentPolymorphicParts() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(Arrays.asList(LoadBuiltins.nil, new PolyInstance(LoadBuiltins.cons, Arrays.asList(LoadBuiltins.any)))));
		gc.leaveFunctionGroup(null);
		assertThat(fnF.type(), PolyTypeMatcher.of(LoadBuiltins.list, Matchers.is(LoadBuiltins.any)));
	}

	@Test
	public void anyIsBasicallyIgnoredWhenWeHaveSomethingElseInAConsolidatedType() {
		gc.visitFunction(fnF);
		gc.result(new ConsolidateTypes(Arrays.asList(LoadBuiltins.number, LoadBuiltins.any)));
		gc.leaveFunctionGroup(null);
		assertThat(fnF.type(), Matchers.is(LoadBuiltins.number));
	}

	@Test
	public void becauseWeResolveAllTheTypesAUnifiableTypeCanBecomeASimplePrimitiveWhichIsEasyToResolve() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.number);
		gc.result(ut);
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@Test
	public void weCanObviouslyHaveAUnifiableTypeOfNumberResolveWithNumberItself() {
		gc.visitFunction(fnF);
		TypeConstraintSet ut = new TypeConstraintSet(repository, state, pos, "tcs");
		ut.canBeType(LoadBuiltins.number);
		gc.result(new ConsolidateTypes(Arrays.asList(ut, LoadBuiltins.number)));
		gc.leaveFunctionGroup(null);
		assertEquals(LoadBuiltins.number, fnF.type());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void ifWeHaveIdentifiedAFunctionAndHaveAnApplicationOfItWeCanDeduceTheCorrectType() {
		gc.visitFunction(fnF);
		UnifiableType utG = state.createUT(); // a function argument "f"
//		TypeConstraintSet utG = new TypeConstraintSet(repository, state, pos);
		UnifiableType result = utG.canBeAppliedTo(Arrays.asList(LoadBuiltins.string)); // (f String) :: ?result
		result.canBeType(LoadBuiltins.nil); // but also can be Nil, so (f String) :: Nil
		gc.result(result);
		gc.leaveFunctionGroup(null);
		assertThat(utG.resolve(), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.nil)));
		System.out.println(utG.resolve());
		assertEquals(LoadBuiltins.nil, fnF.type());
	}
}