package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.dom.DomFunctionGenerator;
import org.flasck.flas.dom.RenderTree;
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
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.rewriter.Rewriter;
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
		Compiler compiler = new Compiler();
		for (String f : args)
			compiler.compile(new File(f));
	}

	private final ErrorResult errors = new ErrorResult();
	private final Rewriter rewriter = new Rewriter(errors);
	private final ApplyCurry curry = new ApplyCurry();
	
	public void compile(File file) {
		String inPkg = file.getName();
		if (!file.isDirectory()) {
			System.out.println("there is no directory " + file);
			return;
		}
		File writeTo = new File(file, inPkg + ".js");
		System.out.println("compiling package " + inPkg + " to " + writeTo);
			
		FileWriter w = null;
		try {
			w = new FileWriter(writeTo);
		} catch (IOException ex) {
			System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
			return;
		}

		for (File f : FileUtils.findFilesMatching(file, "*.fl")) {
			FileReader r = null;
			try {
				r = new FileReader(f);
				// 1. Use indentation to break the input file up into blocks
				List<Block> blocks = makeBlocks(r);
				abortIfErrors();
				
				// 2. Use the parser factory and story to convert blocks to a package definition
				ScopeEntry se = doParsing(inPkg, blocks);
				abortIfErrors();
				
				// 3. Flatten the hierarchy, grouping into things of similar kinds
				//    Resolve symbols and rewrite expressions to reference "scoped" variables
				rewriter.rewrite(se);
				abortIfErrors();

				// 4. Promote template tree definition to individual functions
				List<RenderTree> trees = new ArrayList<RenderTree>();
				for (Template t : rewriter.templates)
					promoteTemplateFunctions(rewriter.functions, trees, t);
				abortIfErrors();
				
				// 5. Extract methods and convert to functions
				MethodConvertor.convert(rewriter.functions, rewriter.methods);
				abortIfErrors();

				// 6. Convert event handlers to functions
				MethodConvertor.convertEvents(rewriter.functions, rewriter.eventHandlers);
				abortIfErrors();
				rewriter.dump();

				// 7. Prepare Typechecker & load types
				TypeChecker tc = new TypeChecker(errors);
				populateTypes(tc, se.scope(), inPkg); // this is intended to just load in builtin stuff.  We should have a better pre-flattened version of that
				tc.populateTypes(rewriter);
				abortIfErrors();
			
				// Prepare target to hold "code"
				JSTarget target = new JSTarget(inPkg);
				Generator gen = new Generator(target);
			
				// Generate Class Definitions
				for (Entry<String, CardGrouping> cg : rewriter.cards.entrySet())
					gen.generate(cg.getKey(), cg.getValue());
				for (Entry<String, ContractImplements> ci : rewriter.cardImplements.entrySet())
					gen.generateContract(ci.getKey(), ci.getValue());
				
				// 8. Now look specifically at the functions we've assembled & grouped
				
				//   a. build orchards
				//   b. dependency analysis
				//   c. HSIE transformation
				//   d. typechecking
				//   e. generate JSForms
			
				// break this up
//				processScope(target.forms, tc, rewriter.functions, ((PackageDefn)se.getValue()).innerScope(), 1);
				List<Orchard<FunctionDefinition>> defns = new DependencyAnalyzer(tc.errors).analyze(rewriter.functions);
				if (tc.errors.hasErrors())
					throw new ErrorResultException(tc.errors);
				for (Orchard<FunctionDefinition> d : defns) {
					Orchard<HSIEForm> oh = hsieOrchard(d);
					if (tc.errors.hasErrors())
						throw new ErrorResultException(tc.errors);
					tc.typecheck(oh);
					if (tc.errors.hasErrors())
						throw new ErrorResultException(tc.errors);
					handleCurrying(tc, oh);
			
					// 8e. generation of JSForms
					generateOrchard(gen, target.forms, oh);
					abortIfErrors();
				}
				
				// 9. Generate render & dependency trees
				renderTemplateTrees(gen, target.forms, trees);
				abortIfErrors();
				
				// 10. Issue JavaScript
				target.writeTo(w);
				abortIfErrors();
			} catch (ErrorResultException ex) {
				try {
					((ErrorResult)ex.errors).showTo(new PrintWriter(System.out));
				} catch (IOException ex2) {
					ex2.printStackTrace();
				}
			} catch (IOException ex1) {
				ex1.printStackTrace();
			} finally {
				if (r != null) try { r.close(); } catch (IOException ex3) {}
			}
		}
		try { w.close(); } catch (IOException ex) {}
		FileUtils.copyFileToStream(writeTo, System.out);
	}

	private void abortIfErrors() throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	private void promoteTemplateFunctions(Map<String, FunctionDefinition> functions, List<RenderTree> trees, Template template) {
		DomFunctionGenerator gen = new DomFunctionGenerator(template, functions);
		gen.generateTree(template.topLine);
		for (Entry<String, FunctionDefinition> x2 : functions.entrySet()) {
			FunctionDefinition rfn = (FunctionDefinition) x2.getValue();
			functions.put(rfn.name, rfn);
		}
		trees.addAll(gen.trees);
	}

	private ScopeEntry doParsing(String inPkg, List<Block> blocks) throws ErrorResultException {
		Object obj = new FLASStory().process(inPkg, blocks);
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

	protected void populateTypes(TypeChecker tc, Scope scope, String mine) {
		for (Entry<String, ScopeEntry> x : scope) {
			Object val = x.getValue().getValue();
			if (val instanceof PackageDefn) {
				if (mine == null || !x.getKey().equals(mine))
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

	private Orchard<HSIEForm> hsieOrchard(Orchard<FunctionDefinition> d) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<FunctionDefinition> t : d)
			hsieTree(ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(Orchard<HSIEForm> ret, Tree<FunctionDefinition> t, Node<FunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		HSIEForm hsie = HSIE.handle(errors, node.getEntry());
		if (parent == null) {
			tree = ret.addTree(hsie);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, hsie);

		for (Node<FunctionDefinition> x : t.getChildren(node))
			hsieTree(ret, t, x, tree, parent);
	}

	private void handleCurrying(TypeChecker tc, Orchard<HSIEForm> oh) {
		for (Tree<HSIEForm> t : oh)
			for (HSIEForm h : t.allNodes())
				curry.rewrite(tc, h);
	}

	private void generateOrchard(Generator gen, List<JSForm> forms, Orchard<HSIEForm> oh) {
		for (Tree<HSIEForm> t : oh)
			generateTree(gen, forms, t, t.getRoot());
	}
	
	private void generateTree(Generator gen, List<JSForm> forms, Tree<HSIEForm> t, Node<HSIEForm> node) {
		forms.add(gen.generate(node.getEntry()));
		for (Node<HSIEForm> n : t.getChildren(node))
			generateTree(gen, forms, t, n);
	}

//	private void processScope(List<JSForm> forms, TypeChecker tc, Map<String, FunctionDefinition> functions, Scope scope, int scopeDepth) {
//		for (Entry<String, ScopeEntry> x : scope) {
//			String name = x.getValue().getKey();
//			Object val = x.getValue().getValue();
////			if (val instanceof PackageDefn) {
////				processScope(forms, tc, functions, ((PackageDefn) val).innerScope(), scopeDepth+1);
////			} else 
//			if (val instanceof FunctionDefinition) {
//				functions.put(name, (FunctionDefinition) val);
//			} else if (val instanceof StructDefn) {
//				StructDefn sd = (StructDefn) val;
//				tc.addStructDefn(sd);
//				forms.add(gen.generate(name, sd));
//			} else if (val instanceof TypeDefn) {
//				TypeDefn td = (TypeDefn) val;
//				tc.addTypeDefn(td);
//			} else if (val instanceof ContractDecl) {
//				// currently, I don't think anything needs to be written in this case
//				continue;
//			} else if (val instanceof CardDefinition) {
//				CardDefinition card = (CardDefinition) val;
//				forms.add(gen.generate(name, card));
//
//				{
//					StructDefn sd = new StructDefn(name);
//					if (card.state != null) {
//						for (StructField sf : card.state.fields)
//							sd.fields.add(sf);
//					}
//					for (ContractImplements ci : card.contracts) {
//						if (ci.referAsVar != null)
//							sd.fields.add(new StructField(new TypeReference(ci.type), ci.referAsVar));
//					}
//					tc.addStructDefn(sd);
//				}
//				
//				int pos = 0;
//				for (ContractImplements ci : card.contracts) {
//					forms.add(gen.generateContract(name, ci, pos));
//					forms.add(gen.generateContractCtor(name, ci, pos));
//					pos++;
//				}
//				pos = 0;
//				for (HandlerImplements hi : card.handlers) {
//					if (!hi.boundVars.isEmpty()) {
//						String hname = name +"._H"+pos;
//						System.out.println("Creating class for handler " + hname);
//						StructDefn sd = new StructDefn(hname);
//						// Doing this seems clever, but I'm not really sure that it is
//						// We need to make sure that in doing this, everything typechecks to the same set of variables, whereas we normally insert fresh variables every time we use the type
//						for (int i=0;i<hi.boundVars.size();i++)
//							sd.args.add("A"+i);
//						int j=0;
//						for (String s : hi.boundVars) {
//							sd.fields.add(new StructField(new TypeReference(null, "A"+j), s));
//							j++;
//						}
//						tc.addStructDefn(sd);
//					}
//					forms.add(gen.generateHandler(name, hi, pos));
//					forms.add(gen.generateHandlerCtor(name, hi, pos));
//					pos++;
//				}
//
//				for (Entry<String, ScopeEntry> x2 : card.innerScope()) {
//					if (x2.getValue().getValue() instanceof FunctionDefinition) {
//						functions.put(x2.getValue().getKey(), (FunctionDefinition) x2.getValue().getValue());
//					} else if (x2.getValue().getValue() instanceof EventHandlerDefinition) {
//						FunctionDefinition fd = MethodConvertor.convert(card.innerScope(), name, (EventHandlerDefinition)x2.getValue().getValue());
//						functions.put(fd.name, fd);
//					} else
//						throw new UtilException("Need to handle " + x2);
//				}
//			} else
//				throw new UtilException("Need to handle " + x.getKey() + " of type " + val.getClass());
//		}
//	}

	private void renderTemplateTrees(Generator gen, List<JSForm> forms, List<RenderTree> trees) {
		for (RenderTree t : trees) {
			JSForm block = gen.generateTemplateTree(t.card, t.template);
			forms.add(block);
			gen.generateTree(block, t.ret);
		}
	}

}
