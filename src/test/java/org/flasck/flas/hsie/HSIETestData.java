package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class HSIETestData {

	static Map<String, AbsoluteVar> ctorTypes = new HashMap<>();
	static {
		ctorTypes.put("Number", new AbsoluteVar(null, "Number", org.flasck.flas.typechecker.Type.builtin(null, "Number")));
		ctorTypes.put("Cons", new AbsoluteVar(null, "Cons", new StructDefn(null, "Cons", false)));
		ctorTypes.put("Nil", new AbsoluteVar(null, "Nil", new StructDefn(null, "Nil", false)));
	}
	
	public static HSIEForm testPrimes() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		return thingy("primes", 0, 0, 3,
			externals,
			null,
			"RETURN var 2 clos2 0 clos0 1 clos1", "CLOSURE 0",
				"{", "Cons", "5",
			"Nil",
			"}", "CLOSURE 1",
				"{", "Cons", "3",
			"var 0 clos0",
			"}", "CLOSURE 2",
				"{", "Cons", "2",
			"var 1 clos1", "}"
		);
	}

	public static HSIEForm simpleFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("simple", 0, 1, 0,
			externals, null, "RETURN 1"
		);
	}

	public static HSIEForm idFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("id", 0, 1, 0,
			externals, ctorTypes, "RETURN var 0 x"
		);
	}

	public static HSIEForm numberIdFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("numberId", 0, 1, 0,
			externals,
			ctorTypes,
			"HEAD 0", "SWITCH 0 Number",
				"{",
			"RETURN var 0 n",
			"}", "ERROR"
		);
	}

	public static HSIEForm fib() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.plus");
		externals.add("FLEval.minus");
		externals.add("Number");
		return thingy("fib", 0, 1, 5,
			externals,
			ctorTypes,
			"HEAD 0", "SWITCH 0 Number",
				"{", "IF 0 ev0 0",
					"{",
				"RETURN 1",
				"}", "IF 0 ev0 1",
					"{",
				"RETURN 1",
			"}",
			"}", // Since we do this before type checking, there is no guarantee var 0 is an integer
			"RETURN var 5 clos5 1 clos1 2 clos2 3 clos3 4 clos4", "CLOSURE 1",
				"{", "FLEval.minus", "var 0 n",
			"1",
			"}", "CLOSURE 2",
				"{", "fib",
			"var 1 clos1",
			"}", "CLOSURE 3",
				"{", "FLEval.minus", "var 0 n",
			"2",
			"}", "CLOSURE 4",
				"{", "fib",
			"var 3 clos3",
			"}", "CLOSURE 5",
				"{", "FLEval.plus", "var 2 clos2",
			"var 4 clos4", "}"
		);
	}

	public static HSIEForm take() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("FLEval.minus");
		externals.add("Nil");
		externals.add("Number");
		return thingy("take", 0, 2, 5,
			externals,
			ctorTypes,
			"HEAD 1", "SWITCH 1 Cons",
				"{",
				"BIND 2 1 head",
				"BIND 3 1 tail",
				"HEAD 0", "SWITCH 0 Number",
					"{", "IF 0 ev0 0", // expr E1
						"{",
					"RETURN Nil",
				"}", // expr E0
				"}",
			"RETURN var 6 clos6 4 clos4 5 clos5", 
			"}", "SWITCH 1 Nil", // expr E0
				"{",
			"RETURN Nil",  // it would seem that none of the cases match
			"}",
			// when we get here, we know that Arg#1 is NOT Nil or Cons - thus only E1 _could_ match
			"ERROR", "CLOSURE 4",
				"{", "FLEval.minus", "var 0 n",
			"1",
			"}", "CLOSURE 5",
				"{", "take", "var 4 clos4",
			"var 3 b",
			"}", "CLOSURE 6",
				"{", "Cons", "var 2 a",
			"var 5 clos5", "}"
		);
	}

	public static HSIEForm takeConsCase() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		externals.add("-");
		return thingy("take", 0, 2, 5,
			externals,
			ctorTypes,
			"HEAD 1",
			"SWITCH 1 Cons", "{",
				"BIND 2 1 head",
				"BIND 3 1 tail",
				"HEAD 0",
				"SWITCH 0 Number", "{", 
					"IF 0 var0 0", "{",
						"RETURN Nil",
					"}",
				"}",
				"RETURN var 6 clos6 4 clos4 5 clos5",
			"}",
			"ERROR",
			"CLOSURE 4", "{", 
				"-", "var 0 clos0","1",
			"}",
			"CLOSURE 5", "{",
				"take",	"var 4 clos4", "var 3 clos3",
			"}",
			"CLOSURE 6", "{",
				"Cons",	"var 2 clos2", "var 5 clos5",
			"}"
		);
	}

	public static HSIEForm mutualF() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("ME.f_0.g");
		return thingy("ME.f", 0, 1, 1,
			externals,
			null,
			"RETURN var 1 clos1", "CLOSURE 1",
				"{", "ME.f_0.g",
			"2", "}"
		);
	}

	public static HSIEForm mutualG() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.mul");
		return thingy("ME.f_0.g", 2, 1, 1,
			externals,
			null,
			"RETURN var 3 clos3", "CLOSURE 3",
				"{", "FLEval.mul", "var 0 x",
			"var 2 y", "}"
		);
	}

	public static HSIEForm simpleF() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("ME.f_0.g");
		return thingy("ME.f", 0, 1, 1,
			externals,
			null,
			"RETURN var 1 clos1",
			"CLOSURE 1", "{",
				"ME.f_0.g", "var 0 var0",
			"}"
		);
	}

	public static HSIEForm simpleG() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.mul");
		return thingy("ME.f_0.g", 2, 1, 1,
			externals,
			null,
			"RETURN var 3 clos3",
			"CLOSURE 3", "{",
				"FLEval.mul", "2", "var 2 clos2",
			"}"
		);
	}

	public static HSIEForm splitF() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("ME.f_0.g");
		externals.add("ME.f_1.g");
		externals.add("Cons");
		externals.add("Nil");
		return thingy("ME.f", 0, 2, 4,
			externals,
			ctorTypes,
			"HEAD 0",
			"SWITCH 0 Cons","{",
				"BIND 2 0 head",
				"BIND 3 0 tail",
				"RETURN var 5 clos5",
			"}",
			"SWITCH 0 Nil",	"{",
				"RETURN var 4 clos4",
			"}",
			"ERROR",
			"CLOSURE 4", "{",
				"ME.f_0.g",	"2",
			"}",
			"CLOSURE 5", "{",
				"ME.f_1.g", "var 1 k", "var 2 a",
			"}"
		);
	}

	public static HSIEForm splitF_G1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.mul");
		return thingy("ME.f_0.g", 6, 1, 1,
			externals,
			null,
			"RETURN var 7 clos7", "CLOSURE 7",
				"{", "FLEval.mul", "var 1 x",
			"var 6 y", "}"
		);
	}

	public static HSIEForm splitF_G2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.plus");
		return thingy("ME.f_1.g", 6, 2, 1,
			externals,
			null,
			"RETURN var 8 clos8", "CLOSURE 8",
				"{", "FLEval.plus", "var 6 p",
			"var 7 q", "}"
		);
	}

	public static HSIEForm returnPlus1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("f", 0, 0, 1, externals,
			null, "RETURN plus1"
		);
	}

	public static HSIEForm plus1Of1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("ME.f", 0, 0, 1, externals,
			null,
			"RETURN var 0 clos0", "CLOSURE 0",
				"{", "plus1",
			"1", "}"
		);
	}

	public static HSIEForm plus2And2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus");
		return thingy("f", 0, 0, 1,
			externals,
			null,
			"RETURN var 0 clos0", "CLOSURE 0",
				"{", "plus", "2",
			"2", "}"
		);
	}

	public static HSIEForm idDecode() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("id");
		externals.add("decode");
		return thingy("ME.f", 0, 0, 3,
			externals,
			null,
			"RETURN var 2 clos2 0 clos0 1 clos1", "CLOSURE 0",
				"{", "id",
			"32",
			"}", "CLOSURE 1",
				"{", "decode",
			"var 0 clos0", 
			"}", "CLOSURE 2",
				"{", "id",
			"var 1 clos1", "}"
		);
	}

	public static HSIEForm rdf1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.minus");
		externals.add("ME.g");
		return thingy("ME.f", 0, 1, 2,
			externals,
			null,
			"RETURN var 2 clos2 1 clos1", "CLOSURE 1",
				"{", "FLEval.minus", "var 0 x",
			"1",
			"}", "CLOSURE 2",
				"{",	"ME.g",
			"var 1 clos1", "}"
		);
	}

	public static HSIEForm rdf2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.plus");
		externals.add("ME.f");
		return thingy("ME.g", 0, 1, 2,
			externals,
			null,
			"RETURN var 2 clos2 1 clos1", "CLOSURE 1",
				"{", "FLEval.plus", "var 0 x",
			"1",
			"}", "CLOSURE 2",
				"{", "ME.f",
			"var 1 clos1", "}"
		);
	}

	public static HSIEForm simpleIf() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.compeq");
		return thingy("ME.fact", 0, 1, 1,
			externals,
			null, "IF 1 clos1",
				"{",
			"RETURN 1 clos1",
			"}",
			"ERROR", "CLOSURE 1",
				"{", "FLEval.compeq", "var 0 n",
			"1", "}"
		);
	}

	public static HSIEForm simpleIfElse() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.compeq");
		externals.add("FLEval.mul");
		externals.add("FLEval.minus");
		return thingy("ME.fact", 0, 1, 4,
			externals,
			null, "IF 1 clos1",
				"{",
			"RETURN 1",
			"}",
			"RETURN var 4 clos4 2 clos2 3 clos3", "CLOSURE 1",
				"{", "FLEval.compeq", "var 0 n",
			"1",
			"}", "CLOSURE 2",
				"{", "FLEval.minus", "var 0 n",
			"1",
			"}", "CLOSURE 3",
				"{", "ME.fact",
			"var 2 clos2",
			"}", "CLOSURE 4",
				"{", "FLEval.mul", "var 0 n",
			"var 3 clos3", "}"
		);
	}

	public static HSIEForm directLet() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.plus");
		return thingy("ME.f", 0, 0, 2,
			externals,
			null,
			"RETURN var 1 clos1 0 _x", "CLOSURE 1",
				"{", "FLEval.plus", "var 0 _x",
			"var 0 _x",
			"}", "CLOSURE 0",
				"{", "FLEval.plus", "2",
			"2", "}"
		);
	}

	private static HSIEForm thingy(String name, int alreadyUsed, int nformal, int nbound, List<String> dependsOn, Map<String, AbsoluteVar> ctorTypes, String... commands) {
		HSIEForm ret = new HSIEForm(CodeType.FUNCTION, name, alreadyUsed, nformal, nbound, dependsOn);
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
				if (ctorTypes == null)
					throw new UtilException("need to pass in ctortypes");
				AbsoluteVar av = ctorTypes.get(ps[2]);
				if (av == null)
					throw new UtilException("You need to define the AV for test type " + ps[2]);
				prev = b.switchCmd(null, ret.var(Integer.parseInt(ps[1])), av);
			} else if (ps[0].equals("IF")) {
				int vidx = Integer.parseInt(ps[1]);
				CreationOfVar var = new CreationOfVar(ret.var(vidx), null, ps[2]);
				if (ps.length == 3) {
					// the closure case
					prev = b.ifCmd(var);
				} else {
					try {
						prev = b.ifCmd(var, Integer.parseInt(ps[3]));
					} catch (NumberFormatException ex) {
						prev = b.ifCmd(var, Boolean.parseBoolean(ps[3]));
					}
				}
			} else if (ps[0].equals("BIND")) {
				prev = b.bindCmd(ret.var(Integer.parseInt(ps[1])), ret.var(Integer.parseInt(ps[2])), ps[3]);
			} else if (ps[0].equals("CLOSURE")) {
				prev = ret.closure(ret.var(Integer.parseInt(ps[1])));
			} else if (ps[0].equals("RETURN")) {
				Object tmp = analyze(ret, ps, 1);
				List<CreationOfVar> deps = null;
				if (tmp instanceof CreationOfVar) {
					deps = new ArrayList<CreationOfVar>();
					for (int j=4;j+1<ps.length;j+=2)
						deps.add(new CreationOfVar(ret.var(Integer.parseInt(ps[j])), null, ps[j+1]));
				}
				prev = b.doReturn(null, tmp, deps);
			} else if (ps[0].equals("ERROR")) {
				b.caseError();
				prev = null;
			} else if (ps[0].equals("var") && ps.length == 3) {
				prev = b.push(null, analyze(ret, ps, 0));
			} else if (Character.isDigit(ps[0].charAt(0))) {
				prev = b.push(null, Integer.parseInt(ps[0]));
			} else {
				prev = b.push(null, new AbsoluteVar(null, ps[0], null));
			}
			
		}
		return ret;
	}

	private static Object analyze(HSIEForm ret, String[] ps, int from) {
		if (ps[from].equals("var"))
			return new CreationOfVar(ret.var(Integer.parseInt(ps[from+1])), null, ps[from+2]);
		else if (Character.isDigit(ps[from].charAt(0)))
			return Integer.parseInt(ps[from]);
		else
			return new AbsoluteVar(null, ps[from], null);
	}
	
	public static void assertHSIE(HSIEForm expected, HSIEForm actual) {
		System.out.println("---- Check expecting:");
		expected.dump(null);
		System.out.println("---------- actual:");
		actual.dump(null);
		System.out.println("----------");
		assertEquals("incorrect name", expected.fnName, actual.fnName);
		assertEquals("incorrect # of formals", expected.nformal, actual.nformal);
		assertEquals("incorrect # of total vars", expected.vars.size(), actual.vars.size());
		assertEquals("incorrect # of externals", expected.externals.size(), actual.externals.size());
		Iterator<Object> ee = expected.externals.iterator();
		Iterator<Object> ae = actual.externals.iterator();
		while (ee.hasNext())
			assertEquals("incorrect external", ee.next(), ae.next());
		compareBlocks(expected, actual);
		for (int i=expected.nformal;i<expected.vars.size();i++) {
			HSIEBlock ec = expected.getClosure(expected.vars.get(i));
			HSIEBlock ac = actual.getClosure(actual.vars.get(i));
			if (ec == null && ac == null)
				continue; // it was a bound var
			assertNotNull("Did not find 'expected' closure " + i, ec);
			assertNotNull("Did not find 'actual' closure " + i, ac);
			assertEquals("incorrect number of commands in closure " + i, ec.nestedCommands().size(), ac.nestedCommands().size());
			for (int j=0;j<ac.nestedCommands().size();j++) {
				assertEquals(ec.nestedCommands().get(j).toString(), ac.nestedCommands().get(j).toString());
			}
		}
	}

	private static void compareBlocks(HSIEBlock expected, HSIEBlock actual) {
		List<HSIEBlock> ecmds = expected.nestedCommands();
		List<HSIEBlock> acmds = actual.nestedCommands();
		assertEquals("incorrect number of commands", ecmds.size(), acmds.size());
		for (int i=0;i<ecmds.size();i++) {
			assertEquals(ecmds.get(i).toString(), acmds.get(i).toString());
			compareBlocks(ecmds.get(i), acmds.get(i));
		}
	}
}
