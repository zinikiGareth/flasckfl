package org.flasck.flas;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.flim.FlimReader;
import org.flasck.flas.repository.flim.FlimWriter;
import org.flasck.jvm.assembly.FLASAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.zinutils.streamedlogger.api.Level;
import org.zinutils.utils.FileUtils;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		boolean failed;
		try {
			failed = standardCompiler(args);
		} catch (Throwable e) {
			e.printStackTrace();
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
		if (config.flimdir() != null) {
			new FlimReader(repository).read(config.flimdir());
		}
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
		
		if (config.upto == PhaseTo.PARSING)
			return null;
		
		if (compiler.resolve())
			return null;
		
		FunctionGroups ordering = compiler.lift();
		compiler.analyzePatterns();
		if (errors.hasErrors())
			return null;
		
		if (config.doTypeCheck) {
			compiler.doTypeChecking(ordering);
			if (errors.hasErrors())
				return null;
			compiler.dumpTypes(config.writeTypesTo);
			if (errors.hasErrors())
				return null;
		}

		if (compiler.convertMethods())
			return null;
		
		if (compiler.buildEventMaps())
			return null;
		
		if (compiler.generateCode(config))
			return null;
		
		if (compiler.runUnitTests(config))
			return null;

		if (config.html != null) {
			try (FileWriter fos = new FileWriter(config.html)) {
				File fldir = new File(config.root, "flascklib/js");
				FileUtils.cleanDirectory(fldir);
				FileUtils.assertDirectory(fldir);
				List<File> library = FileUtils.findFilesMatching(new File(config.flascklib), "*");
				for (File f : library) {
					FileUtils.copy(f, fldir);
				}
				FLASAssembler asm = new FLASAssembler(fos, "flascklib");
				compiler.generateHTML(asm, config);
			}
			if (config.openHTML)
				Desktop.getDesktop().open(config.html);
		}

		if (config.flimdir() != null) {
			FlimWriter writer = new FlimWriter(repository, config.flimdir());
			for (File input : config.inputs)
				if (!writer.export(input.getName()))
					return null;
		}

		return compiler;
	}

	public static void setLogLevels() {
		StaticLoggerBinder.setLevel("Compiler", Level.WARN);
		StaticLoggerBinder.setLevel("Traverser", Level.WARN);
		StaticLoggerBinder.setLevel("Lifter", Level.WARN);
		StaticLoggerBinder.setLevel("Patterns", Level.WARN);
		StaticLoggerBinder.setLevel("TOPatterns", Level.WARN);
		StaticLoggerBinder.setLevel("TypeChecker", Level.WARN);
		StaticLoggerBinder.setLevel("TCUnification", Level.WARN);
		StaticLoggerBinder.setLevel("HSI", Level.WARN);
		StaticLoggerBinder.setLevel("Generator", Level.WARN);
		StaticLoggerBinder.setLevel("TestRunner", Level.WARN);
		StaticLoggerBinder.setLevel("io.webfolder.ui4j", Level.WARN);
		StaticLoggerBinder.setLevel("IdemHandler", Level.WARN);
		StaticLoggerBinder.setLevel("Dispatcher", Level.WARN);
		StaticLoggerBinder.setLevel("GLS", Level.WARN);
		StaticLoggerBinder.setLevel("TxManagerThreading", Level.WARN);
		StaticLoggerBinder.setLevel("awstxstore", Level.WARN);
		StaticLoggerBinder.setLevel("org.ziniki.awstxstore", Level.WARN);
	}
}
