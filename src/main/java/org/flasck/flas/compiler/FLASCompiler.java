package org.flasck.flas.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.sugardetox.SugarDetox;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class FLASCompiler implements ScriptCompiler {
	static final Logger logger = LoggerFactory.getLogger("Compiler");
	private boolean dumpTypes = false;
	private final List<File> pkgdirs = new ArrayList<File>();
	private ByteCodeEnvironment bce = new ByteCodeEnvironment();
	private File writeRW;
	private DroidBuilder builder;
	private File writeFlim;
	private File writeDepends;
	private File writeHSIE;
	private File trackTC;
	private File writeJS;
	private File writeTestReports;

	public void searchIn(File file) {
		pkgdirs.add(file);
	}
	
	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put it
	public void writeDroidTo(File file, boolean andBuild) {
		if (file.getPath().equals("null"))
			return;
		builder = new DroidBuilder(file, bce);
		if (!andBuild)
			builder.dontBuild();
		builder.init();
	}

	public void writeRWTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeRW = file;
	}

	public void writeFlimTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeFlim = file;
	}

	public void writeDependsTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeDepends = file;
	}

	public void writeHSIETo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeHSIE = file;
	}

	public void trackTC(File file) {
		this.trackTC = new File(file, "types");
	}

	public void writeJSTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeJS = file;
	}

	public void writeTestReportsTo(File file) {
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		this.writeTestReports = file;
	}

	public void dumpTypes() {
		this.dumpTypes = true;
	}
	
	// The objective of this method is to convert an entire package directory at one go
	// Thus the entire context of this is a single package
	public boolean compile(File dir) throws ErrorResultException, IOException {
		String inPkg = dir.getName();
		if (!dir.isDirectory()) {
			ErrorResult errors = new ErrorResult();
			errors.message((InputPosition)null, "there is no input directory " + dir);
			throw new ErrorResultException(errors);
		}
		File writeTo = new File((writeJS!=null?writeJS:dir), inPkg + ".js");
		File exportTo = new File((writeFlim!=null?writeFlim:dir), inPkg + ".flim");
		System.out.println("compiling package " + inPkg + " to " + writeTo);
			
		boolean failed = false;
		ErrorResult errors = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = new Scope(null);
		final List<String> pkgs = new ArrayList<String>();
		pkgs.add(inPkg);
		
		for (File f : FileUtils.findFilesMatching(dir, "*.fl")) {
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
			return false;
		
		// 3. Rework any "syntatic sugar" forms into their proper forms
		new SugarDetox(errors).detox(scope);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
		
		FileWriter wjs = null;
		FileOutputStream wex = null;
		PrintWriter tcPW = null;
		boolean success = false;
		try {
			ImportPackage rootPkg = Builtin.builtins();
			final Rewriter rewriter = new Rewriter(errors, pkgdirs, rootPkg);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors, rewriter);
			final DroidGenerator dg = new DroidGenerator(hsie, builder);

			rewriter.importPackage1(rootPkg);
			
			rewriter.rewritePackageScope(inPkg, scope);
			abortIfErrors(errors);
			
			if (writeRW != null) {
				rewriter.writeGeneratableTo(new File(writeRW, "analysis.txt"));
			}

			Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>(rewriter.functions);

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

			if (writeRW != null) {
				rewriter.writeGeneratableFunctionsTo(new File(writeRW, "functions.txt"), functions);
			}

			// 7. Do dependency analysis on functions and group them together in orchards
			DependencyAnalyzer da = new DependencyAnalyzer();
			List<Set<RWFunctionDefinition>> defns = da.analyze(functions);
			abortIfErrors(errors);
			if (writeDepends != null)
				writeDependencies(da, defns);
			
			for (Set<RWFunctionDefinition> orch : defns) {
				showDefns(orch);
			}
			
			// 8. Now process each set
			//   a. convert functions to HSIE
			//   b. typechecking

			TypeChecker2 tc2 = new TypeChecker2(errors, rewriter);
			if (trackTC != null) {
				tcPW = new PrintWriter(trackTC);
				tc2.trackTo(tcPW);
			}
			tc2.populateTypes();
			abortIfErrors(errors);

			PrintWriter hsiePW = null;
			if (writeHSIE != null) {
				hsiePW = new PrintWriter(new File(writeHSIE, inPkg));
			}

			for (Set<RWFunctionDefinition> d : defns) {
				hsie.createForms(d);
			}
			
			for (Set<RWFunctionDefinition> d : defns) {
				
				// 8a. Convert each orchard to HSIE
				Set<HSIEForm> forms = hsie.orchard(d);
				abortIfErrors(errors);
				dumpForms(hsiePW, forms);
				
				// 8b. Typecheck all the methods together
				tc2.typecheck(forms);
				abortIfErrors(errors);
			}
			if (hsiePW != null)
				hsiePW.close();

			// 9. Generate code from templates
			final TemplateGenerator tgen = new TemplateGenerator(rewriter, dg);
			tgen.generate(rewriter, target);
			
			// 10. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc2, hsie.allForms());
			abortIfErrors(errors);

			// 11. Save learned state for export
			tc2.writeLearnedKnowledge(exportTo, inPkg, dumpTypes);

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
				return false;
			}
			target.writeTo(wjs);

			// 13b. Issue Droid
			try {
				dg.write();
			} catch (Exception ex) {
				System.err.println("Cannot write to " + builder.androidDir + ": " + ex.getMessage());
				ex.printStackTrace();
				return false;
			}
			abortIfErrors(errors);

			success = true;
		} finally {
			try { if (wjs != null) wjs.close(); } catch (IOException ex) {}
			try { if (wex != null) wex.close(); } catch (IOException ex) {}
			if (tcPW != null)
				tcPW.close();
		}

		for (File f : FileUtils.findFilesMatching(dir, "*.ut")) {
			MultiTextEmitter results;
			boolean close;
			if (writeTestReports != null && writeTestReports.isDirectory()) {
				results = new MultiTextEmitter(new File(writeTestReports, f.getName().replaceFirst(".ut$", ".txt")));
				close = true;
			} else {
				results = new MultiTextEmitter(System.out);
				close = false;
			}
			
			try {
				UnitTestRunner utr = new UnitTestRunner(results, this, f);
				utr.run();
			} finally {
			if (close)
				results.close();
			}
		}
		
		// TODO: we also want to support general *.pt (protocol test) files and run them against cards/services that claim to support that protocol
		
		return success;
	}


	@Override
	public List<Class<?>> createJVM(String pkg, String flas) {
		// TODO Auto-generated method stub
		return null;
	}

	private void writeDependencies(DependencyAnalyzer da, List<Set<RWFunctionDefinition>> defns) throws IOException {
		PrintWriter pw = new PrintWriter(new File(writeDepends, "depends.txt"));
		da.dump(pw);
		for (Set<RWFunctionDefinition> s : defns) {
			for (RWFunctionDefinition d : s) {
				pw.println(d.name());
			}
			pw.println("-----");
		}
		pw.close();
	}

	private void showDefns(Set<RWFunctionDefinition> defns) {
		for (RWFunctionDefinition d : defns)
			logger.info("  " + d.name());
	}

	private void dumpForms(PrintWriter hsiePW, Set<HSIEForm> hs) {
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

	private void handleCurrying(ApplyCurry curry, TypeChecker2 tc, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			curry.rewrite(tc, h);
	}

	private void generateForms(Generator gen, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection) {
			gen.generate(h);
		}
	}

	public ByteCodeEnvironment getBCE() {
		return bce;
	}

	public void setDumpTypes(boolean b) {
		this.dumpTypes = b;
	}

	public DroidBuilder getBuilder() {
		return builder;
	}
}
