package org.flasck.flas.jsform;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushCmd;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class JSForm {
	private final String text;
	private List<JSForm> block = null;

	public JSForm(String text) {
		this.text = text;
	}

	private JSForm needBlock() {
		if (block == null)
			block = new ArrayList<JSForm>();
		return this;
	}

	public JSForm add(JSForm inner) {
		needBlock();
		block.add(inner);
		return this;
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
			ret.append(";\n");
	}
	
	private void indent(Writer ret, int ind) throws IOException {
		for (int i=0;i<ind;i++)
			ret.append(' ');
	}

	public static JSForm function(String fnName, int nformal) {
		List<String> vars = new ArrayList<String>();
		for (int i=0;i<nformal;i++)
			vars.add("v"+i);
		return new JSForm(fnName + " = function(" + String.join(", ", vars) + ")").needBlock();
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

	public static List<JSForm> ret(ReturnCmd r, HSIEForm form) {
		List<JSForm> ret = new ArrayList<JSForm>();
		if (r.fn != null)
			ret.add(new JSForm("return " + r.fn));
		else if (r.var != null) {
			if (r.deps != null) {
				for (Var v : r.deps) {
					ret.add(new JSForm("var v" + v.idx + " = " + closure(form.getClosure(v))));
				}
			}
			ret.add(new JSForm("return " + closure(form.getClosure(r.var))));
		}
		else if (r.ival != null)
			ret.add(new JSForm("return " + r.ival));
		else
			throw new UtilException("What are you returning " + r);
		return ret;
	}

	private static String closure(HSIEBlock closure) {
		StringBuilder sb = new StringBuilder("FLEval.closure(");
		String sep = "";
		for (HSIEBlock b : closure.nestedCommands()) {
			PushCmd c = (PushCmd) b;
			sb.append(sep);
			if (c.fn != null)
				sb.append(mapName(c.fn));
			else if (c.ival != null)
				sb.append(c.ival);
			else if (c.var != null)
				sb.append("v"+ c.var.idx);
			else
				throw new UtilException("What are you pushing? " + c);
			sep = ", ";
		}
		sb.append(")");
		return sb.toString();
	}

	private static String mapName(String fn) {
		switch (fn) {
		// Something like this will need to happen for builtin operators
		case "-":
			return "FLEval.minus";
			
		// This is a hack
		case "Cons":
			return "List.cons";
		default:
			return fn;
		}
	}
}
