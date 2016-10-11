package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.flasck.flas.Compiler;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.cgharness.CGHClassLoaderImpl;
import org.zinutils.cgharness.CGHarnessRunner;
import org.zinutils.exceptions.UtilException;
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
		addMethod(bcc, "testSomething", new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath())).flush();
			}
		});
		return generate(cl, bcc);
	}
	
	public static void runGolden(String s) throws Exception {
		System.out.println("Run golden test for " + s);
		File etmp = new File(s, "errors-tmp"); // may or may not be needed
		File pform = new File(s, "parser-tmp");
		File jsto = new File(s, "jsout-tmp");
		File hsie = new File(s, "hsie-tmp");
		File flim = new File(s, "flim-tmp");
		FileUtils.deleteDirectoryTree(etmp);
		clean(pform);
		clean(jsto);
		clean(hsie);
		clean(flim);
		try {
			Compiler.setLogLevels();
			Compiler compiler = new Compiler();
			File dir = new File(s, "test.golden");
			for (File input : dir.listFiles()) {
				StoryRet sr = compiler.parse("test.golden", FileUtils.readFile(input));
				Indenter pw = new Indenter(new File(pform, input.getName().replace(".fl", ".pf")));
				if (sr.top != null && sr.top.getValue() != null)
					dumpRecursive(pw, sr.top.getValue());
				pw.close();
			}
//			assertGolden(new File(s, "pform"), pform);
			
			// read these kinds of things from "new File(s, ".settings")"
	//		compiler.writeDroidTo(new File("null"));
	//		compiler.searchIn(new File("src/main/resources/flim"));
			
			compiler.dumpTypes();
			compiler.writeJSTo(jsto);
			compiler.writeHSIETo(hsie);
			compiler.writeFlimTo(flim);
			compiler.compile(dir);
			
			// Now assert that we matched things ...
//			assertGolden(new File(s, "jsout"), jsto);
		} catch (ErrorResultException ex) {
			// either way, write the errors to a suitable directory
			FileUtils.assertDirectory(etmp);
			PrintWriter pw = new PrintWriter(new File(etmp, "errors"));
			ex.errors.showTo(pw, 0);

			File errors = new File(s, "errors");
			if (errors.isDirectory()) {
				// we expected this, so check the errors are correct ...
				assertGolden(errors, etmp);
			} else {
				// we didn't expect the error, so by definition is an error
				ex.errors.showTo(new PrintWriter(System.out), 0);
				fail("unexpected compilation errors");
			}
		}
	}

	private static void dumpRecursive(Indenter pw, Object obj) {
		if (obj == null) {
			pw.println("Error - null");
		} else if (obj instanceof PackageDefn) {
			PackageDefn se = (PackageDefn) obj;
			pw.println("package " + se.name);
			dumpScope(pw, se.innerScope());
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
			pw.println(tp.var);
			dumpRecursive(pw.indent(), tp.type);
		} else if (obj instanceof VarPattern) {
			VarPattern tp = (VarPattern) obj;
			pw.println(tp.var);
		} else if (obj instanceof ConstructorMatch) {
			ConstructorMatch cm = (ConstructorMatch) obj;
			pw.println(cm.ctor);
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
			pw.println(ae.fn.toString());
			dumpList(pw, ae.args);
		} else if (obj instanceof FunctionDefinition) {
			// NOTE: this should go away and just be the individual cases
			FunctionDefinition fd = (FunctionDefinition) obj;
//			pw.println(fd.name);
			for (FunctionCaseDefn fcd : fd.cases) {
				dumpRecursive(pw, fcd);
			}
		} else if (obj instanceof FunctionCaseDefn) {
			FunctionCaseDefn fcd = (FunctionCaseDefn) obj;
			pw.println(fcd.intro.name);
			dumpList(pw, fcd.intro.args);
			pw.println(" =");
			dumpRecursive(pw.indent(), fcd.expr);
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
			pw.println("struct " + sd.name());
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructField) {
			StructField sf = (StructField) obj;
			pw.println(sf.name);
			dumpRecursive(pw.indent(), sf.type);
			if (sf.init != null) {
				pw.println(" <-");
				dumpRecursive(pw.indent(), sf.init);
			}
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
		} else if (obj instanceof Type) { // NOTE: this has to go below all its subclasses!
			Type t = (Type) obj;
			if (t.iam != WhatAmI.REFERENCE)
				throw new UtilException("I don't think this should happen: " + t.iam);
			pw.println(t.name());
		} else
			throw new UtilException("Cannot handle dumping " + obj.getClass());
	}

	private static void dumpScope(Indenter pw, Scope s) {
		Indenter pi = pw.indent();
		for (String k : s.keys()) {
			Object o = s.get(k);
			dumpRecursive(pi, o);
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
				// TODO: should do a visual line by line diff here ...
				System.out.println("Need visual diff");
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
