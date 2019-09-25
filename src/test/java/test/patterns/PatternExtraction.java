package test.patterns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.patterns.HSIPatternTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class PatternExtraction {
	public interface REType extends RepositoryEntry, Type {
	}

	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final RepositoryReader r = context.mock(RepositoryReader.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	private final CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void anUntypedVariable() {
		RepositoryEntry any = context.mock(REType.class, "Any");
		context.checking(new Expectations() {{
			oneOf(state).hasVar("test.repo.fred.x"); will(returnValue(null));
			oneOf(r).get("Any"); will(returnValue(any));
		}});
		HSIPatternOptions po = new HSIPatternOptions();
		po.addVar(new VarName(pos, nameF, "x"));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(any, ty);
	}

	@Test
	public void aPolymorphicVariable() {
		UnifiableType xv = context.mock(UnifiableType.class);
		PolyType pa = new PolyType(pos, "A");
		context.checking(new Expectations() {{
			oneOf(state).hasVar("test.repo.fred.x"); will(returnValue(xv));
			oneOf(xv).resolve(); will(returnValue(pa));
		}});
		HSIPatternOptions po = new HSIPatternOptions();
		po.addVar(new VarName(pos, nameF, "x"));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(pa, ty);
	}

	@Test
	public void aTypedVariable() {
		HSIPatternOptions po = new HSIPatternOptions();
		TypeReference tr = new TypeReference(pos, "Number");
		tr.bind(LoadBuiltins.number);
		po.addTyped(tr, new VarName(pos, nameF, "v"));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(LoadBuiltins.number, ty);
	}

	@Test
	public void aUnionThatIncludesAnyWillReturnAny() {
		HSIPatternOptions po = new HSIPatternOptions();
		TypeReference tr = new TypeReference(pos, "Number");
		tr.bind(LoadBuiltins.number);
		po.addTyped(tr, new VarName(pos, nameF, "t"));
		TypeReference a = new TypeReference(pos, "Any");
		a.bind(LoadBuiltins.any);
		po.addTyped(a, new VarName(pos, nameF, "a"));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(LoadBuiltins.any, ty);
	}

	@Test
	public void simplestCaseWithJustAConstructor() {
		RepositoryEntry nil = context.mock(REType.class, "Nil");
		context.checking(new Expectations() {{
			oneOf(r).get("Nil"); will(returnValue(nil));
		}});
		HSIPatternOptions po = new HSIPatternOptions();
		po.addCM("Nil", new HSIPatternTree(0));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(nil, ty);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void caseWithTwoConstructorsThatArePartOfASingleUnion() {
		RepositoryEntry bool = context.mock(REType.class, "Boolean");
		context.checking(new Expectations() {{
			oneOf(r).findUnionWith((Set<String>) with(Matchers.contains("False", "True"))); will(returnValue(bool));
		}});
		HSIPatternOptions po = new HSIPatternOptions();
		po.addCM("True", new HSIPatternTree(0));
		po.addCM("False", new HSIPatternTree(0));
		Type ty = po.minimalType(state, r);
		assertNotNull(ty);
		assertEquals(bool, ty);
	}

}
