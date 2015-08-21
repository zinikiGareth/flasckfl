package org.flasck.flas.sampleData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.blocker.BlockerTests;

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

	public static Block fibBlock2() {
		return builder()
			.line("fib 1 = 1")
			.build();
	}

	public static Block fibBlockN() {
		return builder()
			.line("fib n = fib (n-1) + fib (n-2)")
//				.line("fib n = + (fib (- n 1)) (fib (- n 2))")
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
	

	public static List<Block> allFib() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder().line("fib 0 = 1").build());
		ret.add(builder().line("fib 1 = 1").build());
		ret.add(builder().line("fib n = fib (n-1) + fib (n-2)").build());
		return ret;
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
	
	public static Block structIntroBlock() {
		return builder()
			.line("struct Nil")
			.build();
	}

	public static Block structBlockWithParametersAndFields() {
		return builder()
			.line("struct Cons A")
			.indent()
			.line("A head")
			.line("(List A) tail")
			.build();
	}
	
	public static Block typeBlock() {
		return builder()
			.line("type List E = Nil | Cons E")
			.build();
	}


	public static Block contractIntroBlock() {
		return builder()
			.line("contract OnTick")
			.build();
	}

	public static Block contractWithMethodBlock() {
		return builder()
			.line("contract OnTick")
			.indent()
			.line("up call x")
			.build();
	}

	public static List<Block> simpleMutualRecursionBlock() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder()
			.line("f x = g 2")
				.indent()
				.line("g y = x * y")
			.build());
		return ret;
	}

	public static List<Block> splitNestedBlocks() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder()
			.line("f Nil x = g 2")
			.indent()
			.line("g y = x * y")
			.build());
		ret.add(builder()
			.line("f (Cons { head: a, tail: b }) k = g k a")
			.indent()
			.line("g p q = p + q")
			.build());
		return ret;
	}

	public static List<Block> cardWithMethods() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder().line("concat a b = a").build());
		ret.add(builder()
			.line("card Mycard")
				.indent()
				.line("state")
					.indent()
					.line("String title")
					.exdent()
				.line("template")
					.indent()
					.line("(render 'hello')")
					.exdent()
				.line("render s = DOM.Element 'h1' [] [] [('action', action title s)]")
				.line("event action curr s ev")
					.indent()
					.line("title <- concat title s")
			.build());
		return ret;
	}

	public static List<Block> simpleIf() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder()
			.line("fact n")
				.indent()
				.line("if (n == 1) = 1")
			.build());
		return ret;
	}

	public static List<Block> simpleIfElse() {
		List<Block> ret = new ArrayList<Block>();
		ret.add(builder()
			.line("fact n")
				.indent()
				.line("if (n == 1) = 1")
				.line("else = n * fact (n-1)")
			.build());
		return ret;
	}

	public static Block comment(String string) {
		return builder().comment(string).build();
	}

	private static BlockBuilder builder() {
		return new BlockBuilder("test");
	}

	public static void assertBlocksEqual(Block expected, Block actual) {
		BlockerTests.showBlock(0, expected);
		BlockerTests.showBlock(0, actual);
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