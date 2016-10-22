package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.flasck.flas.Compiler;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.Template;
import org.flasck.flas.commonBase.template.TemplateList;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.StoryRet;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.exceptions.UtilException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringUtil;

public class GoldenCGRunner extends CGHarnessRunner {
	public GoldenCGRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError, IOException, ErrorResultException {
		super(builder, figureClasses());
	}
	
	private static Class<?>[] figureClasses() throws IOException, ErrorResultException {
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		CGHClassLoaderImpl cl = new CGHClassLoaderImpl();
		
		Pattern p = null;
		String match = System.getProperty("org.flasck.golden.pattern");
		if (match != null && match.length() > 0) {
			p = Pattern.compile(match);
		}

		List<Class<?>> ret = new ArrayList<Class<?>>();
		for (File f : new File("src/golden").listFiles()) {
			if (f.isDirectory() && (p == null || p.matcher(f.getName()).find()))
				ret.add(goldenTest(bce, cl, f));
		}
		return ret.toArray(new Class<?>[ret.size()]);
	}

	private static Class<?> goldenTest(ByteCodeEnvironment bce, CGHClassLoaderImpl cl, final File f) {
		ByteCodeCreator bcc = emptyTestClass(bce, "test" + StringUtil.capitalize(f.getName()));
		boolean ignoreTest = new File(f, "ignore").exists();
			
		addMethod(bcc, "testFlasckCompilation", ignoreTest, new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath())).flush();
			}
		});
		return generate(cl, bcc);
	}
	
	public static void runGolden(String s) throws Exception {
		System.out.println("Run golden test for " + s);
		File importFrom = new File(s, "import");
		File pform = new File(s, "parser-tmp");
		File jsto = new File(s, "jsout-tmp");
		File hsie = new File(s, "hsie-tmp");
		File flim = new File(s, "flim-tmp");
		FileUtils.deleteDirectoryTree(new File(s, "errors-tmp"));
		clean(pform);
		clean(jsto);
		clean(hsie);
		clean(flim);
		Compiler.setLogLevels();
		Compiler compiler = new Compiler();
		File dir = new File(s, "test.golden");
		ErrorResult er = new ErrorResult();
		for (File input : dir.listFiles()) {
			StoryRet sr = compiler.parse("test.golden", FileUtils.readFile(input));
			Indenter pw = new Indenter(new File(pform, input.getName().replace(".fl", ".pf")));
			if (sr.scope != null) {
				pw.println("package test.golden");
				dumpScope(pw, sr.scope);
			}
			if (sr.er != null) {
				er.merge(sr.er);
			}
			pw.close();
		}
		if (er.hasErrors()) {
			handleErrors(s, er);
			return;
		}
		assertGolden(new File(s, "pform"), pform);
		
		if (importFrom.isDirectory())
			compiler.searchIn(importFrom);

		// infer these kinds of things from the existence of directories; or else from some kind of settings.xml file
//		compiler.writeDroidTo(new File("null"));
		
//			compiler.dumpTypes();
		try {
			compiler.writeJSTo(jsto);
			compiler.writeHSIETo(hsie);
			compiler.writeFlimTo(flim);
			compiler.compile(dir);
		} catch (ErrorResultException ex) {
			handleErrors(s, ex.errors);
		}
		
		// Now assert that we matched things ...
		assertGolden(new File(s, "flim"), flim);
		assertGolden(new File(s, "hsie"), hsie);
		assertGolden(new File(s, "jsout"), jsto);
	}

	protected static void handleErrors(String s, ErrorResult er) throws FileNotFoundException, IOException {
		// either way, write the errors to a suitable directory
		File etmp = new File(s, "errors-tmp"); // may or may not be needed
		FileUtils.assertDirectory(etmp);
		PrintWriter pw = new PrintWriter(new File(etmp, "errors"));
		er.showTo(pw, 0);

		File errors = new File(s, "errors");
		if (errors.isDirectory()) {
			// we expected this, so check the errors are correct ...
			assertGolden(errors, etmp);
		} else {
			// we didn't expect the error, so by definition is an error
			er.showTo(new PrintWriter(System.out), 0);
			fail("unexpected compilation errors");
		}
	}

	private static void dumpRecursive(Indenter pw, Object obj) {
		if (obj == null) {
			pw.println("Error - null");
		} else if (obj instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) obj;
			pw.println("cdecl " + cd.name());
			dumpList(pw, cd.methods);
		} else if (obj instanceof ContractMethodDecl) {
			ContractMethodDecl cmd = (ContractMethodDecl) obj;
			pw.println(cmd.dir + " " + cmd.name);
			dumpList(pw, cmd.args);
		} else if (obj instanceof ConstPattern) {
			ConstPattern cp = (ConstPattern) obj;
			pw.println(cp.type + ": " + cp.value);
		} else if (obj instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) obj;
			pw.println("[type] " + tp.var);
			dumpRecursive(pw.indent(), tp.type);
		} else if (obj instanceof VarPattern) {
			VarPattern tp = (VarPattern) obj;
			pw.println("[var] " + tp.var);
		} else if (obj instanceof TuplePattern) {
			TuplePattern tp = (TuplePattern) obj;
			pw.println("[tuple]");
			dumpList(pw, tp.args);
		} else if (obj instanceof ConstructorMatch) {
			ConstructorMatch cm = (ConstructorMatch) obj;
			pw.println("[ctor] " + cm.ctor);
			dumpList(pw, cm.args);
		} else if (obj instanceof ConstructorMatch.Field) {
			ConstructorMatch.Field cf = (ConstructorMatch.Field) obj;
			pw.println(cf.field);
			dumpRecursive(pw.indent(), cf.patt);
		} else if (obj instanceof NumericLiteral) {
			NumericLiteral nl = (NumericLiteral) obj;
			pw.println("# " + nl.text);
		} else if (obj instanceof StringLiteral) {
			StringLiteral nl = (StringLiteral) obj;
			pw.println("'' " + nl.text);
		} else if (obj instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) obj;
			pw.println(uv.var);
		} else if (obj instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) obj;
			if (ae.fn instanceof ApplyExpr) {
				dumpRecursive(pw.indent(), ae.fn);
			} else
				pw.println(ae.fn.toString());
			dumpList(pw, ae.args);
		} else if (obj instanceof IfExpr) {
			IfExpr ie = (IfExpr) obj;
			pw.println("if " + ie.guard.toString());
			dumpRecursive(pw.indent(), ie.ifExpr);
			if (ie.elseExpr != null) {
				pw.println("else");
				dumpRecursive(pw.indent(), ie.elseExpr);
			}
//		} else if (obj instanceof FunctionDefinition) {
//			for (FunctionCaseDefn fcd : ((FunctionDefinition) obj).cases)
//				dumpRecursive(pw, fcd);
		} else if (obj instanceof FunctionCaseDefn) {
			FunctionCaseDefn fcd = (FunctionCaseDefn) obj;
			pw.println(fcd.intro.name);
			dumpList(pw, fcd.intro.args);
			pw.println(" =");
			dumpRecursive(pw.indent(), fcd.expr);
			dumpScope(pw, fcd.innerScope());
		} else if (obj instanceof CardDefinition) {
			CardDefinition cd = (CardDefinition) obj;
			pw.println("card " + cd.name);
			dumpRecursive(pw.indent(), cd.state);
			dumpRecursive(pw.indent(), cd.template);
		} else if (obj instanceof StateDefinition) {
			StateDefinition sd = (StateDefinition) obj;
			pw.println("state");
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructDefn) {
			StructDefn sd = (StructDefn) obj;
			pw.println("struct " + sd.name() + polys(sd));
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructField) {
			StructField sf = (StructField) obj;
			pw.println(sf.name);
			dumpRecursive(pw.indent(), sf.type);
			if (sf.init != null) {
				pw.println(" <-");
				dumpRecursive(pw.indent(), sf.init);
			}
		} else if (obj instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) obj;
			pw.println("handler " + hi.name() + " " + hi.hiName + " (" + (hi.inCard?"card":"free") + ")");
			dumpList(pw, hi.boundVars);
			dumpList(pw, hi.methods);
		} else if (obj instanceof MethodCaseDefn) {
			MethodCaseDefn mcd = (MethodCaseDefn) obj;
			pw.println("case " + mcd.caseName());
			dumpList(pw.indent(), mcd.intro.args);
			dumpList(pw.indent(), mcd.messages);
			dumpScope(pw, mcd.innerScope());
		} else if (obj instanceof MethodMessage) {
			MethodMessage mm = (MethodMessage) obj;
			if (mm.slot != null)
				pw.println("assign " + mm.slot + " <-");
			else
				pw.println("invoke <-");
			dumpRecursive(pw.indent(), mm.expr);
		} else if (obj instanceof Template) {
			Template t = (Template) obj;
			pw.println("template" + (t.prefix != null ? " " + t.prefix : ""));
			dumpRecursive(pw.indent(), t.content);
		} else if (obj instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) obj;
			pw.println("."); // many other fields go here ...
			dumpList(pw, td.nested);
			// dump formats and handlers
		} else if (obj instanceof TemplateList) {
			TemplateList td = (TemplateList) obj;
			pw.println("+ " + td.listVar + " " + td.iterVar); // many other fields go here ...
			dumpRecursive(pw.indent(), td.template);
			// dump formats and handlers
		} else if (obj instanceof ContentString) {
			ContentString ce = (ContentString) obj;
			pw.println("'' " + ce.text);
			// dump formats and handlers
		} else if (obj instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) obj;
			dumpRecursive(pw, ce.expr);
			// dump formats and handlers
		} else if (obj instanceof FunctionTypeReference) {
			FunctionTypeReference t = (FunctionTypeReference) obj;
			pw.println(t.name());
			Indenter ind = pw.indent();
			for (TypeReference a : t.args)
				dumpRecursive(ind, a);
		} else if (obj instanceof TupleMember) {
			TupleMember t = (TupleMember) obj;
			LocatedName locatedName = t.ta.vars.get(t.which);
			pw.println(locatedName.text);
			dumpRecursive(pw.indent(), t.ta);
		} else if (obj instanceof TupleAssignment) {
			TupleAssignment t = (TupleAssignment) obj;
			pw.println("(" + t.vars + ")");
			dumpRecursive(pw.indent(), t.expr);
		} else if (obj instanceof TypeReference) {
			TypeReference t = (TypeReference) obj;
			pw.println(t.name());
			if (t.hasPolys()) {
				Indenter ind = pw.indent();
				for (TypeReference p : t.polys())
					dumpRecursive(ind, p);
			}
		} else
			throw new UtilException("Cannot handle dumping " + obj.getClass());
	}

	private static String polys(StructDefn sd) {
		if (sd.polys() == null || sd.polys().isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (PolyType tr : sd.polys()) {
			sb.append(" ");
			sb.append(tr.name());
		}
		return sb.toString();
	}

	private static void dumpScope(Indenter pw, Scope s) {
		Indenter pi = pw.indent();
		for (ScopeEntry k : s) {
			dumpRecursive(pi, k.getValue());
		}
	}

	protected static void dumpList(Indenter pw, List<?> objs) {
		Indenter pi = pw.indent();
		for (Object x : objs)
			dumpRecursive(pi, x);
	}

	private static void assertGolden(File golden, File jsto) {
		if (!golden.isDirectory())
			fail("There is no golden directory " + golden);
		if (!jsto.isDirectory())
			fail("There is no generated directory " + jsto);
		for (File f : jsto.listFiles())
			assertTrue("There is no golden file for the generated " + f, new File(golden, f.getName()).exists());
		for (File f : golden.listFiles()) {
			File gen = new File(jsto, f.getName());
			assertTrue("There is no generated file for the golden " + f, gen.exists());
			String goldhash = Crypto.hash(f);
			String genhash = Crypto.hash(gen);
			if (!goldhash.equals(genhash)) {
				RunProcess proc = new RunProcess("diff");
				proc.arg("-C5");
				proc.arg(f.getPath());
				proc.arg(gen.getPath());
				proc.redirectStdout(System.out);
				proc.redirectStderr(System.err);
				proc.execute();
				proc.getExitCode();
			}
			assertEquals("Files " + f + " and " + gen + " differed", goldhash, genhash);
		}
	}

	private static void clean(File dir) {
		FileUtils.cleanDirectory(dir);
		FileUtils.assertDirectory(dir);
	}

	@Override
	protected void cleanUp() {
		// compiler.destroy();
	}

	@Override
	protected String getName() {
		return "FLAS Golden Tests";
	}
}
