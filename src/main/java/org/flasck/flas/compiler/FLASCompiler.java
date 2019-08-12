package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.sugardetox.SugarDetox;
import org.flasck.flas.template.TemplateTraversor;
import org.flasck.flas.testrunner.UnitTests;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FLASCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	public static boolean backwardCompatibilityMode = true;
	private final List<File> pkgdirs = new ArrayList<File>();
	private File writeRW;
	private DroidBuilder builder = new DroidBuilder();
//	private File writeTestReports;
	private PrintWriter errorWriter;
	private Configuration config;

	public FLASCompiler(Configuration config) {
		this.config = config;
	}
	
	public ErrorMark processInput(Configuration config, Repository repository, File input, ErrorMark mark) {
		try {
			if (config.tda)
				mark = parse(repository, input, mark);
		} catch (Throwable ex) {
			reportException(ex);
		} finally {
			errors().showFromMark(mark, errorWriter(), 0);
		}
		return mark;
	}

	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put
	// it
//	@Override
//	public void writeDroidTo(File file, boolean andBuild) {
//		if (file == null || file.getPath().equals("null"))
//			return;
//		builder = new DroidBuilder(file);
//		if (!andBuild)
//			builder.dontBuild();
//	}
//
//	public void internalBuildJVM() {
//		builder = new DroidBuilder();
//		builder.dontBuild();
//	}
//
//	@Override
//	public void writeRWTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeRW = file;
//	}
//
//	@Override
//	public void writeFlimTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeFlim = file;
//	}
//
//	@Override
//	public void writeDependsTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeDepends = file;
//	}

//	@Override
//	public void writeHSIETo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeHSIE = file;
//	}

//	@Override
//	public void trackTC(File file) {
//		if (file != null)
//			this.trackTC = new File(file, "types");
//		else
//			this.trackTC = null;
//	}
//
//	@Override
//	public void writeJSTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeJS = file;
//	}
//
//	@Override
//	public void writeTestReportsTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
////		this.writeTestReports = file;
//	}

	public void errorWriter(PrintWriter printWriter) {
		this.errorWriter = printWriter;
	}

	// Complete initialization by preparing the compiler for use
//	public void scanWebZips() {
//		if (webzips.isEmpty())
//			return;
//		if (webzipdir == null) {
//			errors.message((InputPosition) null, "using webzips requires a webzipdir");
//			return;
//		}
//		for (String s : webzips)
//			scanWebZip(s);
//	}

	// Now read and parse all the files, passing it on to the alleged phase2
	public ErrorMark parse(Repository repository, File dir, ErrorMark mark) {
		if (!dir.isDirectory())
			throw new RuntimeException("there is no input directory " + dir);

		String inPkg = dir.getName();
		checkPackageName(inPkg);
		System.out.println("Package " + inPkg);
		ParsingPhase flp = new ParsingPhase(config.errors, inPkg, (TopLevelDefinitionConsumer)repository);
		List<File> files = FileUtils.findFilesMatching(dir, "*.fl");
		files.sort(new FileNameComparator());
		for (File f : files) {
			System.out.println(" > " + f.getName());
			flp.process(f);
			config.errors.showFromMark(mark, errorWriter, 4);
			mark = config.errors.mark();
		}
		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			System.out.println(" > " + f.getName());
			ParsingPhase utp = new ParsingPhase(config.errors, inPkg, FileUtils.dropExtension(f.getName()), (UnitTestDefinitionConsumer)repository);
			utp.process(f);
			config.errors.showFromMark(mark, errorWriter, 4);
			mark = config.errors.mark();
		}
		if (config.errors.hasErrors())
			return mark;
		return mark;
	}

	private void checkPackageName(String inPkg) {
		String[] bits = inPkg.split("\\.");
		for (String s : bits) {
			if (!Character.isLowerCase(s.charAt(0)))
				throw new RuntimeException("Package must have valid package name");
		}
	}

	CompileResult stage2(ErrorReporter er, String priorPackage, IScope priorScope, String inPkg, Scope scope)
			throws ErrorResultException, IOException {
		ErrorResult errors = (ErrorResult) er;
		File writeTo = config.writeJS != null ? new File(config.writeJS, inPkg + ".js") : null;
		File exportTo = config.writeFlim != null ? new File(config.writeFlim, inPkg + ".flim") : null;

		// 3. Rework any "syntatic sugar" forms into their proper forms
		new SugarDetox(errors).detox(scope);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);

		FileWriter wjs = null;
		FileOutputStream wex = null;
		PrintWriter tcPW = null;
		try {
			ImportPackage rootPkg = Builtin.builtins();
			final Rewriter rewriter = new Rewriter(errors, pkgdirs, rootPkg);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors, rewriter);
			final ByteCodeEnvironment bce = new ByteCodeEnvironment();

			rewriter.importPackage1(rootPkg);

//			for (CompileResult cr : priors) {
//				PackageImporter.importInto(rewriter.pkgFinder, errors, rewriter, cr.getPackage().uniqueName(),
//						cr.exports());
//			}

			rewriter.rewritePackageScope(priorPackage, priorScope, inPkg, scope);
			abortIfErrors(errors);

			if (writeRW != null) {
				rewriter.writeGeneratableTo(new File(writeRW, "analysis.txt"));
			}

			rewriter.checkCardContractUsage();

			// 5. Register JS and Droid code generators with the visitors
			JSTarget target = new JSTarget(inPkg);
			for (ScopeEntry e : scope) {
				if (e.getValue() instanceof UnitTests) {
					target.ensurePackagesFor(e.getKey());
				}
			}
			Generator gen = new Generator(target);
			rewriter.registerCodeGenerator(gen);
			final DroidGenerator dg = new DroidGenerator(bce, builder);
			dg.registerWith(rewriter);

			rewriter.visitGenerators();

			// System.out.println("defns = " + rewriter.functions.keySet());

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
//			if (config.writeDepends != null)
//				writeDependencies(da, defns);

			for (Set<RWFunctionDefinition> orch : defns) {
				showDefns(orch);
			}

			// 8. Now process each set
			// a. convert functions to HSIE
			// b. typechecking

			TypeChecker2 tc2 = new TypeChecker2(errors, rewriter);
//			if (trackTC != null) {
//				tcPW = new PrintWriter(trackTC);
//				tc2.trackTo(tcPW);
//			}
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

				// TODO: this is over-eager. We should check if we depend on any of the failures
				// are required by subsequent steps.
				abortIfErrors(errors);
			}
			// 9. Check whether functions are curried and add in the appropriate indications
			// if so
			handleCurrying(curry, tc2, hsie.allForms());
			abortIfErrors(errors);

			if (config.writeHSIE != null) {
				PrintWriter hsiePW = new PrintWriter(new File(config.writeHSIE, inPkg));
				dumpForms(hsiePW, hsie.allForms());
				hsiePW.close();
			}

			// 10. Generate code from templates
			new TemplateTraversor(rewriter, Arrays.asList(dg.templateGenerator(), gen.templateGenerator()))
					.generate(rewriter, target);

			// 11. Save learned state for export
			if (exportTo != null)
				tc2.writeLearnedKnowledge(exportTo, inPkg, config.dumpTypes);

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
			if (config.jvmDir() != null) {
				try {
					// Doing this makes things clean, but stops you putting multiple things in the
					// same directory
					// FileUtils.cleanDirectory(writeJVM);
					for (ByteCodeCreator bcc : bce.all()) {
						File wto = new File(config.jvmDir(),
								FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
						bcc.writeTo(wto);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					errors.message((InputPosition) null, ex.toString());
				}
			}

			// 13c. Do all that Droid stuff
			try {
				builder.generateAppObject(bce, inPkg);
				builder.write(bce);
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition) null, ex.getMessage());
			}
			abortIfErrors(errors);

			return new CompileResult(scope, bce, tc2).addJS(writeTo);
		} finally {
			try {
				if (wjs != null)
					wjs.close();
			} catch (IOException ex) {
			}
			try {
				if (wex != null)
					wex.close();
			} catch (IOException ex) {
			}
			if (tcPW != null)
				tcPW.close();
		}
	}

//	private void writeDependencies(DependencyAnalyzer da, List<Set<RWFunctionDefinition>> defns) throws IOException {
//		PrintWriter pw = new PrintWriter(new File(writeDepends, "depends.txt"));
//		da.dump(pw);
//		for (Set<RWFunctionDefinition> s : defns) {
//			for (RWFunctionDefinition d : s) {
//				pw.println(d.uniqueName());
//			}
//			pw.println("-----");
//		}
//		pw.close();
//	}
//
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

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
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
		return config.errors.hasErrors();
	}

	public void reportException(Throwable ex) {
		config.errors.reportException(ex);
	}

	public ErrorReporter errors() {
		return config.errors;
	}

	public Writer errorWriter() {
		return errorWriter;
	}
}