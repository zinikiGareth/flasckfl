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
			compiler.processInputFromDirectory(input);
		for (File web : config.webs)
			compiler.splitWeb(web);

		if (compiler.stage2())
			return null;
		else
			return compiler;
	}

	public static void setLogLevels() {
		Level level = Level.WARN;
		String arg = System.getProperty("org.flas.compiler.tracing");
		if (arg != null) {
			level = Level.valueOf(arg.toUpperCase());
		}
		StaticLoggerBinder.setLevel("Compiler", level);
		StaticLoggerBinder.setLevel("Traverser", level);
		StaticLoggerBinder.setLevel("Resolver", level);
		StaticLoggerBinder.setLevel("Lifter", level);
		StaticLoggerBinder.setLevel("Patterns", level);
		StaticLoggerBinder.setLevel("TOPatterns", level);
		StaticLoggerBinder.setLevel("TypeChecker", level);
		StaticLoggerBinder.setLevel("TCUnification", level);
		StaticLoggerBinder.setLevel("HSI", level);
		StaticLoggerBinder.setLevel("Generator", level);
		StaticLoggerBinder.setLevel("TestRunner", level);
		StaticLoggerBinder.setLevel("io.webfolder.ui4j", level);
		StaticLoggerBinder.setLevel("IdemHandler", level);
		StaticLoggerBinder.setLevel("TestStages", level);
		StaticLoggerBinder.setLevel("Dispatcher", level);
		StaticLoggerBinder.setLevel("Send", level);
		StaticLoggerBinder.setLevel("GLS", level);
		StaticLoggerBinder.setLevel("TxManagerThreading", level);
		StaticLoggerBinder.setLevel("assembler", level);
		StaticLoggerBinder.setLevel("awstxstore", level);
		StaticLoggerBinder.setLevel("org.ziniki.awstxstore", level);
		
		StaticLoggerBinder.setLevel("ZiWSH", level);
		StaticLoggerBinder.setLevel("ZiwshClient", level);
		StaticLoggerBinder.setLevel("Ziniki", level);
		StaticLoggerBinder.setLevel("ZinikiTI", level);
		StaticLoggerBinder.setLevel("IMUnitOfWork", level);
		StaticLoggerBinder.setLevel("Broker", level);
		StaticLoggerBinder.setLevel("Utils", level);
		StaticLoggerBinder.setLevel("BrokerDelegate", level);
		StaticLoggerBinder.setLevel("BTResponder", level);
		StaticLoggerBinder.setLevel("ZinDelivery", level);
		StaticLoggerBinder.setLevel("DoGet", level);
		StaticLoggerBinder.setLevel("Uploader", level);
	}
}
