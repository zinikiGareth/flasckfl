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
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.zinutils.streamedlogger.api.Level;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		if (args != null && args.length >= 1 && "--lsp".equals(args[0])) {
			LSPMain.run(args);
			return; // there should be background threads keeping this alive ...
		}
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

		FLASCompiler ret = commonCompiler(errors, config);
		// This is to do with Android
//		if (compiler.getBuilder() != null)
//			compiler.getBuilder().build();
		// TODO: option to upload to a Ziniki Server
		
		return errors.hasErrors() || ret == null;
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
		FLASCompiler compiler = new FLASCompiler(config, errors, repository);
		if (compiler.loadFLIM())
			return null;
		if (config.inputs.isEmpty()) {
			errors.message((InputPosition)null, "there are no input packages");
			return null;
		}
		for (File input : config.inputs)
			compiler.processInput(input);
		for (File web : config.webs)
			compiler.splitWeb(web);

		if (compiler.stage2())
			return null;
		else
			return compiler;
	}

	public static void setLogLevels() {
		StaticLoggerBinder.setLevel("Compiler", Level.WARN);
		StaticLoggerBinder.setLevel("Traverser", Level.WARN);
		StaticLoggerBinder.setLevel("Resolver", Level.WARN);
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
		StaticLoggerBinder.setLevel("Send", Level.WARN);
		StaticLoggerBinder.setLevel("GLS", Level.WARN);
		StaticLoggerBinder.setLevel("TxManagerThreading", Level.WARN);
		StaticLoggerBinder.setLevel("awstxstore", Level.WARN);
		StaticLoggerBinder.setLevel("org.ziniki.awstxstore", Level.WARN);
		
		StaticLoggerBinder.setLevel("ZiWSH", Level.WARN);
		StaticLoggerBinder.setLevel("Ziniki", Level.WARN);
		StaticLoggerBinder.setLevel("ZinikiTI", Level.WARN);
		StaticLoggerBinder.setLevel("IMUnitOfWork", Level.WARN);
		StaticLoggerBinder.setLevel("BrokerDelegate", Level.WARN);
		StaticLoggerBinder.setLevel("BTResponder", Level.WARN);
		StaticLoggerBinder.setLevel("ZinDelivery", Level.WARN);
		StaticLoggerBinder.setLevel("DoGet", Level.WARN);
	}
}
