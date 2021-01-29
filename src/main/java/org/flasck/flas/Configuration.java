package org.flasck.flas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.compiler.modules.OptionModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.jvm.ziniki.PackageSources;
import org.zinutils.utils.FileUtils;

public class Configuration {
	private final ServiceLoader<OptionModule> optionModules;
	public final ErrorReporter errors;
	public boolean unitjvm = true, unitjs = true;
	public boolean systemjvm = true, systemjs = true;
	public boolean dumpTypes;
	public final List<File> readFlims = new ArrayList<>();
	public File root;
	public boolean doTypeCheck = true;
	public boolean generateJS = true;
	public boolean generateJVM = true;
	public File writeJS;
	public File html;
	public File writeJVM;
	PhaseTo upto = PhaseTo.COMPLETE;
	File dumprepo = null;
	public final List<File> inputs = new ArrayList<File>();
	public final List<File> webs = new ArrayList<File>();
	private File writeTestReportsTo;
	private File writeErrorsTo;
	public File writeTypesTo;
	private String jstestdir;
	public String specifiedTestName;
	public String flascklibDir;
	public PackageSources flascklibCPV;
	public List<PackageSources> moduleCOs;
	public List<PackageSources> dependencies;
	public boolean openHTML;
	public final List<File> includeFrom = new ArrayList<File>();
	public final List<File> modules = new ArrayList<>();

	public Configuration(ErrorReporter errors, String[] args) {
		this.optionModules = ServiceLoader.load(OptionModule.class);
		this.errors = errors;
		if (args != null)
			process(args);
	}

	private void process(String[] args) {
		for (int i=0;i<args.length-1;i++) {
			if (args[i].equals("--root")) {
				if (root != null) {
					System.out.println("--root can only be specified once");
					System.exit(1);
				} else {
					root = new File(args[++i]);
				}
			}
		}
		File moduledir = new File(".");
		for (int i=0;i<args.length;i++) {
			String arg = args[i];
			if (arg == null || arg.length() == 0)
				continue;
			int hasMore = args.length-i-1;
			if (arg.startsWith("-")) {
				if (arg.equals("--root")) {
					if (hasMore == 0) {
						System.out.println("--root <dir>");
						System.exit(1);
					}
					// was processed above
					++i;
				} else if (arg.equals("--flascklib")) {
					if (hasMore == 0) {
						System.out.println("--flascklib <dir>");
						System.exit(1);
					}
					this.flascklibDir = args[++i]; // definitely NOT under root 
				} else if (arg.equals("--moduledir")) {
					if (hasMore == 0) {
						System.out.println("--moduledir <dir>");
						System.exit(1);
					}
					moduledir = new File(args[++i]); // definitely NOT under root
				} else if (arg.equals("--module")) {
					this.modules.add(new File(moduledir, args[++i])); 
				} else if (arg.equals("--phase"))
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
				} else if (arg.equals("--html")) {
					if (hasMore == 0) {
						System.out.println("--html <file>");
						System.exit(1);
					}
					html = new File(root, args[++i]);
				} else if (arg.equals("--open")) {
					openHTML = true;
					// TODO: will also want "--card-dir" to go and look for other cards
					// This may also be "--flim"
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
					File flimDir = new File(args[++i]);
					readFlims.add(flimDir);
				} else if (arg.equals("--incl")) {
					if (hasMore == 0) {
						System.out.println("--incl <dir>");
						System.exit(1);
					}
					includeFrom.add(new File(args[++i]));
				} else if (arg.equals("--jsout")) {
					if (hasMore == 0) {
						System.out.println("--jsout <dir>");
						System.exit(1);
					}
					writeJS = new File(root, args[++i]);
				} else if (arg.equals("--no-unit-js")) {
					unitjs = false;
				} else if (arg.equals("--no-unit-jvm")) {
					systemjs = false;
				} else if (arg.equals("--no-system-js")) {
					unitjs = false;
				} else if (arg.equals("--no-system-jvm")) {
					systemjvm = false;
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
					for (OptionModule om : optionModules) {
						int cnt = om.options(errors, args, i);
						if (cnt > 0) {
							i += cnt-1;
							matched = true;
							break;
						}
					}
					if (!matched) {
						errors.message((InputPosition)null, "unknown option: " + arg);
					}
				}
			} else {
				inputs.add(new File(root, arg));
			}
		}
		if (html != null && flascklibDir == null) {
			errors.message((InputPosition)null, "Use of --html requires --flascklib");
		}
		if (openHTML && html == null) {
			errors.message((InputPosition)null, "Use of --open requires --html");
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

	public File writeTestReportsTo() {
		if (writeTestReportsTo != null)
			return writeTestReportsTo;
		else
			return null;
	}

	public File flimdir() {
		if (readFlims.isEmpty())
			return null;
		return readFlims.get(0);
	}

	public File dumprepo() {
		return dumprepo;
	}

	public PhaseTo upto() {
		return upto;
	}

	public <T extends OptionModule> T getOptionsModule(Class<T> clz) {
		for (OptionModule o : optionModules) {
			if (clz.isInstance(o))
				return clz.cast(o);
		}
		return null;
	}
}
