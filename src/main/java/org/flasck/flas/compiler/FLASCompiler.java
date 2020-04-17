package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.TypeDumper;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.jvm.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FLASCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	public static boolean backwardCompatibilityMode = true;
	private final ErrorReporter errors;
	private final Repository repository;
	private final PrintWriter errorWriter;
//	private final DroidBuilder builder = new DroidBuilder();

	public FLASCompiler(ErrorReporter errors, Repository repository, PrintWriter errorWriter) {
		this.errors = errors;
		this.repository = repository;
		this.errorWriter = errorWriter;
	}
	
	public ErrorMark processInput(ErrorMark mark, File input) {
		try {
			mark = parse(mark, input);
		} catch (Throwable ex) {
			reportException(ex);
		} finally {
			errors.showFromMark(mark, errorWriter, 0);
		}
		return mark;
	}

	// Now read and parse all the files, passing it on to the alleged phase2
	public ErrorMark parse(ErrorMark mark, File dir) {
		if (!dir.isDirectory())
			throw new RuntimeException("there is no input directory " + dir);

		String inPkg = dir.getName();
		checkPackageName(inPkg);
		System.out.println(" |" + inPkg);
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer)repository);
		List<File> files = FileUtils.findFilesMatching(dir, "*.fl");
		files.sort(new FileNameComparator());
		for (File f : files) {
			System.out.println("    " + f.getName());
			flp.process(f);
			errors.showFromMark(mark, errorWriter, 4);
			mark = errors.mark();
		}
		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			System.out.println("    " + f.getName());
			String file = FileUtils.dropExtension(f.getName());
			UnitTestFileName utfn = new UnitTestFileName(new PackageName(inPkg), "_ut_" + file);
			UnitTestPackage utp = new UnitTestPackage(utfn);
			repository.unitTestPackage(errors, utp);
			ParsingPhase parser = new ParsingPhase(errors, utfn, new ConsumeDefinitions(errors, repository, utp));
			parser.process(f);
			errors.showFromMark(mark, errorWriter, 4);
			mark = errors.mark();
		}
		if (errors.hasErrors())
			return mark;
		return mark;
	}
	
	public boolean resolve(ErrorMark mark) {
		Resolver resolver = new RepositoryResolver(errors, repository);
		repository.traverseWithImplementedMethods(resolver);
		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 0);
			return true;
		} else
			return false;
	}

	public FunctionGroups lift() {
		return new RepositoryLifter().lift(repository);
	}
	
	public void analyzePatterns() {
		StackVisitor sv = new StackVisitor();
		new PatternAnalyzer(errors, repository, sv);
		repository.traverseLifted(sv);
	}

	public void doTypeChecking(ErrorMark mark, FunctionGroups ordering) {
		StackVisitor sv = new StackVisitor();
		new TypeChecker(errors, repository, sv);
		repository.traverseInGroups(sv, ordering);
		if (errors.hasErrors())
			errors.showFromMark(mark, errorWriter, 0);
	}

	public void dumpTypes(File ty) throws FileNotFoundException {
		// dump types if specified
		if (ty != null) {
			FileOutputStream fos = new FileOutputStream(ty);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
			Visitor dumper = new TypeDumper(pw);
			repository.traverse(dumper);
			pw.close();
		}
	}

	public boolean convertMethods(ErrorMark mark) {
		StackVisitor sv = new StackVisitor();
		new ConvertRepositoryMethods(sv, errors);
		repository.traverseWithMemberFields(sv);
		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 0);
			return true;
		} else
			return false;
	}
	
	public boolean generateCode(ErrorMark mark, Configuration config) {
		JSEnvironment jse = new JSEnvironment(config.jsDir());
		// TODO: do we need multiple BCEs (or partitions, or something) for the different packages?
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		populateBCE(bce);
		
		StackVisitor jsstack = new StackVisitor();
		new JSGenerator(jse, jsstack);
		StackVisitor jvmstack = new StackVisitor();
		new JVMGenerator(bce, jvmstack);

		if (config.generateJS)
			repository.traverseWithHSI(jsstack);
		if (config.generateJVM)
			repository.traverseWithHSI(jvmstack);
		
		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 0);
			return true;
		}
		
		if (config.generateJS)
			saveJSE(config.jsDir(), jse);
		if (config.generateJVM)
			saveBCE(config.jvmDir(), bce);

		Map<File, PrintWriter> writers = new HashMap<>();
		if (config.generateJVM && config.unitjvm) {
			BCEClassLoader bcl = new BCEClassLoader(bce);
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl);
			jvmRunner.runAll(writers);
		}

		if (config.generateJS && config.unitjs) {
			JSRunner jsRunner = new JSRunner(config, repository, jse);
			jsRunner.runAll(writers);
		}
		writers.values().forEach(w -> w.close());

		if (errors.hasErrors()) {
			errors.showFromMark(mark, errorWriter, 0);
			return true;
		}
		return false;
	}

	public void populateBCE(ByteCodeEnvironment bce) {
		ByteCodeCreator ec = bce.newClass("org.flasck.flas.testrunner.JVMRunner");
		ec.dontGenerate();
		ec.defineField(true, Access.PUBLIC, J.FLEVALCONTEXT, "cxt");
	}

	public void saveBCE(File jvmDir, ByteCodeEnvironment bce) {
//		bce.dumpAll(true);
		if (jvmDir != null) {
			FileUtils.assertDirectory(jvmDir);
			try {
				// Doing this makes things clean, but stops you putting multiple things in the
				// same directory
				// FileUtils.cleanDirectory(writeJVM);
				for (ByteCodeCreator bcc : bce.all()) {
					File wto = new File(jvmDir,
							FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
					bcc.writeTo(wto);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition) null, ex.toString());
			}
		}
	}

	public void saveJSE(File jsDir, JSEnvironment jse) {
		if (jsDir != null) {
			try {
				jse.writeAllTo(jsDir);
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition) null, ex.toString());
			}
//			jse.dumpAll(true);
		}
	}

	private void checkPackageName(String inPkg) {
		String[] bits = inPkg.split("\\.");
		for (String s : bits) {
			if (!Character.isLowerCase(s.charAt(0)))
				throw new RuntimeException("Package must have valid package name");
		}
	}

//	public DroidBuilder getBuilder() {
//		return builder;
//	}

	public void reportException(Throwable ex) {
		errors.reportException(ex);
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
}