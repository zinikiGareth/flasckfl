package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.dom.DomFunctionGenerator;
import org.flasck.flas.dom.RenderTree;
import org.flasck.flas.dom.UpdateTree;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;
import org.zinutils.utils.FileUtils;

public class Compiler {
	public static void main(String[] args) {
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
		Compiler compiler = new Compiler();
		for (String f : args)
			compiler.compile(new File(f));
	}

	public void compile(File file) {
		String inPkg = file.getName();
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		File writeTo = new File(file, inPkg + ".js");
		System.out.println("compiling package " + inPkg + " to " + writeTo);
			
		boolean failed = false;
		Scope top = Builtin.builtinScope();
		PackageDefn pd = new PackageDefn(top, inPkg);
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
				List<Block> blocks = makeBlocks(r);
				
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
		
		FileWriter w = null;
		boolean success = false;
		try {
			// 3. Flatten the hierarchy, grouping into things of similar kinds
			//    Resolve symbols and rewrite expressions to reference "scoped" variables
			final ErrorResult errors = new ErrorResult();
			final Rewriter rewriter = new Rewriter(errors);
			final ApplyCurry curry = new ApplyCurry();

			for (ScopeEntry se : entries)
				rewriter.rewrite(se);
			abortIfErrors(errors);

			// 4. Promote template tree definition to individual functions
			List<RenderTree> trees = new ArrayList<RenderTree>();
			List<UpdateTree> updates = new ArrayList<UpdateTree>();
			for (Template t : rewriter.templates)
				promoteTemplateFunctions(errors, rewriter.functions, trees, updates, t);
			abortIfErrors(errors);
			
			// 5. Extract methods and convert to functions
			MethodConvertor.convert(rewriter.functions, rewriter.methods);
			abortIfErrors(errors);

			// 6. Convert event handlers to functions
			MethodConvertor.convertEvents(rewriter.functions, rewriter.eventHandlers);
			abortIfErrors(errors);
//				rewriter.dump();

			// 7. Prepare Typechecker & load types
			TypeChecker tc = new TypeChecker(errors);
			populateTypes(tc, top, pkgs); // this is intended to just load in builtin stuff.  We should have a better pre-flattened version of that
			tc.populateTypes(rewriter);
			abortIfErrors(errors);
		
			// 8. Generate Class Definitions
			JSTarget target = new JSTarget(inPkg);
			Generator gen = new Generator(errors, target);

			for (Entry<String, StructDefn> sd : rewriter.structs.entrySet())
				gen.generate(sd.getValue());
			for (Entry<String, CardGrouping> cg : rewriter.cards.entrySet())
				gen.generate(cg.getKey(), cg.getValue());
			for (Entry<String, ContractImplements> ci : rewriter.cardImplements.entrySet())
				gen.generateContract(ci.getKey(), ci.getValue());
			for (Entry<String, ContractService> cs : rewriter.cardServices.entrySet())
				gen.generateService(cs.getKey(), cs.getValue());
			for (Entry<String, HandlerImplements> hi : rewriter.cardHandlers.entrySet())
				gen.generateHandler(hi.getKey(), hi.getValue());
			
			// 9. Do dependency analysis on functions and group them together in orchards
			List<Orchard<FunctionDefinition>> defns = new DependencyAnalyzer(errors).analyze(rewriter.functions);
			abortIfErrors(errors);

			// 10. Now process each orchard
			//   a. convert functions to HSIE
			//   b. typechecking
			//   c. curry functions that don't have enough args
			//   d. generate JSForms
		
			for (Orchard<FunctionDefinition> d : defns) {
				// 10a. Convert each orchard to HSIE
				Orchard<HSIEForm> oh = hsieOrchard(errors, d);
				abortIfErrors(errors);
				
				// 10b. Typecheck an orchard together
				tc.typecheck(oh);
				abortIfErrors(errors);

				// 10c. Check whether functions are curried and add in the appropriate indications if so
				handleCurrying(curry, tc, oh);
				abortIfErrors(errors);

				// 10d. generation of JSForms
				generateOrchard(gen, oh);
				abortIfErrors(errors);
			}
			
			// 11. Generate render & dependency trees
			renderTemplateTrees(gen, trees, updates);
			abortIfErrors(errors);
			
			// 12. Issue JavaScript
			try {
				w = new FileWriter(writeTo);
			} catch (IOException ex) {
				System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
				return;
			}

			target.writeTo(w);
			abortIfErrors(errors);

			success = true;
		} catch (ErrorResultException ex) {
			try {
				((ErrorResult)ex.errors).showTo(new PrintWriter(System.out), 4);
			} catch (IOException ex2) {
				ex2.printStackTrace();
			}
		} catch (IOException ex1) {
			ex1.printStackTrace();
		} finally {
			try { if (w != null) w.close(); } catch (IOException ex) {}
			if (success)
				FileUtils.copyFileToStream(writeTo, System.out);
		}

		// TODO: look for *.ut (unit test) and *.pt (protocol test) files and compile & execute them, too.
	}

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	private void promoteTemplateFunctions(ErrorResult errors, Map<String, FunctionDefinition> functions, List<RenderTree> trees, List<UpdateTree> updates, Template template) {
		DomFunctionGenerator gen = new DomFunctionGenerator(errors, template, functions);
		gen.generateTree(template.topLine);
		for (Entry<String, FunctionDefinition> x2 : functions.entrySet()) {
			FunctionDefinition rfn = (FunctionDefinition) x2.getValue();
			functions.put(rfn.name, rfn);
		}
		trees.addAll(gen.trees);
		updates.add(new UpdateTree(gen.prefix, gen.updates));
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
	private List<Block> makeBlocks(FileReader r) throws IOException, ErrorResultException {
		Object res = Blocker.block(r);
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
			} else if (val instanceof TypeDefn) {
				tc.addTypeDefn((TypeDefn) val);
			} else if (val instanceof Type) {
				tc.addExternal(x.getValue().getKey(), (Type)val);
			} else if (val instanceof CardDefinition || val instanceof ContractDecl) {
//				System.out.println("Not adding anything for " + x.getValue().getKey() + " " + val);
			} else if (val == null) {
//				System.out.println("Cannot add type for " + x.getValue().getKey() + " as it is null");
			} else 
				throw new UtilException("Cannot handle " + val);
		}
	}

	private Orchard<HSIEForm> hsieOrchard(ErrorResult errors, Orchard<FunctionDefinition> d) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<FunctionDefinition> t : d)
			hsieTree(errors, ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(ErrorResult errors, Orchard<HSIEForm> ret, Tree<FunctionDefinition> t, Node<FunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		HSIEForm hsie = new HSIE(errors).handle(node.getEntry());
		if (parent == null) {
			tree = ret.addTree(hsie);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, hsie);

		for (Node<FunctionDefinition> x : t.getChildren(node))
			hsieTree(errors, ret, t, x, tree, parent);
	}

	private void handleCurrying(ApplyCurry curry, TypeChecker tc, Orchard<HSIEForm> oh) {
		for (Tree<HSIEForm> t : oh)
			for (HSIEForm h : t.allNodes())
				curry.rewrite(tc, h);
	}

	private void generateOrchard(Generator gen, Orchard<HSIEForm> oh) {
		for (Tree<HSIEForm> t : oh)
			generateTree(gen,  t, t.getRoot());
	}
	
	private void generateTree(Generator gen, Tree<HSIEForm> t, Node<HSIEForm> node) {
		gen.generate(node.getEntry());
		for (Node<HSIEForm> n : t.getChildren(node))
			generateTree(gen, t, n);
	}

	private void renderTemplateTrees(Generator gen, List<RenderTree> trees, List<UpdateTree> updates) {
		for (RenderTree t : trees) {
			JSForm block = gen.generateTemplateTree(t.card, t.template);
			gen.generateTree(block, t.ret);
		}
		for (UpdateTree t : updates) {
			JSForm block = gen.generateUpdateTree(t.prefix);
			gen.generateUpdates(block, t.prefix, t.updates);
		}
	}
}
