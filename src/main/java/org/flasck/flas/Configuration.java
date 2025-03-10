package org.flasck.flas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.PhaseTo;
import org.flasck.flas.compiler.modules.OptionModule;
import org.flasck.flas.compiler.modules.PreCompilationModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.jvm.ziniki.PackageSources;
import org.ziniki.paas.interfaces.ContentStorage;
import org.ziniki.tdastore.support.InternalContentStorage;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.utils.FileUtils;

public class Configuration {
	private final ServiceLoader<OptionModule> optionModules;
	private final ServiceLoader<PreCompilationModule> precompilationModules;
	public final ErrorReporter errors;
	public boolean unitjvm = true, unitjs = false;
	public boolean systemjvm = true, systemjs = false;
	public boolean usesplitter = true;
	public File projectDir;
	public File writeFlim = null;
	public boolean doTypeCheck = true;
	public boolean generateJS = true;
	public boolean generateJVM = true;
	public boolean genapps = true;
	public String html;
	public String inclPrefix = "";
	PhaseTo upto = PhaseTo.COMPLETE;
	File dumprepo = null;
	public final List<File> inputs = new ArrayList<File>();
	public final List<File> webs = new ArrayList<File>();
	private File writeTestReportsTo;
	private File writeErrorsTo;
	public File writeTypesTo;
	public String jstestdir;
	public String specifiedTestName;
	public String flascklibDir;
	public File moduleDir; // TODO: the equivalent thing for ContentStore
	public PackageSources flascklibCPV;
	public List<PackageSources> moduleCOs;
	public List<PackageSources> dependencies;
	public final List<File> includeFrom = new ArrayList<File>();
	public final List<File> loadJars = new ArrayList<File>();
	public final List<String> modules = new ArrayList<>(); // just the "names" of the modules - we will use the
															// "moduleDir" and known rules to find the actual items we
															// want
	public ContentStorage contentStore = new InternalContentStorage();

	public Configuration(ErrorReporter errors, String[] args) {
		this.optionModules = ServiceLoader.load(OptionModule.class);
		this.precompilationModules = ServiceLoader.load(PreCompilationModule.class);
		this.errors = errors;
		if (args != null)
			process(args);
	}

	private void process(String[] args) {
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("--project-dir")) {
				if (projectDir != null) {
					System.out.println("--project-dir can only be specified once");
					System.exit(1);
				} else if (i == args.length - 1) {
					System.out.println("--project-dir <dir>");
					System.exit(1);
				} else {
					projectDir = new File(args[++i]);
				}
			}
		}
		boolean error = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg == null || arg.length() == 0)
				continue;
			int hasMore = args.length - i - 1;
			if (arg.startsWith("-")) {
				if (arg.equals("--project-dir")) {
					// was processed above
					++i;
				} else if (arg.equals("--module")) {
					this.modules.add(args[++i]);
				} else if (arg.equals("--html")) {
					if (html != null) {
						System.out.println("--html can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--html <file>");
						System.exit(1);
					}
					html = args[++i];
				} else if (arg.equals("--incl-prefix")) {
					if (!inclPrefix.equals("")) {
						System.out.println("--incl-prefix can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--incl-prefix <prefix>");
						System.exit(1);
					}
					inclPrefix = args[++i];
				} else if (arg.equals("--testname")) {
					if (specifiedTestName != null) {
						System.out.println("--testname can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--testname <name>");
						System.exit(1);
					}
					specifiedTestName = args[++i];
				}

// TODO: I'm not sure this works the way I want
// it says "read" for the first one and "include" for the second
// so at least rename "read"
// plus, the writing one shouldn't be a list

				// flim import and export
				else if (arg.equals("--flim")) {
					if (writeFlim != null) {
						System.out.println("--flim can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--flim <dir>");
						System.exit(1);
					}
					writeFlim = new File(args[++i]);
				} else if (arg.equals("--import")) {
					if (hasMore == 0) {
						System.out.println("--import <dir>");
						System.exit(1);
					}
					includeFrom.add(new File(args[++i]));
				}

				// turn things on or off
				else if (arg.equals("--phase")) {
					if (upto != PhaseTo.COMPLETE) {
						System.out.println("--phase can only be specified once");
						error = true;
					}
					upto = PhaseTo.valueOf(args[++i]);
				} else if (arg.equals("--unit-js")) {
					unitjs = true;
				} else if (arg.equals("--no-unit-jvm")) {
					unitjvm = false;
				} else if (arg.equals("--system-js")) {
					systemjs = true;
				} else if (arg.equals("--no-system-jvm")) {
					systemjvm = false;
				} else if (arg.equals("--no-gen-apps")) {
					genapps = false;
				}

				// things in unusual places
				else if (arg.equals("--flascklib")) {
					if (flascklibDir != null) {
						System.out.println("--flascklib can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--flascklib <dir>");
						System.exit(1);
					}
					this.flascklibDir = args[++i]; // definitely NOT under root
				} else if (arg.equals("--store-html")) {
					if (jstestdir != null) {
						System.out.println("--store-html can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--store-html <dir>");
						System.exit(1);
					}
					jstestdir = args[++i];
				} else if (arg.equals("--moduledir")) {
					if (moduleDir != null) {
						System.out.println("--moduledir can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--moduledir <dir>");
						System.exit(1);
					}
					moduleDir = new File(args[++i]); // definitely NOT under root
				} else if (arg.equals("--web")) {
					if (hasMore == 0) {
						System.out.println("--web <dir|zip>");
						System.exit(1);
					}
					webs.add(new File(projectDir, args[++i]));
				}

				// --capture options, mainly used by the golden tests ...
				else if (arg.equals("--capture-repository")) {
					if (specifiedTestName != null) {
						System.out.println("--capture-repository can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--capture-repository <dir>");
						System.exit(1);
					}
					dumprepo = new File(projectDir, args[++i]);
				} else if (arg.equals("--errors")) {
					if (writeErrorsTo != null) {
						System.out.println("--errors can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--errors <dir>");
						System.exit(1);
					}
					writeErrorsTo = new File(projectDir, args[++i]);
				} else if (arg.equals("--types")) {
					if (writeTypesTo != null) {
						System.out.println("--types can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--types <dir>");
						System.exit(1);
					}
					writeTypesTo = new File(projectDir, args[++i]);
				} else if (arg.equals("--testReports")) {
					if (writeTestReportsTo != null) {
						System.out.println("--testReports can only be specified once");
						error = true;
					}
					if (hasMore == 0) {
						System.out.println("--testReports <dir>");
						System.exit(1);
					}
					writeTestReportsTo = new File(projectDir, args[++i]);
				}

				// at this point, give up and ask for help...
				else {
					boolean matched = false;
					for (OptionModule om : optionModules) {
						int cnt = om.options(errors, args, i);
						if (cnt > 0) {
							i += cnt - 1;
							matched = true;
							break;
						}
					}
					if (!matched) {
						errors.message((InputPosition) null, "unknown option: " + arg);
					}
				}
			} else {
				inputs.add(new File(projectDir, arg));
			}
		}
		if (error) {
			System.exit(1);
		}
		if (moduleDir == null && !modules.isEmpty()) {
			errors.message((InputPosition) null, "cannot specify --module without --moduledir");
		}
		if (html != null && flascklibDir == null) {
			errors.message((InputPosition) null, "Use of --html requires --flascklib");
		}
	}

	public boolean preCompilation() {
		try {
			boolean ret = true;
			for (PreCompilationModule m : precompilationModules) {
				ret &= m.preCompilation(this);
			}
			return ret;
		} catch (Exception ex) {
			throw WrappedException.wrap(ex);
		}
	}

	public String jsTestDir() {
		File front;
		if (projectDir != null) {
			if (projectDir.isAbsolute())
				front = projectDir;
			else
				front = FileUtils.combine(System.getProperty("user.dir"), projectDir);
		} else
			front = new File(System.getProperty("user.dir"));
		if (jstestdir != null)
			if (new File(jstestdir).isAbsolute())
				return jstestdir;
			else
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
		return writeFlim;
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
