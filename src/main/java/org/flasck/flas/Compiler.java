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
import org.flasck.flas.TemplateAbstractModel.AbstractTreeNode;
import org.flasck.flas.TemplateAbstractModel.Handler;
import org.flasck.flas.TemplateAbstractModel.OrCase;
import org.flasck.flas.TemplateAbstractModel.VisualTree;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
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
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
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
import org.flasck.flas.parsedForm.ObjectDefn;
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
import org.flasck.flas.parsedForm.UnionTypeDefn;
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
import org.zinutils.utils.StringComparator;

public class Compiler {
	public static void main(String[] args) {
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
		Compiler compiler = new Compiler();
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
					compiler.pkgFinder.searchIn(new File(args[++i]));
				} else {
					System.out.println("unknown option: " + f);
					compiler.success = false;
					break;
				}
				continue;
			}
			compiler.compile(new File(f));
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
			final HSIE hsie = new HSIE(errors, rewriter);

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
			Generator gen = new Generator(errors, hsie, target);

			for (Entry<String, StructDefn> sd : rewriter.structs.entrySet())
				gen.generate(sd.getValue());
			for (Entry<String, CardGrouping> kv : rewriter.cards.entrySet()) {
				CardGrouping grp = kv.getValue();
				gen.generate(kv.getKey(), grp);
				for (ContractGrouping ctr : grp.contracts) {
					ContractImplements ci = rewriter.cardImplements.get(ctr.implName);
					if (ci == null)
						throw new UtilException("How did this happen?");
					ContractDecl cd = rewriter.contracts.get(ci.name());
					if (cd == null)
						throw new UtilException("How did this happen?");
					Set<ContractMethodDecl> requireds = new TreeSet<ContractMethodDecl>(); 
					for (ContractMethodDecl m : cd.methods) {
						if (m.dir.equals("down") /* && is required */)
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
			for (Entry<String, ContractImplements> ci : rewriter.cardImplements.entrySet())
				gen.generateContract(ci.getKey(), ci.getValue());
			for (Entry<String, ContractService> cs : rewriter.cardServices.entrySet())
				gen.generateService(cs.getKey(), cs.getValue());
			for (Entry<String, HandlerImplements> hi : rewriter.cardHandlers.entrySet())
				gen.generateHandler(hi.getKey(), hi.getValue());
			
			// 4. Do dependency analysis on functions and group them together in orchards
			List<Orchard<FunctionDefinition>> defns = new DependencyAnalyzer(errors).analyze(rewriter.functions);
			abortIfErrors(errors);

			// 5. Now process each orchard
			//   a. convert functions to HSIE
			//   b. typechecking
		
			Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());
			for (Orchard<FunctionDefinition> d : defns) {
				// 6a. Convert each orchard to HSIE
				Orchard<HSIEForm> oh = hsieOrchard(errors, hsie, d);
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
			abortIfErrors(errors);

			// 7. Generate code from templates
			for (Template cg : rewriter.templates) {
				TemplateAbstractModel tam = makeAbstractTemplateModel(errors, rewriter, hsie, cg);
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

			// 8. D3 definitions may generate card functions; promote these onto the cards
			for (D3Invoke d3 : rewriter.d3s)
				promoteD3Methods(errors, mc, forms, rewriter.functions, d3);
			
			// 9. Check whether functions are curried and add in the appropriate indications if so
			handleCurrying(curry, tc, forms.values());
			abortIfErrors(errors);

			// 10. generation of JSForms
			generateForms(gen, forms.values());
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
			tc.writeLearnedKnowledge(wex, dumpTypes);

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
			try { if (wjs != null) wjs.close(); } catch (IOException ex) {}
			try { if (wex != null) wex.close(); } catch (IOException ex) {}
//			if (success)
//				FileUtils.copyFileToStream(writeTo, System.out);
		}

		// TODO: look for *.ut (unit test) and *.pt (protocol test) files and compile & execute them, too.
	}

	private TemplateAbstractModel makeAbstractTemplateModel(ErrorResult errors, Rewriter rewriter, HSIE hsie, Template cg) {
		TemplateAbstractModel ret = new TemplateAbstractModel(cg.prefix, rewriter, cg.scope);
		matmRecursive(errors, hsie, ret, null, null, cg.content);
		return ret;
	}

	private void matmRecursive(ErrorResult errors, HSIE hsie, TemplateAbstractModel tam, AbstractTreeNode atn, VisualTree tree, TemplateLine content) {
		if (content instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) content;
			List<Handler> handlers = new ArrayList<Handler>();
			for (EventHandler eh : td.handlers) {
				handlers.add(new Handler(tam.ehId(), eh.action, hsie.handleExpr(eh.expr, HSIEForm.CodeType.FUNCTION)));
			}
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, td.customTag, td.attrs, td.formats, handlers);
			b.sid = tam.nextSid();
			VisualTree vt = new VisualTree(b, null);
			if (atn == null) {
				atn = new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt);
				tam.nodes.add(atn);
				vt.containsThing = AbstractTreeNode.TOP;
			} else
				tree.children.add(vt);
			for (TemplateLine x : td.nested)
				matmRecursive(errors, hsie, tam, atn, vt, x);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_formatTop");
		} else if (content instanceof TemplateList) {
			TemplateList tl = (TemplateList) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "ul", new ArrayList<Object>(), tl.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.divThing.listVar = ((CardMember)tl.listVar).var;
			pvt.containsThing = AbstractTreeNode.LIST;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)tl.listVar).var, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_assign");
			tam.fields.add(((CardMember)tl.listVar).var, "itemInserted", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemInserted");
			tam.fields.add(((CardMember)tl.listVar).var, "itemChanged", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemChanged");
			
			// This is where we separate the "included-in-parent" tree from the "I own this" tree
			VisualTree vt = new VisualTree(null, null);
			atn = new AbstractTreeNode(AbstractTreeNode.LIST, atn, b.id, b.sid, vt);
			atn.var = pvt.divThing.listVar;
			tam.nodes.add(atn);

			// Now generate the nested template in that
			matmRecursive(errors, hsie, tam, atn, vt, tl.template);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_formatList");
		} else if (content instanceof TemplateCases) {
			TemplateCases cases = (TemplateCases) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
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
				matmRecursive(errors, hsie, tam, atn, vt, tor.template);
				tam.cardMembersCause(tor.cond, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_switch");
				atn.cases.add(new OrCase(hsie.handleExpr(new ApplyExpr(tor.location(), tam.scope.fromRoot(tor.location(), "=="), cases.switchOn, tor.cond), HSIEForm.CodeType.CARD), vt));
			}
		} else if (content instanceof ContentString) {
			ContentString cs = (ContentString) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "span", new ArrayList<Object>(), cs.formats, new ArrayList<Handler>());
			VisualTree vt = new VisualTree(b, cs.text);
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt));
			else
				tree.children.add(vt);
		} else if (content instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "span", new ArrayList<Object>(), ce.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null, ce.editable());
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
			atn.expr = hsie.handleExpr(ce.expr, HSIEForm.CodeType.CARD);
			if (ce.editable()) {
				atn.editable = ce.editable();
				if (ce.expr instanceof CardMember) {
					atn.editfield = ((CardMember)ce.expr).var;
				} else if (ce.expr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) ce.expr;
					if (!(ae.fn instanceof AbsoluteVar) || !(((AbsoluteVar)ae.fn).id.equals("FLEval.field")))
						throw new UtilException("Invalid expr for edit field " + ae.fn);
					atn.editobject = hsie.handleExpr(ae.args.get(0), HSIEForm.CodeType.CARD);
					atn.editfield = ((StringLiteral)ae.args.get(1)).text;
				} else
					throw new UtilException("Do not know how to/you should not be able to edit a field of type " + ce.expr.getClass());
			}
		} else if (content instanceof CardReference) {
			CardReference card = (CardReference) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
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
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.D3;
			pvt.divThing.name = d3i.d3.name;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)d3i.d3.data).var, "assign", Generator.lname(tam.prefix, true) + "_" + b.id);
			
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

	private void promoteD3Methods(ErrorResult errors, MethodConvertor mc, Map<String, HSIEForm> forms, Map<String, FunctionDefinition> functions, D3Invoke d3) {
		Object init = d3.scope.fromRoot(d3.d3.dloc, "NilMap");
		AbsoluteVar assoc = d3.scope.fromRoot(d3.d3.dloc, "Assoc");
		AbsoluteVar cons = d3.scope.fromRoot(d3.d3.dloc, "Cons");
		AbsoluteVar nil = d3.scope.fromRoot(d3.d3.dloc, "Nil");
		AbsoluteVar tuple = d3.scope.fromRoot(d3.d3.dloc, "()");
		ListMap<String, Object> byKey = new ListMap<String, Object>();
		for (D3PatternBlock p : d3.d3.patterns) {
			for (D3Section s : p.sections.values()) {
				if (!s.properties.isEmpty()) {
					Object pl = nil; // prepend to an empty list
					for (PropertyDefn prop : s.properties.values()) {
						// TODO: only create functions for things that depend on the class
						// constants can just be used directly
						FunctionLiteral efn = functionWithArgs(d3.d3.prefix, functions, d3.scope, CollectionUtils.listOf(new TypedPattern(null, "D3Element", null, d3.d3.iter)), prop.value);
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
					MethodInContext mic = new MethodInContext(d3.scope, MethodInContext.EVENT, null, null, fi.name, HSIEForm.CodeType.CARD, method); // PROB NEEDS D3Action type
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
		FunctionCaseDefn fcd = new FunctionCaseDefn(d3.scope, d3.d3.dloc, d3f.name, d3f.args, init);
		FunctionDefinition func = new FunctionDefinition(null, HSIEForm.CodeType.CARD, d3f, CollectionUtils.listOf(fcd));
		functions.put(d3f.name, func);
	}

	private FunctionLiteral functionWithArgs(String prefix, Map<String, FunctionDefinition> functions, Scope scope, List<Object> args, Object expr) {
		String name = "_gen_" + (nextFn++);

		FunctionIntro d3f = new FunctionIntro(null, prefix + "." + name, args);
		FunctionCaseDefn fcd = new FunctionCaseDefn(scope, null, d3f.name, d3f.args, expr);
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
			} else if (val instanceof CardDefinition || val instanceof ContractDecl) {
//				System.out.println("Not adding anything for " + x.getValue().getKey() + " " + val);
			} else if (val == null) {
//				System.out.println("Cannot add type for " + x.getValue().getKey() + " as it is null");
			} else 
				throw new UtilException("Cannot handle " + val);
		}
	}

	private Orchard<HSIEForm> hsieOrchard(ErrorResult errors, HSIE hsie, Orchard<FunctionDefinition> d) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<FunctionDefinition> t : d)
			hsieTree(errors, hsie, ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(ErrorResult errors, HSIE hsie, Orchard<HSIEForm> ret, Tree<FunctionDefinition> t, Node<FunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		HSIEForm form = hsie.handle(node.getEntry());
		if (parent == null) {
			tree = ret.addTree(form);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, form);

		for (Node<FunctionDefinition> x : t.getChildren(node))
			hsieTree(errors, hsie, ret, t, x, tree, parent);
	}

	private void handleCurrying(ApplyCurry curry, TypeChecker tc, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			curry.rewrite(tc, h);
	}

	private void generateForms(Generator gen, Collection<HSIEForm> collection) {
		for (HSIEForm h : collection)
			gen.generate(h);
	}
}
