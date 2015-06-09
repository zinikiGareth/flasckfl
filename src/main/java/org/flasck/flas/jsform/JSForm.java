package org.flasck.flas.jsform;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectRelative;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class JSForm {
	private final String text;
	private String endWith = ";";
	private List<JSForm> block = null;
	private int insertPoint = 0;

	public JSForm(String text) {
		this.text = text;
	}

	public JSForm comma() {
		endWith = ",";
		return this;
	}

	public JSForm needBlock() {
		if (block == null)
			block = new ArrayList<JSForm>();
		return this;
	}

	public JSForm strict() {
		needBlock().add(new JSForm("\"use strict\""));
		insertPoint++;
		return this;
	}

	public JSForm add(JSForm inner) {
		needBlock();
		block.add(inner);
		return this;
	}

	/* Add a sub-form to this form at the "start" of the block,
	 * but after any "use strict" or other inserted items
	 */
	public void insert(JSForm generate) {
		block.add(insertPoint++, generate);
	}

	public JSForm addAll(List<JSForm> inner) {
		needBlock();
		block.addAll(inner);
		return this;
	}

	public void writeTo(Writer w) throws IOException {
		toString(w, 0);
	}

	@Override
	public String toString() {
		try {
			StringWriter ret = new StringWriter();
			this.toString(ret, 0);
			return ret.toString();
		} catch (IOException ex) {
			return null;
		}
	}

	private void toString(Writer ret, int ind) throws IOException {
		indent(ret, ind);
		ret.append(text);
		if (block != null) {
			ret.append(" {\n");
			for (JSForm f : block)
				f.toString(ret, ind+2);
			indent(ret, ind);
			ret.append("}\n");
		} else
			ret.append(endWith + "\n");
	}
	
	private void indent(Writer ret, int ind) throws IOException {
		for (int i=0;i<ind;i++)
			ret.append(' ');
	}

	// TODO: replace this with an FLEVAL function that creates a package & returns it
	// then we can store that in a variable and return that to the user
	// the one thing would be what do we call the variable?  I think we just want
	// to use the top level name & then it gives us that package and the others are
	// nested inside it
	@Deprecated
	public static JSForm packageForm(String key) {
		return new JSForm(key + " = function()").needBlock();
	}

	public static JSForm function(String fnName, List<Var> hsvs, int alreadyUsed, int nformal) {
		List<String> vars = new ArrayList<String>();
		for (int i=0;i<nformal;i++)
			vars.add(hsvs.get(alreadyUsed + i).toString());
		return new JSForm(fnName + " = function(" + String.join(", ", vars) + ")").strict();
	}

	public static List<JSForm> head(Var v) {
		return CollectionUtils.listOf(
			new JSForm("v" + v.idx + " = FLEval.head(v" + v.idx+")"),
			new JSForm("if (v" + v.idx + " instanceof FLError)")
				.add(new JSForm("return v" + v.idx))
		);
	}

	public static JSForm switchOn(String ctor, Var var) {
		if (ctor.equals("Number")) {
			return new JSForm("if (FLEval.isInteger(v" + var.idx + "))").needBlock();
		}
		return new JSForm("if (v" + var.idx + " && v" + var.idx+"._ctor == " + ctor +")").needBlock();
	}

	public static JSForm bind(BindCmd h) {
		return new JSForm("var v" + h.bind.idx + " = v" + h.from.idx + "." + h.field);
	}

	public static JSForm ifCmd(IFCmd c) {
		return new JSForm("if (v" + c.var.idx + " === " + c.value +")").needBlock();
	}

	public static JSForm error(String fnName) {
		return new JSForm("return FLEval.error(\""+fnName +": case not handled\")");
	}

	public static void assign(JSForm into, String assgn, HSIEForm form) {
		ReturnCmd r = (ReturnCmd) form.nestedCommands().get(0);
		if (r.fn != null)
			into.add(new JSForm(assgn + " = " + r.fn));
		else if (r.var != null) {
			if (r.deps != null) {
				for (Var v : r.deps) {
					into.add(new JSForm("var v" + v.idx + " = " + closure(form.mytype, form.getClosure(v))));
				}
			}
			into.add(new JSForm(assgn + " = " + closure(form.mytype, form.getClosure(r.var))));
		}
		else if (r.ival != null)
			into.add(new JSForm(assgn + " = " + r.ival));
		else
			throw new UtilException("What are you returning " + r);
	}

	public static List<JSForm> ret(ReturnCmd r, HSIEForm form) {
		List<JSForm> ret = new ArrayList<JSForm>();
		StringBuilder sb = new StringBuilder();
		sb.append("return ");
//		if (r.fn != null)
//			ret.add(new JSForm("return " + r.fn));
		if (r.var != null) {
			if (r.deps != null) {
				for (Var v : r.deps) {
					ret.add(new JSForm("var v" + v.idx + " = " + closure(form.mytype, form.getClosure(v))));
				}
			}
			ret.add(new JSForm("return " + closure(form.mytype, form.getClosure(r.var))));
		} else {
			appendValue(sb, form.mytype, r, 0);
//		else if (r.ival != null)
//			ret.add(new JSForm("return " + r.ival));
//		else if (r.sval != null)
//			ret.add(new JSForm("return '" + r.sval.text + "'"));
//		else
//			throw new UtilException("What are you returning " + r);
			ret.add(new JSForm(sb.toString()));
		}
		return ret;
	}

	private static String closure(Type fntype, HSIEBlock closure) {
		StringBuilder sb;
		ExternalRef fn = ((PushCmd)closure.nestedCommands().get(0)).fn;
		if (fn instanceof ObjectRelative)
			sb = new StringBuilder("FLEval.oclosure(" + (((ObjectRelative)fn).fromHandler?"this._card":"this") + ", ");
		else
			sb = new StringBuilder("FLEval.closure(");
		int pos = 0;
		boolean isField = false;
		for (HSIEBlock b : closure.nestedCommands()) {
			PushCmd c = (PushCmd) b;
			if (pos > 0)
				sb.append(", ");
			if (c.fn != null && pos == 0) {
				isField = "FLEval.field".equals(c.fn);
			}
			 if (c.fn != null && isField && pos == 2)
				sb.append("'" + c.fn + "'");
			else
				appendValue(sb, fntype, c, pos);
			pos++;
		}
		sb.append(")");
		return sb.toString();
	}

	private static void appendValue(StringBuilder sb, Type fntype, PushReturn c, int pos) {
		if (c.fn != null) {
			if (c.fn instanceof AbsoluteVar)
				sb.append(c.fn.uniqueName());
			else if (c.fn instanceof ObjectRelative) {
				sb.append(c.fn.uniqueName());
			} else if (c.fn instanceof CardMember) {
				if (fntype == Type.CARD || fntype == Type.EVENTHANDLER)
					sb.append("this." + ((CardMember)c.fn).var);
				else if (fntype == Type.HANDLER || fntype == Type.CONTRACT)
					sb.append("this._card." + ((CardMember)c.fn).var);
				else
					throw new UtilException("Can't handle " + fntype + " with card member");
			}
			else if (c.fn instanceof HandlerLambda) {
				if (fntype == Type.HANDLER)
					sb.append("this." + ((HandlerLambda)c.fn).var);
				else
					throw new UtilException("Can't handle " + fntype + " with handler lambda");
			} else
				throw new UtilException("Can't handle " + c.fn + " of type " + c.fn.getClass());
//					sb.append(mapName(c.fn.uniqueName()));
		} else if (c.ival != null)
			sb.append(c.ival);
		else if (c.var != null)
			sb.append("v"+ c.var.idx);
		else if (c.sval != null)
			sb.append("'" + c.sval.text + "'");
		else
			throw new UtilException("What are you pushing? " + c);
	}

	/*
	private static String mapName(String fn) {
//		System.out.println("Need to map " + fn);
		if (fn.startsWith("_card"))
			return "this."+fn;
		else if (fn.startsWith("_handler"))
			return "this"+fn.substring(8);
		else if (fn.startsWith("_scoped."))
			return fn.substring(8);
		return fn;
	}
	*/
}
