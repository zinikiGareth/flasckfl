package org.flasck.flas.sampleData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.SingleLine;
import org.flasck.flas.parser.BlockParsingTests;

public class BlockTestData {
	public static Block packageWithComments() {
		return builder()
			.lineNo(2)
			.line("package org.ziniki")
			.comment("fred")
			.indent()
			.line("fib n = fib (n-1) + fib (n-2)")
			.comment("bert")
			.build();
	}

	public static Block fibBlock1() {
		return builder()
			.line("fib 0 = 1")
			.build();
	}

	public static Block packageAndFibN() {
		return builder()
			.line("package org.ziniki")
			.indent()
			.line("fib n = fib (n-1) + fib (n-2)")
			.build();
	}
	
	public static Block packageAndFibNWithG() {
		return builder()
			.line("package org.ziniki")
			.indent()
			.line("fib n = fib (n-1) + fib (n-2)")
			.line("g n a b = g (n-1) (a+b) a")
			.build();
	}
	
	public static Block packageCardStack() {
		return builder()
			.lineNo(3)
			.line("package org.cardstack")
			.build();
	}
	
	public static Block packageAndFibNSplit() {
		return builder()
			.line("package org.ziniki")
			.indent()
			.line("fib n = fib (n-1)")
			.continuation(2, "+ fib (n-2)")
			.build();
	}
	
	public static Block contractIntroBlock() {
		return builder()
			.line("contract ctr")
			.build();
	}

	public static Block contractWithMethodBlock() {
		return builder()
			.line("contract ctr")
			.indent()
			.line("up call x")
			.build();
	}


	public static Block comment(String string) {
		return builder().comment(string).build();
	}

	private static BlockBuilder builder() {
		return new BlockBuilder();
	}

	public static void assertBlocksEqual(Block expected, Block actual) {
		BlockParsingTests.showBlock(0, expected);
		BlockParsingTests.showBlock(0, actual);
		int lines = expected.line.lines.size();
		assertEquals(expected.line.lines.size(), actual.line.lines.size());
		for (int i=0;i<lines;i++) {
			SingleLine exl = expected.line.lines.get(i);
			SingleLine acl = actual.line.lines.get(i);
			assertEquals(exl.lineNo, acl.lineNo);
			if (exl.indent == null) {
				assertNull(acl.indent);
			} else {
				assertEquals(exl.indent.tabs, acl.indent.tabs);
				assertEquals(exl.indent.spaces, acl.indent.spaces);
			}
			assertEquals(exl.line, acl.line);
		}
		assertEquals(expected.nested.size(), actual.nested.size());
		for (int i=0;i<expected.nested.size();i++) 
			assertBlocksEqual(expected.nested.get(i), actual.nested.get(i));
	}
}
