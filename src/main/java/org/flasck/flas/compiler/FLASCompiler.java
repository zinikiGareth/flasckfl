package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.flasck.flas.Configuration;
import org.flasck.flas.assembler.BuildApplicationAssembly;
import org.flasck.flas.assembler.FLASAssembler;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.compiler.templates.EventBuilder;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.RepositoryLifter;
import org.flasck.flas.method.ConvertRepositoryMethods;
import org.flasck.flas.parsedForm.EventHolder;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.assembly.BuildAssembly;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.AssemblyVisitor;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.TypeDumper;
import org.flasck.flas.testrunner.JSRunner;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.flas.testrunner.TestResultWriter;
import org.flasck.jvm.J;
import org.flasck.jvm.fl.FLCommonEvalContext;
import org.flasck.jvm.fl.TrivialEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.deployment.concepts.ApplicationAssembly;
import org.ziniki.interfaces.ContentObject;
import org.ziniki.splitter.SplitMetaData;
import org.ziniki.splitter.Splitter;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FLASCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	public static boolean backwardCompatibilityMode = true;
	private final ErrorReporter errors;
	private final Repository repository;
	private final Splitter splitter;
//	private final DroidBuilder builder = new DroidBuilder();
	private JSEnvironment jse;
	private Map<EventHolder, EventTargetZones> eventMap;
	private ByteCodeEnvironment bce;

	public FLASCompiler(ErrorReporter errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
		this.splitter = new Splitter(x -> errors.message(new InputPosition(x.file, 0, 0, x.text), x.message));
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
			UnitTestPackage utp = new UnitTestPackage(new InputPosition(file, 1, 0, ""), utfn);
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
	
	public boolean generateCode(Configuration config) {
		jse = new JSEnvironment(config.jsDir());
		bce = new ByteCodeEnvironment();
		populateBCE(bce);
		
		StackVisitor jsstack = new StackVisitor();
		new JSGenerator(repository, jse, jsstack, eventMap);
		StackVisitor jvmstack = new StackVisitor();
		new JVMGenerator(repository, bce, jvmstack, eventMap);

		if (config.generateJS)
			repository.traverseWithHSI(jsstack);
		if (config.generateJVM)
			repository.traverseWithHSI(jvmstack);
		
		if (errors.hasErrors()) {
			return true;
		}
		
		if (config.generateJS)
			saveJSE(config.jsDir(), jse);
		if (config.generateJVM)
			saveBCE(config.jvmDir(), bce);

		return errors.hasErrors();
	}
	
	public boolean runUnitTests(Configuration config) {
		Map<String, String> allTemplates = extractTemplatesFromWebs();
		Map<File, TestResultWriter> writers = new HashMap<>();
		if (config.generateJVM && config.unitjvm) {
			BCEClassLoader bcl = new BCEClassLoader(bce);
			JVMRunner jvmRunner = new JVMRunner(config, repository, bcl, allTemplates);
			jvmRunner.runAll(writers);
			jvmRunner.reportErrors(errors);
		}

		if (config.generateJS && config.unitjs) {
			JSRunner jsRunner = new JSRunner(config, repository, jse, allTemplates);
			jsRunner.runAll(writers);
			jsRunner.reportErrors(errors);
		}
		writers.values().forEach(w -> w.close());

		return errors.hasErrors();
	}

	public void storeAssemblies(AssemblyVisitor storer) {
		if (jse != null)
			repository.traverseAssemblies(errors, jse, storer);
	}

	public void generateHTML(FLASAssembler asm, File html) {
		System.err.println("herllo");
		repository.traverseAssemblies(errors, jse, new BuildApplicationAssembly(new FLCommonEvalContext(new TrivialEnvironment())) {
			@Override
			protected ContentObject upload(String name, File f, String ctype) {
				System.err.println("F called with " + name + " " + ctype);
				return new ContentObject() {
					
					@Override
					public String url() {
						// This is a complete hack!
						// TODO: We should go through the webzip options and find it and put it in a directory somewhere - probably called "css"
						if (ctype.startsWith("text/javascript")) {
							return "jsout/" + name;
						} else {
							return f.getPath();
						}
					}
					
					@Override
					public String asString() {
						return FileUtils.readFile(f);
					}
				};
			}

			@Override
			protected ContentObject upload(String name, InputStream byteArrayInputStream, long length, boolean b, String ctype) throws IOException {
				System.err.println("BS called with " + name + " " + ctype);
				String val = FileUtils.readNStream(length, byteArrayInputStream);
				return new ContentObject() {
					
					@Override
					public String url() {
						if ("text/css".equals(ctype)) {
							return "ui/" + name;
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
		ByteCodeCreator ec = bce.newClass("org.flasck.flas.testrunner.JVMRunner");
		ec.dontGenerate();
		ec.defineField(true, Access.PUBLIC, J.FLEVALCONTEXT, "cxt");
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

	public void saveJSE(File jsDir, JSEnvironment jse) {
		if (jsDir != null) {
			try {
				jse.writeAllTo(jsDir);
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

//	public DroidBuilder getBuilder() {
//		return builder;
//	}

	public void reportException(Throwable ex) {
		errors.reportException(ex);
	}
}