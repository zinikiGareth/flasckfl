package org.flasck.flas.compiler.jsgen.creators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.JavaMethodNameProvider;
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
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class BasicJVMCreationContext implements JVMCreationContext {
	private final ByteCodeSink bcc;
	private final NewMethodDefiner md;
	private final Var runner;
	private Var cxt;
	private final Var args;
	private final Map<JSExpr, Var> vars = new HashMap<>();
	private final Map<JSExpr, IExpr> stack = new HashMap<>();
	private final Map<Slot, IExpr> slots = new HashMap<>();
	private final Map<JSBlockCreator, IExpr> blocks = new HashMap<>();
	private final boolean isCtor;

	static class MethodCxt {
		ByteCodeSink bcc;
		GenericAnnotator ann;
		String returnsA;
	}

	// creating a unit test
	public BasicJVMCreationContext(ByteCodeEnvironment bce, UnitTestName fnName, List<JSVar> as) {
		bcc = bce.newClass(fnName.javaName());
		bcc.generateAssociatedSourceFile();
		isCtor = false;
		GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "dotest");
		PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
		PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
		ann.returns(JavaType.void_);
		md = ann.done();
		cxt = c1.getVar();
		args = null;
		this.runner = r1.getVar();
		vars.put(as.get(0), this.runner);
//		md.lenientMode(true);
	}

	// ctor
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing clzName, List<JSVar> as, List<JSExpr> superArgs) {
		bcc = bce.get(clzName.javaName());
		bcc.generateAssociatedSourceFile();
		isCtor = true;
		GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
		PendingVar c1 = null;
		PendingVar r1 = null;
		Map<JSVar, PendingVar> tmp = new HashMap<>();
		for (JSVar v : as) {
			PendingVar ai = ann.argument(v.type(), v.asVar());
			tmp.put(v, ai);
			if (v.asVar().equals("_cxt"))
				c1 = ai; 
			else if (v.asVar().equals("runner"))
				r1 = ai; 
		}
		md = ann.done();
		if (c1 != null)
			cxt = c1.getVar();
		if (r1 != null)
			runner = r1.getVar();
		else
			runner = null;
		for (Entry<JSVar, PendingVar> e : tmp.entrySet()) {
			vars.put(e.getKey(), e.getValue().getVar());
		}
		args = null;
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
	}

	// class member
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing clzName, String name, NameOfThing fnName, boolean wantArgumentList, List<JSVar> as, String returnsA) {
		this(figureMemberClassThings(bce, clzName, name, fnName, returnsA), wantArgumentList, as);
	}

	private static MethodCxt figureMemberClassThings(ByteCodeEnvironment bce, NameOfThing clzName, String name, NameOfThing fnName, String returnsA) {
		MethodCxt ret = new MethodCxt();
		ret.returnsA = returnsA;
		if (fnName == null) {
			ret.bcc = bce.getOrCreate(clzName.javaName());
			ret.ann = GenericAnnotator.newMethod(ret.bcc, false, name);
		} else {
			ret.bcc = bce.getOrCreate(fnName.javaClassName());
			ret.ann = GenericAnnotator.newMethod(ret.bcc, false, ((JavaMethodNameProvider)fnName).javaMethodName());
		}
		return ret;
	}
	
	// "static" function
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing clzName, String name, NameOfThing fnName, boolean wantArgumentList, List<JSVar> as) {
		this(figureStaticClassThings(bce, fnName, clzName, name, as.size()-1), wantArgumentList, as);
	}

	private static MethodCxt figureStaticClassThings(ByteCodeEnvironment bce, NameOfThing fnName, NameOfThing clzName, String name, int nfargs) {
		MethodCxt ret = new MethodCxt();
		ret.returnsA = J.OBJECT;
		if (fnName == null) {
			ret.bcc = bce.getOrCreate(clzName.javaName());
		} else
			ret.bcc = bce.getOrCreate(fnName.javaName());
		ret.bcc.generateAssociatedSourceFile();
		IFieldInfo fi = ret.bcc.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
		fi.constValue(nfargs);
		ret.ann = GenericAnnotator.newMethod(ret.bcc, true, fnName != null || name == null ? "eval" : name);
		return ret;
	}

	// common to anything that is in a class (static or member)
	private BasicJVMCreationContext(MethodCxt mc, boolean wantArgumentList, List<JSVar> as) {
		this.bcc = mc.bcc;
		isCtor = false;
		PendingVar c1 = null;
		PendingVar a1 = null;
		Map<JSVar, PendingVar> tmp = new HashMap<>();
		if (wantArgumentList) {
			c1 = mc.ann.argument(J.FLEVALCONTEXT, "cxt");
			a1 = mc.ann.argument("[" + J.OBJECT, "args");
		} else {
			for (JSVar v : as) {
				PendingVar ai = mc.ann.argument(v.type(), v.asVar());
				tmp.put(v, ai);
				if (v.asVar().equals("_cxt"))
					c1 = ai; 
			}
		}
		mc.ann.returns(mc.returnsA);
		md = mc.ann.done();
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
//		md.lenientMode(true);
	}

	// split for if true/false blocks
	private BasicJVMCreationContext(ByteCodeSink bcc, NewMethodDefiner md, Var runner, Var cxt, Var args) {
		this.bcc = bcc;
		isCtor = false;
		this.md = md;
		this.runner = runner;
		this.cxt = cxt;
		this.args = args;
	}

	@Override
	public JVMCreationContext split() {
		BasicJVMCreationContext ret = new BasicJVMCreationContext(bcc, md, runner, cxt, args);
		ret.vars.putAll(vars);
		ret.stack.putAll(stack);
		ret.slots.putAll(slots);
		ret.blocks.putAll(blocks);
		return ret;
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
	public void setCxt(Var cxt) {
		this.cxt = cxt;
	}

	@Override
	public Var fargs() {
		return args;
	}

	@Override
	public boolean isCtor() {
		return isCtor;
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
		if (key == null)
			throw new CantHappenException("cannot have key be null");
		if (stack.containsKey(key))
			throw new CantHappenException("duplicate entry for: " + key);
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
					push = J.BUILTINPKGFNS+"."+fn.baseName();
				else
					push = J.BUILTINPKG+"."+fn.baseName();
			}
		} else
			push = fn.javaName();
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
		case "{}":
			inner = "MakeHash";
			break;
		case ":":
			inner = "hashPair";
			break;
		default:
			return null;
		}
		return J.BUILTINPKGFNS + "." + inner;
	}
}
