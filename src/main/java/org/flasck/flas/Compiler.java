package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.depedencies.DependencyAnalyzer;
import org.flasck.flas.dom.DomFunctionGenerator;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.jsgen.TemplateRenderState;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
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

	private final Rewriter rewriter = new Rewriter();
	private final Generator gen = new Generator();
	
	public void compile(File file) {
		String inPkg = file.getName();
		System.out.println("compiling package " + inPkg);
		File writeTo = new File(file, inPkg + ".js");
			
		FileWriter w = null;
		try {
			w = new FileWriter(writeTo);
		} catch (IOException ex) {
			System.err.println("Cannot write to " + writeTo + ": " + ex.getMessage());
			return;
		}

		for (File f : FileUtils.findFilesMatching(file, "*.fl"))
			compile(inPkg, w, f);
		try { w.close(); } catch (IOException ex) {}
		FileUtils.copyFileToStream(writeTo, System.out);
	}

	private void compile(String inPkg, FileWriter w, File f) {
		FileReader r = null;
		try {
			r = new FileReader(f);
			Object blks = Blocker.block(r);
			if (blks instanceof ErrorResult) {
				((ErrorResult)blks).showTo(new PrintWriter(System.err));
				return;
			}
			@SuppressWarnings("unchecked")
			List<Block> blocks = (List<Block>) blks;
			List<JSForm> forms = new ArrayList<JSForm>();
			Object obj = new FLASStory().process(inPkg, blocks);
			if (obj instanceof ErrorResult) {
				throw new ErrorResultException((ErrorResult)obj);
			} else if (obj instanceof Scope) {
				Scope scope = (Scope) obj;
				scope = rewriter.rewrite(scope);
//				List<String> pkglist = emitPackages(forms, scope, inPkg);
				assertPackage(forms, inPkg);
				Map<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
				TypeChecker tc = new TypeChecker();
				for (Entry<String, Entry<String, Object>> x : scope.outer) {
					Object val = x.getValue().getValue();
					if (val instanceof StructDefn) {
						tc.addStructDefn((StructDefn) val);
					} else if (val instanceof TypeDefn) {
						tc.addTypeDefn((TypeDefn) val);
					} else if (val instanceof Type) {
						tc.addExternal(x.getValue().getKey(), (Type)val);
					}
				}
				processScope(forms, tc, functions, scope, 1);
				List<Orchard<FunctionDefinition>> defns = new DependencyAnalyzer(tc.errors).analyze(functions);
				if (tc.errors.hasErrors())
					throw new ErrorResultException(tc.errors);
				for (Orchard<FunctionDefinition> d : defns) {
					Orchard<HSIEForm> oh = hsieOrchard(d);
					tc.typecheck(oh);
					if (tc.errors.hasErrors())
						throw new ErrorResultException(tc.errors);
					generateOrchard(forms, oh);
				}
				for (JSForm js : forms) {
					js.writeTo(w);
					w.write("\n");
				}
//				if (pkglist.size() == 1)
					w.write(inPkg + ";\n");
//				else {
//					w.write("{ ");
//					w.write(String.join(", ", pkglist));
//					w.write(" }\n");
//				}
			} else
				System.err.println("Failed to parse; got " + obj);
		} catch (ErrorResultException ex) {
			try {
				((ErrorResult)ex.errors).showTo(new PrintWriter(System.out));
			} catch (IOException ex2) {
				ex.printStackTrace();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (r != null) try { r.close(); } catch (IOException ex) {}
		}
	}

	private Orchard<HSIEForm> hsieOrchard(Orchard<FunctionDefinition> d) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (Tree<FunctionDefinition> t : d)
			hsieTree(ret, t, t.getRoot(), null, null);
		return ret;
	}

	private void hsieTree(Orchard<HSIEForm> ret, Tree<FunctionDefinition> t, Node<FunctionDefinition> node, Tree<HSIEForm> tree, Node<HSIEForm> parent) {
		HSIEForm hsie = HSIE.handle(node.getEntry());
		if (parent == null) {
			tree = ret.addTree(hsie);
			parent = tree.getRoot();
		} else
			parent = tree.addChild(parent, hsie);

		for (Node<FunctionDefinition> x : t.getChildren(node))
			hsieTree(ret, t, x, tree, parent);
	}

	private void generateOrchard(List<JSForm> forms, Orchard<HSIEForm> oh) {
		for (Tree<HSIEForm> t : oh)
			generateTree(forms, t, t.getRoot());
	}
	
	private void generateTree(List<JSForm> forms, Tree<HSIEForm> t, Node<HSIEForm> node) {
		forms.add(gen.generate(node.getEntry()));
		for (Node<HSIEForm> n : t.getChildren(node))
			generateTree(forms, t, n);
	}

	private void processScope(List<JSForm> forms, TypeChecker tc, Map<String, FunctionDefinition> functions, Scope scope, int scopeDepth) {
		for (Entry<String, Entry<String, Object>> x : scope) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
//			if (val instanceof PackageDefn) {
//				processScope(forms, tc, functions, ((PackageDefn) val).innerScope(), scopeDepth+1);
//			} else 
			if (val instanceof FunctionDefinition) {
				functions.put(name, (FunctionDefinition) val);
			} else if (val instanceof StructDefn) {
				StructDefn sd = (StructDefn) val;
				tc.addStructDefn(sd);
				forms.add(gen.generate(name, sd));
			} else if (val instanceof TypeDefn) {
				TypeDefn td = (TypeDefn) val;
				tc.addTypeDefn(td);
			} else if (val instanceof ContractDecl) {
				// currently, I don't think anything needs to be written in this case
				continue;
			} else if (val instanceof CardDefinition) {
				CardDefinition card = (CardDefinition) val;
				forms.add(gen.generate(name, card));

				{
					StructDefn sd = new StructDefn(name);
					if (card.state != null) {
						for (StructField sf : card.state.fields)
							sd.fields.add(sf);
					}
					for (ContractImplements ci : card.contracts) {
						if (ci.referAsVar != null)
							sd.fields.add(new StructField(new TypeReference(ci.type), ci.referAsVar));
					}
					tc.addStructDefn(sd);
				}
				
				int pos = 0;
				for (ContractImplements ci : card.contracts) {
					forms.add(gen.generateContract(name, ci, pos));
					for (MethodDefinition m : ci.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, "_C"+pos, m);
						functions.put(fd.name, fd);
					}
					pos++;
				}
				pos = 0;
				for (HandlerImplements hi : card.handlers) {
					if (!hi.boundVars.isEmpty()) {
						String hname = name +"._H"+pos;
						System.out.println("Creating class for handler " + hname);
						StructDefn sd = new StructDefn(hname);
						// Doing this seems clever, but I'm not really sure that it is
						// We need to make sure that in doing this, everything typechecks to the same set of variables, whereas we normally insert fresh variables every time we use the type
						for (int i=0;i<hi.boundVars.size();i++)
							sd.args.add("A"+i);
						int j=0;
						for (String s : hi.boundVars) {
							sd.fields.add(new StructField(new TypeReference(null, "A"+j), s));
							j++;
						}
						tc.addStructDefn(sd);
					}
					forms.add(gen.generateHandler(name, hi, pos));
					for (MethodDefinition m : hi.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, "_H"+pos, m);
						functions.put(fd.name, fd);
					}
					pos++;
				}
				
				if (card.template != null) {
					DomFunctionGenerator gen = new DomFunctionGenerator(functions, scope, card.state);
					gen.generate(card.template);
//					TemplateRenderState trs = new TemplateRenderState(name);
//					for (TemplateLine tl : card.template)
//						forms.add(gen.generateTemplateLine(trs, tl));
				}
			} else
				throw new UtilException("Need to handle " + x.getKey() + " of type " + val.getClass());
		}
	}

//	private List<String> emitPackages(List<JSForm> forms, Scope scope, String defPkg) {
//		boolean havePkg = false;
//		List<String> plist = new ArrayList<String>();
//		for (Entry<String, Entry<String, Object>> ko : scope) {
//			Entry<String, Object> o = ko.getValue();
//			if (o.getValue() instanceof PackageDefn) {
//				havePkg = true;
//				assertPackage(forms, plist, o.getKey());
//			}
//		}
//		if (!havePkg) {
//			assertPackage(forms, plist, defPkg);
//		}
//		return plist;
//	}

	private void assertPackage(List<JSForm> forms, /* List<String> plist, */String key) {
		String keydot = key+".";
		int idx = -1;
		while ((idx = keydot.indexOf('.', idx+1))!= -1) {
			String tmp = keydot.substring(0, idx);
			forms.add(JSForm.packageForm(tmp));
//			plist.add(tmp);
		}
//		plist.add(key);
	}

}
