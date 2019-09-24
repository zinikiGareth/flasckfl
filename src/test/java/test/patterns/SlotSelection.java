package test.patterns;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.patterns.HSIPatternTree;
import org.junit.Test;

public class SlotSelection {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");

	@Test
	public void theTrivialCaseAlwaysGivesZero() {
		HSIPatternTree tree = new HSIPatternTree(1);
		assertEquals(0, tree.selectSlot());
	}

	@Test
	public void aConstructorIsPreferredToAVar() {
		HSIPatternTree tree = new HSIPatternTree(2);
		tree.get(0).addCM("Nil", new HSIPatternTree(0));
		tree.get(1).addVar(new VarName(pos, nameF, "x"));
		assertEquals(0, tree.selectSlot());
	}

	@Test
	public void aConstructorIsPreferredToAVarEvenIfLater() {
		HSIPatternTree tree = new HSIPatternTree(2);
		tree.get(0).addVar(new VarName(pos, nameF, "x"));
		tree.get(1).addCM("Nil", new HSIPatternTree(0));
		assertEquals(1, tree.selectSlot());
	}

}
