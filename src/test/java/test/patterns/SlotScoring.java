package test.patterns;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.LoadBuiltins;
import org.junit.Test;

public class SlotScoring {
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");

	@Test
	public void justAVarScoresZero() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addVar(new VarPattern(pos, new VarName(pos, nameF, "v")), null);
		assertEquals(0, tree.score());
	}

	@Test
	public void oneTypeScores1() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "List").bind(LoadBuiltins.list), new VarName(pos, nameF, "t")), null);
		assertEquals(1, tree.score());
	}

	@Test
	public void twoTypesScores2() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "List").bind(LoadBuiltins.list), new VarName(pos, nameF, "t")), null);
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, nameF, "n")), null);
		assertEquals(2, tree.score());
	}

	@Test
	public void aConstructorScores3() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.requireCM(LoadBuiltins.nil);
		assertEquals(3, tree.score());
	}

	@Test
	public void eachConstructorScores3() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.requireCM(LoadBuiltins.nil);
		tree.requireCM(LoadBuiltins.cons);
		assertEquals(6, tree.score());
	}

	@Test
	public void letsFaceItAnyIsNotATypeRestriction() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "List").bind(LoadBuiltins.list), new VarName(pos, nameF, "t")), null);
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "Any").bind(LoadBuiltins.any), new VarName(pos, nameF, "v")), null);
		assertEquals(1, tree.score());
	}

	@Test
	public void checkThingsAddUpTheWayYouWouldExpect() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.requireCM(LoadBuiltins.nil);
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "List").bind(LoadBuiltins.list), new VarName(pos, nameF, "t")), null);
		tree.requireCM(LoadBuiltins.cons);
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), new VarName(pos, nameF, "n")), null);
		tree.addTyped(new TypedPattern(pos, new TypeReference(pos, "Any").bind(LoadBuiltins.any), new VarName(pos, nameF, "v")), null);
		tree.addVar(new VarPattern(pos, new VarName(pos, nameF, "v")), null);
		assertEquals(8, tree.score());
	}

}
