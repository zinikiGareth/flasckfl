package org.flasck.flas.parser;

import static org.junit.Assert.*;

import java.util.List;

import org.flasck.flas.parsedForm.Block;
import org.junit.Test;

public class BlockParsingTests {

	@Test
	public void testBlockerProducesSensibleBlocks() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n");
		assertEquals(1, blocks.size());
		Block b = blocks.get(0);
		assertEquals(1, b.line.lines.size());
		assertEquals(1, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.ziniki", b.line.lines.get(0).line);
		assertEquals(1, b.nested.size());
		Block nb = b.nested.get(0);
		assertEquals(1, nb.line.lines.size());
		assertEquals(2, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("fib n = fib (n-1) + fib (n-2)", nb.line.lines.get(0).line);
		assertEquals(0, nb.nested.size());
	}

	@Test
	public void testBlockerCanHandleContinuations() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1)\n\t\t  + fib (n-2)\n");
		assertEquals(1, blocks.size());
		Block b = blocks.get(0);
		assertEquals(1, b.line.lines.size());
		assertEquals(1, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.ziniki", b.line.lines.get(0).line);
		assertEquals(1, b.nested.size());
		Block nb = b.nested.get(0);
		assertEquals(2, nb.line.lines.size());
		assertEquals(2, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("fib n = fib (n-1)", nb.line.lines.get(0).line);
		assertEquals(3, nb.line.lines.get(1).lineNo);
		assertEquals(2, nb.line.lines.get(1).indent.tabs);
		assertEquals(2, nb.line.lines.get(1).indent.spaces);
		assertEquals("+ fib (n-2)", nb.line.lines.get(1).line);
		assertEquals(0, nb.nested.size());
	}

	
	@Test
	public void testCommentsDontGetLost() {
		List<Block> blocks = Blocker.block("hello\n\tpackage org.ziniki\nfred\n\t\tfib n = fib (n-1) + fib (n-2)\nbert\n");
		assertEquals(2, blocks.size());
		Block b = blocks.get(0);
		assertEquals(1, b.line.lines.size());
		assertEquals(1, b.line.lines.get(0).lineNo);
		assertNull(b.line.lines.get(0).indent);
		assertEquals("hello", b.line.lines.get(0).line);
		b = blocks.get(1);
		assertEquals(1, b.line.lines.size());
		assertEquals(2, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.ziniki", b.line.lines.get(0).line);
		assertEquals(2, b.nested.size());
		Block nb = b.nested.get(0);
		assertEquals(1, nb.line.lines.size());
		assertEquals(3, nb.line.lines.get(0).lineNo);
		assertNull(nb.line.lines.get(0).indent);
		assertEquals("fred", nb.line.lines.get(0).line);
		nb = b.nested.get(1);
		assertEquals(4, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("fib n = fib (n-1) + fib (n-2)", nb.line.lines.get(0).line);
		assertEquals(1, nb.nested.size());
		nb = nb.nested.get(0);
		assertEquals(1, nb.line.lines.size());
		assertNull(nb.line.lines.get(0).indent);
		assertEquals("bert", nb.line.lines.get(0).line);
		assertEquals(0, nb.nested.size());
	}

	@Test
	public void testBlockerCanHandleExdenting() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n\tpackage org.cardstack\n");
		assertEquals(2, blocks.size());
		Block b = blocks.get(0);
		assertEquals(1, b.line.lines.size());
		assertEquals(1, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.ziniki", b.line.lines.get(0).line);
		assertEquals(1, b.nested.size());
		Block nb = b.nested.get(0);
		assertEquals(1, nb.line.lines.size());
		assertEquals(2, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("fib n = fib (n-1) + fib (n-2)", nb.line.lines.get(0).line);
		assertEquals(0, nb.nested.size());
		b = blocks.get(1);
		assertEquals(1, b.line.lines.size());
		assertEquals(3, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.cardstack", b.line.lines.get(0).line);
		assertEquals(0, b.nested.size());
	}

	@Test
	public void testBlockerCanHandleMultipleDefnsAtSameScope() {
		List<Block> blocks = Blocker.block("\tpackage org.ziniki\n\t\tfib n = fib (n-1) + fib (n-2)\n\t\tg n a b = g (n-1) (a+b) a\n");
		showBlocks(0, blocks);
		assertEquals(1, blocks.size());
		Block b = blocks.get(0);
		assertEquals(1, b.line.lines.size());
		assertEquals(1, b.line.lines.get(0).lineNo);
		assertEquals(1, b.line.lines.get(0).indent.tabs);
		assertEquals(0, b.line.lines.get(0).indent.spaces);
		assertEquals("package org.ziniki", b.line.lines.get(0).line);
		assertEquals(2, b.nested.size());
		Block nb = b.nested.get(0);
		assertEquals(1, nb.line.lines.size());
		assertEquals(2, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("fib n = fib (n-1) + fib (n-2)", nb.line.lines.get(0).line);
		assertEquals(0, nb.nested.size());
		nb = b.nested.get(1);
		assertEquals(1, nb.line.lines.size());
		assertEquals(3, nb.line.lines.get(0).lineNo);
		assertEquals(2, nb.line.lines.get(0).indent.tabs);
		assertEquals(0, nb.line.lines.get(0).indent.spaces);
		assertEquals("g n a b = g (n-1) (a+b) a", nb.line.lines.get(0).line);
		assertEquals(0, nb.nested.size());
	}

	private void showBlocks(int ind, List<Block> blocks) {
		for (Block b : blocks) {
			showBlock(ind, b);
		}
	}

	private void showBlock(int ind, Block b) {
		for (int i=0;i<ind;i++) {
			System.out.print(' ');
		}
		System.out.print(b.line.lines.get(0).lineNo);
		System.out.print(": ");
		System.out.print(b.line.lines.get(0).line);
		System.out.println();
		showBlocks(ind+2, b.nested);
	}
}
