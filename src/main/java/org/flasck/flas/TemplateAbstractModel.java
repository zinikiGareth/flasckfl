package org.flasck.flas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.ListMapMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class TemplateAbstractModel {
	public static interface Addable {
		public void add(Base b);
		public String id();
		public int nextId();
	}
	
	public static class Base {
		public final String id;

		public Base(String id) {
			this.id = id;
		}
		
		public String id() {
			return id;
		}
	}

	public static class Struct extends Base implements Addable {
		public final List<Base> children = new ArrayList<Base>();
		private int vid = 1;

		public Struct(String id) {
			super(id);
		}

		public void add(Base b) {
			children.add(b);
		}
		
		public int nextId() {
			return vid++;
		}
	}

	public abstract class Formattable extends Base {
		public final String tag;
		public final Map<String, String> staticAttrs = new TreeMap<String, String>(new StringComparator());
		public String sid = null;
		public boolean complexAttrs = false;

		public Formattable(String id, String tag) {
			super(id);
			this.tag = tag;
		}
	}
	
	public class Block extends Formattable {
		public final String parent;
		public final Map<String, HSIEForm> handlers;

		public Block(String id, String parent, String customTag, Map<String, HSIEForm> handlers) {
			super(id, (customTag != null)?customTag:"div");
			this.parent = parent;
			this.handlers = handlers;
		}
	}
	
	public class ULList extends Formattable implements Addable {
		public final List<Base> children = new ArrayList<Base>();
		public final String parent;
		public final String struct;
		private final Addable inside;

		public ULList(String id, Addable inside, String parent) {
			super(id, "ul");
			this.inside = inside;
			this.parent = parent;
			this.struct = inside.id();
			sid = "sid" + inside.nextId();
		}

		public void add(Base b) {
			children.add(b);
		}

		public int nextId() {
			return inside.nextId();
		}
	}

	public static class Content extends Base {
		public final String struct;
		public final String sid;
		public final String span;
		public final HSIEForm expr;
		public final String parent;

		public Content(String id, Addable inside, String inDiv, HSIEForm expr) {
			super(id);
			this.expr = expr;
			struct = inside.id();
			parent = inDiv;
			sid = "sid" + inside.nextId();
			span = "span" + inside.nextId();
		}
	}

	public static class VisualTree {
		public int containsThing;
		public Block divThing;
		public List<VisualTree> children = new ArrayList<VisualTree>();
		
		public VisualTree(Block div) {
			this.divThing = div;
		}
	}
	
	public static class AbstractTreeNode {
		public static final int NOTHING = 0;
		public static final int TOP = 1;
		public static final int LIST = 2;
		public static final int CONTENT = 3;
		public final int type;
		public final String id;
		public final String sid;
		public final VisualTree tree;
		public final List<Object> slotUpdaters = new ArrayList<Object>();
		public final List<Object> slotFormatters = new ArrayList<Object>();
		public final AbstractTreeNode nestedIn;
		public HSIEForm expr;
		
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
	public final Struct root;
	public final String prefix;
	public final ListMapMap<String, String, String> fields = new ListMapMap<String, String, String>();
//	public final Map<String, Map<String, List<String>>> fields = new HashMap<String, Map<String, List<String>>>();

	public TemplateAbstractModel(String prefix) {
		this.prefix = prefix;
		this.root = createStruct();
	}

	public Struct createStruct() {
		Struct s = new Struct("struct_" + nextId++);
		return s;
	}

	public Content createContent(Addable inside, String inDiv, HSIEForm expr) {
		String name = "content_" + nextId++;
		Content s = new Content(name, inside, inDiv, expr);
		for (Object x : expr.externals) {
			if (x instanceof CardMember) {
				CardMember y = (CardMember)x;
				fields.add(y.var, "assign", y.card+".prototype._"+ name);
			}
		}
		return s;
	}

	public ULList createList(Addable parent, String parentDiv, List<Object> formats) {
		String name = "list_" + nextId++;
		ULList ret = new ULList(name, parent, parentDiv);
		handleFormats(parent, ret, formats);
		return ret;
	}
	
	public Block createBlock(Addable parent, String parentDiv, String customTag, List<Object> attrs, List<Object> formats, Map<String, HSIEForm> handlers) {
		String name = "block_" + nextId++;
		Block ret = new Block(name, parentDiv, customTag, handlers);
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
		handleFormats(parent, ret, formats);
		return ret;
	}

	protected void handleFormats(Addable inside, Formattable ret, List<Object> formats) {
		boolean allSimple = true;
		StringBuilder simple = new StringBuilder();
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
				System.out.println("Need to handle expression formats");
				allSimple = false;
			} else
				throw new UtilException("Cannot handle format of type " + o.getClass());
		}
		if (!allSimple) {
			ret.complexAttrs = true;
			ret.sid = "sid" + nextId++; // inside.nextId();
		}
		else if (allSimple && simple.length() > 0)
			ret.staticAttrs.put("class", simple.substring(1));
	}
	
	public String nextSid() {
		return "sid" + nextId++;
	}
	
	@Override
	public String toString() {
		return "TAM[" + prefix + "]";
	}
}
