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
import org.flasck.flas.TemplateAbstractModel.AbstractTreeNode;
import org.flasck.flas.TemplateAbstractModel.Handler;
import org.flasck.flas.TemplateAbstractModel.OrCase;
import org.flasck.flas.TemplateAbstractModel.VisualTree;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.dependencies.DependencyAnalyzer;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.ListMap;
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

	// TODO: move this into a separate class, like DOMFG used to be
	int nextFn = 1;
	
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

			/*
			// 4. Promote template tree definition to individual functions
			List<RenderTree> trees = new ArrayList<RenderTree>();
			List<UpdateTree> updates = new ArrayList<UpdateTree>();
			for (Template t : rewriter.templates)
				promoteTemplateFunctions(errors, rewriter.functions, trees, updates, t);
			abortIfErrors(errors);
			 */
			
			// 4b. Do the same for D3 invocations
			for (D3Invoke d3 : rewriter.d3s)
				promoteD3Methods(errors, rewriter.functions, d3);
			
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
			
			// 11. Generate code for templating
			for (Template cg : rewriter.templates) {
				TemplateAbstractModel tam = makeAbstractTemplateModel(errors, rewriter, cg);
				gen.generate(tam, null, null);
				JSForm onUpdate = JSForm.flex(tam.prefix + ".onUpdate =").needBlock();
				JSForm prev = null;
				for (String field : tam.fields.key1Set()) {
					if (prev != null)
						prev.comma();
					JSForm curr = JSForm.flex("'" + field + "':").needBlock();
					JSForm pa = null;
					for (String action : tam.fields.key2Set(field)) {
						JSForm ca = JSForm.flex("'" + action + "':").nestArray();
						ca.add(JSForm.flex(String.join(",", tam.fields.get(field, action))).noSemi());
						if (pa != null)
							pa.comma();
						curr.add(ca);
						pa = ca;
					}
					onUpdate.add(curr);
					prev = curr;
				}
				target.add(onUpdate);
			}

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
				System.out.println("done");
//				FileUtils.copyFileToStream(writeTo, System.out);
		}

		// TODO: look for *.ut (unit test) and *.pt (protocol test) files and compile & execute them, too.
	}

	private TemplateAbstractModel makeAbstractTemplateModel(ErrorResult errors, Rewriter rewriter, Template cg) {
		TemplateAbstractModel ret = new TemplateAbstractModel(cg.prefix, rewriter, cg.scope);
		matmRecursive(errors, ret, null, null, cg.content);
		return ret;
	}

	private void matmRecursive(ErrorResult errors, TemplateAbstractModel tam, AbstractTreeNode atn, VisualTree tree, TemplateLine content) {
		if (content instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) content;
			List<Handler> handlers = new ArrayList<Handler>();
			for (EventHandler eh : td.handlers) {
				handlers.add(new Handler(tam.ehId(), eh.action, new HSIE(errors).handleExpr(eh.expr, HSIEForm.Type.FUNCTION)));
			}
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(td.customTag, td.attrs, td.formats, handlers);
			VisualTree vt = new VisualTree(b, null);
			if (atn == null) {
				atn = new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt);
				tam.nodes.add(atn);
				vt.containsThing = AbstractTreeNode.TOP;
			} else
				tree.children.add(vt);
			for (TemplateLine x : td.nested)
				matmRecursive(errors, tam, atn, vt, x);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_formatTop");
		} else if (content instanceof TemplateList) {
			TemplateList tl = (TemplateList) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("ul", new ArrayList<Object>(), tl.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.divThing.listVar = ((CardMember)tl.listVar).var;
			pvt.containsThing = AbstractTreeNode.LIST;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)tl.listVar).var, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_clear");
			tam.fields.add(((CardMember)tl.listVar).var, "itemInserted", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemInserted");
			tam.fields.add(((CardMember)tl.listVar).var, "itemChanged", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemChanged");
			
			// This is where we separate the "included-in-parent" tree from the "I own this" tree
			VisualTree vt = new VisualTree(null, null);
			atn = new AbstractTreeNode(AbstractTreeNode.LIST, atn, b.id, b.sid, vt);
			tam.nodes.add(atn);

			// Now generate the nested template in that
			matmRecursive(errors, tam, atn, vt, tl.template);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_formatList");
		} else if (content instanceof TemplateCases) {
			TemplateCases cases = (TemplateCases) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.CASES;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.cardMembersCause(cases.switchOn, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_switch");
			
			// This is where we separate the "included-in-parent" tree from the "I own this" tree
			atn = new AbstractTreeNode(AbstractTreeNode.CASES, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			for (TemplateOr tor : cases.cases) {
				// Now generate each nested template in that
				VisualTree vt = new VisualTree(null, null);
				matmRecursive(errors, tam, atn, vt, tor.template);
				tam.cardMembersCause(tor.cond, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_switch");
				atn.cases.add(new OrCase(new HSIE(errors).handleExpr(new ApplyExpr(tam.scope.fromRoot("=="), cases.switchOn, tor.cond), HSIEForm.Type.CARD), vt));
			}
		} else if (content instanceof ContentString) {
			ContentString cs = (ContentString) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("span", new ArrayList<Object>(), cs.formats, new ArrayList<Handler>());
			VisualTree vt = new VisualTree(b, cs.text);
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt));
			else
				tree.children.add(vt);
		} else if (content instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("span", new ArrayList<Object>(), ce.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.CONTENT;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.cardMembersCause(ce.expr, "assign", Generator.lname(tam.prefix, true) + "_" + b.id);
			
			// Now we need to create a new ATN for the _content_ function
			// VisualTree vt = new VisualTree(null);
			atn = new AbstractTreeNode(AbstractTreeNode.CONTENT, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.expr = new HSIE(errors).handleExpr(ce.expr, HSIEForm.Type.FUNCTION);
		} else if (content instanceof CardReference) {
			CardReference card = (CardReference) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.CARD;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			
			atn = new AbstractTreeNode(AbstractTreeNode.CARD, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.card = card;
		} else if (content instanceof D3Invoke) {
			D3Invoke d3i = (D3Invoke) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock("div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.D3;
			pvt.divThing.name = d3i.d3.name;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)d3i.d3.data).var, "assign", tam.prefix + ".prototype._" + b.id);
			
			atn = new AbstractTreeNode(AbstractTreeNode.D3, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.d3 = d3i;
		} else 
			throw new UtilException("TL type " + content.getClass() + " not supported");
	}

	private void abortIfErrors(ErrorResult errors) throws ErrorResultException {
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
	}

	private void promoteD3Methods(ErrorResult errors, Map<String, FunctionDefinition> functions, D3Invoke d3) {
		Object init = d3.scope.fromRoot("NilMap");
		AbsoluteVar assoc = d3.scope.fromRoot("Assoc");
		AbsoluteVar cons = d3.scope.fromRoot("Cons");
		AbsoluteVar nil = d3.scope.fromRoot("Nil");
		AbsoluteVar tuple = d3.scope.fromRoot("()");
		ListMap<String, Object> byKey = new ListMap<String, Object>();
		for (D3PatternBlock p : d3.d3.patterns) {
			for (D3Section s : p.sections.values()) {
				if (!s.properties.isEmpty()) {
					Object pl = nil; // prepend to an empty list
					for (PropertyDefn prop : s.properties.values()) {
						// TODO: only create functions for things that depend on the class
						// constants can just be used directly
						FunctionLiteral efn = functionWithArgs(d3.d3.prefix, functions, d3.scope, CollectionUtils.listOf(new TypedPattern(null, "D3Element", null, d3.d3.iter)), prop.value);
						Object pair = new ApplyExpr(tuple, new StringLiteral(prop.name), efn);
						pl = new ApplyExpr(cons, pair, pl);
					}
					byKey.add(s.name, new ApplyExpr(tuple, p.pattern, pl));
				}
				else if (!s.actions.isEmpty()) { // something like enter, that is a "method"
					FunctionIntro fi = new FunctionIntro(d3.d3.prefix + "._d3_" + d3.d3.name + "_" + s.name+"_"+p.pattern.text, new ArrayList<Object>());
					MethodCaseDefn mcd = new MethodCaseDefn(fi);
					mcd.messages.addAll(s.actions);
					MethodDefinition method = new MethodDefinition(fi, CollectionUtils.listOf(mcd));
					MethodInContext mic = new MethodInContext(d3.scope, fi.name, HSIEForm.Type.CARD, method); // PROB NEEDS D3Action type
					MethodConvertor.convert(functions, CollectionUtils.listOf(mic));
					byKey.add(s.name, new FunctionLiteral(fi.name));
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
				list = new ApplyExpr(cons, lo.get(i), list);
			init = new ApplyExpr(assoc, new StringLiteral(k.getKey()), list, init);
		}
		FunctionLiteral data = functionWithArgs(d3.d3.prefix, functions, d3.scope, new ArrayList<Object>(), d3.d3.data);
		init = new ApplyExpr(assoc, new StringLiteral("data"), data, init);

		FunctionIntro d3f = new FunctionIntro(d3.d3.prefix + "._d3init_" + d3.d3.name, new ArrayList<Object>());
		FunctionCaseDefn fcd = new FunctionCaseDefn(d3.scope, d3f.name, d3f.args, init);
		FunctionDefinition func = new FunctionDefinition(HSIEForm.Type.CARD, d3f, CollectionUtils.listOf(fcd));
		functions.put(d3f.name, func);
	}

	private FunctionLiteral functionWithArgs(String prefix, Map<String, FunctionDefinition> functions, Scope scope, List<Object> args, Object expr) {
		String name = "_gen_" + (nextFn++);

		FunctionIntro d3f = new FunctionIntro(prefix + "." + name, args);
		FunctionCaseDefn fcd = new FunctionCaseDefn(scope, d3f.name, d3f.args, expr);
		FunctionDefinition func = new FunctionDefinition(HSIEForm.Type.CARD, d3f, CollectionUtils.listOf(fcd));
		functions.put(d3f.name, func);

		return new FunctionLiteral(d3f.name);
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
}
