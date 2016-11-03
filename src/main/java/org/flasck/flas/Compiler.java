package org.flasck.flas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.droidgen.DroidBuilder;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.sugardetox.SugarDetox;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.Orchard;
import org.zinutils.utils.FileUtils;

public class Compiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	
	public static void main(String[] args) {
		setLogLevels();
		Compiler compiler = new Compiler();
		try {
			for (int i=0;i<args.length;i++) {
				String f = args[i];
				int hasMore = args.length-i-1;
				if (f.startsWith("-")) {
					if (f.equals("--dump"))
						compiler.dumpTypes = true;
					else if (f.equals("--wflim")) {
						if (hasMore == 0) {
							System.out.println("--wflim <dir>");
							System.exit(1);
						}
						compiler.writeFlimTo(new File(args[++i]));
					} else if (f.equals("--flim")) {
						if (hasMore == 0) {
							System.out.println("--flim <dir>");
							System.exit(1);
						}
						compiler.searchIn(new File(args[++i]));
					} else if (f.equals("--jsout")) {
						if (hasMore == 0) {
							System.out.println("--jsout <dir>");
							System.exit(1);
						}
						compiler.writeJSTo(new File(args[++i]));
					} else if (f.equals("--android")) {
						if (hasMore == 0) {
							System.out.println("--android <build-dir>");
							System.exit(1);
						}
						compiler.writeDroidTo(new File(args[++i]));
					} else {
						boolean matched = false;
						if (compiler.builder != null) { // consider droid build options
							matched = true;
							if (f.equals("--clean")) {
								compiler.builder.cleanFirst();
							} else if (f.equals("--jack")) {
								compiler.builder.useJack();
							} else if (f.equals("--jni")) {
								if (hasMore == 0) {
									System.out.println("--jni <arch>");
									System.exit(1);
								}
								compiler.builder.restrictJni(args[++i]);
							} else if (f.equals("--launch")) {
								if (hasMore == 0) {
									System.out.println("--launch <card>");
									System.exit(1);
								}
								compiler.builder.setLaunchCard(args[++i]);
							} else if (f.equals("--lib")) {
								if (hasMore == 0) {
									System.out.println("--lib <file|dir>");
									System.exit(1);
								}
								compiler.builder.useLib(args[++i]);
							} else if (f.equals("--maven")) {
								if (hasMore == 0) {
									System.out.println("--maven <mvn_entry>");
									System.exit(1);
								}
								compiler.builder.useMaven(args[++i]);
							} else if (f.equals("--css")) {
								if (hasMore == 0) {
									System.out.println("--css <file|dir>");
									System.exit(1);
								}
								compiler.builder.useCSS(args[++i]);
							} else if (f.equals("--package")) {
								if (hasMore == 0) {
									System.out.println("--package <local=ziniki:version>");
									System.exit(1);
								}
								compiler.builder.usePackage(args[++i]);
							} else
								matched = false;
						}
						if (!matched) {
							System.out.println("unknown option: " + f);
							compiler.success = false;
							break;
						}
					}
					continue;
				}
				try {
					compiler.compile(new File(f));
					if (!compiler.success)
						break;
				} catch (ErrorResultException ex) {
					try {
						ex.errors.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
					} catch (IOException ex2) {
						ex2.printStackTrace();
					}
					compiler.success = false;
					break;
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
			if (compiler.builder != null && compiler.success)
				compiler.builder.build();
		} catch (ArgumentException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}
		if (compiler.success) {
			System.out.println("done");
			System.exit(0);
		} else
			System.exit(1);
	}

	public static void setLogLevels() {
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("DroidGen").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("Rewriter").setLevel(Level.ERROR);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
	}

	private boolean success;
	private boolean dumpTypes = false;
	private final List<File> pkgdirs = new ArrayList<File>();
	private ByteCodeEnvironment bce = new ByteCodeEnvironment();
	private DroidBuilder builder;
	private File writeFlim;
	private File writeHSIE;
	private File writeJS;

	public void searchIn(File file) {
		pkgdirs.add(file);
	}
	
	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put it
	public void writeDroidTo(File file) {
		if (file.getPath().equals("null"))
			return;
		builder = new DroidBuilder(file, bce);
		builder.init();
	}

	public void writeFlimTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeFlim = file;
	}

	public void writeHSIETo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeHSIE = file;
	}

	public void writeJSTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeJS = file;
	}

	public void dumpTypes() {
		this.dumpTypes = true;
	}
	
	// The objective of this method is to convert an entire package directory at one go
	// Thus the entire context of this is a single package
	public void compile(File file) throws ErrorResultException, IOException {
		String inPkg = file.getName();
		if (!file.isDirectory()) {
			ErrorResult errors = new ErrorResult();
			errors.message((InputPosition)null, "there is no input directory " + file);
			throw new ErrorResultException(errors);
		}
		File writeTo = new File((writeJS!=null?writeJS:file), inPkg + ".js");
		File exportTo = new File((writeFlim!=null?writeFlim:file), inPkg + ".flim");
		System.out.println("compiling package " + inPkg + " to " + writeTo);
			
		boolean failed = false;
		ErrorResult errors = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = new Scope(null);
		final List<String> pkgs = new ArrayList<String>();
		pkgs.add(inPkg);
		
		for (File f : FileUtils.findFilesMatching(file, "*.fl")) {
			System.out.println(" > " + f.getName());
			FileReader r = null;
			try {
				r = new FileReader(f);

				// 1. Use indentation to break the input file up into blocks
				List<Block> blocks = makeBlocks(errors, f.getName(), r);
				
				// 2. Use the parser factory and story to convert blocks to a package definition
				storyProc.process(inPkg, scope, errors, blocks, true);
			} catch (IOException ex1) {
				failed = true;
				ex1.printStackTrace();
			} finally {
				if (r != null) try { r.close(); } catch (IOException ex3) {}
			}
		}

		if (errors.hasErrors())
			throw new ErrorResultException(errors);

		if (failed)
			return;
		
		// 3. Rework any "syntatic sugar" forms into their proper forms
		new SugarDetox(errors).detox(scope);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
		
		FileWriter wjs = null;
		FileOutputStream wex = null;
		success = false;
		try {
			ImportPackage rootPkg = Builtin.builtins();
			final Rewriter rewriter = new Rewriter(errors, pkgdirs, rootPkg);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors);
			final DroidGenerator dg = new DroidGenerator(hsie, builder);

			rewriter.importPackage1(rootPkg);
			
			rewriter.rewritePackageScope(inPkg, scope);
			abortIfErrors(errors);

			Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>(rewriter.functions);

			// 4. Prepare Typechecker & load types
			TypeChecker tc = new TypeChecker(errors);
			tc.populateTypes(rewriter);
			abortIfErrors(errors);
		
			// 5. Generate Class Definitions
			JSTarget target = new JSTarget(inPkg);
			Generator gen = new Generator(target);

			dg.generateAppObject();
			
			for (Entry<String, RWStructDefn> sd : rewriter.structs.entrySet()) {
				gen.generate(sd.getValue());
				dg.generate(sd.getValue());
			}
			for (Entry<String, CardGrouping> kv : rewriter.cards.entrySet()) {
				CardGrouping grp = kv.getValue();
				gen.generate(kv.getKey(), grp);
				dg.generate(kv.getKey(), grp);
				for (ContractGrouping ctr : grp.contracts) {
					RWContractImplements ci = rewriter.cardImplements.get(ctr.implName);
					if (ci == null)
						throw new UtilException("Could not find contract implements for " + ctr.implName);
					RWContractDecl cd = rewriter.contracts.get(ci.name());
					if (cd == null)
						throw new UtilException("Could not find contract decl for " + ci.name());
					Set<RWContractMethodDecl> requireds = new TreeSet<RWContractMethodDecl>(); 
					for (RWContractMethodDecl m : cd.methods) {
						if (m.dir.equals("down") && m.required)
							requireds.add(m);
					}
					for (RWMethodDefinition m : ci.methods) {
						boolean haveMethod = false;
						for (RWContractMethodDecl dc : cd.methods) {
							if (dc.dir.equals("down") && (ctr.implName +"." + dc.name).equals(m.name())) {
								if (dc.args.size() != m.nargs())
									errors.message(m.location(), "incorrect number of arguments in declaration, expected " + dc.args.size());
								requireds.remove(dc);
								haveMethod = true;
								break;
							}
						}
						if (!haveMethod)
							errors.message(m.location(), "cannot implement down method " + m.name() + " because it is not in the contract declaration");
					}
					if (!requireds.isEmpty()) {
						for (RWContractMethodDecl d : requireds)
							errors.message(ci.location(), ci.name() + " does not implement " + d);
					}
				}
			}
			for (Entry<String, RWContractDecl> c : rewriter.contracts.entrySet()) {
				dg.generateContractDecl(c.getKey(), c.getValue());
			}
			for (Entry<String, RWContractImplements> ci : rewriter.cardImplements.entrySet()) {
				gen.generateContract(ci.getKey(), ci.getValue());
				dg.generateContractImpl(ci.getKey(), ci.getValue());
			}
			for (Entry<String, RWContractService> cs : rewriter.cardServices.entrySet()) {
				gen.generateService(cs.getKey(), cs.getValue());
				dg.generateService(cs.getKey(), cs.getValue());
			}
			for (Entry<String, RWHandlerImplements> hi : rewriter.callbackHandlers.entrySet()) {
				gen.generateHandler(hi.getKey(), hi.getValue());
				dg.generateHandler(hi.getKey(), hi.getValue());
			}

//			System.out.println("defns = " + rewriter.functions.keySet());
			
			// 6. Convert methods to functions
			MethodConvertor mc = new MethodConvertor(errors, rewriter);
			mc.convertContractMethods(rewriter, functions, rewriter.methods);
			mc.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
			mc.convertStandaloneMethods(rewriter, functions, rewriter.standalone.values());
			abortIfErrors(errors);

			// 7. Do dependency analysis on functions and group them together in orchards
			List<Orchard<RWFunctionDefinition>> defns = new DependencyAnalyzer(errors).analyze(functions);
			abortIfErrors(errors);
			
//			System.out.println("tree = " + defns);

			// 8. Now process each orchard
			//   a. convert functions to HSIE
			//   b. typechecking

			PrintWriter hsiePW = null;
			if (writeHSIE != null) {
				hsiePW = new PrintWriter(new File(writeHSIE, inPkg));
			}

			for (Orchard<RWFunctionDefinition> d : defns) {
				hsie.createForms(d);
			}
			
			for (Orchard<RWFunctionDefinition> d : defns) {
				
				// 8a. Convert each orchard to HSIE
				Set<HSIEForm> oh = hsie.orchard(d);
				abortIfErrors(errors);
				dumpOrchard(hsiePW, oh);
				
				// 8b. Typecheck all the methods together
				tc.typecheck(oh);
				abortIfErrors(errors);
			}
			if (hsiePW != null)
				hsiePW.close();

			// 9. Generate code from templates
			final TemplateGenerator tgen = new TemplateGenerator(rewriter, dg);
			tgen.generate(rewriter, target);
			
			// 10. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc, hsie.allForms());
			abortIfErrors(errors);

			// 11. Save learned state for export
			tc.writeLearnedKnowledge(exportTo, inPkg, dumpTypes);

			// 12. generation of JSForms
			generateForms(gen, hsie.allForms());
			dg.generate(hsie.allForms());
			abortIfErrors(errors);

			// 13. Write final outputs
			
			// 13a. Issue JavaScript
			try {
				wjs = new FileWriter(writeTo);
			} catch (IOException ex) {
				System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
				return;
			}
			target.writeTo(wjs);

			// 13b. Issue Droid
			try {
				dg.write();
			} catch (Exception ex) {
				System.err.println("Cannot write to " + builder.androidDir + ": " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			abortIfErrors(errors);

			success = true;
		} finally {
			try { if (wjs != null) wjs.close(); } catch (IOException ex) {}
			try { if (wex != null) wex.close(); } catch (IOException ex) {}
		}

		// TODO: look for *.ut (unit test) and *.pt (protocol test) files and compile & execute them, too.
	}

	private void dumpOrchard(PrintWriter hsiePW, Set<HSIEForm> hs) {
		if (hsiePW == null)
			return;
		
		boolean first = true;
		for (HSIEForm h : hs) {
			if (first)
				first = false;
			else
				hsiePW.println("-------");
			h.dump(hsiePW);
		}
		hsiePW.println("=======");
	}

	// Just obtain a parse tree 
	public StoryRet parse(String inPkg, String input) {
		ErrorResult er = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = new Scope(null);
		StoryRet ret = new StoryRet(er, scope);
		StringReader r = null;
		try {
			r = new StringReader(input);

			// 1. Use indentation to break the input file up into blocks
			List<Block> blocks = makeBlocks(er, "-", r);
			if (er.hasErrors())
				return ret;
			
			// 2. Use the parser factory and story to convert blocks to a package definition
			storyProc.process(inPkg, scope, er, blocks, true);
			return ret;
		} catch (IOException ex1) {
			ex1.printStackTrace();
			return null;
		} finally {
			r.close();
		}
	}

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	@SuppressWarnings("unchecked")
	private List<Block> makeBlocks(ErrorResult er, String file, Reader r) throws IOException {
		Object res = Blocker.block(file, r);
		if (res instanceof ErrorResult) {
			er.merge((ErrorResult) res);
			return null;
		}
		return (List<Block>) res;
	}

	private void handleCurrying(ApplyCurry curry, TypeChecker tc, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			curry.rewrite(tc, h);
	}

	private void generateForms(Generator gen, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			gen.generate(h);
	}

	public ByteCodeEnvironment getBCE() {
		return bce;
	}
}
