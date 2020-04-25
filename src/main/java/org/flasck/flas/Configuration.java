package org.flasck.flas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.AssemblyVisitor;
import org.zinutils.utils.FileUtils;

public class Configuration {
	public final ErrorReporter errors;
	public boolean unitjvm = true, unitjs = true;
	public boolean dumpTypes;
	private List<File> searchFlim = new ArrayList<>();
	private File root;
	public boolean doTypeCheck = true;
	public boolean generateJS = true;
	public boolean generateJVM = true;
	public File writeFlim;
	public File writeHSIE;
	public File writeJS;
//	private File writeDroid;
	public File writeJVM;
//	private boolean buildDroid = true;
	PhaseTo upto = PhaseTo.COMPLETE;
	File dumprepo = null;
	public final List<File> inputs = new ArrayList<File>();
	public final List<File> webs = new ArrayList<File>();
	private File writeTestReportsTo;
	private File writeErrorsTo;
	public File writeTypesTo;
	private String jstestdir;
	public String specifiedTestName;
	public AssemblyVisitor storer;

	public Configuration(ErrorReporter errors, String[] args) {
		this.errors = errors;
		process(args);
	}

	private void process(String[] args) {
		for (int i=0;i<args.length;i++) {
			String arg = args[i];
			if (arg == null)
				continue;
			int hasMore = args.length-i-1;
			if (arg.startsWith("-")) {
				if (arg.equals("--root"))
					root = new File(args[++i]);
				else if (arg.equals("--phase"))
					upto = PhaseTo.valueOf(args[++i]);
				else if (arg.equals("--dumprepo"))
					dumprepo = new File(root, args[++i]);
				else if (arg.equals("--errors")) {
					if (hasMore == 0) {
						System.out.println("--errors <dir>");
						System.exit(1);
					}
					writeErrorsTo = new File(root, args[++i]);
				} else if (arg.equals("--types")) {
					if (hasMore == 0) {
						System.out.println("--types <dir>");
						System.exit(1);
					}
					writeTypesTo = new File(root, args[++i]);
				} else if (arg.equals("--testReports")) {
					if (hasMore == 0) {
						System.out.println("--testReports <dir>");
						System.exit(1);
					}
					writeTestReportsTo = new File(root, args[++i]);
				} else if (arg.equals("--store-html")) {
					if (hasMore == 0) {
						System.out.println("--store-html <dir>");
						System.exit(1);
					}
					jstestdir = args[++i];
				} else if (arg.equals("--testname")) {
					if (hasMore == 0) {
						System.out.println("--testname <name>");
						System.exit(1);
					}
					specifiedTestName = args[++i];
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
					generateJS = false;
					unitjs = false;
				} else if (arg.equals("--no-unit-jvm")) {
					generateJVM = false;
					unitjvm = false;
//				} else if (arg.equals("--android")) {
//					if (hasMore == 0) {
//						System.out.println("--android <build-dir>");
//						System.exit(1);
//					}
//					writeDroid = new File(root, args[++i]);
				} else if (arg.equals("--jvmout")) {
					if (hasMore == 0) {
						System.out.println("--jvmout <build-dir>");
						System.exit(1);
					}
					writeJVM = new File(root, args[++i]);
				} else if (arg.equals("--web")) {
					if (hasMore == 0) {
						System.out.println("--web <dir|zip>");
						System.exit(1);
					}
					webs.add(new File(root, args[++i]));
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

	public String jsTestDir() {
		File front;
		if (root != null) {
			if (root.isAbsolute())
				front = root;
			else
				front = FileUtils.combine(System.getProperty("user.dir"), root);
		} else
			front = new File(System.getProperty("user.dir"));
		if (jstestdir != null)
			return new File(front, jstestdir).getPath();
		else
			return front.getPath();
	}
	
	public File writeErrorsTo() {
		return writeErrorsTo;
	}

	public File writeTestReportsTo(File f) {
		if (writeTestReportsTo != null)
			return writeTestReportsTo;
		else
			return f.getParentFile();
	}
}
