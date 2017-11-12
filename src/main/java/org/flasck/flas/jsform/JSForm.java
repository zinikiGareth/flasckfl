package org.flasck.flas.jsform;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ClosureGenerator;
import org.flasck.flas.vcode.hsieForm.CurryClosure;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Var;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.exceptions.UtilException;

public class JSForm {

	private static Map<String, String> renamers = new HashMap<>();
	private final String text;
	private String endWith = ";";
	private List<JSForm> block = null;
	private int insertPoint = 0;
	private boolean isArray = false;
	
	static {
		renamers.put("concat", "StdLib.concat");
		renamers.put("==", "FLEval.compeq");
		renamers.put("++", "append");
		renamers.put("+", "FLEval.plus");
		renamers.put("-", "FLEval.minus");
		renamers.put("*", "FLEval.mul");
		renamers.put("/", "FLEval.div");
		renamers.put("^", "FLEval.exp");
	}

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
	public static JSForm packageForm(String key) {
		JSForm ret = JSForm.flex("if (typeof " + key + " === 'undefined')").needBlock();
		ret.add(new JSForm(key + " = function()").needBlock());
		return ret;
	}

	public static JSForm function(String fnName, List<Var> hsvs, Set<ScopedVar> scoped, int nformal) {
		List<String> vars = new ArrayList<String>();
		for (int j=0;j<scoped.size();j++)
			vars.add("s"+j);
		for (int i=0;i<nformal;i++)
			vars.add(hsvs.get(i).toString());
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
		return Arrays.asList(
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
		return new JSForm("if (FLEval.isA(v" + var.idx + ", '" + ctor +"'))").needBlock();
	}

	public static JSForm bind(BindCmd h) {
		return new JSForm("var v" + h.bind.idx + " = v" + h.from.idx + "." + h.field);
	}

	public static List<JSForm> ifCmd(HSIEForm form, IFCmd c) {
		List<JSForm> ret = new ArrayList<JSForm>();
		ClosureGenerator clos = form.getClosure(c.var.var);
		if (clos != null) {
			closure(form, clos, new OutputHandler<String>() {
				@Override
				public void result(String expr) {
					ret.add(new JSForm("var v" + c.var.var.idx + " = " + expr));
				}
			});
		}
		if (c.value != null)
			ret.add(new JSForm("if (v" + c.var.var.idx + " === " + c.value + ")").needBlock());
		else
			ret.add(new JSForm("if (v" + c.var.var.idx + ")").needBlock());
		return ret;
	}

	public static JSForm error(FunctionName fnName) {
		return new JSForm("return FLEval.error(\""+fnName.jsName() +": case not handled\")");
	}

	public static List<JSForm> ret(HSIEForm form, PushReturn r) {
		List<JSForm> ret = new ArrayList<JSForm>();
		StringBuilder sb = new StringBuilder();
		sb.append("return ");
		if (r instanceof PushVar) {
			PushVar pv = (PushVar)r;
			if (pv.var.var.idx < form.nformal) {
				ret.add(new JSForm("return " + pv.var.var));
			} else if (pv.deps != null) {
				for (VarInSource v : pv.deps) {
					closure(form, form.getClosure(v.var), new OutputHandler<String>() {
						@Override
						public void result(String expr) {
							ret.add(new JSForm("var v" + v.var.idx + " = " + expr));
						}
					});
				}
				closure(form, form.getClosure(pv.var.var), new OutputHandler<String>() {
					@Override
					public void result(String expr) {
						ret.add(new JSForm("return " + expr));
					}
				});
			}
		} else {
			appendValue(form, sb, r, 0);
			ret.add(new JSForm(sb.toString()));
		}
		return ret;
	}

	private static void closure(HSIEForm form, ClosureGenerator closure, OutputHandler<String> handler) {
		if (closure instanceof CurryClosure) {
			CurryClosure curry = (CurryClosure)closure;
			curry.handleCurry(form.needsCardMember(), new JSCurryHandler(form), handler);
			return;
		}
		StringBuilder sb;
		HSIEBlock c0 = closure.nestedCommands().get(0);
		boolean needsObject = false;
		boolean fromHandler = false;
		if (c0 instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)c0).fn;
			if (fn instanceof ObjectReference || fn instanceof CardFunction) {
				needsObject = true;
				fromHandler |= fn.fromHandler();
			} else if (fn.toString().equals("FLEval.curry")) {
				ExternalRef f2 = ((PushExternal)closure.nestedCommands().get(1)).fn;
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
			PushReturn c = (PushReturn) b;
			if (pos > 0)
				sb.append(", ");
			if (c instanceof PushExternal && pos == 0) {
				isField = "FLEval.field".equals(((PushExternal)c).fn.uniqueName());
			}
			if (c instanceof PushExternal && isField && pos == 2)
				sb.append("'" + ((PushExternal)c).fn + "'");
			else
				appendValue(form, sb, c, pos);
			pos++;
		}
		sb.append(")");
		handler.result(sb.toString());
	}
	
	public static String rename(String fn) {
		if (renamers .containsKey(fn))
			return renamers.get(fn);
		else
			return fn;
	}

	static void appendValue(HSIEForm form, final StringBuilder sb, PushReturn c, int pos) {
		c.visit(new JSPushArgument(form, sb), new OutputHandler<String>() {
			@Override
			public void result(String expr) {
			}
		});
	}
}
