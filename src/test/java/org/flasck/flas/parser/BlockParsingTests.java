package org.flasck.flas.parser;

import static org.junit.Assert.*;

import java.util.List;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.SingleLine;
import org.flasck.flas.sampleData.BlockTestData;
import org.junit.Test;

public class BlockParsingTests {

	@Test
	public void testBlockerProducesSensibleBlocks() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n");
		assertEquals(1, blocks.size());
		BlockTestData.assertBlocksEqual(BlockTestData.packageAndFibN(), blocks.get(0));
	}

	@Test
	public void testBlockerCanHandleContinuations() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1)\n\t\t  + fib (n-2)\n");
		assertEquals(1, blocks.size());
		BlockTestData.assertBlocksEqual(BlockTestData.packageAndFibNSplit(), blocks.get(0));
	}

	@Test
	public void testCommentsDontGetLost() {
		List<Block> blocks = Blocker.block("hello\n\tpackage org.ziniki\nfred\n\t\tfib n = fib (n-1) + fib (n-2)\nbert\n");
		assertEquals(2, blocks.size());
		BlockTestData.assertBlocksEqual(BlockTestData.comment("hello"), blocks.get(0));
		BlockTestData.assertBlocksEqual(BlockTestData.packageWithComments(), blocks.get(1));
	}

	@Test
	public void testBlockerCanHandleExdenting() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n\tpackage org.cardstack\n");
		assertEquals(2, blocks.size());
		BlockTestData.assertBlocksEqual(BlockTestData.packageAndFibN(), blocks.get(0));
		BlockTestData.assertBlocksEqual(BlockTestData.packageCardStack(), blocks.get(1));
	}

	@Test
	public void testBlockerCanHandleMultipleDefnsAtSameScope() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n\t\tg n a b = g (n-1) (a+b) a\n");
		assertEquals(1, blocks.size());
		BlockTestData.assertBlocksEqual(BlockTestData.packageAndFibNWithG(), blocks.get(0));
	}

	public static void showBlocks(int ind, List<Block> blocks) {
		for (Block b : blocks) {
			showBlock(ind, b);
		}
	}

	public static void showBlock(int ind, Block b) {
		for (SingleLine sl : b.line.lines) {
			for (int i=0;i<ind;i++) {
				System.out.print(' ');
			}
			System.out.print(sl.lineNo);
			System.out.print(": ");
			System.out.print(sl.line);
			System.out.println();
		}
		showBlocks(ind+2, b.nested);
	}
}
