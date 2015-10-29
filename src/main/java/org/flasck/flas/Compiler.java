package org.flasck.flas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.typechecker.CardTypeInfo;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.typechecker.Type.WhatAmI;
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
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
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
						System.out.println("unknown option: " + f);
						compiler.success = false;
						break;
					}
					continue;
				}
				compiler.compile(new File(f));
			}
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

	// TODO: move this into a separate class, like DOMFG used to be
	int nextFn = 1;
	private boolean success;
	private boolean dumpTypes = false;
	private final PackageFinder pkgFinder = new PackageFinder();
	private ByteCodeEnvironment bce = new ByteCodeEnvironment();
	private File androidDir;

	public void searchIn(File file) {
		pkgFinder.searchIn(file);
	}
	
	// Simultaneously specify that we *WANT* to generate Android and *WHERE* to put it
	public void writeDroidTo(File file) {
		androidDir = file;
	}

	public void compile(File file) {
		String inPkg = file.getName();
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		File writeTo = new File(file, inPkg + ".js");
		File exportTo = new File(file, inPkg + ".flim");
		System.out.println("compiling package " + inPkg + " to " + writeTo);
			
		boolean failed = false;
		Scope top = Builtin.builtinScope();
		PackageDefn pd = new PackageDefn(new InputPosition(file.getName(), 0, 0, inPkg), top, inPkg);
		final List<ScopeEntry> entries = new ArrayList<ScopeEntry>();
		final List<String> pkgs = new ArrayList<String>();
		pkgs.add(inPkg);
		entries.add(pd.myEntry());
		
		for (File f : FileUtils.findFilesMatching(file, "*.fl")) {
			System.out.println(" > " + f.getName());
			FileReader r = null;
			try {
				r = new FileReader(f);

				// 1. Use indentation to break the input file up into blocks
				List<Block> blocks = makeBlocks(f.getName(), r);
				
				// 2. Use the parser factory and story to convert blocks to a package definition
				doParsing(pd.myEntry(), blocks);
			} catch (ErrorResultException ex) {
				failed = true;
				try {
					((ErrorResult)ex.errors).showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
				} catch (IOException ex2) {
					ex2.printStackTrace();
				}
			} catch (IOException ex1) {
				failed = true;
				ex1.printStackTrace();
			} finally {
				if (r != null) try { r.close(); } catch (IOException ex3) {}
			}
		}

		if (failed)
			return;
		
		FileWriter wjs = null;
		FileOutputStream wex = null;
		success = false;
		try {
			// 1. Flatten the hierarchy, grouping into things of similar kinds
			//    Resolve symbols and rewrite expressions to reference "scoped" variables
			final ErrorResult errors = new ErrorResult();
			final Rewriter rewriter = new Rewriter(errors, pkgFinder);
			final ApplyCurry curry = new ApplyCurry();
			final HSIE hsie = new HSIE(errors, rewriter, top);
			final DroidGenerator dg = new DroidGenerator(hsie, bce, androidDir);

			for (ScopeEntry se : entries)
				rewriter.rewrite(se);
			abortIfErrors(errors);

			// 2. Prepare Typechecker & load types
			TypeChecker tc = new TypeChecker(errors);
			populateTypes(tc, top, pkgs); // this is intended to just load in builtin stuff.  We should have a better pre-flattened version of that
			tc.populateTypes(rewriter);
			abortIfErrors(errors);
		
			// 3. Generate Class Definitions
			JSTarget target = new JSTarget(inPkg);
			Generator gen = new Generator(hsie, target);

			for (Entry<String, StructDefn> sd : rewriter.structs.entrySet()) {
				gen.generate(sd.getValue());
				dg.generate(sd.getValue());
			}
			for (Entry<String, CardGrouping> kv : rewriter.cards.entrySet()) {
				CardGrouping grp = kv.getValue();
				compileInits(hsie, tc, kv.getValue());

				gen.generate(kv.getKey(), grp);
				dg.generate(kv.getKey(), grp);
				for (ContractGrouping ctr : grp.contracts) {
					ContractImplements ci = rewriter.cardImplements.get(ctr.implName);
					if (ci == null)
						throw new UtilException("How did this happen?");
					ContractDecl cd = rewriter.contracts.get(ci.name());
					if (cd == null)
						throw new UtilException("How did this happen?");
					Set<ContractMethodDecl> requireds = new TreeSet<ContractMethodDecl>(); 
					for (ContractMethodDecl m : cd.methods) {
						if (m.dir.equals("down") && m.required)
							requireds.add(m);
					}
					for (MethodDefinition m : ci.methods) {
						boolean haveMethod = false;
						for (ContractMethodDecl dc : cd.methods) {
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
						for (ContractMethodDecl d : requireds)
							errors.message(ci.location(), ci.name() + " does not implement " + d);
					}
				}
			}
			for (Entry<String, ContractImplements> ci : rewriter.cardImplements.entrySet()) {
				gen.generateContract(ci.getKey(), ci.getValue());
				dg.generateContract(ci.getKey(), ci.getValue());
			}
			for (Entry<String, ContractService> cs : rewriter.cardServices.entrySet()) {
				gen.generateService(cs.getKey(), cs.getValue());
				dg.generateService(cs.getKey(), cs.getValue());
			}
			for (Entry<String, HandlerImplements> hi : rewriter.callbackHandlers.entrySet()) {
				gen.generateHandler(hi.getKey(), hi.getValue());
				dg.generateHandler(hi.getKey(), hi.getValue());
			}
			
			// 4. Do dependency analysis on functions and group them together in orchards
			List<Orchard<FunctionDefinition>> defns = new DependencyAnalyzer(errors).analyze(rewriter.functions);
			abortIfErrors(errors);

			// 5. Now process each orchard
			//   a. convert functions to HSIE
			//   b. typechecking
		
			Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());
			for (Orchard<FunctionDefinition> d : defns) {
				// 6a. Convert each orchard to HSIE
				Orchard<HSIEForm> oh = hsieOrchard(errors, hsie, forms, d);
				abortIfErrors(errors);
				
				// 6b. Typecheck an orchard together
				tc.typecheck(oh);
				abortIfErrors(errors);

				for (Tree<HSIEForm> t : oh)
					for (HSIEForm h : t.allNodes())
						forms.put(h.fnName, h);
			}

			// Now go back and handle all the "special cases" that sit at the top of the tree, such as methods and templates
			
			MethodConvertor mc = new MethodConvertor(errors, hsie, tc, rewriter.contracts);

			// 6. Typecheck contract methods and event handlers, convert to functions and compile to HSIE
			mc.convertContractMethods(forms, rewriter.methods);
			mc.convertEventHandlers(forms, rewriter.eventHandlers);
			mc.convertStandaloneMethods(forms, rewriter.standalone.values());
			abortIfErrors(errors);
			
			// 7. Generate code from templates
			final TemplateGenerator tgen = new TemplateGenerator(rewriter, hsie, tc, curry, dg);
			tgen.generate(target);

			// 8. D3 definitions may generate card functions; promote these onto the cards
			for (D3Invoke d3 : rewriter.d3s)
				promoteD3Methods(errors, rewriter, mc, forms, d3);
			
			// 9. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc, forms.values());
			abortIfErrors(errors);

			// 10. generation of JSForms
			generateForms(gen, forms.values());
			dg.generate(forms.values());
			abortIfErrors(errors);

			// 11a. Issue JavaScript
			try {
				wjs = new FileWriter(writeTo);
			} catch (IOException ex) {
				System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
				return;
			}
			target.writeTo(wjs);

			
			// 11b. Save learned state for export
			try {
				wex = new FileOutputStream(exportTo);
			} catch (IOException ex) {
				System.err.println("Cannot write to " + exportTo + ": " + ex.getMessage());
				return;
			}
			tc.writeLearnedKnowledge(wex, inPkg, dumpTypes);

			try {
				dg.write();
			} catch (Exception ex) {
				System.err.println("Cannot write to " + androidDir + ": " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			abortIfErrors(errors);

			success = true;
		} catch (ErrorResultException ex) {
			try {
				((ErrorResult)ex.errors).showTo(new PrintWriter(System.out), 4);
			} catch (IOException ex2) {
				ex2.printStackTrace();
				System.err.println(ex2);
			}
		} catch (Exception ex1) {
			ex1.printStackTrace();
			System.err.println(UtilException.unwrap(ex1));
		} finally {
			try { if (wjs != null) wjs.close(); } catch (IOException ex) {}
			try { if (wex != null) wex.close(); } catch (IOException ex) {}
//			if (success)
//				FileUtils.copyFileToStream(writeTo, System.out);
		}

		// TODO: look for *.ut (unit test) and *.pt (protocol test) files and compile & execute them, too.
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
				for (StructField sf : c.struct.fields) {
					if (sf.name.equals(kv.getKey())) {
						Type st = sf.type;
						while (st.iam == WhatAmI.INSTANCE)
							st = st.innerType();
						boolean ok = false;
						if (st instanceof UnionTypeDefn) {
							for (Type t1 : ((UnionTypeDefn)st).cases)
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

	private void promoteD3Methods(ErrorResult errors, Rewriter rewriter, MethodConvertor mc, Map<String, HSIEForm> forms, D3Invoke d3) {
		Map<String, FunctionDefinition> functions = new TreeMap<String, FunctionDefinition>(new StringComparator()); 
		Object init = d3.scope.fromRoot(d3.d3.dloc, "NilMap");
		PackageVar assoc = d3.scope.fromRoot(d3.d3.dloc, "Assoc");
		PackageVar cons = d3.scope.fromRoot(d3.d3.dloc, "Cons");
		PackageVar nil = d3.scope.fromRoot(d3.d3.dloc, "Nil");
		PackageVar tuple = d3.scope.fromRoot(d3.d3.dloc, "()");
		Type d3Elt = (Type)d3.scope.fromRoot(d3.d3.dloc, "D3Element").defn;
		ListMap<String, Object> byKey = new ListMap<String, Object>();
		for (D3PatternBlock p : d3.d3.patterns) {
			for (D3Section s : p.sections.values()) {
				if (!s.properties.isEmpty()) {
					Object pl = nil; // prepend to an empty list
					for (PropertyDefn prop : s.properties.values()) {
						// TODO: only create functions for things that depend on the class
						// constants can just be used directly
						FunctionLiteral efn = functionWithArgs(d3.d3.prefix, functions, d3.scope, CollectionUtils.listOf(new TypedPattern(null, d3Elt, null, d3.d3.iter)), prop.value);
						Object pair = new ApplyExpr(prop.location, tuple, new StringLiteral(prop.location, prop.name), efn);
						pl = new ApplyExpr(prop.location, cons, pair, pl);
					}
					byKey.add(s.name, new ApplyExpr(s.location, tuple, p.pattern, pl));
				}
				else if (!s.actions.isEmpty()) { // something like enter, that is a "method"
					FunctionIntro fi = new FunctionIntro(s.location, d3.d3.prefix + "._d3_" + d3.d3.name + "_" + s.name+"_"+p.pattern.text, new ArrayList<Object>());
					MethodCaseDefn mcd = new MethodCaseDefn(fi);
					mcd.messages.addAll(s.actions);
					MethodDefinition method = new MethodDefinition(fi, CollectionUtils.listOf(mcd));
					MethodInContext mic = new MethodInContext(rewriter, null, d3.scope, MethodInContext.EVENT, null, null, fi.name, HSIEForm.CodeType.CARD, method); // PROB NEEDS D3Action type
					mc.convertContractMethods(forms, CollectionUtils.listOf(mic));
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
		FunctionLiteral data = functionWithArgs(d3.d3.prefix, functions, d3.scope, new ArrayList<Object>(), d3.d3.data);
		init = new ApplyExpr(null, assoc, new StringLiteral(null, "data"), data, init);

		FunctionIntro d3f = new FunctionIntro(d3.d3.dloc, d3.d3.prefix + "._d3init_" + d3.d3.name, new ArrayList<Object>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(d3.d3.dloc, d3f.name, d3f.args, init);
		FunctionDefinition func = new FunctionDefinition(null, HSIEForm.CodeType.CARD, d3f, CollectionUtils.listOf(fcd));
		functions.put(d3f.name, func);
		
		for (FunctionDefinition fd : functions.values())
			mc.addFunction(forms, fd);
	}

	private FunctionLiteral functionWithArgs(String prefix, Map<String, FunctionDefinition> functions, Scope scope, List<Object> args, Object expr) {
		String name = "_gen_" + (nextFn++);

		FunctionIntro d3f = new FunctionIntro(null, prefix + "." + name, args);
		FunctionCaseDefn fcd = new FunctionCaseDefn(null, d3f.name, d3f.args, expr);
		FunctionDefinition func = new FunctionDefinition(null, HSIEForm.CodeType.CARD, d3f, CollectionUtils.listOf(fcd));
		functions.put(d3f.name, func);

		return new FunctionLiteral(d3f.location, d3f.name);
	}

	private ScopeEntry doParsing(ScopeEntry se, List<Block> blocks) throws ErrorResultException {
		Object obj = new FLASStory().process(se, blocks);
		if (obj instanceof ErrorResult) {
			throw new ErrorResultException((ErrorResult)obj);
		} else if (obj instanceof ScopeEntry) {
			return (ScopeEntry) obj;
		} else
			throw new UtilException("Parsing returned: " + obj);
	}

	@SuppressWarnings("unchecked")
	private List<Block> makeBlocks(String file, FileReader r) throws IOException, ErrorResultException {
		Object res = Blocker.block(file, r);
		if (res instanceof ErrorResult)
			throw new ErrorResultException((ErrorResult) res);
		return (List<Block>) res;
	}

	private void populateTypes(TypeChecker tc, Scope scope, List<String> parsed) {
		for (Entry<String, ScopeEntry> x : scope) {
			Object val = x.getValue().getValue();
			if (val instanceof PackageDefn) {
				if (parsed == null || !parsed.contains(x.getKey()))
					populateTypes(tc, ((PackageDefn)val).innerScope(), null);
			} else if (val instanceof StructDefn) {
//				System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				tc.addStructDefn((StructDefn) val);
			} else if (val instanceof ObjectDefn) {
//				System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				tc.addObjectDefn((ObjectDefn) val);
			} else if (val instanceof UnionTypeDefn) {
				tc.addTypeDefn((UnionTypeDefn) val);
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
	}

	private Orchard<HSIEForm> hsieOrchard(ErrorResult errors, HSIE hsie, Map<String, HSIEForm> previous, Orchard<FunctionDefinition> d) {
		logger.info("HSIE transforming orchard in parallel: " + d);
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<FunctionDefinition> t : d)
			hsieTree(errors, hsie, previous, ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(ErrorResult errors, HSIE hsie, Map<String, HSIEForm> previous, Orchard<HSIEForm> ret, Tree<FunctionDefinition> t, Node<FunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		logger.info("HSIE transforming " + node.getEntry().name);
		HSIEForm form = hsie.handle(previous, node.getEntry());
		if (parent == null) {
			tree = ret.addTree(form);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, form);

		for (Node<FunctionDefinition> x : t.getChildren(node))
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
