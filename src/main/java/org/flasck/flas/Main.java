package org.flasck.flas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;

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

		Repository repository = new Repository();
		FLASCompiler compiler = new FLASCompiler(errors, repository, ew);
//		compiler.scanWebZips();
		LoadBuiltins.applyTo(repository);
		for (File input : config.inputs)
			mark = compiler.processInput(mark, input);

		if (config.dumprepo != null) {
			try {
				repository.dumpTo(config.dumprepo);
			} catch (IOException ex) {
				System.out.println("Could not dump repository to " + config.dumprepo);
			}
		}
		
		if (errors.hasErrors()) {
			errors.showFromMark(mark, ew, 0);
			return true;
		} else if (config.upto == PhaseTo.PARSING)
			return false;
		
		if (config.upto == PhaseTo.TEST_TRAVERSAL) {
			return testTraversal(repository);
		}

		if (compiler.resolve(mark))
			return true;
		
		FunctionGroups ordering = compiler.lift();
		compiler.analyzePatterns();
		
		if (config.doTypeCheck) {
			compiler.doTypeChecking(mark, ordering);
			compiler.dumpTypes(config.writeTypesTo);
			if (errors.hasErrors())
				return true;
		}

		if (compiler.convertMethods(mark))
			return true;
		
		if (compiler.generateCode(mark, config))
			return true;

		// This is to do with Android
//		if (compiler.getBuilder() != null)
//			compiler.getBuilder().build();
		
		return errors.hasErrors();
	}

	private static boolean testTraversal(Repository repository) {
		try {
			repository.traverse(new LeafAdapter());
			return false;
		} catch (Throwable t) {
			t.printStackTrace(System.out);
			return true;
		}
	}

	public static void setLogLevels() {
		LogManager.getLogger("io.webfolder.ui4j").setLevel(Level.WARN);
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("DroidGen").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("Rewriter").setLevel(Level.ERROR);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
	}
}
