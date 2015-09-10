package org.flasck.flas.jsform;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
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
	private boolean isArray = false;

	public JSForm(String text) {
		this.text = text;
	}

	public JSForm comma() {
		endWith = ",";
		return this;
	}
	
	public JSForm needSemi() {
		endWith = ";";
		return this;
	}

	public JSForm noSemi() {
		endWith = "";
		return this;
	}

	public JSForm needBlock() {
		if (block == null) {
			block = new ArrayList<JSForm>();
			endWith = "";
		}
		return this;
	}

	public JSForm nestArray() {
		if (block != null)
			throw new UtilException("Cannot have both block and array");
		needBlock();
		isArray = true;
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
		toString(w, 0, true, false);
	}

	@Override
	public String toString() {
		try {
			StringWriter ret = new StringWriter();
			this.writeTo(ret);
			return ret.toString();
		} catch (IOException ex) {
			return null;
		}
	}

	private void toString(Writer ret, int ind, boolean indentLine, boolean inArray) throws IOException {
		if (indentLine)
			indent(ret, ind);
		ret.append(text);
		if (block != null) {
			if (isArray)
				ret.append(" [");
			else if (inArray)
				ret.append("{\n");
			else
				ret.append(" {\n");
			boolean indNext = !isArray;
			String sep = "";
			for (JSForm f : block) {
				ret.append(sep);
				f.toString(ret, isArray?ind:ind+2, indNext, isArray);
//				indNext = true;
				if (isArray)
					sep =", ";
			}
			if (inArray || (indentLine && !isArray))
				indent(ret, ind);
			if (isArray)
				ret.append("]");
			else
				ret.append("}");
		}
		ret.append(endWith);
		if (!inArray)
			ret.append("\n");
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

	// Create functions for non-HSIE things (such as templates)
	public static JSForm flexFn(String name, List<String> vars) {
		return new JSForm(name + " = function(" + String.join(", ", vars) + ")").strict();
	}

	public static JSForm flex(String text) {
		return new JSForm(text);
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
		if (ctor.equals("Boolean")) {
			return new JSForm("if (typeof v" + var.idx + " === 'boolean')").needBlock();
		}
		if (ctor.equals("String")) {
			return new JSForm("if (typeof v" + var.idx + " === 'string')").needBlock();
		}
		if (ctor.equals("Any")) {
			return new JSForm("if (v" + var.idx + ")").needBlock();
		}
		return new JSForm("if (v" + var.idx + " && v" + var.idx+"._ctor == '" + ctor +"')").needBlock();
	}

	public static JSForm bind(BindCmd h) {
		return new JSForm("var v" + h.bind.idx + " = v" + h.from.idx + "." + h.field);
	}

	public static JSForm ifCmd(IFCmd c) {
		return new JSForm("if (v" + c.var.var.idx + " === " + c.value +")").needBlock();
	}

	public static JSForm error(String fnName) {
		return new JSForm("return FLEval.error(\""+fnName +": case not handled\")");
	}

	public static void assign(JSForm into, String assgn, HSIEForm form) {
		ReturnCmd r = (ReturnCmd) form.nestedCommands().get(0);
		if (r.fn != null) {
			StringBuilder sb = new StringBuilder(assgn + " = ");
			appendValue(sb, form.mytype, r, 0);
			into.add(new JSForm(sb.toString()));
		} else if (r.var != null) {
			if (r.deps != null) {
				for (CreationOfVar v : r.deps) {
					into.add(new JSForm("var v" + v.var.idx + " = " + closure(form.mytype, form.getClosure(v.var))));
				}
			}
			into.add(new JSForm(assgn + " = " + closure(form.mytype, form.getClosure(r.var.var))));
		}
		else if (r.ival != null)
			into.add(new JSForm(assgn + " = " + r.ival));
		else if (r.sval != null)
			into.add(new JSForm(assgn + " = " + r.sval));
		else if (r.tlv != null)
			// This is specific to accessing the TLV from the template, which seems the most likely case :-)
			// If not, we should probably add more cases to CodeType and distinguish them here
			into.add(new JSForm(assgn + " = this._src_" + r.tlv.name + "." + r.tlv.name));
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
			if (r.var.var.idx < form.nformal) {
				ret.add(new JSForm("return " + r.var.var));
			} else if (r.deps != null) {
				for (CreationOfVar v : r.deps) {
					ret.add(new JSForm("var v" + v.var.idx + " = " + closure(form.mytype, form.getClosure(v.var))));
				}
				ret.add(new JSForm("return " + closure(form.mytype, form.getClosure(r.var.var))));
			}
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

	private static String closure(CodeType fntype, HSIEBlock closure) {
		StringBuilder sb;
		ExternalRef fn = ((PushCmd)closure.nestedCommands().get(0)).fn;
		boolean needsObject = false;
		boolean fromHandler = fntype == CodeType.AREA;
		if (fn != null) {
			if (fn instanceof ObjectReference || fn instanceof CardFunction) {
				needsObject = true;
				fromHandler |= fn.fromHandler();
			} else if (fn.toString().equals("FLEval.curry")) {
				ExternalRef f2 = ((PushCmd)closure.nestedCommands().get(1)).fn;
				if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
					needsObject = true;
					fromHandler |= f2.fromHandler();
				}
			}
		}
		if (needsObject)
			sb = new StringBuilder("FLEval.oclosure(" + (fromHandler?"this._card":"this") + ", ");
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

	private static void appendValue(StringBuilder sb, CodeType fntype, PushReturn c, int pos) {
		if (c.fn != null) {
			if (c.fn instanceof AbsoluteVar)
				sb.append(c.fn.uniqueName());
			else if (c.fn instanceof ObjectReference) {
				sb.append(c.fn.uniqueName());
			} else if (c.fn instanceof CardFunction) {
				String jsname = c.fn.uniqueName();
				int idx = jsname.lastIndexOf(".");
				jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
				sb.append(jsname);
			} else if (c.fn instanceof CardMember) {
				if (fntype == CodeType.CARD || fntype == CodeType.EVENTHANDLER)
					sb.append("this." + ((CardMember)c.fn).var);
				else if (fntype == CodeType.HANDLER || fntype == CodeType.CONTRACT || fntype == CodeType.AREA)
					sb.append("this._card." + ((CardMember)c.fn).var);
				else
					throw new UtilException("Can't handle " + fntype + " for card member");
			}
			else if (c.fn instanceof HandlerLambda) {
				if (fntype == CodeType.HANDLER)
					sb.append("this." + ((HandlerLambda)c.fn).var);
				else
					throw new UtilException("Can't handle " + fntype + " with handler lambda");
			} else
				throw new UtilException("Can't handle " + c.fn + " of type " + c.fn.getClass());
//					sb.append(mapName(c.fn.uniqueName()));
		} else if (c.ival != null)
			sb.append(c.ival);
		else if (c.var != null)
			sb.append("v"+ c.var.var.idx);
		else if (c.sval != null)
			sb.append("'" + c.sval.text + "'");
		else if (c.tlv != null) {
			sb.append("this._src_" + c.tlv.name + "." + c.tlv.name);
		} else if (c.func != null) {
			int x = c.func.name.lastIndexOf('.');
			if (x == -1)
				throw new UtilException("Invalid function name: " + c.func.name);
			else
				sb.append(c.func.name.substring(0, x+1) + "prototype" + c.func.name.substring(x));
		} else if (c.csr != null) {
			if (c.csr.fromHandler)
				sb.append("this._card");
			else
				sb.append("this");
		} else
			throw new UtilException("What are you pushing? " + c);
	}
}
