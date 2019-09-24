package test.patterns;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.patterns.HSIPatternTree;
import org.junit.Test;

public class SlotScoring {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");

	@Test
	public void justAVarScoresZero() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addVar(new VarName(pos, nameF, "v"));
		assertEquals(0, tree.score());
	}

	@Test
	public void oneTypeScores1() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypeReference(pos, "List"), new VarName(pos, nameF, "t"));
		assertEquals(1, tree.score());
	}

	@Test
	public void twoTypesScores2() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypeReference(pos, "List"), new VarName(pos, nameF, "t"));
		tree.addTyped(new TypeReference(pos, "Number"), new VarName(pos, nameF, "n"));
		assertEquals(2, tree.score());
	}

	@Test
	public void aConstructorScores3() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addCM("Nil", new HSIPatternTree(0));
		assertEquals(3, tree.score());
	}

	@Test
	public void eachConstructorScores3() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addCM("Nil", new HSIPatternTree(0));
		tree.addCM("Cons", new HSIPatternTree(2));
		assertEquals(6, tree.score());
	}

	@Test
	public void letsFaceItAnyIsNotATypeRestriction() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addTyped(new TypeReference(pos, "List"), new VarName(pos, nameF, "t"));
		tree.addTyped(new TypeReference(pos, "Any"), new VarName(pos, nameF, "v"));
		assertEquals(1, tree.score());
	}

	@Test
	public void checkThingsAddUpTheWayYouWouldExpect() {
		HSIPatternOptions tree = new HSIPatternOptions();
		tree.addCM("Nil", new HSIPatternTree(0));
		tree.addTyped(new TypeReference(pos, "List"), new VarName(pos, nameF, "t"));
		tree.addCM("Cons", new HSIPatternTree(2));
		tree.addTyped(new TypeReference(pos, "Number"), new VarName(pos, nameF, "n"));
		tree.addTyped(new TypeReference(pos, "Any"), new VarName(pos, nameF, "v"));
		tree.addVar(new VarName(pos, nameF, "v"));
		assertEquals(8, tree.score());
	}

}
