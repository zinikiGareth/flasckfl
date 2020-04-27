package org.flasck.flas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.zinutils.streamedlogger.api.Level;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		boolean failed;
		try {
			failed = standardCompiler(args);
		} catch (Throwable e) {
			Logger logger = LoggerFactory.getLogger("Compiler");
			logger.error("exception thrown", e);
			failed = true;
		}
		System.exit(failed?1:0);
	}

	public static boolean standardCompiler(String... args) throws IOException {
		ErrorResult errors = new ErrorResult();
		Configuration config = new Configuration(errors, args);

		commonCompiler(errors, config);
		// This is to do with Android
//		if (compiler.getBuilder() != null)
//			compiler.getBuilder().build();
		// TODO: option to upload to a Ziniki Server
		
		return errors.hasErrors();
	}
	
	// If we are the embedded Ziniki compiler, store the resulting package in S3
	public static boolean uploader(ErrorResult errors, Configuration config) throws IOException {
		FLASCompiler compiler = commonCompiler(errors, config);
		if (errors.hasErrors())
			return true;

		// TODO: check that all the hashes and signatures match
		if (config.storer != null) {
			compiler.storeAssemblies(config.storer);
		}
		
		return errors.hasErrors();
	}
	

	private static FLASCompiler commonCompiler(ErrorResult errors, Configuration config) throws IOException, FileNotFoundException {
		Writer osw;
		File f = config.writeErrorsTo();
		if (f != null)
			osw = new FileWriter(f);
		else
			osw = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
		PrintWriter ew = new PrintWriter(osw, true);
		FLASCompiler compiler = doCompilation(errors, config, ew);
		if (errors.hasErrors()) {
			errors.showTo(ew, 0);
			return null;
		}
		return compiler;
	}

	private static FLASCompiler doCompilation(ErrorResult errors, Configuration config, PrintWriter ew) throws IOException, FileNotFoundException {
		if (errors.hasErrors()) {
			return null;
		}

		Repository repository = new Repository();
		FLASCompiler compiler = new FLASCompiler(errors, repository);
		LoadBuiltins.applyTo(errors, repository);
		if (config.inputs.isEmpty()) {
			errors.message((InputPosition)null, "there are no input packages");
			return null;
		}
		for (File input : config.inputs)
			compiler.processInput(input);
		for (File web : config.webs)
			compiler.splitWeb(web);

		if (config.dumprepo != null) {
			try {
				repository.dumpTo(config.dumprepo);
			} catch (IOException ex) {
				System.out.println("Could not dump repository to " + config.dumprepo);
			}
		}
		
		if (errors.hasErrors() || config.upto == PhaseTo.PARSING)
			return null;
		
		if (config.upto == PhaseTo.TEST_TRAVERSAL) {
			testTraversal(repository);
			return null;
		}

		if (compiler.resolve())
			return null;
		
		FunctionGroups ordering = compiler.lift();
		compiler.analyzePatterns();
		if (errors.hasErrors())
			return null;
		
		if (config.doTypeCheck) {
			compiler.doTypeChecking(ordering);
			compiler.dumpTypes(config.writeTypesTo);
			if (errors.hasErrors())
				return null;
		}

		if (compiler.convertMethods())
			return null;
		
		if (compiler.generateCode(config))
			return null;
		
		if (compiler.generateAssemblies(config.storer))
			return null;

		return compiler;
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
		StaticLoggerBinder.setLevel("io.webfolder.ui4j", Level.WARN);
		StaticLoggerBinder.setLevel("Compiler", Level.WARN);
		StaticLoggerBinder.setLevel("DroidGen", Level.WARN);
		StaticLoggerBinder.setLevel("Generator", Level.WARN);
		StaticLoggerBinder.setLevel("HSIE", Level.WARN);
		StaticLoggerBinder.setLevel("Rewriter", Level.ERROR);
		StaticLoggerBinder.setLevel("TypeChecker", Level.WARN);
	}
}
