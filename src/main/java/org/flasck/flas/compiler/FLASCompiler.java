package org.flasck.flas.compiler;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.templates.EventBuilder;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.EventHolder;
import org.flasck.flas.parsedForm.assembly.ApplicationAssembly;
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
import org.flasck.flas.repository.flim.FlimWriter;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.TypeDumper;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.flas.testrunner.TestResultWriter;
import org.flasck.jvm.J;
import org.flasck.jvm.assembly.CardInitializer;
import org.flasck.jvm.assembly.FLASAssembler;
import org.flasck.jvm.ziniki.ContentObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.SplitMetaData;
import org.ziniki.splitter.Splitter;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FLASCompiler implements CompileUnit {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	private final Configuration config;
	private final ErrorReporter errors;
	private final Repository repository;
	private final Splitter splitter;
	private final ServiceLoader<ParserModule> modules;
	private final List<URI> brokenUris = new ArrayList<>();
	private TaskQueue tasks;
	private File cardsFolder;
	private DirectedAcyclicGraph<String> pkgs;
	private JSEnvironment jse;
	private Map<EventHolder, EventTargetZones> eventMap;
	private ByteCodeEnvironment bce;

	public FLASCompiler(Configuration config, ErrorReporter errors, Repository repository) {
		this.config = config;
		this.errors = errors;
		this.repository = repository;
		this.splitter = new Splitter(x -> errors.message(new InputPosition(x.file, 0, 0, null, x.text), x.message));
		this.modules = ServiceLoader.load(ParserModule.class);
	}

	public void taskQueue(TaskQueue tasks) {
		this.tasks = tasks;
	}

	public void setCardsFolder(File cardsFolder) {
		this.cardsFolder = cardsFolder;
	}

	public void lspLoadFLIM(URI uri) {
		errors.beginProcessing(uri);
		loadFLIM();
		errors.doneProcessing(brokenUris);
	}

	public boolean loadFLIM() {
		LoadBuiltins.applyTo(errors, repository);
		pkgs = new DirectedAcyclicGraph<>();
		FlimReader reader = new FlimReader(errors, repository);
		for (File dir : config.includeFrom) {
			reader.read(pkgs, dir, config.inputs);
			if (errors.hasErrors())
				return true;
		}
		if (config.flimdir() != null) {
			reader.read(pkgs, config.flimdir(), config.inputs);
			if (errors.hasErrors())
				return true;
		}
		return false;
	}

	public void processInput(File input) {
		try {
			parse(input);
		} catch (Throwable ex) {
			reportException(ex);
		}
	}

	public void splitWeb(File web) {
		if (!web.canRead()) {
			errors.message((InputPosition) null, "there is no web input: " + web);
			return;
		}
		try {
			SplitMetaData md = splitter.split(web);
			repository.webData(md);
		} catch (IOException ex) {
			errors.message((InputPosition) null, "error splitting: " + web);
		}
	}

	public void parse(File dir) {
		if (!dir.isDirectory()) {
			errors.message((InputPosition) null, "there is no input directory " + dir);
			return;
		}
		

		String inPkg = dir.getName();
		checkPackageName(inPkg);
		System.out.println(" |" + inPkg);
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer) repository, modules);
		List<File> files = FileUtils.findFilesMatching(dir, "*.fl");
		files.sort(new FileNameComparator());
		logger.info("parsing fl");
		for (File f : files) {
			System.out.println("    " + f.getName());
			flp.process(f);
		}
		List<File> utfiles = FileUtils.findFilesMatching(dir, "*.ut");
		utfiles.sort(new FileNameComparator());
		logger.info("parsing ut");
		for (File f : utfiles) {
			System.out.println("    " + f.getName());
			String file = FileUtils.dropExtension(f.getName());
			UnitTestFileName utfn = new UnitTestFileName(new PackageName(inPkg), "_ut_" + file);
			UnitTestPackage utp = new UnitTestPackage(new InputPosition(file, 1, 0, null, ""), utfn);
			repository.unitTestPackage(errors, utp);
			ParsingPhase parser = new ParsingPhase(errors, utfn, new ConsumeDefinitions(errors, repository, utp));
			parser.process(f);
		}
		List<File> fafiles = FileUtils.findFilesMatching(dir, "*.fa");
		fafiles.sort(new FileNameComparator());
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		logger.info("parsing fa");
		for (File f : fafiles) {
			System.out.println("    " + f.getName());
			fap.process(f);
		}
		List<File> stfiles = FileUtils.findFilesMatching(dir, "*.st");
		stfiles.sort(new FileNameComparator());
		logger.info("parsing st");
		for (File f : stfiles) {
			System.out.println("    " + f.getName());
			String file = FileUtils.dropExtension(f.getName());
			UnitTestFileName stfn = new UnitTestFileName(new PackageName(inPkg), "_st_" + file);
			SystemTest st = new SystemTest(stfn);
			repository.systemTest(errors, st);
			ParsingPhase parser = new ParsingPhase(errors, stfn, st, (TopLevelDefinitionConsumer) repository, modules);
			parser.process(f);
		}
	}

	@Override
	public void parse(URI uri, String text) {
		System.out.println("Compiling " + uri);
		errors.beginProcessing(uri);
		repository.parsing(uri);
		parseOne(uri);
		repository.done();
		errors.doneProcessing(brokenUris);
		tasks.readyWhenYouAre(uri, this);
	}

	private void parseOne(URI uri) {
		File file = new File(uri.getPath());
		String inPkg = file.getParentFile().getName();
		String name = file.getName();
		String type = FileUtils.extension(name);

		if (type == null) {
			errors.logMessage("could not compile " + inPkg + "/" + file);
			return;
		}
		switch (type) {
		case ".fl":
			parseFL(file, inPkg, name);
			break;
		case ".ut":
			break;
		case ".st":
			break;
		case ".fa":
			parseFA(file, inPkg, name);
			break;
		default:
			errors.logMessage("could not compile " + inPkg + "/" + file);
			break;
		}
	}

	private void parseFL(File file, String inPkg, String name) {
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer) repository, modules);
		errors.logMessage("compiling " + name + " in " + inPkg);
		flp.process(file);
	}

	private void parseFA(File file, String inPkg, String name) {
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		errors.logMessage("compiling " + file.getName() + " in " + inPkg);
		fap.process(file);
	}

	public void attemptRest(URI uri) {
		// if there were previously files that were corrupt, try compiling them again
		List<URI> broken = new ArrayList<>(this.brokenUris);
		for (URI b : broken) {
			parseOne(b);
		}

		// If some are still broken, we cannot proceed
		if (!errors.getAllBrokenURIs().isEmpty())
			return;

		if (cardsFolder != null && cardsFolder.isDirectory()) {
			splitWeb(cardsFolder);
		}

		// do the rest of the compilation
//		sendRepo();
		errors.beginProcessing(uri);
		repository.clean();
		if (stage2()) {
			// worked
		}
		errors.doneProcessing(this.brokenUris);
	}

	public boolean stage2() {
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

		Set<String> usedrefs = new TreeSet<>();
		if (config.flimdir() != null) {
			FlimWriter writer = new FlimWriter(repository, config.flimdir());
			List<String> process = new ArrayList<>();
			for (File f : config.inputs)
				process.add(f.getName());
			while (!process.isEmpty()) {
				String input = process.remove(0);
				Set<String> refs = writer.export(input);
				if (refs == null)
					return true;
				pkgs.ensure(input);
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

		if (config.html != null) {
			try (FileWriter fos = new FileWriter(config.html)) {
				File fldir = new File(config.root, "flascklib/js");
				FileUtils.cleanDirectory(fldir);
				FileUtils.assertDirectory(fldir);
				File cssdir = new File(config.root, "css");
				FileUtils.cleanDirectory(cssdir);
				FileUtils.assertDirectory(cssdir);
				for (SplitMetaData wd : repository.allWebs()) {
					ZipInputStream zoo = wd.processedZip();
					ZipEntry ze;
					while ((ze = zoo.getNextEntry()) != null) {
						if (ze.getName().endsWith(".css"))
							FileUtils.copyStreamToFileWithoutClosing(zoo, new File(cssdir, ze.getName()));
					}
					zoo.close();
				}
				List<File> library = FileUtils.findFilesMatching(new File(config.flascklib), "*");
				for (File f : library) {
					FileUtils.copy(f, fldir);
				}
				FLASAssembler asm = new FLASAssembler(fos, "flascklib");
				if (!config.includeFrom.isEmpty()) {
					File incdir = new File("includes/js");
					File ct = new File(config.root, incdir.getPath());
					FileUtils.cleanDirectory(ct);
					FileUtils.assertDirectory(ct);
					for (File f : config.includeFrom) {
						for (File i : FileUtils.findFilesMatching(f, "*.js")) {
							FileUtils.copy(i, ct);
							asm.includeJS(new File(incdir, i.getName()));
						}
					}
				}
				generateHTML(asm);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			if (config.openHTML) {
				try {
					Desktop.getDesktop().open(config.html);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
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
			FileOutputStream fos = new FileOutputStream(ty);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
			RepositoryVisitor dumper = new TypeDumper(pw);
			repository.traverse(dumper);
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
		jse = new JSEnvironment(config.jsDir(), pkgs);
		bce = new ByteCodeEnvironment();
		populateBCE(bce);

		StackVisitor jsstack = new StackVisitor();
		new JSGenerator(repository, jse, jsstack, eventMap);

		if (config.generateJS)
			repository.traverseWithHSI(jsstack);

		if (errors.hasErrors()) {
			return true;
		}

		if (config.generateJS) {
			saveJSE(config.jsDir(), jse, bce);
			saveBCE(config.jvmDir(), bce);
		}

		return errors.hasErrors();
	}

	public boolean runUnitTests(Configuration config, Map<File, TestResultWriter> writers) {
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		if (config.generateJVM && config.unitjvm) {
			BCEClassLoader bcl = new BCEClassLoader(bce);
			for (File f : config.includeFrom)
				bcl.addClassesFrom(f);
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl, allTemplates);
			jvmRunner.runAllUnitTests(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.unitjs) {
			JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates, this.getClass().getClassLoader());
			jsRunner.runAllUnitTests(writers);
			jsRunner.reportErrors(errors);
		}

		return errors.hasErrors();
	}

	public boolean runSystemTests(Configuration config, Map<File, TestResultWriter> writers) {
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		BCEClassLoader bcl = null;
		if (config.generateJVM && config.systemjvm) {
			bcl = new BCEClassLoader(bce);
			for (File f : config.includeFrom)
				bcl.addClassesFrom(f);
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl, allTemplates);
			jvmRunner.runAllSystemTests(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.systemjs) {
			ClassLoader cl = bcl != null ? bcl : this.getClass().getClassLoader();
			JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates, cl);
			jsRunner.runAllSystemTests(writers);
			jsRunner.reportErrors(errors);
		}

		return errors.hasErrors();
	}

	public void storeAssemblies(AssemblyVisitor storer) {
		if (jse != null)
			repository.traverseAssemblies(errors, jse, storer);
	}

	public void generateHTML(FLASAssembler asm) {
		repository.traverseAssemblies(errors, jse, new AssemblyVisitor() {
			private List<String> inits = new ArrayList<>();
			private List<String> css = new ArrayList<>();
			private List<String> js = new ArrayList<>();
			private List<ContentObject> temps = new ArrayList<>();

			@Override
			public void visitAssembly(ApplicationAssembly a) {
			}
			
			@Override
			public void visitResource(String name, ZipInputStream zis) throws IOException {
			}
			
			@Override
			public void visitPackage(String pkg) {
				inits.add(pkg);
			}
			
			@Override
			public void compiledPackageFile(File f) {
				String jsdir = config.jsDir().getPath();
				if (config.root != null)
					jsdir = jsdir.replace(config.root.getPath(), "");
				js.add(jsdir.replaceAll("^/*", "") + "/" + f.getName());
			}
			
			@Override
			public void visitCardTemplate(String cardName, InputStream is, long length) throws IOException {
				String s = FileUtils.readNStream(length, is);
				ContentObject co = new ContentObject() {
					@Override
					public String url() {
						return null;
					}
					
					@Override
					public String asString() {
						return "    <template id='" + cardName + "'>\n" + s + "\n    </template>\n";
					}
				};
				temps.add(co);
			}
			
			@Override
			public void visitCSS(String name, ZipInputStream zis, long length) throws IOException {
				css.add(name);
			}
			
			@Override
			public void leaveAssembly(ApplicationAssembly a) throws IOException {
				asm.begin();
				asm.title(a.getTitle());
				asm.afterTitle();
				for (ContentObject co : temps)
					asm.templates(co);
				asm.beginCss();
				for (String c : css)
					asm.css("css/" + c);
				asm.endCss();
				asm.beginJs();
				for (String j : js)
					asm.javascript(j);
				asm.endJs();
				asm.beginInit();
				asm.initializer(new CardInitializer() {
					@Override
					public Iterable<String> packages() {
						return inits;
					}
					
					@Override
					public String mainCard() {
						return a.mainCard();
					}
				});
				asm.endInit();
				asm.end();
			}
			
			@Override
			public void traversalDone() throws Exception {
			}
		}); 
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
		if (jvmDir != null) {
			FileUtils.assertDirectory(jvmDir);
			try {
				// Doing this makes things clean, but stops you putting multiple things in the
				// same directory
				// FileUtils.cleanDirectory(writeJVM);
				for (ByteCodeCreator bcc : bce.all()) {
					File wto = new File(jvmDir, FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
					bcc.writeTo(wto);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition) null, ex.toString());
			}
		}
	}

	public void saveJSE(File jsDir, JSEnvironment jse, ByteCodeEnvironment bce) {
		if (jsDir != null) {
			try {
				jse.writeAllTo(jsDir);
//				jse.asivm();
				jse.generate(bce);
			} catch (Exception ex) {
				ex.printStackTrace();
				errors.message((InputPosition) null, ex.toString());
			}
//			jse.dumpAll(true);
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