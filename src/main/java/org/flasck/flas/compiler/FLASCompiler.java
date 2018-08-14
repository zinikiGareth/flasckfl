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
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.flim.PackageImporter;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.htmlzip.BuilderSink;
import org.flasck.flas.htmlzip.MultiSink;
import org.flasck.flas.htmlzip.Sink;
import org.flasck.flas.htmlzip.SplitZip;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.newtypechecker.TypeChecker2;
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
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class FLASCompiler implements ScriptCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
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
	private File webzipdir;
	private File webdownloaddir;
	private BuilderSink sink = new BuilderSink();

	public void searchIn(File file) {
		pkgdirs.add(file);
	}
	
	public void unitTestPath(File file) {
		utpaths.add(file);
	}
	
	public void writeJVMTo(File file) {
		writeJVM = file;
	}
	
	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put it
	public void writeDroidTo(File file, boolean andBuild) {
		if (file.getPath().equals("null"))
			return;
		builder = new DroidBuilder(file);
		if (!andBuild)
			builder.dontBuild();
	}
	
	public void internalBuildJVM() {
		builder = new DroidBuilder();
		builder.dontBuild();
	}

	public void writeRWTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeRW = file;
	}

	public void writeFlimTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeFlim = file;
	}

	public void writeDependsTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeDepends = file;
	}

	public void writeHSIETo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeHSIE = file;
	}

	public void trackTC(File file) {
		this.trackTC = new File(file, "types");
	}

	public void writeJSTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeJS = file;
	}

	public void writeTestReportsTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeTestReports = file;
	}

	public void includePrior(CompileResult cr) {
		priors.add(cr);
	}

	public void dumpTypes() {
		this.dumpTypes = true;
	}
	
	// The objective of this method is to convert an entire package directory at one go
	// Thus the entire context of this is a single package
	public CompileResult compile(File dir) throws ErrorResultException, IOException, ClassNotFoundException {
		String inPkg = dir.getName();
		if (!dir.isDirectory()) {
			ErrorResult errors = new ErrorResult();
			errors.message((InputPosition)null, "there is no input directory " + dir);
			throw new ErrorResultException(errors);
		}

		boolean failed = false;
		ErrorResult errors = new ErrorResult();
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

		if (errors.hasErrors())
			throw new ErrorResultException(errors);

		if (failed)
			return null;

		CompileResult cr = stage2(errors, null, inPkg, scope);
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
				FLASCompiler sc = new FLASCompiler();
				sc.includePrior(cr);
				// TODO: we probably need to configure the compiler here ...
				UnitTestRunner utr = new UnitTestRunner(errors, sc, cr);
				utr.sendResultsTo(new FileUnitTestResultHandler(results));
				
				// We presumably needs some set of options to say which runners
				// we want to execute - could be more than one
				if (unitjvm) {
					JVMRunner jvmRunner = new JVMRunner(cr);
					for (File p : utpaths)
						jvmRunner.considerResource(p);
					utr.run(f, jvmRunner);
				}
				if (unitjs) {
					JSRunner jsRunner = new JSRunner(cr);
					utr.run(f, jsRunner);
				}
			} finally {
			if (close)
				results.close();
			}
		}
		
		// TODO: we also want to support general *.pt (protocol test) files and run them against cards/services that claim to support that protocol
		
		return cr;
	}

	private CompileResult stage2(ErrorResult errors, CompileResult prior, String inPkg, Scope scope) throws ErrorResultException, IOException {
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
			
			rewriter.rewritePackageScope(prior, inPkg, scope);
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
	public CompileResult createJVM(String pkg, CompileResult prior, String flas) throws IOException, ErrorResultException {
		this.internalBuildJVM();
		ErrorResult errors = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = Scope.topScope(pkg);
		readIntoScope(pkg, errors, storyProc, scope, "script.fl", new StringReader(flas));
		return stage2(errors, prior, pkg, scope);
	}

	@Override
	public CompileResult createJVM(String pkg, CompileResult prior, Scope scope) throws IOException, ErrorResultException {
		this.internalBuildJVM();
		return stage2(new ErrorResult(), prior, pkg, scope);
	}

	@Override
	public CompileResult createJS(String pkg, CompileResult prior, Scope scope) throws IOException, ErrorResultException {
		return stage2(new ErrorResult(), prior, pkg, scope);
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

	public void unitjs(boolean b) {
		this.unitjs = b;
	}

	public void unitjvm(boolean b) {
		this.unitjvm = b;
	}

	public void webZipDownloads(File file) {
		this.webdownloaddir = file;
	}

	public void webZipDir(File file) {
		this.webzipdir = file;
	}

	public boolean useWebZip(String called) {
		if (webzipdir == null) {
			System.err.println("Must specify webzipdir before adding zips");
			return true;
		}
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
			return true;
		}
		SplitZip sz = new SplitZip();
		try {
			sz.split(new MultiSink(sink, new ShowCardSink()), f);
		} catch (IOException ex) {
			System.err.println("Failed to read " + f);
			System.err.println(ex);
			return false;
		}
//		sink.dump();
		return false;
	}
}

class ShowCardSink implements Sink {
	private String currentFile;

	@Override
	public void zipLocation(File fromZip) {
	}

	@Override
	public void beginFile(String file) {
		this.currentFile = file;
	}

	@Override
	public void card(String tag, int from, int to) {
		System.out.println("Recovered webzip card " + tag + " from " + currentFile);
	}

	@Override
	public void holeid(String holeName, int from, int to) {
	}

	@Override
	public void hole(int from, int to) {
	}

	@Override
	public void identityAttr(String called, int from, int to) {
	}

	@Override
	public void dodgyAttr(int from, int to) {
	}

	@Override
	public void fileEnd() {
	}
}

