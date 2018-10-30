package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.ConfigVisitor;
import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.flim.PackageImporter;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.htmlzip.BuilderSink;
import org.flasck.flas.htmlzip.MultiSink;
import org.flasck.flas.htmlzip.ShowCardSink;
import org.flasck.flas.htmlzip.SplitZip;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.sugardetox.SugarDetox;
import org.flasck.flas.template.TemplateTraversor;
import org.flasck.flas.testrunner.FileUnitTestResultHandler;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.flas.testrunner.TestScript;
import org.flasck.flas.testrunner.UnitTestPhase;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.cbstore.json.FLConstructorServer;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class FLASCompiler implements ScriptCompiler, ConfigVisitor {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	public static boolean backwardCompatibilityMode = true;
	private boolean dumpTypes = false;
	private boolean unitjs;
	private boolean unitjvm;
	private final List<File> pkgdirs = new ArrayList<File>();
	private File writeRW;
	private DroidBuilder builder = new DroidBuilder();
	private File writeFlim;
	private File writeDepends;
	private File writeHSIE;
	private File writeJVM;
	private File trackTC;
	private File writeJS;
	private File writeTestReports;
	private final List<CompileResult> priors = new ArrayList<>();
	private final List<File> utpaths = new ArrayList<File>();
	private final List<String> webzips = new ArrayList<>();
	private File webzipdir;
	private File webdownloaddir;
	private BuilderSink sink = new BuilderSink();
	private ErrorResult errors = new ErrorResult();
	private PrintWriter errorWriter;

	public FLASCompiler(Configuration config) {
		if (config != null)
			config.visit(this);
	}

	// configuration aspects - should we break this off into a subclass?
	@Override
	public void dumpTypes(boolean d) {
		this.dumpTypes = d;
	}
	
	@Override
	public void searchIn(File file) {
		pkgdirs.add(file);
	}
	
	public void unitTestPath(File file) {
		utpaths.add(file);
	}
	
	@Override
	public void writeJVMTo(File file) {
		writeJVM = file;
	}
	
	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put it
	@Override
	public void writeDroidTo(File file, boolean andBuild) {
		if (file == null || file.getPath().equals("null"))
			return;
		builder = new DroidBuilder(file);
		if (!andBuild)
			builder.dontBuild();
	}
	
	public void internalBuildJVM() {
		builder = new DroidBuilder();
		builder.dontBuild();
	}

	@Override
	public void writeRWTo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeRW = file;
	}

	@Override
	public void writeFlimTo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeFlim = file;
	}

	@Override
	public void writeDependsTo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeDepends = file;
	}

	@Override
	public void writeHSIETo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeHSIE = file;
	}

	@Override
	public void trackTC(File file) {
		if (file != null)
			this.trackTC = new File(file, "types");
		else
			this.trackTC = null;
	}

	@Override
	public void writeJSTo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeJS = file;
	}

	@Override
	public void writeTestReportsTo(File file) {
		if (file != null && !file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeTestReports = file;
	}

	@Override
	public void unitjs(boolean b) {
		this.unitjs = b;
	}

	@Override
	public void unitjvm(boolean b) {
		this.unitjvm = b;
	}

	@Override
	public void webZipDownloads(File file) {
		this.webdownloaddir = file;
	}

	@Override
	public void webZipDir(File file) {
		this.webzipdir = file;
	}

	@Override
	public void useWebZip(String called) {
		webzips.add(called);
	}
	
	@Deprecated
	public void includePrior(CompileResult cr) {
		priors.add(cr);
	}

	public void errorWriter(PrintWriter printWriter) {
		this.errorWriter = printWriter;
	}

	// Complete initialization by preparing the compiler for use
	public void scanWebZips() {
		if (webzips.isEmpty())
			return;
		if (webzipdir == null) {
			errors.message((Block)null, "using webzips requires a webzipdir");
			return;
		}
		for (String s : webzips)
			scanWebZip(s);
	}

	private void scanWebZip(final String called) {
		File f = new File(webzipdir, called);
		if (webdownloaddir != null) {
			File dl = new File(webdownloaddir, called);
			if (dl.exists()) {
				System.out.println("Moving download file " + dl + " to " + f);
				FileUtils.copy(dl, f);
				FileUtils.deleteDirectoryTree(dl);
			}
		}
		if (!f.exists()) {
			System.err.println("There is no webzip " + f);
		}
		SplitZip sz = new SplitZip();
		try {
			sz.split(new MultiSink(sink, new ShowCardSink()), f);
		} catch (IOException ex) {
			System.err.println("Failed to read " + f);
			System.err.println(ex);
		}
	}

	// Now read and parse all the files, passing it on to the alleged phase2
	public void parse(File dir) {
		if (!dir.isDirectory()) {
			errors.message((InputPosition)null, "there is no input directory " + dir);
			return;
		}

		String inPkg = dir.getName();
		System.out.println("Package " + inPkg);
		ActualPhase2Processor p2 = new ActualPhase2Processor(errors, this, inPkg);
		ParsingPhase p1 = new ParsingPhase(errors, p2);
		ErrorMark mark = errors.mark();
		for (File f : FileUtils.findFilesMatching(dir, "*.fl")) {
			System.out.println(" > " + f.getName());
			p1.process(f);
			errors.showFromMark(mark, errorWriter, 4);
			mark = errors.mark();
			
		}
		UnitTestPhase ut = new UnitTestPhase(errors, p2);
		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			System.out.println(" > " + f.getName());
			ut.process(f);
			errors.showFromMark(mark, errorWriter, 4);
			mark = errors.mark();
		}
		if (errors.hasErrors())
			return;
		p2.process();
		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 4);
			return;
		}
		ut.runTests(unitjvm, unitjs, writeTestReports, utpaths, p2.grabBCE());
		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 4);
			return;
		}
	}

	// The objective of this method is to convert an entire package directory at one go
	// Thus the entire context of this is a single package
	@Deprecated // I'm deprecating this because I want to go to a different flow
	public CompileResult compile(File dir) throws ErrorResultException, IOException, ClassNotFoundException {
		String inPkg = dir.getName();
		if (!dir.isDirectory()) {
			errors.message((InputPosition)null, "there is no input directory " + dir);
			return null;
		}

		boolean failed = false;
		final FLASStory storyProc = new FLASStory();
		final Scope scope = Scope.topScope(inPkg);
		final List<String> pkgs = new ArrayList<String>();
		pkgs.add(inPkg);
		
		for (File f : FileUtils.findFilesMatching(dir, "*.fl")) {
			System.out.println(" > " + f.getName());
			FileReader r = null;
			try {
				r = new FileReader(f);
				readIntoScope(inPkg, errors, storyProc, scope, f.getName(), r);
			} catch (IOException ex1) {
				failed = true;
				ex1.printStackTrace();
			} finally {
				if (r != null) try { r.close(); } catch (IOException ex3) {}
			}
		}

		if (failed || errors.hasErrors())
			return null;

		CompileResult cr = stage2(errors, null, null, inPkg, scope);
		if (cr == null)
			return null;

		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			MultiTextEmitter results;
			boolean close;
			if (writeTestReports != null && writeTestReports.isDirectory()) {
				results = new MultiTextEmitter(new File(writeTestReports, f.getName().replaceFirst(".ut$", ".txt")));
				close = true;
			} else {
				results = new MultiTextEmitter(System.out);
				close = false;
			}
			
			try {
				FLASCompiler sc = new FLASCompiler(null);
				sc.includePrior(cr);
				sc.writeJVMTo(this.writeJVM);
				// TODO: we probably need to configure the compiler here ...
				UnitTestRunner utr = new UnitTestRunner(errors);
				utr.sendResultsTo(new FileUnitTestResultHandler(results));
				
				// We presumably needs some set of options to say which runners
				// we want to execute - could be more than one
				if (unitjvm) {
					JVMRunner jvmRunner = new JVMRunner(cr, new FLConstructorServer(cr.bce.getClassLoader()));
					for (File p : utpaths)
						jvmRunner.considerResource(p);
					TestScript scr = utr.prepare(sc, jvmRunner, cr.getPackage().uniqueName() +".script", cr.getScope(), f);
					utr.run(jvmRunner, scr);
				}
				if (unitjs) {
					JSRunner jsRunner = new JSRunner(cr);
					TestScript scr = utr.prepare(sc, jsRunner, cr.getPackage().uniqueName() +".script", cr.getScope(), f);
					utr.run(jsRunner, scr);
				}
			} finally {
			if (close)
				results.close();
			}
		}
		
		// TODO: we also want to support general *.pt (protocol test) files and run them against cards/services that claim to support that protocol
		
		return cr;
	}

	CompileResult stage2(ErrorReporter er, String priorPackage, IScope priorScope, String inPkg, Scope scope) throws ErrorResultException, IOException {
		ErrorResult errors = (ErrorResult) er;
		File writeTo = writeJS!= null ? new File(writeJS, inPkg + ".js"):null;
		File exportTo = writeFlim!=null?new File(writeFlim, inPkg + ".flim"):null;
			
		// 3. Rework any "syntatic sugar" forms into their proper forms
		new SugarDetox(errors).detox(scope);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
		
		FileWriter wjs = null;
		FileOutputStream wex = null;
		PrintWriter tcPW = null;
		try {
			ImportPackage rootPkg = Builtin.builtins();
			final Rewriter rewriter = new Rewriter(errors, pkgdirs, rootPkg, sink);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors, rewriter);
			final ByteCodeEnvironment bce = new ByteCodeEnvironment();

			rewriter.importPackage1(rootPkg);
			
			for (CompileResult cr : priors) {
				PackageImporter.importInto(rewriter.pkgFinder, errors, rewriter, cr.getPackage().uniqueName(), cr.exports());
			}
			
			rewriter.rewritePackageScope(priorPackage, priorScope, inPkg, scope);
			abortIfErrors(errors);
			
			if (writeRW != null) {
				rewriter.writeGeneratableTo(new File(writeRW, "analysis.txt"));
			}

			rewriter.checkCardContractUsage();

			// 5. Register JS and Droid code generators with the visitors
			JSTarget target = new JSTarget(inPkg);
			Generator gen = new Generator(target);
			rewriter.registerCodeGenerator(gen);
			final DroidGenerator dg = new DroidGenerator(bce, builder);
			dg.registerWith(rewriter);

			rewriter.visitGenerators();
			
//			System.out.println("defns = " + rewriter.functions.keySet());
			
			// 6. Convert methods to functions
			Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>(rewriter.functions);
			MethodConvertor mc = new MethodConvertor(errors, rewriter);
			mc.convertContractMethods(rewriter, functions, rewriter.methods);
			mc.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
			mc.convertStandaloneMethods(rewriter, functions, rewriter.standalone.values());
			abortIfErrors(errors);

			if (writeRW != null) {
				rewriter.writeGeneratableFunctionsTo(new File(writeRW, "functions.txt"), functions);
			}

			// 7. Do dependency analysis on functions and group them together in orchards
			DependencyAnalyzer da = new DependencyAnalyzer();
			List<Set<RWFunctionDefinition>> defns = da.analyze(functions);
			abortIfErrors(errors);
			if (writeDepends != null)
				writeDependencies(da, defns);
			
			for (Set<RWFunctionDefinition> orch : defns) {
				showDefns(orch);
			}
			
			// 8. Now process each set
			//   a. convert functions to HSIE
			//   b. typechecking

			TypeChecker2 tc2 = new TypeChecker2(errors, rewriter);
			if (trackTC != null) {
				tcPW = new PrintWriter(trackTC);
				tc2.trackTo(tcPW);
			}
			tc2.populateTypes();
			abortIfErrors(errors);

			for (Set<RWFunctionDefinition> d : defns) {
				hsie.createForms(d);
			}
			hsie.liftLambdas();
			
			for (Set<RWFunctionDefinition> d : defns) {
				
				// 8a. Convert each orchard to HSIE
				Set<HSIEForm> forms = hsie.orchard(d);
				abortIfErrors(errors);
				
				// 8b. Typecheck all the methods together
				tc2.typecheck(forms);
				
				// TODO: this is over-eager.  We should check if we depend on any of the failures are required by subsequent steps.
				abortIfErrors(errors);
			}
			// 9. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc2, hsie.allForms());
			abortIfErrors(errors);
			
			if (writeHSIE != null) {
				PrintWriter hsiePW = new PrintWriter(new File(writeHSIE, inPkg));
				dumpForms(hsiePW, hsie.allForms());
				hsiePW.close();
			}

			// 10. Generate code from templates
			new TemplateTraversor(rewriter, Arrays.asList(dg.templateGenerator(), gen.templateGenerator())).generate(rewriter, target);
			
			// 11. Save learned state for export
			if (exportTo != null)
				tc2.writeLearnedKnowledge(exportTo, inPkg, dumpTypes);

			// 12. generation of JSForms
			generateForms(Arrays.asList(gen, dg), hsie.allForms());
			abortIfErrors(errors);

			// 13. Write final outputs
			
			// 13a. Issue JavaScript
			if (writeTo != null) {
				try {
					wjs = new FileWriter(writeTo);
				} catch (IOException ex) {
					System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
					return null;
				}
				target.writeTo(wjs);
			}

			// 13b. Issue JVM bytecodes
			if (writeJVM != null) {
				try {
					// Doing this makes things clean, but stops you putting multiple things in the same directory
//					FileUtils.cleanDirectory(writeJVM);
					for (ByteCodeCreator bcc : bce.all()) {
						File wto = new File(writeJVM, FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
						bcc.writeTo(wto);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message((InputPosition)null, ex.toString());
				}
			}

			// 13c. Do all that Droid stuff
			try {
				builder.generateAppObject(bce, inPkg);
				builder.write(bce);
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition)null, ex.getMessage());
			}
			abortIfErrors(errors);

			return new CompileResult(scope, bce, tc2).addJS(writeTo);
		} finally {
			try { if (wjs != null) wjs.close(); } catch (IOException ex) {}
			try { if (wex != null) wex.close(); } catch (IOException ex) {}
			if (tcPW != null)
				tcPW.close();
		}
	}

	protected void readIntoScope(String inPkg, ErrorResult errors, final FLASStory storyProc, final Scope scope, String fileName, Reader r) throws IOException {
		// 1. Use indentation to break the input file up into blocks
		List<Block> blocks = makeBlocks(errors, fileName, r);
		
		// 2. Use the parser factory and story to convert blocks to a package definition
		storyProc.process(inPkg, scope, errors, blocks, true);
	}

	@Override
	public CompileResult createJVM(String pkg, String priorPackage, IScope priorScope, String flas) throws IOException, ErrorResultException {
		this.internalBuildJVM();
		ErrorResult errors = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = Scope.topScope(pkg);
		readIntoScope(pkg, errors, storyProc, scope, "script.fl", new StringReader(flas));
		return stage2(errors, priorPackage, priorScope, pkg, scope);
	}

	@Override
	public CompileResult createJVM(String pkg, String priorPackage, IScope priorScope, Scope scope) throws IOException, ErrorResultException {
		this.internalBuildJVM();
		return stage2(new ErrorResult(), priorPackage, priorScope, pkg, scope);
	}

	@Override
	public CompileResult createJS(String pkg, String priorPackage, IScope priorScope, Scope scope) throws IOException, ErrorResultException {
		return stage2(new ErrorResult(), priorPackage, priorScope, pkg, scope);
	}

	private void writeDependencies(DependencyAnalyzer da, List<Set<RWFunctionDefinition>> defns) throws IOException {
		PrintWriter pw = new PrintWriter(new File(writeDepends, "depends.txt"));
		da.dump(pw);
		for (Set<RWFunctionDefinition> s : defns) {
			for (RWFunctionDefinition d : s) {
				pw.println(d.uniqueName());
			}
			pw.println("-----");
		}
		pw.close();
	}

	private void showDefns(Set<RWFunctionDefinition> defns) {
		for (RWFunctionDefinition d : defns)
			logger.info("  " + d.uniqueName());
	}

	private void dumpForms(PrintWriter hsiePW, Collection<HSIEForm> hs) {
		if (hsiePW == null)
			return;
		
		boolean first = true;
		for (HSIEForm h : hs) {
			if (first)
				first = false;
			else
				hsiePW.println("-------");
			h.dump(hsiePW);
		}
	}

	// Just obtain a parse tree 
	public StoryRet parse(String inPkg, String input) {
		ErrorResult er = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = Scope.topScope(inPkg);
		StoryRet ret = new StoryRet(er, scope);
		StringReader r = null;
		try {
			r = new StringReader(input);

			// 1. Use indentation to break the input file up into blocks
			List<Block> blocks = makeBlocks(er, "-", r);
			if (er.hasErrors())
				return ret;
			
			// 2. Use the parser factory and story to convert blocks to a package definition
			storyProc.process(inPkg, scope, er, blocks, true);
			return ret;
		} catch (IOException ex1) {
			ex1.printStackTrace();
			return null;
		} finally {
			r.close();
		}
	}

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	@SuppressWarnings("unchecked")
	private List<Block> makeBlocks(ErrorResult er, String file, Reader r) throws IOException {
		Object res = Blocker.block(file, r);
		if (res instanceof ErrorResult) {
			er.merge((ErrorResult) res);
			return null;
		}
		return (List<Block>) res;
	}

	private void handleCurrying(ApplyCurry curry, TypeChecker2 tc, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			curry.rewrite(tc, h);
	}

	private void generateForms(List<HSIEFormGenerator> gens, Collection<HSIEForm> collection) {
		for (HSIEFormGenerator g : gens)
			for (HSIEForm h : collection)
				g.generate(h);
	}

	public DroidBuilder getBuilder() {
		return builder;
	}

	public boolean hasErrors() {
		return errors.hasErrors();
	}
}