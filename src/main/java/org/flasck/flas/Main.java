package org.flasck.flas;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		boolean failed = noExit(args);
		System.exit(failed?1:0);
	}

	public static boolean noExit(String[] args) {
		Configuration config = new Configuration();
		try {
			List<File> inputs = config.process(args);
			if (inputs.isEmpty()) {
				System.err.println("No input directories specified");
				return true;
			}
			FLASCompiler compiler = new FLASCompiler(config);
			compiler.errorWriter(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));
			compiler.scanWebZips();
			Repository repository = new Repository();
			for (File input : inputs)
				processInput(config, compiler, repository, input);

			if (compiler.dumpRepo != null) {
				try {
					repository.dumpTo(compiler.dumpRepo);
				} catch (IOException ex) {
					System.out.println("Could not dump repository to " + compiler.dumpRepo);
				}
			}
			
			if (compiler.hasErrors())
				return true;
			else if (compiler.phaseTo == PhaseTo.PARSING)
				return false;
			
			if (compiler.phaseTo == PhaseTo.TEST_TRAVERSAL) {
				try {
					repository.traverse(new LeafAdapter());
					return false;
				} catch (Throwable t) {
					t.printStackTrace(System.out);
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
		} catch (ArgumentException ex) {
			System.err.println(ex.getMessage());
			return true;
		}
	}

	private static void processInput(Configuration config, FLASCompiler compiler, Repository repository, File input) {
		ErrorMark mark = compiler.errors().mark();
		try {
			if (config.tda)
				mark = compiler.parse(repository, input);
		} catch (Throwable ex) {
			compiler.reportException(ex);
		} finally {
			compiler.errors().showFromMark(mark, compiler.errorWriter(), 0);
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
