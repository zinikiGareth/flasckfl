package org.flasck.flas.compiler.jsgen.creators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class BasicJVMCreationContext implements JVMCreationContext {
	private final ByteCodeSink bcc;
	private final NewMethodDefiner md;
	private final Var runner;
	private final Var cxt;
	private final Var args;
	private final Map<JSExpr, Var> vars = new HashMap<>();
	private final Map<JSExpr, IExpr> stack = new HashMap<>();
	private final Map<Slot, IExpr> slots = new HashMap<>();
	private final Map<JSBlockCreator, IExpr> blocks = new HashMap<>();
	private final boolean isStatic;
	private final boolean isCtor;
	
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing clzName, String name, NameOfThing fnName, boolean isStatic, boolean wantArgumentList, List<JSVar> as, String returnsA, List<JSExpr> superArgs) {
		this.isStatic = isStatic;
		if (fnName == null && name == null) {
			// it's a constructor
			if (clzName instanceof CSName)
				bcc = bce.get(clzName.javaClassName());
			else
				bcc = bce.get(clzName.javaName());
			GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
			PendingVar c1 = null;
			Map<JSVar, PendingVar> tmp = new HashMap<>();
			for (JSVar v : as) {
				PendingVar ai = ann.argument(v.type(), v.asVar());
				tmp.put(v, ai);
				if (v.asVar().equals("_cxt"))
					c1 = ai; 
			}
			md = ann.done();
			cxt = c1.getVar();
			for (Entry<JSVar, PendingVar> e : tmp.entrySet()) {
				vars.put(e.getKey(), e.getValue().getVar());
			}
			args = null;
			this.runner = null;
			this.isCtor = true;
			IExpr[] sas = new IExpr[superArgs.size()];
			int i=0;
			for (JSExpr jv : superArgs) {
				if (jv instanceof JSVar)
					sas[i++] = vars.get(jv);
				else {
					jv.generate(this);
					sas[i++] = argAsIs(jv);
				}
			}
			md.callSuper("void", bcc.getSuperClass(), "<init>", sas).flush();
		} else if (fnName instanceof UnitTestName) {
			bcc = bce.newClass(fnName.javaName());
			bcc.generateAssociatedSourceFile();
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "dotest");
			PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
			PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
			ann.returns(JavaType.void_);
			md = ann.done();
			cxt = c1.getVar();
			args = null;
			this.runner = r1.getVar();
			vars.put(as.get(0), this.runner);
			this.isCtor = false;
		} else if (!isStatic) {
			if (clzName instanceof CSName)
				bcc = bce.get(clzName.javaClassName());
			else
				bcc = bce.get(clzName.javaName());
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, name);
			PendingVar c1 = null;
			PendingVar a1 = null;
			Map<JSVar, PendingVar> tmp = new HashMap<>();
			if (wantArgumentList) {
				c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
				a1 = ann.argument("[" + J.OBJECT, "args");
			} else {
				for (JSVar v : as) {
					PendingVar ai = ann.argument(v.type(), v.asVar());
					tmp.put(v, ai);
					if (v.asVar().equals("_cxt"))
						c1 = ai; 
				}
			}
			ann.returns(returnsA);
			md = ann.done();
			cxt = c1 == null ? null : c1.getVar();
			if (wantArgumentList) {
				args = a1.getVar();
				for (int ap=1;ap<as.size();ap++) {
					JSVar v = as.get(ap);
					stack.put(v, md.arrayElt(args, md.intConst(ap-1)));
				}
			} else {
				args = null;
				for (Entry<JSVar, PendingVar> e : tmp.entrySet()) {
					vars.put(e.getKey(), e.getValue().getVar());
				}
			}
			this.runner = null;
			this.isCtor = false;
		} else {
			if (fnName == null) {
				if (clzName instanceof CSName)
					bcc = bce.getOrCreate(clzName.javaClassName());
				else
					bcc = bce.getOrCreate(clzName.javaName());
			} else
				bcc = bce.getOrCreate(fnName.javaClassName());
			bcc.generateAssociatedSourceFile();
			IFieldInfo fi = bcc.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			fi.constValue(as.size()-1);
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, fnName != null || name == null ? "eval" : name);
			PendingVar c1 = null;
			PendingVar a1 = null;
			Map<JSVar, PendingVar> tmp = new HashMap<>();
			if (wantArgumentList) {
				c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
				a1 = ann.argument("[" + J.OBJECT, "args");
			} else {
				for (JSVar v : as) {
					PendingVar ai = ann.argument(v.type(), v.asVar());
					tmp.put(v, ai);
					if (v.asVar().equals("_cxt"))
						c1 = ai; 
				}
			}
			ann.returns(J.OBJECT);
			md = ann.done();
			cxt = c1 == null ? null : c1.getVar();
			if (wantArgumentList) {
				args = a1.getVar();
				for (int ap=1;ap<as.size();ap++) {
					JSVar v = as.get(ap);
					stack.put(v, md.arrayElt(args, md.intConst(ap-1)));
				}
			} else {
				args = null;
				for (Entry<JSVar, PendingVar> e : tmp.entrySet()) {
					vars.put(e.getKey(), e.getValue().getVar());
				}
			}
			this.runner = null;
			this.isCtor = false;
		}
//		md.lenientMode(true);
	}

	private BasicJVMCreationContext(ByteCodeSink bcc, NewMethodDefiner md, Var runner, Var cxt, Var args, boolean isCtor, boolean isStatic) {
		this.bcc = bcc;
		this.md = md;
		this.runner = runner;
		this.cxt = cxt;
		this.args = args;
		this.isCtor = isCtor;
		this.isStatic = isStatic;
	}

	@Override
	public JVMCreationContext split() {
		BasicJVMCreationContext ret = new BasicJVMCreationContext(bcc, md, runner, cxt, args, isCtor, isStatic);
		ret.vars.putAll(vars);
		ret.stack.putAll(stack);
		ret.slots.putAll(slots);
		ret.blocks.putAll(blocks);
		return ret;
	}
	
	@Override
	public void inherit(boolean isFinal, Access access, String type, String name) {
		bcc.inheritsField(isFinal, access, type, name);
	}
	
	@Override
	public ByteCodeSink clazz() {
		return bcc;
	}
	
	@Override
	public NewMethodDefiner method() {
		return md;
	}

	@Override
	public IExpr helper() {
		return runner;
	}

	@Override
	public IExpr cxt() {
		return cxt;
	}
	
	@Override
	public Var fargs() {
		return args;
	}

	@Override
	public void bindVar(JSExpr local, Var v) {
		vars.put(local, v);
	}

	@Override
	public boolean hasLocal(JSExpr key) {
		return stack.containsKey(key);
	}

	@Override
	public void local(JSExpr key, IExpr e) {
		if (stack.containsKey(key))
			throw new NotImplementedException("duplicate entry for: " + key);
		stack.put(key, e);
	}

	@Override
	public void block(JSBlock key, List<IExpr> blk) {
		blocks.put(key, md.block(blk.toArray(new IExpr[blk.size()])));
	}

	@Override
	public String figureName(NameOfThing fn) {
		String push = null;
		if (fn.container() == null) {
			push = resolveOpName(fn.baseName());
			if (push == null) {
				if (fn instanceof FunctionName)
					push = J.FLEVAL+"$"+fn.baseName();
				else
					push = J.BUILTINPKG+"."+fn.baseName();
			}
		} else if (fn instanceof HandlerName) {
			push = fn.javaName();
		} else
			push = fn.javaClassName();
		return push;
	}

	@Override
	public IExpr stmt(JSExpr stmt) {
		if (!stack.containsKey(stmt))
			throw new NotImplementedException("there is nothing in the stack for " + stmt);
		return stack.get(stmt);
	}

	@Override
	public IExpr arg(JSExpr jsExpr) {
		IExpr a = argAsIs(jsExpr);
		if (a == null)
			return null;
		return md.as(a, J.OBJECT);
	}

	@Override
	public IExpr argAs(JSExpr jsExpr, JavaType type) {
		IExpr a = argAsIs(jsExpr);
		if (a == null)
			return null;
		return md.as(a, type.getActual());
	}

	@Override
	public IExpr argAsIs(JSExpr jsExpr) {
		if (vars.containsKey(jsExpr))
			return vars.get(jsExpr);
		else if (stack.containsKey(jsExpr))
			return stack.get(jsExpr);
		else if (jsExpr instanceof JSLiteral) {
			JSLiteral l = (JSLiteral) jsExpr;
			l.generate(this);
			return stack.get(l);
		} else if (jsExpr instanceof JSString) {
			JSString l = (JSString) jsExpr;
			if (l.value() == null)
				return md.as(md.aNull(), J.STRING);
			else
				return md.stringConst(l.value());
		} else {
			jsExpr.generate(this);
			if (stack.containsKey(jsExpr))
				return stack.get(jsExpr);
			else
				throw new NotImplementedException("there is no var for " + jsExpr.getClass() + " " + jsExpr);
		}
	}

	@Override
	public IExpr blk(JSBlockCreator blk) {
		if (!blocks.containsKey(blk))
			throw new NotImplementedException("there is no block " + blk);
		return blocks.get(blk);
	}

	@Override
	public void done(JSBlockCreator blk) {
		blk(blk).flush();
		// we could avoid this hack by introducing an explicit JSReturnVoid which does nothing for JS but does this for JVM
		if (isCtor) {
			md.returnVoid().flush();
		}
	}

	private String resolveOpName(String op) {
		String inner;
		switch (op) {
		case "&&":
			inner = "And";
			break;
		case "||":
			inner = "Or";
			break;
		case "!":
			inner = "Not";
			break;
		case "==":
			inner = "IsEqual";
			break;
		case ">=":
			inner = "GreaterEqual";
			break;
		case ">":
			inner = "GreaterThan";
			break;
		case "<=":
			inner = "LessEqual";
			break;
		case "<":
			inner = "LessThan";
			break;
		case "+":
			inner = "Plus";
			break;
		case "-":
			inner = "Minus";
			break;
		case "*":
			inner = "Mul";
			break;
		case "/":
			inner = "Div";
			break;
		case "%":
			inner = "Mod";
			break;
		case "++":
			inner = "strAppend";
			break;
		case "[]":
			return J.NIL;
		case "()":
			return "MakeTuple";
		default:
			return null;
		}
		return J.FLEVAL + "$" + inner;
	}
}
