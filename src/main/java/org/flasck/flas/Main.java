package org.flasck.flas;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.testrunner.JVMRunner;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class Main {
	public static void main(String[] args) throws IOException {
		setLogLevels();
		boolean failed = noExit(args);
		System.exit(failed?1:0);
	}

	public static boolean noExit(String... args) throws IOException {
		ErrorResult errors = new ErrorResult();
		PrintWriter ew = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
		Configuration config = new Configuration(errors, args);
		if (errors.hasErrors()) {
			errors.showTo(ew, 0);
			return true;
		}
		// TODO: 2019-07-22 I'm going to suggest that we want separate things for parsing and generation and the like
		// and that we can move between them ...
		// So Compiler should just become "Parser" I think, and possibly move processInput there and move more context there.
		FLASCompiler compiler = new FLASCompiler(config);
		compiler.errorWriter(ew);
//		compiler.scanWebZips();
		Repository repository = new Repository();
		for (File input : config.inputs)
			compiler.processInput(config, repository, input);

		if (config.dumprepo != null) {
			try {
				repository.dumpTo(config.dumprepo);
			} catch (IOException ex) {
				System.out.println("Could not dump repository to " + config.dumprepo);
			}
		}
		
		if (compiler.hasErrors())
			return true;
		else if (config.upto == PhaseTo.PARSING)
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

		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		JVMGenerator jvmGenerator = new JVMGenerator(config.jvmDir());
		repository.traverse(new Traverser(jvmGenerator));

		if (config.unitjvm) {
			BCEClassLoader bcl = new BCEClassLoader(bce);
			ErrorMark mark = errors.mark();
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl);
			jvmRunner.runAll();
			errors.showFromMark(mark, ew, 0);
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

	public static void setLogLevels() {
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("DroidGen").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("Rewriter").setLevel(Level.ERROR);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
	}
}
