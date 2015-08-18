package org.flasck.flas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.SetMapMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class TemplateAbstractModel {
	public static class Handler {
		public final int id;
		public final String on;
		public final HSIEForm code;
		
		public Handler(int id, String on, HSIEForm code) {
			this.id = id;
			this.on = on;
			this.code = code;
		}
	}

	public static class Block {
		public final String id;
		public final String ns;
		public final String tag;
		public final Map<String, String> staticAttrs = new TreeMap<String, String>(new StringComparator());
		public String sid = null;
		public Object complexAttrs;
		public String listVar;
		public String name;
		public final List<Handler> handlers;

		public Block(String id, String tag, List<Handler> handlers) {
			this.id = id;
			this.tag = (tag != null)?tag:"div";
			if (this.tag.equals("svg"))
				this.ns = "http://www.w3.org/2000/svg";
			else
				this.ns = null;
			this.handlers = handlers;
		}
	}
	
	public static class OrCase {
		public final HSIEForm expr;
		public final VisualTree tree;

		public OrCase(HSIEForm expr, VisualTree tree) {
			this.expr = expr;
			this.tree = tree;
		}

	}

	public static class VisualTree {
		public int containsThing;
		public Block divThing;
		public List<VisualTree> children = new ArrayList<VisualTree>();
		public final String text;
		
		public VisualTree(Block div, String text) {
			this.divThing = div;
			this.text = text;
		}
	}
	
	public static class AbstractTreeNode {
		public static final int NOTHING = 0;
		public static final int TOP = 1;
		public static final int LIST = 2;
		public static final int CONTENT = 3;
		public static final int CARD = 4;
		public static final int CASES = 5;
		public static final int D3 = 6;
		public final int type;
		public final String id;
		public final String sid;
		public final VisualTree tree;
		public final AbstractTreeNode nestedIn;
		public HSIEForm expr;
		public CardReference card;
		public D3Invoke d3;
		public final List<OrCase> cases = new ArrayList<OrCase>();
		public String var;
		
		public AbstractTreeNode(int type, AbstractTreeNode nestedIn, String id, String sid, VisualTree tree) {
			this.type = type;
			this.nestedIn = nestedIn;
			this.id = id;
			this.sid = sid;
			this.tree = tree;
		}
	}
	
	public final List<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();
	
	private int nextId = 1;
	public final String prefix;
	public final SetMapMap<String, String, String> fields = new SetMapMap<String, String, String>(new StringComparator(), new StringComparator());
	private final Rewriter rewriter;
	public final Scope scope;

	public TemplateAbstractModel(String prefix, Rewriter rewriter, Scope scope) {
		this.prefix = prefix;
		this.rewriter = rewriter;
		this.scope = scope;
	}

	public Block createBlock(String customTag, List<Object> attrs, List<Object> formats, List<Handler> handlers) {
		String name = "block_" + nextId++;
		Block ret = new Block(name, customTag, handlers);
		for (Object o : attrs) {
			if (o instanceof TemplateExplicitAttr) {
				TemplateExplicitAttr tea = (TemplateExplicitAttr) o;
				if (tea.type == TemplateToken.STRING)
					ret.staticAttrs.put(tea.attr, tea.value);
				else
					throw new UtilException("cannot handle attribute value of type " + tea.type);
			} else
				throw new UtilException("cannot handle " + o.getClass() + " as an attribute");
		}
		handleFormats(ret, formats);
		return ret;
	}

	protected void handleFormats(Block ret, List<Object> formats) {
		StringBuilder simple = new StringBuilder();
		Object expr = null;
		for (Object o : formats) {
			if (o instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken) o;
				if (tt.type == TemplateToken.STRING) {
					simple.append(" ");
					simple.append(tt.text);
				} else {
					System.out.println(tt);
					throw new UtilException("Cannot handle format of type " + tt.type);
				}
			} else if (o instanceof ApplyExpr) {
				if (expr == null)
					expr = scope.fromRoot("Nil");
				expr = new ApplyExpr(scope.fromRoot("Cons"), o, expr);
			} else
				throw new UtilException("Cannot handle format of type " + o.getClass());
		}
		if (expr != null) {
			if (simple.length() > 0)
				expr = new ApplyExpr(scope.fromRoot("Cons"), new StringLiteral(simple.substring(1)), expr);
			ret.complexAttrs = expr;
			ret.sid = "sid" + nextId++;
		}
		else if (expr == null && simple.length() > 0)
			ret.staticAttrs.put("class", simple.substring(1));
	}
	
	public String nextSid() {
		return "sid" + nextId++;
	}

	public int ehId() {
		return nextId++;
	}
	
	public void cardMembersCause(VisualTree vt, String action, String fn) {
		if (vt.divThing != null) {
			cardMembersCause(vt.divThing.complexAttrs, action, fn);
		}
		for (VisualTree t : vt.children)
			cardMembersCause(t, action, fn);
	}
	
	public void cardMembersCause(Object expr, String action, String fn) {
		if (expr == null)
			return;
		else if (expr instanceof StringLiteral || expr instanceof AbsoluteVar || expr instanceof TemplateListVar || expr instanceof LocalVar)
			return;
		else if (expr instanceof CardMember) {
			fields.add(((CardMember)expr).var, action, fn);
		} else if (expr instanceof CardFunction) {
			CardFunction cf = (CardFunction) expr;
			String fname = cf.clzName + "." + cf.function;
			if (rewriter.functions.containsKey(fname)) {
				FunctionDefinition func = rewriter.functions.get(fname);
				for (FunctionCaseDefn c : func.cases)
					cardMembersCause(c.expr, action, fn);
			}
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			cardMembersCause(ae.fn, action, fn);
			for (Object a : ae.args)
				cardMembersCause(a, action, fn);
		} else
			throw new UtilException("Cannot handle " + expr.getClass());
	}

	@Override
	public String toString() {
		return "TAM[" + prefix + "]";
	}
}
