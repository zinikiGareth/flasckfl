package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public class Compiler {
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		for (String f : args)
			compiler.compile(new File(f));
	}

	// Tempting as it is to think this is complete, the fact is that it is more difficult than this and we need to consider
	// the fact that as we arbitrarily nest these things, the way in which we reference them changes
	class NamingContext {
		final String prefix;
		final Set<String> defines = new HashSet<String>();
		final Object nested;
		
		public NamingContext(String prefix, Object nested) {
			this.prefix = prefix;
			this.nested = nested;
		}
		
		public String resolve(String name) {
			if (defines.contains(name)) {
				if (prefix == null)
					return name;
				else
					return prefix + "." + name;
			}
			if (nested instanceof NamingContext)
				return ((NamingContext)nested).resolve(name);
			return ((Scope)nested).resolve(name);
		}
	}
	
	private final Generator gen = new Generator();
	
	public void compile(File file) {
		String defPkg = file.getParentFile().getName();
		System.out.println("default package would be " + defPkg);
		File writeTo = new File(file.getParentFile(), file.getName().replace(".fl", ".js"));
		FileWriter w = null;
		FileReader r = null;
		try {
			w = new FileWriter(writeTo);
			r = new FileReader(file);
			List<Block> blocks = Blocker.block(r);
			List<JSForm> forms = new ArrayList<JSForm>();
			Object obj = new FLASStory().process(defPkg, blocks);
			if (obj instanceof ErrorResult) {
				((ErrorResult)obj).showTo(new PrintWriter(System.out));
			} else if (obj instanceof Scope) {
				Scope scope = (Scope) obj;
				rewrite(scope);
				List<String> pkglist = emitPackages(forms, scope, defPkg);
				processScope(forms, scope);
				for (JSForm js : forms) {
					js.writeTo(w);
					w.write("\n");
				}
				if (pkglist.size() == 1)
					w.write(pkglist.get(0) + ";\n");
				else {
					w.write("{ ");
					w.write(String.join(", ", pkglist));
					w.write(" }\n");
				}
			} else
				System.err.println("Failed to parse; got " + obj);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (w != null) try { w.close(); } catch (IOException ex) {}
			if (r != null) try { r.close(); } catch (IOException ex) {}
		}
		FileUtils.copyFileToStream(writeTo, System.out);
	}

	private void rewrite(Scope scope) {
		NamingContext cx = new NamingContext(null, scope);
		for (Entry<String, Object> x : scope) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof CardDefinition) {
				NamingContext c2 = new NamingContext("_this", cx);
				CardDefinition cd = (CardDefinition) val;
				// TODO: Gather locally defined things 
				if (cd.state != null) {
					List<StructField> l = new ArrayList<StructField>(cd.state.fields);
					cd.state.fields.clear();
					for (StructField sf : l) {
						cd.state.fields.add(rewrite(cx, sf));
						c2.defines.add(sf.name);
					}
				}
				for (ContractImplements ci : cd.contracts) {
					c2.defines.add(ci.referAsVar);
				}
				if (cd.template != null) {
					System.out.println("Don't rewrite template yet");
				}
				List<ContractImplements> l = new ArrayList<ContractImplements>(cd.contracts);
				cd.contracts.clear();
				for (ContractImplements ci : l) {
					cd.contracts.add(rewriteCI(c2, ci));
				}
				List<HandlerImplements> ll = new ArrayList<HandlerImplements>(cd.handlers);
				cd.handlers.clear();
				for (HandlerImplements hi : ll) {
					cd.handlers.add(rewriteHI(c2, hi));
				}
			} else
				System.out.println("Can't rewrite " + name + " of type " + val.getClass());
		}
	}

	private ContractImplements rewriteCI(NamingContext cx, ContractImplements ci) {
		ContractImplements ret = new ContractImplements(cx.resolve(ci.type), ci.referAsVar);
		rewrite(cx, ret, ci);
		return ret;
	}

	private HandlerImplements rewriteHI(NamingContext cx, HandlerImplements hi) {
		HandlerImplements ret = new HandlerImplements(cx.resolve(hi.type), hi.boundVars);
		NamingContext c2 = new NamingContext("_this", cx);
		c2.defines.addAll(hi.boundVars);
		rewrite(c2, ret, hi);
		return ret;
	}

	private void rewrite(NamingContext scope, Implements into, Implements orig) {
		System.out.println("Rewriting " + orig.type + " to " + into.type);
		for (MethodDefinition m : orig.methods) {
			into.methods.add(rewrite(scope, m));
		}
	}

	private MethodDefinition rewrite(NamingContext scope, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(scope, c));
		}
		return new MethodDefinition(m.intro, list);
	}

	private MethodCaseDefn rewrite(NamingContext cx, MethodCaseDefn c) {
		MethodCaseDefn ret = new MethodCaseDefn(rewrite(cx, c.intro));
		NamingContext c2 = new NamingContext(null, cx);
		gatherVars(c2.defines, c.intro.args);
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(c2, mm));
		return ret;
	}

	private FunctionIntro rewrite(NamingContext scope, FunctionIntro intro) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(scope, o));
		}
		return new FunctionIntro(intro.name, args);
	}

	private Object rewritePattern(NamingContext scope, Object o) {
		if (o instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) o;
			return new TypedPattern(scope.resolve(tp.type), tp.var);
		} else {
			System.out.println("Couldn't rewrite pattern " + o.getClass());
			return o;
		}
	}

	private MethodMessage rewrite(NamingContext cx, MethodMessage mm) {
		List<String> newSlot = null;
		if (mm.slot != null && !mm.slot.isEmpty()) {
			newSlot = new ArrayList<String>();
			newSlot.add(cx.resolve(mm.slot.get(0)));
			for (int i=1;i<mm.slot.size();i++)
				newSlot.add(mm.slot.get(i));
		}
		return new MethodMessage(newSlot, rewriteExpr(cx, mm.expr));
	}

	private StructField rewrite(NamingContext scope, StructField sf) {
		return new StructField(rewriteType(scope, sf.type), sf.name, rewriteExpr(scope, sf.init));
	}

	private Object rewriteExpr(NamingContext scope, Object expr) {
		if (expr instanceof ItemExpr) {
			ItemExpr ie = (ItemExpr) expr;
			if (ie.tok.type == ExprToken.NUMBER)
				return ie;
			else if (ie.tok.type == ExprToken.IDENTIFIER)
				return new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, scope.resolve(ie.tok.text)));
			else
				return ie;  // symbol or punc
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			if (ae.fn instanceof ItemExpr && ((ItemExpr)ae.fn).tok.text.equals(".")) {
				return new ApplyExpr(ae.fn, rewriteExpr(scope, ae.args.get(0)), ae.args.get(1));
			}
			List<Object> args = new ArrayList<Object>();
			for (Object o : ae.args)
				args.add(rewriteExpr(scope, o));
			return new ApplyExpr(rewriteExpr(scope, ae.fn), args);
		}
		System.out.println("Can't rewrite expr " + expr + " of type " + expr.getClass());
		return expr;
	}

	private Object rewriteType(NamingContext scope, Object type) {
		if (type instanceof TypeReference) {
			TypeReference tr = (TypeReference) type;
			TypeReference ret = new TypeReference(scope.resolve(tr.name));
			for (Object o : tr.args)
				ret.args.add(rewriteType(scope, o));
			return ret;
		}
		System.out.println("Can't rewrite type " + type + " of type " + type.getClass());
		return type;
	}

	private void processScope(List<JSForm> forms, Scope scope) {
		for (Entry<String, Object> x : scope) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof PackageDefn) {
				processScope(forms, ((PackageDefn) val).innerScope());
			} else if (val instanceof FunctionDefinition) {
				HSIEForm hsie = HSIE.handle((FunctionDefinition) val);
				forms.add(gen.generate(hsie));
			} else if (val instanceof StructDefn) {
				StructDefn sd = (StructDefn) val;
				forms.add(gen.generate(name, sd));
			} else if (val instanceof ContractDecl) {
				// currently, I don't think anything needs to be written in this case
				continue;
			} else if (val instanceof CardDefinition) {
				CardDefinition card = (CardDefinition) val;
				
				forms.add(gen.generate(name, card));
				
				int pos = 0;
				for (ContractImplements ci : card.contracts) {
					forms.add(gen.generateImplements(name, ci, pos));
					for (MethodDefinition m : ci.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, "_C"+pos, m);
						HSIEForm hsie = HSIE.handle(fd);
						forms.add(gen.generate(hsie));
					}
					pos++;
				}
				pos = 0;
				for (HandlerImplements hi : card.handlers) {
					forms.add(gen.generateImplements(name, hi, pos));
					for (MethodDefinition m : hi.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, "_H"+pos, m);
						HSIEForm hsie = HSIE.handle(fd);
						forms.add(gen.generate(hsie));
					}
					pos++;
				}
			} else
				throw new UtilException("Need to handle " + x.getKey() + " of type " + val.getClass());
		}
	}

	private List<String> emitPackages(List<JSForm> forms, Scope scope, String defPkg) {
		boolean havePkg = false;
		List<String> plist = new ArrayList<String>();
		for (Entry<String, Object> o : scope) {
			if (o.getValue() instanceof PackageDefn) {
				havePkg = true;
				assertPackage(forms, plist, o.getKey());
			}
		}
		if (!havePkg) {
			assertPackage(forms, plist, defPkg);
		}
		return plist;
	}

	private void assertPackage(List<JSForm> forms, List<String> plist, String key) {
		String keydot = key+".";
		int idx = -1;
		while ((idx = keydot.indexOf('.', idx+1))!= -1) {
			String tmp = keydot.substring(0, idx);
			forms.add(JSForm.packageForm(tmp));
//			plist.add(tmp);
			System.out.println(idx);
		}
		plist.add(key);
	}
	
	private void gatherVars(Set<String> defines, List<Object> args) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern)
				defines.add(((VarPattern)arg).var);
			else if (arg instanceof ConstructorMatch)
				gatherCtor(defines, (ConstructorMatch) arg);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern)
				defines.add(((TypedPattern)arg).var);
			else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(Set<String> defines, ConstructorMatch cm) {
		for (Field x : cm.args) {
			if (x.patt instanceof VarPattern)
				defines.add(((VarPattern)x.patt).var);
			else if (x.patt instanceof ConstructorMatch)
				gatherCtor(defines, (ConstructorMatch)x.patt);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not gathering vars from " + x.patt.getClass());
		}
	}

}
