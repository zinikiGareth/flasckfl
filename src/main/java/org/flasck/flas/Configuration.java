package org.flasck.flas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorReporter;

public class Configuration {
	public final ErrorReporter errors;
	public boolean unitjvm = true, unitjs = true;
	public boolean dumpTypes;
	private List<File> searchFlim = new ArrayList<>();
	private File root;
	public File writeFlim;
	public File writeHSIE;
	public File writeJS;
	private File writeDroid;
	public File writeJVM;
	private boolean buildDroid = true;
	public final boolean tda = true;
	PhaseTo upto = PhaseTo.COMPLETE;
	File dumprepo = null;
	public final List<File> inputs = new ArrayList<File>();
	private File writeTestReportsTo;

	public Configuration(ErrorReporter errors, String[] args) {
		this.errors = errors;
		process(args);
	}

	private void process(String[] args) {
		for (int i=0;i<args.length;i++) {
			String arg = args[i];
			int hasMore = args.length-i-1;
			if (arg.startsWith("-")) {
				if (arg.equals("--dump"))
					dumpTypes = true;
				else if (arg.equals("--root"))
					root = new File(args[++i]);
				else if (arg.equals("--phase"))
					upto = PhaseTo.valueOf(args[++i]);
				else if (arg.equals("--dumprepo"))
					dumprepo = new File(root, args[++i]);
				else if (arg.equals("--testReports")) {
					if (hasMore == 0) {
						System.out.println("--testReports <dir>");
						System.exit(1);
					}
					writeTestReportsTo = new File(root, args[++i]);
				} else if (arg.equals("--flim")) {
					if (hasMore == 0) {
						System.out.println("--flim <dir>");
						System.exit(1);
					}
					searchFlim.add(new File(root, args[++i]));
				} else if (arg.equals("--wflim")) {
					if (hasMore == 0) {
						System.out.println("--wflim <dir>");
						System.exit(1);
					}
					writeFlim = new File(root, args[++i]);
				} else if (arg.equals("--hsie")) {
					if (hasMore == 0) {
						System.out.println("--hsie <dir>");
						System.exit(1);
					}
					writeHSIE = new File(root, args[++i]);
				} else if (arg.equals("--jsout")) {
					if (hasMore == 0) {
						System.out.println("--jsout <dir>");
						System.exit(1);
					}
					writeJS = new File(root, args[++i]);
				} else if (arg.equals("--no-unit-js")) {
					unitjs = false;
				} else if (arg.equals("--no-unit-jvm")) {
					unitjvm = false;
				} else if (arg.equals("--android")) {
					if (hasMore == 0) {
						System.out.println("--android <build-dir>");
						System.exit(1);
					}
					writeDroid = new File(root, args[++i]);
				} else if (arg.equals("--jvm")) {
					if (hasMore == 0) {
						System.out.println("--jvm <build-dir>");
						System.exit(1);
					}
					writeJVM = new File(root, args[++i]);
//				} else if (arg.equals("--webzipdir")) {
//					if (hasMore == 0) {
//						System.out.println("--webzipdir <dir>");
//						System.exit(1);
//					}
//					webZipDir = new File(root, args[++i]);
//				} else if (arg.equals("--webzip")) {
//					if (hasMore == 0) {
//						System.out.println("--webzip <name>");
//						System.exit(1);
//					}
//					useWebZips.add(args[++i]);
//				} else if (arg.equals("--legacy")) {
//					tda = false;
				} else {
					boolean matched = false;
					/*
					DroidBuilder builder = compiler.getBuilder();
					if (builder != null) { // consider droid build options
						matched = true;
						if (arg.equals("--clean")) {
							builder.cleanFirst();
						} else
							matched = false;
					}
					*/
					if (!matched) {
						errors.message((InputPosition)null, "unknown option: " + arg);
					}
				}
			} else {
				inputs.add(new File(root, arg));
			}
		}
	}

	public File jvmDir() {
		if (writeJVM != null)
			return writeJVM;
		else if (root != null)
			return root;
		else
			return new File(".");
	}

	public File jsDir() {
		if (writeJS != null)
			return writeJS;
		else if (root != null)
			return root;
		else
			return new File(".");
	}

	public File writeTestReportsTo(File f) {
		if (writeTestReportsTo != null)
			return writeTestReportsTo;
		else
			return f.getParentFile();
	}
}
