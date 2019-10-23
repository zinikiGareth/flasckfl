package org.flasck.flas.compiler;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.Configuration;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FLASCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	public static boolean backwardCompatibilityMode = true;
	private final List<File> pkgdirs = new ArrayList<File>();
	private File writeRW;
	private DroidBuilder builder = new DroidBuilder();
//	private File writeTestReports;
	private PrintWriter errorWriter;
	private Configuration config;

	public FLASCompiler(Configuration config) {
		this.config = config;
	}
	
	public ErrorMark processInput(Configuration config, Repository repository, File input, ErrorMark mark) {
		try {
			mark = parse(repository, input, mark);
		} catch (Throwable ex) {
			reportException(ex);
		} finally {
			errors().showFromMark(mark, errorWriter(), 0);
		}
		return mark;
	}

	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put
	// it
//	@Override
//	public void writeDroidTo(File file, boolean andBuild) {
//		if (file == null || file.getPath().equals("null"))
//			return;
//		builder = new DroidBuilder(file);
//		if (!andBuild)
//			builder.dontBuild();
//	}
//
//	public void internalBuildJVM() {
//		builder = new DroidBuilder();
//		builder.dontBuild();
//	}
//
//	@Override
//	public void writeRWTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeRW = file;
//	}
//
//	@Override
//	public void writeFlimTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeFlim = file;
//	}
//
//	@Override
//	public void writeDependsTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeDepends = file;
//	}

//	@Override
//	public void writeHSIETo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeHSIE = file;
//	}

//	@Override
//	public void trackTC(File file) {
//		if (file != null)
//			this.trackTC = new File(file, "types");
//		else
//			this.trackTC = null;
//	}
//
//	@Override
//	public void writeJSTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
//		this.writeJS = file;
//	}
//
//	@Override
//	public void writeTestReportsTo(File file) {
//		if (file != null && !file.isDirectory()) {
//			System.out.println("there is no directory " + file);
//			return;
//		}
////		this.writeTestReports = file;
//	}

	public void errorWriter(PrintWriter printWriter) {
		this.errorWriter = printWriter;
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

	// Now read and parse all the files, passing it on to the alleged phase2
	public ErrorMark parse(Repository repository, File dir, ErrorMark mark) {
		if (!dir.isDirectory())
			throw new RuntimeException("there is no input directory " + dir);

		String inPkg = dir.getName();
		checkPackageName(inPkg);
		System.out.println(" |" + inPkg);
		ParsingPhase flp = new ParsingPhase(config.errors, inPkg, (TopLevelDefinitionConsumer)repository);
		List<File> files = FileUtils.findFilesMatching(dir, "*.fl");
		files.sort(new FileNameComparator());
		for (File f : files) {
			System.out.println("    " + f.getName());
			flp.process(f);
			config.errors.showFromMark(mark, errorWriter, 4);
			mark = config.errors.mark();
		}
		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			System.out.println("    " + f.getName());
			String file = FileUtils.dropExtension(f.getName());
			UnitTestFileName utfn = new UnitTestFileName(new PackageName(inPkg), "_ut_" + file);
			UnitTestPackage utp = new UnitTestPackage(utfn);
			repository.unitTestPackage(utp);
			ParsingPhase parser = new ParsingPhase(config.errors, utfn, utp);
			parser.process(f);
			config.errors.showFromMark(mark, errorWriter, 4);
			mark = config.errors.mark();
		}
		if (config.errors.hasErrors())
			return mark;
		return mark;
	}

	private void checkPackageName(String inPkg) {
		String[] bits = inPkg.split("\\.");
		for (String s : bits) {
			if (!Character.isLowerCase(s.charAt(0)))
				throw new RuntimeException("Package must have valid package name");
		}
	}

	public DroidBuilder getBuilder() {
		return builder;
	}

	public boolean hasErrors() {
		return config.errors.hasErrors();
	}

	public void reportException(Throwable ex) {
		config.errors.reportException(ex);
	}

	public ErrorReporter errors() {
		return config.errors;
	}

	public Writer errorWriter() {
		return errorWriter;
	}
}