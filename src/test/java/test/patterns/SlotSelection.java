package test.patterns;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Traverser;
import org.junit.Test;

public class SlotSelection {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");

	@Test
	public void theTrivialCaseAlwaysGivesZero() {
		ArgSlot s0 = new ArgSlot(0, new HSIPatternOptions());
		assertEquals(s0, Traverser.selectSlot(Arrays.asList(s0)));
	}

	@Test
	public void aConstructorIsPreferredToAVar() {
		HSIArgsTree tree = new HSIArgsTree(2);
		tree.get(0).requireCM(LoadBuiltins.nil);
		tree.get(1).addVar(new VarName(pos, nameF, "x"), null);
		ArgSlot s0 = new ArgSlot(0, tree.get(0));
		ArgSlot s1 = new ArgSlot(1, tree.get(1));
		assertEquals(s0, Traverser.selectSlot(Arrays.asList(s0, s1)));
	}

	@Test
	public void aConstructorIsPreferredToAVarEvenIfLater() {
		HSIArgsTree tree = new HSIArgsTree(2);
		tree.get(0).addVar(new VarName(pos, nameF, "x"), null);
		tree.get(1).requireCM(LoadBuiltins.nil);
		ArgSlot s0 = new ArgSlot(0, tree.get(0));
		ArgSlot s1 = new ArgSlot(1, tree.get(1));
		assertEquals(s1, Traverser.selectSlot(Arrays.asList(s0, s1)));
	}

}
