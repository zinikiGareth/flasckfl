package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.slf4j.Logger;
import org.zinutils.exceptions.UtilException;

public class HSIETestData {
	
	static InputPosition posn = new InputPosition("test", 1, 1, null);

	static Map<String, PackageVar> ctorTypes = new HashMap<>();
	static {
		ctorTypes.put("Number", new PackageVar(posn, "Number", org.flasck.flas.types.Type.builtin(posn, "Number")));
		PackageVar nil = new PackageVar(posn, "Nil", new RWStructDefn(posn, "Nil", false));
		PackageVar cons = new PackageVar(posn, "Cons", new RWStructDefn(posn, "Cons", false));
		PackageVar list = new PackageVar(posn, "List", new RWUnionTypeDefn(posn, false, "List", null));
		ctorTypes.put("Cons", cons);
		ctorTypes.put("Nil", nil);
		ctorTypes.put("List", list);
		((RWUnionTypeDefn)list.defn).addCase((Type)nil.defn);
		((RWUnionTypeDefn)list.defn).addCase((Type)cons.defn);
	}
	
	public static HSIEForm testPrimes() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		return thingy("ME.primes", 0, 3, externals,
			null,
			"RETURN var 2 clos2 0 clos0 1 clos1",
			"CLOSURE 0", "{",
				"Cons", "5", "Nil",
			"}",
			"CLOSURE 1", "{",
				"Cons", "3", "var 0 clos0",
			"}",
			"CLOSURE 2", "{",
				"Cons", "2", "var 1 clos1",
			"}"
		);
	}

	public static HSIEForm simpleFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("simple", 1, 0, externals,
			null, "RETURN 1"
		);
	}

	public static HSIEForm idFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("id", 1, 0, externals,
			ctorTypes, "RETURN var 0 x"
		);
	}

	public static HSIEForm numberIdFn() {
		ArrayList<String> externals = new ArrayList<String>();
		return thingy("numberId", 1, 0, externals,
			ctorTypes,
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"RETURN var 0 n",
			"}",
			"ERROR"
		);
	}

	public static HSIEForm fib() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("-");
		externals.add("+");
		externals.add("Number");
		return thingy("ME.fib", 1, 5, externals,
			ctorTypes,
			"HEAD 0",
			"SWITCH 0 Number", "{",
				"IF 0 ev0 0", "{",
					"RETURN 1",
				"}",
				"IF 0 ev0 1", "{",
					"RETURN 1",
				"}",
			"}",
			"RETURN var 5 clos5 1 clos1 2 clos2 3 clos3 4 clos4", // Since we do this before type checking, there is no guarantee var 0 is an integer
			"CLOSURE 1", "{",
				"-", "var 0 ME.fib_2.n", "1",
			"}",
			"CLOSURE 2", "{",
				"ME.fib", "var 1 clos1",
			"}",
			"CLOSURE 3", "{",
				"-", "var 0 ME.fib_2.n", "2",
			"}",
			"CLOSURE 4", "{",
				"ME.fib", "var 3 clos3",
			"}",
			"CLOSURE 5", "{",
				"+", "var 2 clos2", "var 4 clos4",
			"}"
		);
	}

	public static HSIEForm take() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("-");
		externals.add("Nil");
		externals.add("Number");
		return thingy("ME.take", 2, 5, externals,
			ctorTypes,
			"HEAD 1",
			"SWITCH 1 Cons", "{",
				"BIND 2 1 head",
				"BIND 3 1 tail",
				"HEAD 0",
				"SWITCH 0 Number", "{",
					"IF 0 ev0 0", "{", // expr E1
						"RETURN Nil",
					"}",
				"}", // expr E0
				"RETURN var 6 clos6 4 clos4 5 clos5",
			"}", 
			"SWITCH 1 Nil", "{", // expr E0
				"RETURN Nil",
			"}",  // it would seem that none of the cases match
			// when we get here, we know that Arg#1 is NOT Nil or Cons - thus only E1 _could_ match
			"ERROR",
			"CLOSURE 4", "{",
				"-", "var 0 ME.take_2.n", "1",
			"}",
			"CLOSURE 5", "{",
				"ME.take", "var 4 clos4", "var 3 ME.take_2.b",
			"}",
			"CLOSURE 6", "{",
				"Cons", "var 2 ME.take_2.a", "var 5 clos5",
			"}"
		);
	}

	public static HSIEForm takeConsCase() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("Cons");
		externals.add("Nil");
		externals.add("-");
		return thingy("take", 2, 5, externals,
			ctorTypes,
			"HEAD 1",
			"SWITCH 1 Cons",
			"{",
				"BIND 2 1 head",
				"BIND 3 1 tail",
				"HEAD 0",
				"SWITCH 0 Number",
				"{", "IF 0 var0 0", 
					"{", "RETURN Nil",
						"}",
					"}",
				"RETURN var 6 clos6 4 clos4 5 clos5",
			"}",
			"ERROR",
			"CLOSURE 4",
			"{", "-", 
				"var 0 clos0", "1","}",
			"CLOSURE 5",
			"{", "take",
				"var 4 clos4",	"var 3 clos3", "}",
			"CLOSURE 6",
			"{", "Cons",
				"var 2 clos2",	"var 5 clos5", "}"
		);
	}

	public static HSIEForm mutualF() {
		ArrayList<String> externals = new ArrayList<String>();
//		externals.add("ME.f_0.g");
		return thingy("ME.f", 1, 2, externals,
			null,
			"RETURN var 2 clos2 1 ME.f_0.g",
			"CLOSURE 1",
			"{", "ME.f_0.g", "var 0 x", "}",
			"CLOSURE 2 !",
			"{", "var 1 ME.f_0.g", "2", "}"
		);
	}

	public static HSIEForm mutualG() {
		ArrayList<Object> externals = new ArrayList<Object>();
		externals.add("*");
		externals.add(new ScopedVar(posn, "ME.f_0.x", new LocalVar("ME.f", "ME.f_0", posn, "x", null, null), "ME.f"));
		return thingy("ME.f_0.g", 1, 1, externals,
			null,
			"RETURN var 1 clos1",
			"CLOSURE 1",
			"{", "*", "ME.f_0.x", "var 0 y", "}"
		);
	}

	public static HSIEForm simpleF() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("ME.f_0.g");
		return thingy("ME.f", 1, 1, externals,
			null,
			"RETURN var 1 clos1",
			"CLOSURE 1",
			"{", "ME.f_0.g",
				"var 0 var0", "}"
		);
	}

	public static HSIEForm simpleG() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("*");
		return thingy("ME.f_0.g", 1, 1, externals,
			null,
			"RETURN var 1 clos1",
			"CLOSURE 1",
			"{", "*",
				"2", "var 0 clos0", "}"
		);
	}

	public static HSIEForm splitF() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("ME.f_0.g");
		externals.add("ME.f_1.g");
		externals.add("Cons");
		externals.add("Nil");
		return thingy("ME.f", 2, 4, externals,
			ctorTypes,
			"HEAD 0",
			"SWITCH 0 Cons",
			"{",
				"BIND 2 0 head",
				"BIND 3 0 tail",
				"RETURN var 5 clos5",
			"}",
			"SWITCH 0 Nil",
			"{",
				"RETURN var 4 clos4",
			"}",
			"ERROR",
			"CLOSURE 4 !", "{", "ME.f_0.g", "2", "}",
			"CLOSURE 5 !", "{", "ME.f_1.g", "var 1 k", "var 2 a", "}"
		);
	}

	public static HSIEForm splitF_G1() {
		ArrayList<Object> externals = new ArrayList<Object>();
		externals.add("*");
		externals.add(new ScopedVar(posn, "ME.f_0.q", new LocalVar("ME.f", "ME.f_0", posn, "q", null, null), "ME.f"));
		return thingy("ME.f_0.g", 1, 1, externals,
			null,
			"RETURN var 1 clos1",
			"CLOSURE 1", "{", "*", "var 0 x", "ME.f_0.q", "}"
		);
	}

	public static HSIEForm splitF_G2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("+");
		return thingy("ME.f_1.g", 2, 1, externals,
			null,
			"RETURN var 2 clos2",
			"CLOSURE 2", "{",
				"+", "var 0 p", "var 1 q",
			"}"
		);
	}

	public static HSIEForm returnPlus1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("f", 0, 1, externals, null,
			"RETURN plus1"
		);
	}

	public static HSIEForm plus1Of1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("plus1");
		return thingy("ME.f", 0, 1, externals, null,
			"RETURN var 0 clos0",
			"CLOSURE 0", "{",
				"plus1", "1",
			"}"
		);
	}

	public static HSIEForm plus2And2() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("+");
		return thingy("f", 0, 1, externals,
			null,
			"RETURN var 0 clos0",
			"CLOSURE 0", "{",
				"+", "2", "2",
			"}"
		);
	}

	public static HSIEForm idDecode() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("id1");
		externals.add("decode");
		return thingy("ME.f", 0, 3, externals,
			null,
			"RETURN var 2 clos2 0 clos0 1 clos1",
			"CLOSURE 0", "{",
				"id1", "32",
			"}",
			"CLOSURE 1", "{",
				"decode", "var 0 clos0",
			"}", 
			"CLOSURE 2", "{",
				"id1", "var 1 clos1",
			"}"
		);
	}

	public static HSIEForm unionType() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("List");
		return thingy("ME.f", 1, 0, externals,
			ctorTypes,
			"HEAD 0",
			"SWITCH 0 List",
			"{",
				"RETURN 10",
			"}",
			"ERROR"
		);
	}

	public static HSIEForm rdf1() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("-");
		externals.add("ME.g");
		return thingy("ME.f", 1, 2, externals,
			null,
			"RETURN var 2 clos2 1 clos1",
			"CLOSURE 1", "{",
				"-", "var 0 ME.f_0.x", "1",
			"}",
			"CLOSURE 2", "{",
				"ME.g",	"var 1 clos1",
			"}"
		);
	}

	public static HSIEForm rdf2(int offset) {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("+");
		externals.add("ME.f");
		String v0 = "" + (offset + 0);
		String v1 = "" + (offset + 1);
		String v2 = "" + (offset + 2);
		return thingy(offset, "ME.g", 1, 2, externals,
			null,
			"RETURN var " + v2 +" clos"+v2 + " " + v1 + " clos"+v1,
			"CLOSURE " + v1, "{",
				"+", "var " + v0 + " ME.g_0.x", "1",
			"}",
			"CLOSURE " + v2, "{",
				"ME.f", "var " + v1 + " clos"+v1,
			"}"
		);
	}

	public static HSIEForm simpleIf() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("==");
		return thingy("ME.fact", 1, 1, externals,
			null,
			"IF 1 clos1", "{",
				"RETURN 1 clos1",
			"}",
			"ERROR",
			"CLOSURE 1", "{",
				"==", "var 0 n", "1",
			"}"
		);
	}

	public static HSIEForm simpleIfElse() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("==");
		externals.add("*");
		externals.add("-");
		return thingy("ME.fact", 1, 4, externals,
			null,
			"IF 1 clos1", "{",
				"RETURN 1",
			"}",
			"RETURN var 4 clos4 2 clos2 3 clos3",
			"CLOSURE 1", "{",
				"==", "var 0 n", "1",
			"}",
			"CLOSURE 2", "{",
				"-", "var 0 n", "1",
			"}",
			"CLOSURE 3", "{",
				"ME.fact", "var 2 clos2",
			"}",
			"CLOSURE 4", "{",
				"*", "var 0 n", "var 3 clos3",
			"}"
		);
	}

	public static HSIEForm directLet() {
		ArrayList<String> externals = new ArrayList<String>();
		externals.add("FLEval.plus");
		return thingy("ME.f", 0, 2, externals,
			null,
			"RETURN var 1 clos1 0 ME.f._x",
			"CLOSURE 1", "{",
				"FLEval.plus", "var 0 ME.f._x", "var 0 ME.f._x",
			"}",
			"CLOSURE 0", "{",
				"FLEval.plus", "2", "2",
			"}"
		);
	}

	private static HSIEForm thingy(String name, int nformal, int nbound, List<? extends Object> dependsOn, Map<String, PackageVar> ctorTypes, String... commands) {
		return thingy(0, name, nformal, nbound, dependsOn, ctorTypes, commands);
	}
	
	private static HSIEForm thingy(int offset, String name, int nformal, int nbound, List<? extends Object> dependsOn, Map<String, PackageVar> ctorTypes, String... commands) {
		HSIEForm ret = new HSIEForm(new InputPosition("thingy", 1, 1, null), name, nformal, CodeType.FUNCTION, null, new VarFactory());
		for (int i=0;i<nformal;i++)
			ret.vars.add(new Var(offset + i));
		for (int i=0;i<nbound;i++)
			ret.vars.add(new Var(offset + nformal + i));
		for (Object dep : dependsOn)
			ret.dependsOn(dep);
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
				b.head(posn, ret.var(Integer.parseInt(ps[1])));
				prev = null;
			} else if (ps[0].equals("SWITCH")) {
				if (ctorTypes == null)
					throw new UtilException("need to pass in ctortypes");
//				AbsoluteVar av = ctorTypes.get(ps[2]);
//				if (av == null)
//					throw new UtilException("You need to define the AV for test type " + ps[2]);
				prev = b.switchCmd(posn, ret.var(Integer.parseInt(ps[1])), ps[2]);
			} else if (ps[0].equals("IF")) {
				int vidx = Integer.parseInt(ps[1]);
				VarInSource var = new VarInSource(ret.var(vidx), posn, ps[2]);
				if (ps.length == 3) {
					// the closure case
					prev = b.ifCmd(posn, var);
				} else {
					try {
						prev = b.ifCmd(posn, var, Integer.parseInt(ps[3]));
					} catch (NumberFormatException ex) {
						prev = b.ifCmd(posn, var, Boolean.parseBoolean(ps[3]));
					}
				}
			} else if (ps[0].equals("BIND")) {
				prev = b.bindCmd(posn, ret.var(Integer.parseInt(ps[1])), ret.var(Integer.parseInt(ps[2])), ps[3]);
			} else if (ps[0].equals("CLOSURE")) {
				ClosureCmd tmp = ret.closure(posn, ret.var(Integer.parseInt(ps[1])));
				if (ps.length == 3 && ps[2].equals("!"))
					tmp.justScoping = true;
				prev = tmp;
			} else if (ps[0].equals("RETURN")) {
				Object tmp = analyze(ret, ps, 1);
				List<VarInSource> deps = null;
				if (tmp instanceof VarInSource) {
					deps = new ArrayList<VarInSource>();
					for (int j=4;j+1<ps.length;j+=2)
						deps.add(new VarInSource(ret.var(Integer.parseInt(ps[j])), posn, ps[j+1]));
				}
				prev = b.doReturn(posn, tmp, deps);
			} else if (ps[0].equals("ERROR")) {
				b.caseError();
				prev = null;
			} else if (ps[0].equals("var") && ps.length == 3) {
				prev = b.push(posn, analyze(ret, ps, 0));
			} else if (Character.isDigit(ps[0].charAt(0))) {
				prev = b.push(posn, Integer.parseInt(ps[0]));
			} else {
				String s = ps[0];
				Object toPush = null;
				for (Object o : dependsOn) {
					if (o instanceof String && s.equals(o))
						toPush = new PackageVar(posn, s, null);
					else if (o instanceof ScopedVar && ((ScopedVar)o).id.equals(s))
						toPush = o;
				}
				if (toPush == null) {
					// This appears to be a valid case, but I'm dubious ...
					System.out.println("No external/scoped defn for " + s);
//					throw new UtilException("No external/scoped defn for " + s);
					toPush = new PackageVar(posn, s, null);
				}
				prev = b.push(posn, toPush);
			}
			
		}
		return ret;
	}

	private static Object analyze(HSIEForm ret, String[] ps, int from) {
		if (ps[from].equals("var"))
			return new VarInSource(ret.var(Integer.parseInt(ps[from+1])), posn, ps[from+2]);
		else if (Character.isDigit(ps[from].charAt(0)))
			return Integer.parseInt(ps[from]);
		else
			return new PackageVar(posn, ps[from], null);
	}

	protected static HSIEForm doHSIE(ErrorResult errors, Rewriter rw, RWFunctionDefinition fn) {
		HSIE hsie = new HSIE(errors, rw);
		Set<RWFunctionDefinition> o1 = new HashSet<>();
		o1.add(fn);
		hsie.createForms(o1);
		hsie.orchard(o1);
		return hsie.getForm(fn.name);
	}

	public static void assertHSIE(HSIEForm expected, HSIEForm actual) {
		System.out.println("---- Check expecting:");
		expected.dump((Logger)null);
		System.out.println("---------- actual:");
		actual.dump((Logger)null);
		System.out.println("----------");
		assertEquals("incorrect name", expected.fnName, actual.fnName);
		assertEquals("incorrect # of formals", expected.nformal, actual.nformal);
		assertEquals("incorrect # of total vars", expected.vars.size(), actual.vars.size());
		assertEquals("incorrect # of externals", expected.externals.size(), actual.externals.size());
		Iterator<String> ee = expected.externals.iterator();
		Iterator<String> ae = actual.externals.iterator();
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
				assertInstructionEquals(ec.nestedCommands().get(j), ac.nestedCommands().get(j));
			}
		}
	}

	private static void compareBlocks(HSIEBlock expected, HSIEBlock actual) {
		List<HSIEBlock> ecmds = expected.nestedCommands();
		List<HSIEBlock> acmds = actual.nestedCommands();
		assertEquals("incorrect number of commands", ecmds.size(), acmds.size());
		for (int i=0;i<ecmds.size();i++) {
			assertInstructionEquals(ecmds.get(i), acmds.get(i));
			compareBlocks(ecmds.get(i), acmds.get(i));
		}
	}

	public static void assertInstructionEquals(HSIEBlock ex, HSIEBlock ac) {
		assertInstructionEquals(ex.toString(), ac);
	}

	public static void assertInstructionEquals(String exs, HSIEBlock ac) {
		if (exs != null && exs.indexOf("#") != -1)
			exs = exs.substring(0, exs.indexOf("#")).trim();
		String acs = ac.toString();
		if (acs != null && acs.indexOf("#") != -1)
			acs = acs.substring(0, acs.indexOf("#")).trim();
		assertEquals(exs, acs);
	}
}
