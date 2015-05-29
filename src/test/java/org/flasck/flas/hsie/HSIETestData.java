package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;

public class HSIETestData {

	public static HSIEForm testPrimes() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		return thingy("primes", 0, 3, externals,
			"RETURN var 2 0 1",
			"CLOSURE 0", "{",
				"Cons", "5", "Nil",
			"}",
			"CLOSURE 1", "{",
				"Cons", "3", "var 0",
			"}",
			"CLOSURE 2", "{",
				"Cons", "2", "var 1",
			"}"
		);
	}

	public static HSIEForm simpleFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("simple", 1, 0, externals,
			"RETURN 1"
		);
	}

	public static HSIEForm idFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("id", 1, 0, externals,
			"RETURN var 0"
		);
	}

	public static HSIEForm numberIdFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("numberId", 1, 0, externals,
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"RETURN var 0",
			"ERROR"
		);
	}

	public static HSIEForm fib() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("+");
		externals.add("-");
		return thingy("fib", 1, 5, externals,
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"IF 0 0", "{",
					"RETURN 1",
				"}",
				"IF 0 1", "{",
					"RETURN 1",
				"}",
			"}",
			"RETURN var 5 1 2 3 4",
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
			"}"
		);
	}

	public static HSIEForm take() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		externals.add("-");
		return thingy("take", 2, 5, externals,
			"HEAD 1",
			"SWITCH 1 Cons", "{",
				"BIND 2 1 head",
				"BIND 3 1 tail",
				"HEAD 0",
				"SWITCH 0 Number", "{",
					"IF 0 0", "{",
						"RETURN Nil", // expr E1
					"}",
				"}",
				"RETURN var 6 4 5", // expr E0
			"}",
			"SWITCH 1 Nil", "{",
				"HEAD 0",
				"RETURN Nil", // expr E0
			"}",
			// when we get here, we know that Arg#1 is NOT Nil or Cons - thus only E1 _could_ match
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


	public static HSIEForm returnPlus1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("f", 0, 1, externals,
			"RETURN plus1"
		);
	}

	public static HSIEForm plus1Of1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("f", 0, 1, externals,
			"RETURN var 0",
			"CLOSURE 0", "{",
				"plus1", "1",
			"}"
		);
	}

	public static HSIEForm plus2And2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus");
		return thingy("f", 0, 1, externals,
			"RETURN var 0",
			"CLOSURE 0", "{",
				"plus", "2", "2",
			"}"
		);
	}

	public static HSIEForm idDecode() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("id");
		externals.add("decode");
		return thingy("f", 0, 3, externals,
			"RETURN var 2 0 1",
			"CLOSURE 0", "{",
				"id", "32",
			"}",
			"CLOSURE 1", "{",
				"decode", "var 0",
			"}",
			"CLOSURE 2", "{",
				"id", "var 1",
			"}"
		);
	}

	public static HSIEForm rdf1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("-");
		externals.add("g");
		return thingy("f", 1, 2, externals,
			"RETURN var 2 1",
			"CLOSURE 1", "{",
				"-", "var 0", "1",
			"}",
			"CLOSURE 2", "{",
				"g", "var 1",
			"}"
		);
	}

	public static HSIEForm rdf2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("+");
		externals.add("f");
		return thingy("g", 1, 2, externals,
			"RETURN var 2 1",
			"CLOSURE 1", "{",
				"+", "var 0", "1",
			"}",
			"CLOSURE 2", "{",
				"f", "var 1",
			"}"
		);
	}

	private static HSIEForm thingy(String name, int nformal, int nbound, List<String> dependsOn, String... commands) {
		HSIEForm ret = new HSIEForm(name, nformal, nbound, dependsOn);
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
				prev = ret.closure(ret.var(Integer.parseInt(ps[1])));
			} else if (ps[0].equals("RETURN")) {
				Object tmp = analyze(ret, ps, 1);
				List<Var> deps = null;
				if (tmp instanceof Var) {
					deps = new ArrayList<Var>();
					for (int j=3;j<ps.length;j++)
						deps.add(ret.var(Integer.parseInt(ps[j])));
				}
				prev = b.doReturn(tmp, deps);
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
		expected.dump();
		actual.dump();
		assertEquals(expected.fnName, actual.fnName);
		assertEquals(expected.nformal, actual.nformal);
		assertEquals(expected.vars.size(), actual.vars.size());
		assertEquals(expected.externals.size(), actual.externals.size());
		Iterator<String> ee = expected.externals.iterator();
		Iterator<String> ae = actual.externals.iterator();
		while (ee.hasNext())
			assertEquals(ee.next(), ae.next());
		List<HSIEBlock> ecmds = expected.nestedCommands();
		List<HSIEBlock> acmds = actual.nestedCommands();
		assertEquals(ecmds.size(), acmds.size());
		for (int i=0;i<ecmds.size();i++) {
			assertEquals(ecmds.get(i).toString(), acmds.get(i).toString());
		}
		for (int i=expected.nformal;i<expected.vars.size();i++) {
			HSIEBlock ec = expected.getClosure(expected.vars.get(i));
			HSIEBlock ac = actual.getClosure(actual.vars.get(i));
			if (ec == null && ac == null)
				continue; // it was a bound var
			assertNotNull("Did not find expected closure " + i, ec);
			assertNotNull("Did not find actual closure " + i, ac);
			assertEquals(ec.nestedCommands().size(), ac.nestedCommands().size());
			for (int j=0;j<ac.nestedCommands().size();j++) {
				assertEquals(ec.nestedCommands().get(j).toString(), ac.nestedCommands().get(j).toString());
			}
		}
	}
}
