package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;

public class HSIETestData {

	public static HSIEForm fib() {
		return thingy(1, 5, new ArrayList<String>(),
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"IF 0 0", "{",
					"RETURN 1",
				"}",
				"IF 0 1", "{",
					"RETURN 1",
				"}",
			"}",
			// Since we do this before type checking, there is no guarantee var 0 is an integer
			"CLOSURE 1", "{",
				"-", "var 0", "1",
			"}",
			"CLOSURE 2", "{",
				"fib", "var 1",
			"}",
			"CLOSURE 3", "{",
				"-", "var 0", "2",
			"}",
			"CLOSURE 4", "{",
				"fib", "var 3",
			"}",
			"CLOSURE 5", "{",
				"+", "var 2", "var 4",
			"}",
			"RETURN var 5"
		);
	}

	public static HSIEForm take() {
		return thingy(2, 5, new ArrayList<String>(),
			"HEAD 1",
			"SWITCH 1 Nil", "{",
				"HEAD 0",
				"SWITCH 0 Number", "{",
					"IF 0 0", "{",
						"RETURN Nil", // expr E1
					"}",
				"}",
				"RETURN Nil", // expr E0
			"}",
			"SWITCH 1 Cons", "{",
				"BIND 2 0 head",
				"BIND 3 0 tail",
				"HEAD 0",
				"SWITCH 0 Number", "{",
					"IF 0 0", "{",
						"RETURN Nil", // expr E1
					"}",
				"}",
				"RETURN var 6", // expr E0
			"}",
			// when we get here, we know that Arg#1 is NOT Nil or Cons - thus only E1 _could_ match
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"IF 0 0", "{",
					"RETURN Nil", // expr E1
				"}",
			"}",
			"ERROR",  // it would seem that none of the cases match
			"CLOSURE 4", "{",
				"-", "var 0", "1",
			"}",
			"CLOSURE 5", "{",
				"take", "var 4", "var 3",
			"}",
			"CLOSURE 6", "{",
				"Cons", "var 2", "var 5",
			"}"
		);
	}

	private static HSIEForm thingy(int nformal, int nbound, List<String> dependsOn, String... commands) {
		HSIEForm ret = new HSIEForm(nformal, nbound, dependsOn);
		HSIEBlock b = ret;
		List<HSIEBlock> stack = new ArrayList<HSIEBlock>();
		stack.add(0, ret);
		HSIEBlock prev = null;
		for (int i=0;i<commands.length;i++) {
			String c = commands[i];
//			System.out.println(c);
			String[] ps = c.split(" ");
			if (c.equals("{")) {
				stack.add(0, b);
				b = prev;
//				System.out.println(stack.size());
			} else if (c.equals("}")) {
				b = stack.remove(0);
//				System.out.println(stack.size());
			} else if (ps[0].equals("HEAD")) {
				b.head(ret.var(Integer.parseInt(ps[1])));
				prev = null;
			} else if (ps[0].equals("SWITCH")) {
				prev = b.switchCmd(ret.var(Integer.parseInt(ps[1])), ps[2]);
			} else if (ps[0].equals("IF")) {
				// TODO: the final arg here needs to be any constant
				// TODO: this whole thing needs to handle a general user-specified test
				prev = b.ifCmd(ret.var(Integer.parseInt(ps[1])), Integer.parseInt(ps[2]));
			} else if (ps[0].equals("BIND")) {
				prev = b.bindCmd(ret.var(Integer.parseInt(ps[1])), ret.var(Integer.parseInt(ps[2])), ps[3]);
			} else if (ps[0].equals("CLOSURE")) {
				prev = b.closure(ret.var(Integer.parseInt(ps[1])));
			} else if (ps[0].equals("RETURN")) {
				prev = b.doReturn(analyze(ret, ps, 1));
			} else if (ps[0].equals("ERROR")) {
				b.caseError();
				prev = null;
			} else if (ps[0].equals("var") && ps.length == 2) {
				prev = b.push(analyze(ret, ps, 0));
			} else if (Character.isDigit(ps[0].charAt(0))) {
				prev = b.push(Integer.parseInt(ps[0]));
			} else {
				prev = b.push(ps[0]);
			}
			
		}
		return ret;
	}

	private static Object analyze(HSIEForm ret, String[] ps, int from) {
		if (ps[from].equals("var"))
			return ret.var(Integer.parseInt(ps[from+1]));
		else if (Character.isDigit(ps[from].charAt(0)))
			return Integer.parseInt(ps[from]);
		else
			return ps[from];
	}
	
	public static void assertHSIE(HSIEForm expected, HSIEForm actual) {
//		expected.dump();
	}
}
