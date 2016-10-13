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
import java.util.HashMap;
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
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.droidgen.DroidBuilder;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.flim.PackageFinder;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.MethodInContext;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWD3Invoke;
import org.flasck.flas.rewrittenForm.RWD3PatternBlock;
import org.flasck.flas.rewrittenForm.RWD3Section;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWPropertyDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.stories.StoryRet;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.StringComparator;

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
					else if (f.equals("--flim")) {
						if (hasMore == 0) {
							System.out.println("--flim <file>");
							System.exit(1);
						}
						compiler.searchIn(new File(args[++i]));
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

	// TODO: move this into a separate class, like DOMFG used to be
	int nextFn = 1;
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
		ImportPackage rootPkg = Builtin.builtinScope();
		PackageFinder pkgFinder = new PackageFinder(pkgdirs, rootPkg);
		ErrorResult errors = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = new Scope(null, null);
		final List<String> pkgs = new ArrayList<String>();
		pkgs.add(inPkg);
		
		for (File f : FileUtils.findFilesMatching(file, "*.fl")) {
			System.out.println(" > " + f.getName());
			FileReader r = null;
			try {
				r = new FileReader(f);

				// 1. Use indentation to break the input file up into blocks
				List<Block> blocks = makeBlocks(f.getName(), r);
				
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
		
		FileWriter wjs = null;
		FileOutputStream wex = null;
		success = false;
		try {
			final Rewriter rewriter = new Rewriter(errors, pkgFinder);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors, rewriter);
			final DroidGenerator dg = new DroidGenerator(hsie, builder);

			rewriter.importPackage(rootPkg);
			
			rewriter.rewritePackageScope(inPkg, scope);
//			for (ScopeEntry se : entries)
//				rewriter.rewrite(se);
			abortIfErrors(errors);

			// 2. Prepare Typechecker & load types
			TypeChecker tc = new TypeChecker(errors);
			populateTypes(tc, rootPkg, pkgs);
			tc.populateTypes(rewriter);
			abortIfErrors(errors);
		
			// 3. Generate Class Definitions
			JSTarget target = new JSTarget(inPkg);
			Generator gen = new Generator(hsie, target);

			dg.generateAppObject();
			
			for (Entry<String, RWStructDefn> sd : rewriter.structs.entrySet()) {
				gen.generate(sd.getValue());
				dg.generate(sd.getValue());
			}
			for (Entry<String, CardGrouping> kv : rewriter.cards.entrySet()) {
				CardGrouping grp = kv.getValue();
				compileInits(hsie, tc, kv.getValue());

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
							if (dc.dir.equals("down") && (ctr.implName +"." + dc.name).equals(m.intro.name)) {
								if (dc.args.size() != m.intro.args.size())
									errors.message(m.intro.location, "incorrect number of arguments in declaration, expected " + dc.args.size());
								requireds.remove(dc);
								haveMethod = true;
								break;
							}
						}
						if (!haveMethod)
							errors.message(m.intro.location, "cannot implement down method " + m.intro.name + " because it is not in the contract declaration");
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
			
			// 4. Do dependency analysis on functions and group them together in orchards
			List<Orchard<RWFunctionDefinition>> defns = new DependencyAnalyzer(errors).analyze(rewriter.functions);
			abortIfErrors(errors);

			// 5. Now process each orchard
			//   a. convert functions to HSIE
			//   b. typechecking

			PrintWriter hsiePW = null;
			if (writeHSIE != null) {
				hsiePW = new PrintWriter(new File(writeHSIE, inPkg));
			}

			Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());
			for (Orchard<RWFunctionDefinition> d : defns) {
				// 6a. Convert each orchard to HSIE
				Orchard<HSIEForm> oh = hsieOrchard(errors, hsie, forms, d);
				abortIfErrors(errors);
				dumpOrchard(hsiePW, oh);
				
				// 6b. Typecheck an orchard together
				tc.typecheck(oh);
				abortIfErrors(errors);

				for (Tree<HSIEForm> t : oh)
					for (HSIEForm h : t.allNodes())
						forms.put(h.fnName, h);
			}
			if (hsiePW != null)
				hsiePW.close();

			// Now go back and handle all the "special cases" that sit at the top of the tree, such as methods and templates
			
			MethodConvertor mc = new MethodConvertor(errors, hsie, tc, rewriter.contracts);

			// 6. Typecheck contract methods and event handlers, convert to functions and compile to HSIE
			mc.convertContractMethods(rewriter, forms, rewriter.methods);
			mc.convertEventHandlers(rewriter, forms, rewriter.eventHandlers);
			mc.convertStandaloneMethods(rewriter, forms, rewriter.standalone.values());
			abortIfErrors(errors);
			
			// 7. Generate code from templates
			final TemplateGenerator tgen = new TemplateGenerator(rewriter, hsie, tc, curry, dg);
			tgen.generate(rewriter, target);

			// 8. D3 definitions may generate card functions; promote these onto the cards
			for (RWD3Invoke d3 : rewriter.d3s)
				promoteD3Methods(errors, rewriter, mc, forms, d3);
			
			// 9. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc, forms.values());
			abortIfErrors(errors);

			// 10. Save learned state for export
			tc.writeLearnedKnowledge(exportTo, inPkg, dumpTypes);

			// 11. generation of JSForms
			generateForms(gen, forms.values());
			dg.generate(forms.values());
			abortIfErrors(errors);

			// 12a. Issue JavaScript
			try {
				wjs = new FileWriter(writeTo);
			} catch (IOException ex) {
				System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
				return;
			}
			target.writeTo(wjs);

			// 12b. Issue Droid
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

	private void dumpOrchard(PrintWriter hsiePW, Orchard<HSIEForm> oh) {
		if (hsiePW == null)
			return;
		
		boolean first = true;
		for (Tree<HSIEForm> t : oh) {
			if (first)
				first = false;
			else
				hsiePW.println("-------");
			for (HSIEForm h : t.allNodes())
				h.dump(hsiePW);
		}
		hsiePW.println("=======");
	}

	// Just obtain a parse tree 
	public StoryRet parse(String inPkg, String input) throws ErrorResultException {
		ErrorResult er = new ErrorResult();
		final FLASStory storyProc = new FLASStory();
		final Scope scope = new Scope(null, null);
		StoryRet ret = new StoryRet(er, scope);
		StringReader r = null;
		try {
			r = new StringReader(input);

			// 1. Use indentation to break the input file up into blocks
			List<Block> blocks = makeBlocks("-", r);
			
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

	private void compileInits(HSIE hsie, TypeChecker tc, CardGrouping c) {
		for (Entry<String, Object> kv : c.inits.entrySet()) {
			if (kv.getValue() == null)
				continue;
			InputPosition loc = ((Locatable)kv.getValue()).location();
			HSIEForm form = hsie.handleExpr(kv.getValue(), CodeType.FUNCTION);
			kv.setValue(form);
			Type t = tc.checkExpr(form, new ArrayList<Type>(), new ArrayList<InputPosition>());
			if (t != null) {
				while (t.iam == WhatAmI.INSTANCE)
					t = t.innerType();
				// it should be the same as the field type
				for (RWStructField sf : c.struct.fields) {
					if (sf.name.equals(kv.getKey())) {
						Type st = sf.type;
						while (st.iam == WhatAmI.INSTANCE)
							st = st.innerType();
						boolean ok = false;
						if (st instanceof RWUnionTypeDefn) {
							for (Type t1 : ((RWUnionTypeDefn)st).cases)
								if (t1.equals(t))
									ok = true;
						} else
							ok = st.equals(t);
						if (!ok)
							tc.errors.message(loc, "cannot initialize " + sf.name + " with value of type " + t);
					}
				}
			}
		}
	}

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	private void promoteD3Methods(ErrorResult errors, Rewriter rewriter, MethodConvertor mc, Map<String, HSIEForm> forms, RWD3Invoke d3) {
		Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>(new StringComparator()); 
		Object init = rewriter.structs.get("NilMap");
		RWStructDefn assoc = rewriter.structs.get("Assoc");
		RWStructDefn cons = rewriter.structs.get("Cons");
		RWStructDefn nil = rewriter.structs.get("Nil");
		RWFunctionDefinition tuple = rewriter.functions.get("()");
		RWStructDefn d3Elt = rewriter.structs.get("D3Element");
		ListMap<String, Object> byKey = new ListMap<String, Object>();
		for (RWD3PatternBlock p : d3.d3.patterns) {
			for (RWD3Section s : p.sections.values()) {
				if (!s.properties.isEmpty()) {
					Object pl = nil; // prepend to an empty list
					for (RWPropertyDefn prop : s.properties.values()) {
						// TODO: only create functions for things that depend on the class
						// constants can just be used directly
						FunctionLiteral efn = functionWithArgs(d3.d3.prefix, functions, CollectionUtils.listOf(new RWTypedPattern(null, d3Elt, null, d3.d3.iter)), prop.value);
						Object pair = new ApplyExpr(prop.location, tuple, new StringLiteral(prop.location, prop.name), efn);
						pl = new ApplyExpr(prop.location, cons, pair, pl);
					}
					byKey.add(s.name, new ApplyExpr(s.location, tuple, p.pattern, pl));
				}
				else if (!s.actions.isEmpty()) { // something like enter, that is a "method"
					RWFunctionIntro fi = new RWFunctionIntro(s.location, d3.d3.prefix + "._d3_" + d3.d3.name + "_" + s.name+"_"+p.pattern.text, new ArrayList<Object>(), new HashMap<>());
					RWMethodCaseDefn mcd = new RWMethodCaseDefn(fi);
					// TODO: big-divide: presumably we should rewrite the actions?
					mcd.messages.addAll(s.actions);
					RWMethodDefinition method = new RWMethodDefinition(fi);
					MethodInContext mic = new MethodInContext(rewriter, null, MethodInContext.EVENT, null, null, fi.name, HSIEForm.CodeType.CARD, method); // PROB NEEDS D3Action type
					mc.convertContractMethods(rewriter, forms, CollectionUtils.listOf(mic));
					byKey.add(s.name, new FunctionLiteral(fi.location, fi.name));
//					ls = new ApplyExpr(cons, new FunctionLiteral(fi.name), ls);
				} else { // something like layout, that is just a set of definitions
					// This function is generated over in DomFunctionGenerator, because it "fits" better there ...
				}
			}
		}
		for (Entry<String, List<Object>> k : byKey.entrySet()) {
			Object list = nil;
			List<Object> lo = k.getValue();
			for (int i=lo.size()-1;i>=0;i--)
				list = new ApplyExpr(null, cons, lo.get(i), list);
			init = new ApplyExpr(null, assoc, new StringLiteral(null, k.getKey()), list, init);
		}
		FunctionLiteral data = functionWithArgs(d3.d3.prefix, functions, new ArrayList<Object>(), d3.d3.data);
		init = new ApplyExpr(null, assoc, new StringLiteral(null, "data"), data, init);

		RWFunctionIntro d3f = new RWFunctionIntro(d3.d3.dloc, d3.d3.prefix + "._d3init_" + d3.d3.name, new ArrayList<Object>(), null);
		RWFunctionDefinition func = new RWFunctionDefinition(null, HSIEForm.CodeType.CARD, d3f.name, 0, true);
		func.cases.add(new RWFunctionCaseDefn(d3f, init));
		functions.put(d3f.name, func);
		
		for (RWFunctionDefinition fd : functions.values())
			mc.addFunction(forms, fd);
	}

	private FunctionLiteral functionWithArgs(String prefix, Map<String, RWFunctionDefinition> functions, List<Object> args, Object expr) {
		String name = "_gen_" + (nextFn++);

		RWFunctionIntro d3f = new RWFunctionIntro(null, prefix + "." + name, args, null);
		RWFunctionDefinition func = new RWFunctionDefinition(null, HSIEForm.CodeType.CARD, d3f.name, args.size(), true);
		func.cases.add(new RWFunctionCaseDefn(d3f, expr));
		functions.put(d3f.name, func);

		return new FunctionLiteral(d3f.location, d3f.name);
	}

	@SuppressWarnings("unchecked")
	private List<Block> makeBlocks(String file, Reader r) throws IOException, ErrorResultException {
		Object res = Blocker.block(file, r);
		if (res instanceof ErrorResult)
			throw new ErrorResultException((ErrorResult) res);
		return (List<Block>) res;
	}

	private void populateTypes(TypeChecker tc, ImportPackage pkg, List<String> parsed) {
		/* TODO: big-divide
		for (Entry<String, ScopeEntry> x : scope) {
			Object val = x.getValue().getValue();
			if (val instanceof RWStructDefn) {
//				System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				tc.addStructDefn((RWStructDefn) val);
			} else if (val instanceof RWObjectDefn) {
//				System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				tc.addObjectDefn((RWObjectDefn) val);
			} else if (val instanceof RWUnionTypeDefn) {
				tc.addTypeDefn((RWUnionTypeDefn) val);
			} else if (val instanceof Type) {
				tc.addExternal(x.getValue().getKey(), (Type)val);
			} else if (val instanceof CardTypeInfo) {
				tc.addExternalCard((CardTypeInfo)val);
			} else if (val instanceof CardDefinition || val instanceof ContractDecl) {
//				System.out.println("Not adding anything for " + x.getValue().getKey() + " " + val);
			} else if (val == null) {
//				System.out.println("Cannot add type for " + x.getValue().getKey() + " as it is null");
			} else 
				throw new UtilException("Cannot handle " + val);
		}
		*/
	}

	private Orchard<HSIEForm> hsieOrchard(ErrorResult errors, HSIE hsie, Map<String, HSIEForm> previous, Orchard<RWFunctionDefinition> d) {
		logger.info("HSIE transforming orchard in parallel: " + d);
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<RWFunctionDefinition> t : d)
			hsieTree(errors, hsie, previous, ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(ErrorResult errors, HSIE hsie, Map<String, HSIEForm> previous, Orchard<HSIEForm> ret, Tree<RWFunctionDefinition> t, Node<RWFunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		logger.info("HSIE transforming " + node.getEntry().name());
		HSIEForm form = hsie.handle(previous, node.getEntry());
		if (parent == null) {
			tree = ret.addTree(form);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, form);

		for (Node<RWFunctionDefinition> x : t.getChildren(node))
			hsieTree(errors, hsie, previous, ret, t, x, tree, parent);
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
