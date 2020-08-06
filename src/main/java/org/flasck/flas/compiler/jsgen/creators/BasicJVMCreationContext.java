package org.flasck.flas.compiler.jsgen.creators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
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
	
	public BasicJVMCreationContext(ByteCodeEnvironment bce, NameOfThing fnName, boolean isStatic, int ac) {
		if (ac == -420)
			throw new NotImplementedException();
		if (fnName instanceof UnitTestName) {
			bcc = bce.newClass(fnName.javaName());
			bcc.generateAssociatedSourceFile();
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "dotest");
			PendingVar r1 = ann.argument(J.TESTHELPER, "runner");
			PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
			ann.returns(JavaType.void_);
			md = ann.done();
			cxt = c1.getVar();
			args = null;
			runner = r1.getVar();
		} else if (!isStatic) {
			throw new NotImplementedException();
		} else {
			bcc = bce.newClass(fnName.javaClassName());
			bcc.generateAssociatedSourceFile();
			IFieldInfo fi = bcc.defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			fi.constValue(ac);
			GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar c1 = ann.argument(J.FLEVALCONTEXT, "cxt");
			PendingVar a1 = ann.argument("[" + J.OBJECT, "args");
			ann.returns(J.OBJECT);
			md = ann.done();
			cxt = c1.getVar();
			args = a1.getVar();
			runner = null;
		}
//		md.lenientMode(true);
	}

	private BasicJVMCreationContext(ByteCodeSink bcc, NewMethodDefiner md, Var runner, Var cxt, Var args) {
		this.bcc = bcc;
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
	public void recordSlot(Slot s, IExpr e) {
		slots.put(s, e);
	}

	@Override
	public void bindVar(JSExpr local, Var v) {
		vars.put(local, v);
	}

	@Override
	public void local(JSExpr key, IExpr e) {
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
			if (push == null)
				push = J.BUILTINPKG+"."+fn.baseName();
		} else
			push = fn.javaClassName();
		return push;
	}

	@Override
	public IExpr slot(Slot slot) {
		IExpr ret = slots.get(slot);
		if (ret == null) {
			if (slot instanceof ArgSlot) {
				int ap = ((ArgSlot) slot).argpos();
				ret = md.arrayElt(args, md.intConst(ap));
			} else
				throw new NotImplementedException("there is nothing in slot " + slot);
		}
		return ret;
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
			try {
				int x = Integer.parseInt(l.asVar());
				return md.makeNew(J.NUMBER, md.box(md.intConst(x)), md.castTo(md.aNull(), "java.lang.Double"));
			} catch (NumberFormatException ex) {
				throw new NotImplementedException("non-integer cases");
			}
		} else if (jsExpr instanceof JSString) {
			JSString l = (JSString) jsExpr;
			return md.stringConst(l.value());
		} else
			throw new NotImplementedException("there is no var for " + jsExpr.getClass() + " " + jsExpr);
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
		default:
			return null;
		}
		return J.FLEVAL + "$" + inner;
	}
}
