package org.flasck.flas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.compiler.jsgen.JSEnvironment;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.utils.FileUtils;

public class Main {
	public static void main(String[] args) throws IOException {
		setLogLevels();
		boolean failed = noExit(args);
		System.exit(failed?1:0);
	}

	// tested only by golden files
	public static boolean noExit(String... args) throws IOException {
		ErrorResult errors = new ErrorResult();
		ErrorMark mark = errors.mark();
		Configuration config = new Configuration(errors, args);
		Writer osw;
		File f = config.writeErrorsTo();
		if (f != null)
			osw = new FileWriter(f);
		else
			osw = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
		PrintWriter ew = new PrintWriter(osw, true);
		if (errors.hasErrors()) {
			errors.showFromMark(mark, ew, 0);
			return true;
		}
		// TODO: 2019-07-22 I'm going to suggest that we want separate things for parsing and generation and the like
		// and that we can move between them ...
		// So Compiler should just become "Parser" I think, and possibly move processInput there and move more context there.
		FLASCompiler compiler = new FLASCompiler(config);
		compiler.errorWriter(ew);
//		compiler.scanWebZips();
		Repository repository = new Repository();
		LoadBuiltins.applyTo(repository);
		for (File input : config.inputs)
			mark = compiler.processInput(config, repository, input, mark);

		if (config.dumprepo != null) {
			try {
				repository.dumpTo(config.dumprepo);
			} catch (IOException ex) {
				System.out.println("Could not dump repository to " + config.dumprepo);
			}
		}
		
		if (compiler.hasErrors()) {
			errors.showFromMark(mark, ew, 0);
			return true;
		} else if (config.upto == PhaseTo.PARSING)
			return false;
		
		if (config.upto == PhaseTo.TEST_TRAVERSAL) {
			try {
				repository.traverse(new LeafAdapter());
				return false;
			} catch (Throwable t) {
				t.printStackTrace(System.out);
				return true;
			}
		}

		
		// resolution
		{
			Resolver resolver = new RepositoryResolver(errors, repository);
			repository.traverse(resolver);
			if (compiler.hasErrors()) {
				errors.showFromMark(mark, ew, 0);
				return true;
			}
		}
		
		// TODO: do we need multiple BCEs (or partitions, or something) for the different packages?
		{
			ByteCodeEnvironment bce = new ByteCodeEnvironment();
			JSEnvironment jse = new JSEnvironment(config.jsDir());
			
			JVMGenerator jvmGenerator = new JVMGenerator(bce);
			JSGenerator jsGenerator = new JSGenerator(jse);

			repository.traverse(jvmGenerator);
			repository.traverse(jsGenerator);
			
			if (compiler.hasErrors()) {
				errors.showFromMark(mark, ew, 0);
				return true;
			}
			
			saveBCE(errors, config.jvmDir(), bce);
			saveJSE(errors, config.jsDir(), jse);

			Map<File, PrintWriter> writers = new HashMap<>();
			if (config.unitjvm) {
				BCEClassLoader bcl = new BCEClassLoader(bce);
				JVMRunner jvmRunner = new JVMRunner(config, repository, bcl);
				jvmRunner.runAll(writers);
			}

			if (config.unitjs) {
				JSRunner jsRunner = new JSRunner(config, repository, jse);
				jsRunner.runAll(writers);
			}
			writers.values().forEach(w -> w.close());

			if (compiler.hasErrors()) {
				errors.showFromMark(mark, ew, 0);
				return true;
			}
		}

		
//			p2 = new Phase2CompilationProcess();
//			p2.process();
//			if (errors.hasErrors()) {
//				errors.showFromMark(mark, errorWriter, 4);
//				return;
//			}
//			UnitTestPhase ut = new UnitTestPhase(repository);
//			p2.bceTo(ut);
//			if (errors.hasErrors()) {
//				errors.showFromMark(mark, errorWriter, 4);
//				return;
//			}

		// This is to do with Android
		if (compiler.getBuilder() != null)
			compiler.getBuilder().build();
		return compiler.hasErrors();
	}

	private static void saveBCE(ErrorReporter errors, File jvmDir, ByteCodeEnvironment bce) {
		bce.dumpAll(true);
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

	private static void saveJSE(ErrorReporter errors, File jsDir, JSEnvironment jse) {
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

	public static void setLogLevels() {
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("DroidGen").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("Rewriter").setLevel(Level.ERROR);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
	}
}
