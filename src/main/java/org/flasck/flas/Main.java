package org.flasck.flas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.compiler.jsgen.packaging.JSUploader;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.repository.AssemblyVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.flasck.jvm.ziniki.PackageSources;
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
	public static boolean uploader(ErrorResult errors, Configuration config, PackageSources cpv, AssemblyVisitor storer) throws IOException {
		Repository repository = new Repository();
		FLASCompiler compiler = new FLASCompiler(config, errors, repository);
		compiler.uploader((JSUploader) storer);
		compiler.loadFLIMFromContentStore();
		compiler.parse(cpv);
		if (errors.hasErrors())
			return true;
		for (ContentObject web : cpv.webs())
			compiler.splitWeb(web);
		if (errors.hasErrors())
			return true;

		compiler.stage2(Arrays.asList(cpv));
		if (errors.hasErrors())
			return true;

		compiler.storeAssemblies(storer);
		
		return errors.hasErrors();
	}

	private static FLASCompiler commonCompiler(ErrorResult errors, Configuration config) throws IOException, FileNotFoundException {
		FLASCompiler compiler = doCompilation(errors, config);
		if (errors.hasErrors()) {
			Writer osw;
			File f = config.writeErrorsTo();
			if (f != null)
				osw = new FileWriter(f);
			else
				osw = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
			errors.showTo(new PrintWriter(osw, true), 0);
			return null;
		}
		return compiler;
	}

	private static FLASCompiler doCompilation(ErrorResult errors, Configuration config) throws IOException, FileNotFoundException {
		if (errors.hasErrors()) {
			return null;
		}

		Repository repository = new Repository();
		FLASCompiler compiler = new FLASCompiler(config, errors, repository);
		if (compiler.loadFLIMFromFiles())
			return null;
		if (config.inputs.isEmpty()) {
			errors.message((InputPosition)null, "there are no input packages");
			return null;
		}
		List<PackageSources> packages = new ArrayList<>();
		for (File input : config.inputs)
			packages.add(compiler.processInputFromDirectory(input));
		for (File web : config.webs) {
			if (!web.canRead()) {
				errors.message((InputPosition) null, "there is no web input: " + web);
				continue;
			}
			if (web.isDirectory()) {
				compiler.splitWeb(web);
			} else { // assume it is a zip
				compiler.splitWeb(new FileContentObject(web));
			}
		}

		if (compiler.stage2(packages))
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
		StaticLoggerBinder.defaultLevel("Compiler", level);
		StaticLoggerBinder.defaultLevel("Traverser", level);
		StaticLoggerBinder.defaultLevel("Resolver", level);
		StaticLoggerBinder.defaultLevel("Lifter", level);
		StaticLoggerBinder.defaultLevel("Patterns", level);
		StaticLoggerBinder.defaultLevel("TOPatterns", level);
		StaticLoggerBinder.defaultLevel("TypeChecker", level);
		StaticLoggerBinder.defaultLevel("TCUnification", level);
		StaticLoggerBinder.defaultLevel("HSI", level);
		StaticLoggerBinder.defaultLevel("Generator", level);
		StaticLoggerBinder.defaultLevel("TestRunner", level);
		StaticLoggerBinder.defaultLevel("io.webfolder.ui4j", level);
		StaticLoggerBinder.defaultLevel("IdemHandler", level);
		StaticLoggerBinder.defaultLevel("TestStages", level);
		StaticLoggerBinder.defaultLevel("Dispatcher", level);
		StaticLoggerBinder.defaultLevel("Send", level);
		StaticLoggerBinder.defaultLevel("GLS", level);
		StaticLoggerBinder.defaultLevel("TxManagerThreading", level);
		StaticLoggerBinder.defaultLevel("assembler", level);
		StaticLoggerBinder.defaultLevel("awstxstore", level);
		StaticLoggerBinder.defaultLevel("org.ziniki.awstxstore", level);
		
		StaticLoggerBinder.defaultLevel("ZiWSH", level);
		StaticLoggerBinder.defaultLevel("ZiwshClient", level);
		StaticLoggerBinder.defaultLevel("Ziniki", level);
		StaticLoggerBinder.defaultLevel("ZinikiTI", level);
		StaticLoggerBinder.defaultLevel("IMUnitOfWork", level);
		StaticLoggerBinder.defaultLevel("Broker", level);
		StaticLoggerBinder.defaultLevel("Utils", level);
		StaticLoggerBinder.defaultLevel("BrokerDelegate", level);
		StaticLoggerBinder.defaultLevel("BTResponder", level);
		StaticLoggerBinder.defaultLevel("ZinDelivery", level);
		StaticLoggerBinder.defaultLevel("DoGet", level);
		StaticLoggerBinder.defaultLevel("Uploader", level);
	}
}
