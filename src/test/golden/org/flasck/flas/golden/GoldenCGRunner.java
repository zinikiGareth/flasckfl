package org.flasck.flas.golden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Pattern;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.D3Thing;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.tokenizers.TemplateToken;
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
import org.zinutils.utils.Indenter;
import org.zinutils.utils.StringUtil;

public class GoldenCGRunner extends CGHarnessRunner {
	static String checkEverythingS = System.getProperty("org.flasck.golden.check");
	static boolean checkEverything = checkEverythingS == null || !checkEverythingS.equalsIgnoreCase("false");
	static String stripNumbersS = System.getProperty("org.flasck.golden.strip"); 
	static boolean stripNumbers = stripNumbersS != null && stripNumbersS.equalsIgnoreCase("true");
	
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

		ByteCodeCreator bcc = emptyTestClass(bce, "goldenTests");
		for (File f : new File("src/golden").listFiles()) {
			if (f.isDirectory() && (p == null || p.matcher(f.getName()).find()))
				addGoldenTest(bcc, f);
		}
		return new Class<?>[] { generate(cl, bcc) };
	}

	private static void addGoldenTest(ByteCodeCreator bcc, final File f) {
		boolean ignoreTest = new File(f, "ignore").exists();
			
		addMethod(bcc, "test" + StringUtil.capitalize(f.getName()), ignoreTest, new TestMethodContentProvider() {
			@Override
			public void defineMethod(NewMethodDefiner done) {
				done.callStatic(GoldenCGRunner.class.getName(), "void", "runGolden", done.stringConst(f.getPath())).flush();
			}
		});
	}
	
	public static void runGolden(String s) throws Exception {
		System.out.println("Run golden test for " + s);
		File importFrom = new File(s, "import");
		File pform = new File(s, "parser-tmp");
		File rwform = new File(s, "rw-tmp");
		File jsto = new File(s, "jsout-tmp");
		File dependTo = new File(s, "depend-tmp");
		File hsie = new File(s, "hsie-tmp");
		File flim = new File(s, "flim-tmp");
		File tc2 = new File(s, "tc-tmp");
		File droidTo = new File(s, "droid-to");
		File droid = new File(s, "droid-tmp");
		File testReportTo = new File(s, "testReports-tmp");
		
		FileUtils.deleteDirectoryTree(new File(s, "errors-tmp"));
		clean(pform);
		clean(rwform);
		clean(jsto);
		clean(hsie);
		clean(flim);
		clean(droidTo);
		clean(droid);
		clean(tc2);
		
		Main.setLogLevels();
		FLASCompiler compiler = new FLASCompiler();
		compiler.unitTestPath(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		File dir = new File(s, "test.golden");
		ErrorResult er = new ErrorResult();
		for (File input : FileUtils.findFilesMatching(dir, "*.fl")) {
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
		File golden = new File(s, "pform");
		if (stripNumbers) {
			stripPform(pform);
			stripPform(golden);
		}
		assertGolden(golden, pform);
		
		if (importFrom.isDirectory())
			compiler.searchIn(importFrom);

		File depend = new File(s, "depend");
		try {
			compiler.trackTC(tc2);
			compiler.writeRWTo(rwform);
			compiler.writeJSTo(jsto);
			compiler.writeHSIETo(hsie);
			compiler.writeFlimTo(flim);
			compiler.writeDroidTo(droidTo, false);
			if (haveTests(dir)) {
				clean(testReportTo);
				compiler.writeTestReportsTo(testReportTo);
			}
			if (depend.isDirectory()) {
				clean(dependTo);
				compiler.writeDependsTo(dependTo);
			}
			compiler.compile(dir);
			File errors = new File(s, "errors");
			if (errors.isDirectory())
				fail("expected errors, but none occurred");
		} catch (ErrorResultException ex) {
			handleErrors(s, ex.errors);
		}
		
		// Now assert that we matched things ...
		if (depend.isDirectory()) {
			assertGolden(depend, dependTo);
		}
		assertGolden(new File(s, "rw"), rwform);
		File goldhs = new File(s, "hsie");
		if (stripNumbers) {
			stripHSIE(goldhs);
			stripHSIE(hsie);
		}
		assertGolden(new File(s, "jsout"), jsto);
		assertGolden(goldhs, hsie);
		assertGolden(new File(s, "tc"), tc2);
		assertGolden(new File(s, "flim"), flim);

		if (new File(droidTo, "qbout/classes/test/golden").isDirectory()) {
			RunProcess proc = new RunProcess("javap");
			proc.arg("-c");
			for (File f : FileUtils.findFilesMatching(new File(droidTo, "qbout/classes/test/golden"), "*.class")) {
				proc.arg(f.getPath());
			}
			FileOutputStream fos = new FileOutputStream(new File(droid, "droid.clz"));
			proc.redirectStdout(fos);
			proc.redirectStderr(fos);
			proc.execute();
			fos.close();
			
			assertGolden(new File(s, "droid"), droid);
		}
		
		if (haveTests(dir)) {
			assertGolden(new File(s, "testReports"), testReportTo);
		}
	}

	private static boolean haveTests(File dir) {
		return !FileUtils.findFilesMatching(dir, "*.ut").isEmpty();
	}

	// I want to see the locations, but I don't (always) want to be bound to them.  Remove them if desired (i.e. call this method if desired)
	private static void stripPform(File pform) throws IOException {
		for (File pf : pform.listFiles()) {
			File tmp = File.createTempFile("temp", ".pf");
			tmp.deleteOnExit();
			PrintWriter to = new PrintWriter(tmp);
			LineNumberReader lnr = new LineNumberReader(new FileReader(pf));
			try {
				String s;
				while ((s = lnr.readLine()) != null) {
					int idx = s.indexOf(" @{");
					if (idx != -1)
						s = s.substring(0, idx);
					to.println(s);
				}
			} finally {
				to.close();
				lnr.close();
			}
			FileUtils.copy(tmp, pf);
			tmp.delete();
		}
	}

	private static void stripHSIE(File pform) throws IOException {
		for (File pf : pform.listFiles()) {
			File tmp = File.createTempFile("temp", ".hs");
			tmp.deleteOnExit();
			PrintWriter to = new PrintWriter(tmp);
			LineNumberReader lnr = new LineNumberReader(new FileReader(pf));
			try {
				String s;
				while ((s = lnr.readLine()) != null) {
					int idx = s.indexOf(" #");
					if (idx != -1)
						s = StringUtil.trimRight(s.substring(0, idx));
					to.println(s);
				}
			} finally {
				to.close();
				lnr.close();
			}
			FileUtils.copy(tmp, pf);
			tmp.delete();
		}
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
		} else if (obj instanceof String) { // I'm not sure I really believe in this case, but it came up
			pw.println("String: " + (String) obj);
		} else if (obj instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) obj;
			pw.print("cdecl " + cd.nameAsName().jsName());
			dumpPosition(pw, cd.kw, false);
			dumpLocation(pw, cd);
			dumpList(pw, cd.methods);
		} else if (obj instanceof ContractMethodDecl) {
			ContractMethodDecl cmd = (ContractMethodDecl) obj;
			pw.print((cmd.required?"required":"optional") + " " + cmd.dir + " " + cmd.name);
			if (!cmd.required)
				dumpPosition(pw, cmd.rkw, false);
			dumpPosition(pw, cmd.dkw, false);
			dumpLocation(pw, cmd);
			dumpList(pw, cmd.args);
		} else if (obj instanceof ConstPattern) {
			ConstPattern cp = (ConstPattern) obj;
			pw.println(cp.type + ": " + cp.value);
		} else if (obj instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) obj;
			pw.print("[type] " + tp.var);
			dumpPosition(pw, tp.varLocation, true);
			dumpRecursive(pw.indent(), tp.type);
		} else if (obj instanceof VarPattern) {
			VarPattern tp = (VarPattern) obj;
			pw.print("[var] " + tp.var);
			dumpLocation(pw, tp);
		} else if (obj instanceof TuplePattern) {
			TuplePattern tp = (TuplePattern) obj;
			pw.println("[tuple]");
			dumpList(pw, tp.args);
		} else if (obj instanceof ConstructorMatch) {
			ConstructorMatch cm = (ConstructorMatch) obj;
			pw.print("[ctor] " + cm.ctor);
			dumpLocation(pw, cm);
			dumpList(pw, cm.args);
		} else if (obj instanceof ConstructorMatch.Field) {
			ConstructorMatch.Field cf = (ConstructorMatch.Field) obj;
			pw.print(cf.field);
			dumpLocation(pw, cf);
			dumpRecursive(pw.indent(), cf.patt);
		} else if (obj instanceof NumericLiteral) {
			NumericLiteral nl = (NumericLiteral) obj;
			pw.print("# " + nl.text);
			dumpLocation(pw, nl);
		} else if (obj instanceof StringLiteral) {
			StringLiteral sl = (StringLiteral) obj;
			pw.print("'' " + sl.text);
			dumpLocation(pw, sl);
		} else if (obj instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) obj;
			pw.print(uv.var);
			dumpLocation(pw, uv);
		} else if (obj instanceof UnresolvedOperator) {
			UnresolvedOperator uv = (UnresolvedOperator) obj;
			pw.print(uv.op);
			dumpLocation(pw, uv);
		} else if (obj instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) obj;
			pw.print("<apply>");
			dumpLocation(pw, ae);
			dumpRecursive(pw.indent(), ae.fn);
			dumpList(pw, ae.args);
		} else if (obj instanceof IfExpr) {
			IfExpr ie = (IfExpr) obj;
			pw.println("if " + ie.guard.toString());
			dumpRecursive(pw.indent(), ie.ifExpr);
			if (ie.elseExpr != null) {
				pw.println("else");
				dumpRecursive(pw.indent(), ie.elseExpr);
			}
		} else if (obj instanceof CastExpr) {
			CastExpr ce = (CastExpr) obj;
			pw.print("cast " + ce.castTo);
			dumpPosition(pw, ce.ctLoc, true);
			dumpRecursive(pw.indent(), ce.expr);
		} else if (obj instanceof FunctionCaseDefn) {
			FunctionCaseDefn fcd = (FunctionCaseDefn) obj;
			pw.print(fcd.intro.name);
			dumpLocation(pw, fcd);
			dumpList(pw, fcd.intro.args);
			pw.println(" =");
			dumpRecursive(pw.indent(), fcd.expr);
			dumpScope(pw, fcd.innerScope());
		} else if (obj instanceof CardDefinition) {
			CardDefinition cd = (CardDefinition) obj;
			pw.print("card " + cd.cardName.jsName());
			dumpPosition(pw, cd.kw, false);
			dumpLocation(pw, cd);
			if (cd.state != null)
				dumpRecursive(pw.indent(), cd.state);
			for (Template t : cd.templates)
				dumpRecursive(pw.indent(), t);
			dumpList(pw, cd.d3s);
			dumpList(pw, cd.contracts);
			dumpList(pw, cd.handlers);
			dumpList(pw, cd.services);
			dumpScope(pw, cd.innerScope());
		} else if (obj instanceof StateDefinition) {
			StateDefinition sd = (StateDefinition) obj;
			pw.print("state");
			dumpLocation(pw, sd);
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructDefn) {
			StructDefn sd = (StructDefn) obj;
			pw.print("struct " + sd.name() + polys(sd));
			dumpPosition(pw, sd.kw, false);
			dumpPosition(pw, sd.location(), false);
			for (PolyType p : sd.polys())
				dumpPosition(pw, p.location(), false);
			pw.println("");
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructField) {
			StructField sf = (StructField) obj;
			pw.print(sf.name);
			dumpLocation(pw, sf);
			dumpRecursive(pw.indent(), sf.type);
			if (sf.init != null) {
				pw.print(" <-");
				dumpPosition(pw, sf.assOp, true);
				dumpRecursive(pw.indent(), sf.init);
			}
		} else if (obj instanceof ContractImplements) {
			ContractImplements ctr = (ContractImplements) obj;
			pw.print("implements " + ctr.name() + (ctr.referAsVar != null ? " " + ctr.referAsVar : ""));
			dumpPosition(pw, ctr.kw, false);
			dumpPosition(pw, ctr.location(), ctr.referAsVar == null);
			if (ctr.referAsVar != null)
				dumpPosition(pw, ctr.varLocation, true);
			dumpList(pw, ctr.methods);
		} else if (obj instanceof ContractService) {
			ContractService ctr = (ContractService) obj;
			pw.print("service " + ctr.name() + (ctr.referAsVar != null ? " " + ctr.referAsVar : ""));
			dumpPosition(pw, ctr.kw, false);
			dumpPosition(pw, ctr.location(), ctr.referAsVar == null);
			if (ctr.referAsVar != null)
				dumpPosition(pw, ctr.vlocation, true);
			dumpList(pw, ctr.methods);
		} else if (obj instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) obj;
			pw.print("handler " + hi.name() + " " + hi.hiName + " (" + (hi.inCard?"card":"free") + ")");
			dumpPosition(pw, hi.kw, false);
			dumpPosition(pw, hi.typeLocation, false);
			dumpLocation(pw, hi);
			dumpList(pw, hi.boundVars);
			dumpList(pw, hi.methods);
		} else if (obj instanceof MethodCaseDefn) {
			MethodCaseDefn mcd = (MethodCaseDefn) obj;
			pw.print("method " + mcd.caseName().jsName());
			dumpLocation(pw, mcd);
			dumpList(pw, mcd.intro.args);
			dumpList(pw, mcd.messages);
			dumpScope(pw, mcd.innerScope());
		} else if (obj instanceof EventCaseDefn) {
			EventCaseDefn ecd = (EventCaseDefn) obj;
			pw.print("event " + ecd.caseName().jsName());
			dumpPosition(pw, ecd.kw, false);
			dumpLocation(pw, ecd);
			dumpList(pw, ecd.intro.args);
			dumpList(pw, ecd.messages);
			dumpScope(pw, ecd.innerScope());
		} else if (obj instanceof MethodMessage) {
			MethodMessage mm = (MethodMessage) obj;
			if (mm.slot != null) {
				pw.print("assign " + slotName(mm.slot) + " <-");
				for (Locatable x : mm.slot) {
					dumpPosition(pw, x.location(), false);
				}
			} else {
				pw.print("invoke");
			}
			dumpPosition(pw, mm.kw, true);
			dumpRecursive(pw.indent(), mm.expr);
		} else if (obj instanceof Template) {
			Template t = (Template) obj;
			pw.print("template" + (t.name.baseName() != null ? " " + t.name.baseName() : ""));
			dumpPosition(pw, t.kw, false);
			dumpPosition(pw, t.location(), false);
			for (LocatedToken a : t.args) {
				pw.print(" " + a.text);
			}
			for (LocatedToken a : t.args) {
				dumpPosition(pw, a.location, false);
			}
			pw.newline();
			if (t.content != null)
				dumpRecursive(pw.indent(), t.content);
		} else if (obj instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) obj;
			pw.print(".");
			if (td.customTagLoc != null) // cannot test the var because we hack in "li"
				pw.print(" " + td.customTag);
			else if (td.customTagVar != null)
				pw.print(" " + td.customTagVar);
			dumpPosition(pw, td.kw, false);
			if (td.customTagLoc != null)
				dumpPosition(pw, td.customTagLoc, false);
			else if (td.customTagVar != null)
				dumpPosition(pw, td.customTagVarLoc, false);
			pw.newline();
			dumpList(pw, td.attrs);
			dumpList(pw, td.nested);
		} else if (obj instanceof TemplateExplicitAttr) {
			TemplateExplicitAttr attr = (TemplateExplicitAttr) obj;
			pw.print("attr " + attr.attr);
			dumpPosition(pw, attr.location, true);
			dumpRecursive(pw.indent(), attr.value);
		} else if (obj instanceof TemplateList) {
			TemplateList td = (TemplateList) obj;
			pw.print("+ " + td.listExpr);
			if (td.iterVar != null)
				pw.print(" " + td.iterVar);
			dumpPosition(pw, td.kw, false);
			dumpPosition(pw, td.listLoc, td.iterVar == null);
			if (td.iterVar != null)
				dumpPosition(pw, td.iterLoc, true);
			dumpRecursive(pw.indent(), td.template);
		} else if (obj instanceof TemplateReference) {
			TemplateReference tr = (TemplateReference) obj;
			pw.print(tr.name);
			dumpLocation(pw, tr);
			dumpList(pw, tr.args);
		} else if (obj instanceof TemplateCardReference) {
			TemplateCardReference tr = (TemplateCardReference) obj;
			if (tr.explicitCard != null)
				pw.print("Explicit " + tr.explicitCard);
			else
				pw.print("Yoyo "+ tr.yoyoVar);
			dumpLocation(pw, tr);
		} else if (obj instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) obj;
			pw.print("Cases");
			dumpLocation(pw, tc);
			dumpRecursive(pw.indent(), tc.switchOn);
			dumpList(pw, tc.cases);
		} else if (obj instanceof TemplateOr) {
			TemplateOr tor = (TemplateOr) obj;
			if (tor.cond != null)
				pw.print("Or");
			else
				pw.print("Else");
			dumpLocation(pw, tor);
			if (tor.cond != null)
				dumpRecursive(pw.indent(), tor.cond);
			dumpRecursive(pw.indent(), tor.template);
		} else if (obj instanceof ContentString) {
			ContentString ce = (ContentString) obj;
			pw.print("'' " + ce.text);
			dumpPosition(pw, ce.kw, true);
		} else if (obj instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) obj;
			pw.print("<cexpr>");
			dumpPosition(pw, ce.kw, true);
			dumpRecursive(pw.indent(), ce.expr);
		} else if (obj instanceof D3Thing) {
			D3Thing d3 = (D3Thing) obj;
			dumpRecursive(pw, d3.d3);
			dumpList(pw, d3.patterns);
		} else if (obj instanceof D3Intro) {
			D3Intro d3 = (D3Intro) obj;
			pw.print("d3 " + d3.name + " " + d3.iterVar.var);
			dumpPosition(pw, d3.kw, false);
			dumpPosition(pw, d3.nameLoc, false);
			dumpPosition(pw, d3.varLoc, true);
			dumpRecursive(pw.indent(), d3.expr);
		} else if (obj instanceof D3PatternBlock) {
			D3PatternBlock blk = (D3PatternBlock) obj;
			pw.print("Pattern " + blk.pattern.text);
			dumpPosition(pw, blk.kw, false);
			dumpPosition(pw, blk.pattern.location, true);
			for (D3Section x : blk.sections)
				dumpRecursive(pw.indent(), x);
		} else if (obj instanceof D3Section) {
			D3Section s = (D3Section) obj;
			pw.print(s.name);
			dumpLocation(pw, s);
			dumpList(pw, s.properties);
			dumpList(pw, s.actions);
		} else if (obj instanceof PropertyDefn) {
			PropertyDefn d = (PropertyDefn) obj;
			pw.print(d.name);
			dumpLocation(pw, d);
			dumpRecursive(pw, d.value);
		} else if (obj instanceof TemplateToken) {
			// used in formats at least
			TemplateToken tt = (TemplateToken) obj;
			pw.print("format ");
			if (tt.type == TemplateToken.STRING) {
				pw.print("'' " + tt.text);
				dumpPosition(pw, tt.location, true);
			} else if (tt.type == TemplateToken.IDENTIFIER) {
				pw.print(tt.text);
				dumpPosition(pw, tt.location, true);
			} else
				throw new UtilException("Can't handle template token " + tt.type);
		} else if (obj instanceof EventHandler) {
			EventHandler eh = (EventHandler) obj;
			pw.print("=>");
			dumpPosition(pw, eh.kw, true);
			Indenter p2 = pw.indent();
			p2.print(eh.action);
			dumpPosition(p2, eh.actionPos, true);
			dumpRecursive(pw.indent(), eh.expr);
		} else if (obj instanceof FunctionTypeReference) {
			FunctionTypeReference t = (FunctionTypeReference) obj;
			pw.print(t.name());
			dumpLocation(pw, t);
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
			pw.print(t.name());
			dumpLocation(pw, t);
			if (t.hasPolys()) {
				Indenter ind = pw.indent();
				for (TypeReference p : t.polys())
					dumpRecursive(ind, p);
			}
		} else
			throw new UtilException("Cannot handle dumping " + obj.getClass());
		if (obj instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) obj;
			dumpList(pw, tf.formats);
			if (obj instanceof TemplateFormatEvents) {
				TemplateFormatEvents tfe = (TemplateFormatEvents) tf;
				dumpList(pw, tfe.handlers);
			}
		}
	}

	private static void dumpLocation(Indenter pw, Locatable obj) {
		dumpPosition(pw, obj.location(), true);
	}

	private static void dumpPosition(Indenter pw, InputPosition pos, boolean withNL) {
		if (pos == null) {
			pw.print(" @{null}");
		} else {
			pw.print(" @{" + pos.lineNo + ":" + pos.off + "|" + pos.asToken() + "}");
		}
		if (withNL)
			pw.println("");
	}

	private static String slotName(List<Locatable> slot) {
		StringBuilder ret = new StringBuilder();
		for (Locatable s : slot) {
			LocatedToken t = (LocatedToken) s;
			if (ret.length() > 0)
				ret.append(".");
			ret.append(t.text);
		}
		return ret.toString();
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

	private static void assertGolden(File golden, File genned) {
		if (!golden.isDirectory()) {
			if (!checkEverything)
				return;
			fail("There is no golden directory " + golden);
		}
		if (!genned.isDirectory()) {
			if (!checkEverything)
				return;
			fail("There is no generated directory " + genned);
		}
		for (File f : genned.listFiles())
			assertTrue("There is no golden file for the generated " + f, new File(golden, f.getName()).exists());
		for (File f : golden.listFiles()) {
			File gen = new File(genned, f.getName());
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
