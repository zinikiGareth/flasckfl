package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.jsgen.packaging.JSUploader;
import org.flasck.flas.compiler.modules.CompilerComplete;
import org.flasck.flas.compiler.modules.ParserModule;
import org.flasck.flas.compiler.templates.EventBuilder;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.EventHolder;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.assembly.BuildAssembly;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.AssemblyVisitor;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.flim.FlimReader;
import org.flasck.flas.repository.flim.FlimTop;
import org.flasck.flas.repository.flim.FlimWriter;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.TypeDumper;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.flas.testrunner.TestResultWriter;
import org.flasck.jvm.J;
import org.flasck.jvm.assembly.FLASAssembler;
import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.flasck.jvm.ziniki.MemoryContentObject;
import org.flasck.jvm.ziniki.PackageSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.ConcreteMetaData;
import org.ziniki.splitter.SplitMetaData;
import org.ziniki.splitter.Splitter;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.NoSuchDirectoryException;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.utils.FileUtils;

public class FLASCompiler implements CompileUnit {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final Configuration config;
	private final ErrorReporter errors;
	private final Repository repository;
	private final Splitter splitter;
	private final ServiceLoader<ParserModule> modules;
	private final ServiceLoader<CompilerComplete> completeModules;
	private final List<URI> brokenUris = new ArrayList<>();
	private TaskQueue tasks;
	private File cardsFolder;
	// TODO: it feels to me that these should be in the REPO, not on the compiler itself
	private DirectedAcyclicGraph<String> pkgs;
	private JSEnvironment jse;
	private Map<EventHolder, EventTargetZones> eventMap;
	private ByteCodeEnvironment bce;
	private JSUploader uploader;
	private CardDataListener cardDataListener;
	private Map<URI, String> textCache = new TreeMap<>();

	public FLASCompiler(Configuration config, ErrorReporter errors, Repository repository, CardDataListener cardDataListener) {
		logger.info("initializing FLASCompiler");
		this.config = config;
		this.errors = errors;
		this.repository = repository;
		if (this.config.usesplitter) {
			this.splitter = new Splitter(x -> errors.message(new InputPosition(x.file, 0, 0, null, x.text), x.message));
		} else
			this.splitter = null;
		this.modules = ServiceLoader.load(ParserModule.class);
		this.completeModules = ServiceLoader.load(CompilerComplete.class);
		this.cardDataListener = cardDataListener;
	}
	
	public BCEClassLoader classLoader() {
		return new BCEClassLoader(bce);
	}
	
	public void uploader(JSUploader loader) {
		this.uploader = loader;
	}

	public void taskQueue(TaskQueue tasks) {
		this.tasks = tasks;
	}

	public void setCardsFolder(File cardsFolder) {
		this.cardsFolder = cardsFolder;
	}

	public void lspLoadFLIM(URI uri) {
		logger.info("lspLoadFLIM for workspace " + uri);
		errors.beginPhase1(uri);
		loadFLIMFromFiles();
		errors.doneProcessing(brokenUris);
	}

	public boolean loadFLIMFromFiles() {
		LoadBuiltins.applyTo(errors, repository);
		pkgs = new DirectedAcyclicGraph<>();
		FlimReader reader = new FlimReader(errors, repository);

		// read from (multiple) read-only import locations
		for (File dir : config.includeFrom) {
			reader.read(pkgs, dir, config.inputs);
			if (errors.hasErrors())
				return true;
		}
		
		// read from where we write (but beware of recursion)
		reader.read(pkgs, config.writeFlim, config.inputs);
		if (errors.hasErrors())
			return true;
		
		return false;
	}

	public void loadFLIMFromContentStore() {
		LoadBuiltins.applyTo(errors, repository);
		pkgs = new DirectedAcyclicGraph<>();
		Set<FlimTop> importers = new HashSet<>();
		FlimReader reader = new FlimReader(errors, repository);
		if (config.dependencies != null) {
			for (PackageSources ps : config.dependencies) {
				if (ps.flims() != null) {
					for (ContentObject co : ps.flims()) {
						importers.add(reader.read(pkgs, ps.getPackageName(), co));
					}
				}
			}
		}
		if (errors.hasErrors())
			return;
		
		for (FlimTop ft : importers) {
			repository.selectPackage(ft.pkgName());
			ft.resolve();
		}
	}

	public PackageSources processInputFromDirectory(File input) throws ErrorResultException {
		if (!input.isDirectory()) {
			errors.message((InputPosition) null, "there is no input directory " + input);
			return null;
		}
		try {
			PackageSources sources = new FileBasedSources(input, config.webs);
			parse(sources);
			return sources;
		} catch (Throwable ex) {
			reportException(ex);
			throw new ErrorResultException(errors);
		}
	}

	public void splitWeb(File dir) {
		try {
			SplitMetaData md = splitter.split(dir);
			if (cardDataListener != null)
				cardDataListener.provideWebData(md);
			repository.webData(md);
		} catch (IOException ex) {
			errors.message((InputPosition) null, "error splitting: " + dir);
		}
	}

	public void splitWeb(ContentObject co) {
		try {
			// TODO: We should probably allow users to specify this file
			File tmp = File.createTempFile("webdata", ".zip");
			FileOutputStream baos = new FileOutputStream(tmp);
			SplitMetaData md = splitter.split(baos, co.asStream());
			((ConcreteMetaData) md).stream(tmp);
			repository.webData(md);
		} catch (IOException ex) {
			errors.message((InputPosition) null, "error splitting: " + co.key());
		}
	}

	// TODO: need to abstract the System.out as well
	public void parse(PackageSources sources) {
		String inPkg = sources.getPackageName();
		checkPackageName(inPkg);
		System.out.println(" |" + inPkg);
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer) repository, modules);
		logger.info("parsing fl");
		for (ContentObject f : sources.sources()) {
			File fi = new File(f.key());
			if (!validateFileName(fi))
				continue;
			errors.track(fi);
			flp.process(f);
		}
		logger.info("parsing ut");
		for (ContentObject f : sources.unitTests()) {
			File fi = new File(f.key());
			if (!validateFileName(fi))
				continue;
			errors.track(fi);
			String file = FileUtils.dropExtension(fi.getName());
			UnitTestFileName utfn = new UnitTestFileName(new PackageName(inPkg), "_ut_" + file);
			UnitTestPackage utp = new UnitTestPackage(new InputPosition(file, 1, 0, null, ""), utfn);
			repository.unitTestPackage(errors, utp);
			ParsingPhase parser = new ParsingPhase(errors, utfn, new ConsumeDefinitions(errors, repository, utp));
			parser.process(f);
		}
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		logger.info("parsing fa");
		for (ContentObject f : sources.assemblies()) {
			File fi = new File(f.key());
			if (!validateFileName(fi))
				continue;
			errors.track(fi);
			fap.process(f);
		}
		logger.info("parsing st");
		for (ContentObject f : sources.systemTests()) {
			File fi = new File(f.key());
			if (!validateFileName(fi))
				continue;
			errors.track(fi);
			String file = FileUtils.dropExtension(fi.getName());
			PackageName stfn = new PackageName(new PackageName(inPkg), "_st_" + file);
			SystemTest st = new SystemTest(stfn);
			repository.systemTest(errors, st);
			ParsingPhase parser = new ParsingPhase(errors, stfn, st, (TopLevelDefinitionConsumer) repository, modules);
			parser.process(f);
		}
	}

	private boolean validateFileName(File fi) {
		String n = fi.getName();
		if (!assertValidName(n)) {
			errors.message(new InputPosition(n, 0, 0, null, null), "illegal characters in file name");
			return false;
		} else
			return true;
	}

	private boolean assertValidName(String n) {
		n = FileUtils.dropExtension(n);
		if (!Character.isLetter(n.charAt(0)))
			return false;
		for (int i=1;i<n.length();i++) {
			if (!Character.isLetterOrDigit(n.charAt(i)))
				return false;
		}
		return true;
	}

	@Override
	public void parse(URI uri, String text) {
		logger.info("Compiling " + uri);
		errors.beginPhase1(uri);
		parseOne(uri, text);
		repository.done();
		errors.doneProcessing(brokenUris);
		tasks.readyWhenYouAre(uri, this);
	}

	private void parseOne(URI uri, String text) {
		repository.parsing(uri);
		File file = new File(uri.getPath());
		String inPkg = file.getParentFile().getName();
		String name = file.getName();
		String type = FileUtils.extension(name);

		ContentObject co;
		if (text != null) {
			textCache.put(uri, text);
			co = new MemoryContentObject(file, text.getBytes());
		} else
			co = new FileContentObject(file);
		if (type == null) {
			errors.logMessage("could not compile " + inPkg + "/" + file);
			return;
		}
		switch (type) {
		case ".fl":
			parseFL(file, inPkg, name, co);
			break;
		case ".ut":
			break;
		case ".st":
			break;
		case ".fa":
			parseFA(file, inPkg, name, co);
			break;
		default:
			errors.logMessage("could not compile " + inPkg + "/" + file);
			break;
		}
	}

	private void parseFL(File file, String inPkg, String name, ContentObject fileCO) {
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer) repository, modules);
		errors.logMessage("compiling " + name + " in " + inPkg);
		flp.process(fileCO);
	}

	private void parseFA(File file, String inPkg, String name, ContentObject fileCO) {
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		errors.logMessage("compiling " + file.getName() + " in " + inPkg);
		fap.process(fileCO);
	}

	public void attemptRest(URI uri) {
		logger.info("attempting to compile rest of files for " + uri);
		// if there were previously files that were corrupt, try compiling them again
		List<URI> broken = new ArrayList<>(this.brokenUris);
		for (URI b : broken) {
			if (b.equals(uri))
				continue;
			if (textCache.containsKey(b))
				parseOne(b, textCache.get(b));
			else
				parseOne(b, null);
		}

		// If some are still broken, we cannot proceed
		if (!errors.getAllBrokenURIs().isEmpty())
			return;

		if (cardsFolder != null && cardsFolder.isDirectory()) {
			splitWeb(cardsFolder);
		}

		// do the rest of the compilation
		errors.beginPhase2(uri);
		repository.clean();
		if (stage2(null)) { // I don't think this should upload, so it won't need the package sources per se ...
			// worked
		}
		errors.doneProcessing(this.brokenUris);
	}

	public boolean stage2(List<PackageSources> packages) {
		File dump = config.dumprepo();
		if (dump != null) {
			try {
				repository.dumpTo(dump);
			} catch (IOException ex) {
				System.out.println("Could not dump repository to " + dump);
			}
		}

		if (config.upto() == PhaseTo.PARSING)
			return false;

		logger.info("resolving");
		if (resolve())
			return true;

		logger.info("lifting");
		FunctionGroups ordering = lift();
		
		logger.info("analyzing patterns");
		analyzePatterns();
		if (errors.hasErrors())
			return true;

		logger.info("attempting to identify 'constant' functions");
		// In this context, "constant" functions are ones that:
		// have no args
		// may "have" state, but do not use it
		// may be passed contracts, but do not use them
		// i.e. in reality they have no arguments
		// Note, however, that this is transitive, since if they have state and reference another function,
		//   it may depend on the state
		FigureFunctionConstness ffc = new FigureFunctionConstness();
		ffc.processAll(ordering);
		
		if (config.doTypeCheck) {
			logger.info("typechecking");
			doTypeChecking(ordering);
			if (errors.hasErrors())
				return true;
			try {
				dumpTypes(config.writeTypesTo);
			} catch (FileNotFoundException ex) {
				errors.message((InputPosition) null, "cannot open file " + config.writeTypesTo);
				return true;
			}
			if (errors.hasErrors())
				return true;
		}

		logger.info("converting methods");
		if (convertMethods())
			return true;

		logger.info("building event maps");
		if (buildEventMaps())
			return true;

		ListMap<String, String> autolink = new ListMap<>();
		Set<String> usedrefs = new TreeSet<>();
		FlimWriter writer = null;
		List<String> process = new ArrayList<>();
		if (config.flimdir() != null) {
			writer = new FlimWriter(repository, config.flimdir());
			for (PackageSources f : packages) {
				String pn = f.getPackageName();
				process.add(pn);
				if (!f.unitTests().isEmpty()) {
					int x = pn.lastIndexOf(".")+1;
					String pk = pn.substring(x);
					String up = pn+ "._ut_" + pk;
					process.add(up);
					autolink.add(up, pn);
				}
				if (!f.systemTests().isEmpty()) {
					int x = pn.lastIndexOf(".")+1;
					String pk = pn.substring(x);
					String sp = pn+ "._st_" + pk;
					process.add(sp);
					autolink.add(sp, pn);
				}
			}
		} else if (uploader != null) {
			writer = new FlimWriter(repository, uploader);
			for (PackageSources p : packages)
				process.add(p.getPackageName());
		}
		if (writer != null) {
			while (!process.isEmpty()) {
				String input = process.remove(0);
				Set<String> refs = writer.export(input);
				if (refs == null)
					return true;
				pkgs.ensure(input);
				if (autolink.contains(input)) {
					for (String k : autolink.get(input)) {
						pkgs.ensure(k);
						pkgs.ensureLink(input, k);
					}
				}
				for (String s : refs) {
					pkgs.ensure(s);
					pkgs.ensureLink(input, s);
				}
				usedrefs.addAll(refs);
				refs.retainAll(process);
				if (!refs.isEmpty()) {
					for (String s : refs)
						System.out.println("invalid order: package " + input + " depends on " + s
								+ " which has not been processed");
					return true;
				}
			}
		}

		logger.info("generating code");
		if (generateCode(config, pkgs))
			return true;

		Map<File, TestResultWriter> testWriters = new HashMap<>();
		try {
			logger.info("running unit tests");
			if (runUnitTests(config, testWriters))
				return true;

			logger.info("running system tests");
			if (runSystemTests(config, testWriters))
				return true;
		} finally {
			testWriters.values().forEach(w -> w.close());
		}
		
		if (config.genapps) {
			for (Assembly a : repository.getAssemblies()) {
				if (a instanceof ApplicationAssembly) {
					File todir = FileUtils.combine(config.projectDir, "apps", a.name().uniqueName());
					logger.info("build app for " + a + " in " + todir);
					FileUtils.cleanDirectory(todir);
					FileUtils.assertDirectory(todir);
					String idx = "index.html";
					if (config.html != null)
						idx = config.html;
					File saveAs = new File(todir, idx);
					try (FileWriter fos = new FileWriter(saveAs)) {
						FLASAssembler asm = new FLASAssembler(fos);
						generateHTML(asm, todir, a);
					} catch (IOException ex) {
						System.err.println("could not save " + a.name().uniqueName() + " to " + saveAs);
					}
				}
			}
		}

		for (CompilerComplete cc : completeModules) {
			cc.complete(errors, config, packages, bce, jse);
		}

		return false;
	}

	public boolean resolve() {
		Resolver resolver = new RepositoryResolver(errors, repository);
		repository.traverseWithImplementedMethods(resolver);
		if (errors.hasErrors()) {
			return true;
		} else
			return false;
	}

	public FunctionGroups lift() {
		return new RepositoryLifter().lift(repository);
	}

	public void analyzePatterns() {
		StackVisitor sv = new StackVisitor();
		new PatternAnalyzer(errors, repository, sv);
		repository.traverseLifted(sv);
	}

	public void doTypeChecking(FunctionGroups ordering) {
		StackVisitor sv = new StackVisitor();
		new TypeChecker(errors, repository, sv);
		repository.traverseInGroups(sv, ordering);
	}

	public void dumpTypes(File ty) throws FileNotFoundException {
		// dump types if specified
		if (ty != null) {
			OutputStream fos;
			if (ty.equals(config.projectDir))
				fos = System.out;
			else
				fos = new FileOutputStream(ty);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
			RepositoryVisitor dumper = new TypeDumper(pw);
			repository.traverse(dumper);
			pw.flush();
			if (fos != System.out)
				pw.close();
		}
	}

	public boolean convertMethods() {
		StackVisitor sv = new StackVisitor();
		new ConvertRepositoryMethods(sv, errors, repository);
		repository.traverseWithMemberFields(sv);
		return errors.hasErrors();
	}

	public boolean buildEventMaps() {
		StackVisitor stack = new StackVisitor();
		eventMap = new HashMap<EventHolder, EventTargetZones>();
		new EventBuilder(stack, eventMap);
		repository.traverse(stack);

		return errors.hasErrors();
	}

	public boolean generateCode(Configuration config, DirectedAcyclicGraph<String> pkgs) {
		jse = new JSEnvironment(config, errors, repository, pkgs);
		bce = new ByteCodeEnvironment();
		populateBCE(bce);

//		System.out.println("_______CODEGEN_______");
		StackVisitor jsstack = new StackVisitor();
		new JSGenerator(repository, jse, jsstack, eventMap);

		if (config.generateJS)
			repository.traverseWithHSI(jsstack);

		if (errors.hasErrors()) {
			return true;
		}

		if (config.generateJS) {
			saveJSE(null, jse, bce);
			jse.generate(bce);
			saveBCE(null, bce);
		}

		return errors.hasErrors();
	}

	public boolean runUnitTests(Configuration config, Map<File, TestResultWriter> writers) {
		if (!(config.generateJVM && config.unitjvm) && !(config.generateJS && config.unitjs))
			return errors.hasErrors();
		
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		ClassLoader cl = this.getClass().getClassLoader();
		if (config.generateJVM && config.unitjvm) {
			cl = makeBCL(config);
			JVMRunner jvmRunner = new JVMRunner(config, repository, cl, allTemplates);
			jvmRunner.runAllUnitTests(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.unitjs) {
			try {
				JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates, cl);
				jsRunner.runAllUnitTests(writers);
				jsRunner.reportErrors(errors);
				jsRunner.shutdown();
			} catch (Exception ex) {
				errors.reportException(ex);
			}
		}

		return errors.hasErrors();
	}

	public boolean runSystemTests(Configuration config, Map<File, TestResultWriter> writers) {
		if (!(config.generateJVM && config.systemjvm) && !(config.generateJS && config.systemjs))
			return errors.hasErrors();
		
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		ClassLoader cl = this.getClass().getClassLoader();
		if (config.generateJVM && config.systemjvm) {
			cl = makeBCL(config);
			JVMRunner jvmRunner = new JVMRunner(config, repository, cl, allTemplates);
			jvmRunner.runAllSystemTests(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.systemjs) {
			try {
				JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates, cl);
				jsRunner.runAllSystemTests(writers);
				jsRunner.reportErrors(errors);
				jsRunner.shutdown();
			} catch (Exception ex) {
				errors.reportException(ex);
			}
		}

		return errors.hasErrors();
	}

	private BCEClassLoader makeBCL(Configuration config) {
		BCEClassLoader bcl = new BCEClassLoader(bce);
		for (File f : config.includeFrom)
			try {
				for (File g : FileUtils.findFilesMatching(f, "*.jar"))
					bcl.addClassesFrom(g);
			} catch (NoSuchDirectoryException ex) {
				logger.info("ignoring non-existent includeFrom directory " + f);
			}
		for (String m : config.modules) {
			File md = new File(config.moduleDir, m);
			File mjd = new File(md, "jars");
			for (File f : FileUtils.findFilesMatching(mjd, "*.jar")) {
				bcl.addClassesFrom(f);
			}
		}
		return bcl;
	}

	public void storeAssemblies(AssemblyVisitor storer) {
		if (jse != null)
			repository.traverseAssemblies(config, errors, jse, bce, storer);
	}

	public void generateHTML(FLASAssembler asm, File todir, Assembly assembly) {
		repository.traverseAssembly(config, errors, jse, bce, new CompilerAssembler(config, asm, todir), assembly); 
	}

	private Map<String, String> extractTemplatesFromWebs() {
		Map<String, String> ret = new TreeMap<>();
		try {
			for (SplitMetaData smd : repository.allWebs()) {
				try (ZipInputStream zis = smd.processedZip()) {
					ZipEntry ze;
					while ((ze = zis.getNextEntry()) != null) {
						if (ze.getName().endsWith(".html"))
							ret.put(ze.getName().replace(".html", ""),
									new String(FileUtils.readAllStream(zis), Charset.forName("UTF-8")));
					}
				}
			}
		} catch (IOException ex) {
			errors.message(((InputPosition) null), "internal error reading templates from splitter");
		}
		return ret;
	}

	public void populateBCE(ByteCodeEnvironment bce) {
		ByteCodeCreator jr = bce.newClass("org.flasck.flas.testrunner.JVMRunner");
		jr.dontGenerate();
		jr.defineField(true, Access.PUBLIC, J.FLEVALCONTEXT, "cxt");
		ByteCodeCreator card = bce.newClass(J.FLCARD);
		card.dontGenerate();
		card.defineField(false, Access.PUBLIC, J.RENDERTREE, "_renderTree");
		card.defineField(true, Access.PROTECTED, J.STRING, "_rootTemplate");
	}

	public void saveBCE(File jvmDir, ByteCodeEnvironment bce) {
//		bce.dumpAll(true);
		try {
			if (config.flimdir() != null) {
				Comparator<String> invertor = (x,y) -> -x.compareTo(y);
				Set<String> pkgs = new TreeSet<>(invertor);
				for (File s : config.inputs) {
					pkgs.add(s.getName());
				}
				Map<String, JarOutputStream> streams = new TreeMap<>();
				for (ByteCodeCreator c : bce.all()) {
					String clname = c.getCreatedName();
					String pkg = null;
					for (String s : pkgs) {
						if (isInPackage(clname, s)) {
							pkg = s;
							break;
						}
					}
					if (isInPackage(clname, pkg) && !clname.contains("_st_") && !clname.contains("_ut_")) {
						JarOutputStream zos = streams.get(pkg);
						if (zos == null) {
							File f = new File(config.flimdir(), pkg + ".jar");
							zos = new JarOutputStream(new FileOutputStream(f));
							streams.put(pkg, zos);
						}
						c.addToJar(zos);
//						zos.putNextEntry(new ZipEntry(FileUtils.convertDottedToPath(clname).getPath() + ".class"));
//						zos.write(c.generate());
					}
				}
				for (ZipOutputStream zos : streams.values()) 
					zos.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			errors.message((InputPosition) null, ex.toString());
		}
	}

	private boolean isInPackage(String clname, String pkg) {
		return pkg != null && (clname.startsWith(pkg) || (pkg.equals("root.package") && clname.startsWith("org.flasck.jvm.builtin.")));
	}

	public void saveJSE(File jsDir, JSEnvironment jse, ByteCodeEnvironment bce) {
		try {
			jse.generateCOs();
			if (uploader != null) {
				jse.upload(uploader);
			}
			if (config.flimdir() != null) {
				jse.saveCOsTo(config.flimdir());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			errors.message((InputPosition) null, ex.toString());
		}
	}

	private void checkPackageName(String inPkg) {
		String[] bits = inPkg.split("\\.");
		for (String s : bits) {
			if (!Character.isLowerCase(s.charAt(0)))
				throw new RuntimeException("Package must have valid package name");
		}
	}

	@SuppressWarnings("unused")
	private void sendRepo() {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			repository.dumpTo(pw);
			pw.close();
			LineNumberReader lnr = new LineNumberReader(new StringReader(sw.toString()));
			String s;
			while ((s = lnr.readLine()) != null) {
				errors.logMessage(s);
			}
		} catch (Exception ex) {
			errors.logMessage("Error reading repo: " + ex);
		}
	}

	public void reportException(Throwable ex) {
		errors.reportException(ex);
	}
}