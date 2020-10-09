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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.Configuration;
import org.flasck.flas.LSPTaskQueue;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.assembler.BuildApplicationAssembly;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.templates.EventBuilder;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.lsp.CompileTask;
import org.flasck.flas.lsp.Root;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.EventHolder;
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
import org.flasck.jvm.assembly.ApplicationAssembly;
import org.flasck.jvm.assembly.FLASAssembler;
import org.flasck.jvm.fl.FLCommonEvalContext;
import org.flasck.jvm.fl.TrivialEnvironment;
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
	public static boolean backwardCompatibilityMode = true;
	private final Configuration config;
	private final ErrorReporter errors;
	private final Repository repository;
	private final Splitter splitter;
	private final Map<String, Root> roots = new TreeMap<>();
	private TaskQueue tasks;
	private LanguageClient lsp;
	private DirectedAcyclicGraph<String> pkgs;
	private JSEnvironment jse;
	private Map<EventHolder, EventTargetZones> eventMap;
	private ByteCodeEnvironment bce;

	public FLASCompiler(Configuration config, ErrorReporter errors, Repository repository) {
		this.config = config;
		this.errors = errors;
		this.repository = repository;
		this.splitter = new Splitter(x -> errors.message(new InputPosition(x.file, 0, 0, null, x.text), x.message));
	}

	public void taskQueue(LSPTaskQueue tasks) {
		this.tasks = tasks;
	}

	public void connect(LanguageClient client) {
		this.lsp = client;
		errors.connect(client);
	}
	
	public boolean loadFLIM() {
		LoadBuiltins.applyTo(errors, repository);
		pkgs = new DirectedAcyclicGraph<>();
		if (config.flimdir() != null) {
			new FlimReader(errors, repository).read(pkgs, config.flimdir(), config.inputs);
			if (errors.hasErrors())
				return true;
		}
		return false;
	}
	
	public void addRoot(String rootUri) {
		try {
			URI uri = new URI(rootUri + "/");
			Root root = new Root(uri);
			if (roots.containsKey(root.root.getPath()))
				return;
			lsp.logMessage(new MessageParams(MessageType.Log, "opening root " + root.root));
			roots.put(root.root.getPath(), root);
			root.gatherFiles(lsp);
			compileAll(root);
		} catch (URISyntaxException ex) {
			lsp.logMessage(new MessageParams(MessageType.Error, "could not open " + rootUri));
		}
	}
	
	private void compileAll(Root root) {
		for (URI u : root) {
			tasks.submit(new CompileTask(this, u, null));
		}
	}

	public void setCardsFolder(String cardsFolder) {
//		this.submitter.setCardsFolder(cardsFolder);
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
	
	@Override
	public void parse(URI uri, String text) {
		System.out.println("Compiling " + uri);
		errors.beginProcessing(uri);
		repository.parsing(uri);
		parseOne(uri);
		repository.done();
		errors.doneProcessing();
		if (tasks.isReady()) {
			// if there were previously files that were corrupt, try compiling them again
			List<URI> broken = new ArrayList<>(errors.getAllBrokenURIs());
			for (URI b : broken) {
				parseOne(b);
			}
			
			// If some are still broken, we cannot proceed
			if (!errors.getAllBrokenURIs().isEmpty())
				return;

			/*
			for (File ws : workspaces) {
				File web = new File(ws, cardsFolder);
				if (web.isDirectory())
					compiler.splitWeb(web);
			}
			*/

			// do the rest of the compilation
			sendRepo();
		}
	}
	
	private void parseOne(URI uri) {
		Root root = findRoot(uri);
		if (root == null) {
			lsp.logMessage(new MessageParams(MessageType.Error, "could not find root for " + uri));
			return;
		}
		File file = new File(uri.getPath());
		String inPkg = file.getParentFile().getName();
		String name = file.getName();
		String type = FileUtils.extension(name);
		
		switch (type) {
		case ".fl":
			parseFL(file, inPkg, name);
		case ".ut":
			break;
		case ".st":
			break;
		case ".fa":
			parseFA(file, inPkg, name);
		default:
			lsp.logMessage(new MessageParams(MessageType.Log, "could not compile " + FileUtils.makeRelativeTo(file, root.root)));
		}

	}

	private Root findRoot(URI uri) {
		String path = uri.getPath();
		for (Entry<String, Root> e : roots.entrySet()) {
			if (path.startsWith(e.getKey()))
				return e.getValue();
		}
		return null;
	}

	public void parseFL(File file, String inPkg, String name) {
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer)repository);
		lsp.logMessage(new MessageParams(MessageType.Log, "compiling " + name + " in " + inPkg));
		flp.process(file);
	}
	
	public void parseFA(File file, String inPkg, String name) {
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		lsp.logMessage(new MessageParams(MessageType.Log, "compiling " + file.getName() + " in " + inPkg));
		fap.process(file);
	}
	
	private void sendRepo() {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			repository.dumpTo(pw);
			pw.close();
			LineNumberReader lnr = new LineNumberReader(new StringReader(sw.toString()));
			String s;
			while ((s = lnr.readLine()) != null) {
				lsp.logMessage(new MessageParams(MessageType.Log, s));
			}
		} catch (Exception ex) {
			lsp.logMessage(new MessageParams(MessageType.Log, "Error reading repo: " + ex));
		}
	}

	public void parse(File dir) {
		if (!dir.isDirectory()) {
			errors.message((InputPosition)null, "there is no input directory " + dir);
			return;
		}

		String inPkg = dir.getName();
		checkPackageName(inPkg);
		System.out.println(" |" + inPkg);
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer)repository);
		List<File> files = FileUtils.findFilesMatching(dir, "*.fl");
		files.sort(new FileNameComparator());
		for (File f : files) {
			System.out.println("    " + f.getName());
			flp.process(f);
		}
		List<File> utfiles = FileUtils.findFilesMatching(dir, "*.ut");
		utfiles.sort(new FileNameComparator());
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
		for (File f : fafiles) {
			System.out.println("    " + f.getName());
			fap.process(f);
		}
		List<File> stfiles = FileUtils.findFilesMatching(dir, "*.st");
		stfiles.sort(new FileNameComparator());
		for (File f : stfiles) {
			System.out.println("    " + f.getName());
			String file = FileUtils.dropExtension(f.getName());
			UnitTestFileName stfn = new UnitTestFileName(new PackageName(inPkg), "_st_" + file);
			SystemTest st = new SystemTest(stfn);
			repository.systemTest(errors, st);
			ParsingPhase parser = new ParsingPhase(errors, stfn, st, (TopLevelDefinitionConsumer)repository);
			parser.process(f);
		}
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
			return true;
		
		if (resolve())
			return true;
		
		FunctionGroups ordering = lift();
		analyzePatterns();
		if (errors.hasErrors())
			return true;
		
		if (config.doTypeCheck) {
			doTypeChecking(ordering);
			if (errors.hasErrors())
				return true;
			try {
				dumpTypes(config.writeTypesTo);
			} catch (FileNotFoundException ex) {
				errors.message((InputPosition)null, "cannot open file " + config.writeTypesTo);
				return true;
			}
			if (errors.hasErrors())
				return true;
		}

		if (convertMethods())
			return true;
		
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
						System.out.println("invalid order: package " + input + " depends on " + s + " which has not been processed");
					return true;
				}
			}
		}
		
		if (generateCode(config, pkgs))
			return true;
		
		Map<File, TestResultWriter> testWriters = new HashMap<>();
		try {
			if (runUnitTests(config, testWriters))
				return true;

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
				generateHTML(asm, config);
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
			JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates);
			jsRunner.runAllUnitTests(writers);
			jsRunner.reportErrors(errors);
		}

		return errors.hasErrors();
	}

	public boolean runSystemTests(Configuration config, Map<File, TestResultWriter> writers) {
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		if (config.generateJVM && config.systemjvm) {
			BCEClassLoader bcl = new BCEClassLoader(bce);
			for (File f : config.includeFrom)
				bcl.addClassesFrom(f);
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl, allTemplates);
			jvmRunner.runAllSystemTests(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.systemjs) {
			JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates);
			jsRunner.runAllSystemTests(writers);
			jsRunner.reportErrors(errors);
		}

		return errors.hasErrors();
	}

	public void storeAssemblies(AssemblyVisitor storer) {
		if (jse != null)
			repository.traverseAssemblies(errors, jse, storer);
	}

	public void generateHTML(FLASAssembler asm, Configuration config) {
		final File root = config.root;
		final File html = config.html;
		final File cssdir = new File(html.getParent(), "css");
		repository.traverseAssemblies(errors, jse, new BuildApplicationAssembly(new FLCommonEvalContext(new TrivialEnvironment())) {
			@Override
			protected ContentObject upload(String name, File f, String ctype) {
				return new ContentObject() {
					
					@Override
					public String url() {
						String jsdir = config.jsDir().getPath();
						if (root != null)
							jsdir = jsdir.replace(root.getPath(), "");
						return jsdir.replaceAll("^/*", "") + "/" + name;
					}
					
					@Override
					public String asString() {
						return FileUtils.readFile(f);
					}
				};
			}

			@Override
			protected ContentObject upload(String name, InputStream is, long length, boolean b, String ctype) throws IOException {
				String val = FileUtils.readNStream(length, is);
				return new ContentObject() {
					
					@Override
					public String url() {
						if ("text/css".equals(ctype)) {
							FileUtils.assertDirectory(cssdir);
							FileUtils.writeFile(new File(cssdir, name), val);
							return "css/" + name;
						} else {
							return name;
						}
					}
					
					@Override
					public String asString() {
						return val;
					}
				};
			}

			@Override
			protected void save(ApplicationAssembly assembly) {
				try {
					asm.assemble(assembly);
				} catch (Exception ex) {
					System.out.println(ex.toString());
				}
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
							ret.put(ze.getName().replace(".html", ""), new String(FileUtils.readAllStream(zis), Charset.forName("UTF-8")));
					}
				}
			}
		} catch (IOException ex) {
			errors.message(((InputPosition)null), "internal error reading templates from splitter");
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
					File wto = new File(jvmDir,
							FileUtils.convertDottedToSlashPath(bcc.getCreatedName()) + ".class");
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

	public void reportException(Throwable ex) {
		errors.reportException(ex);
	}
}