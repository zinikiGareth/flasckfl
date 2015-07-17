package org.flasck.flas;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.collections.ListMapMap;

public class TemplateAbstractModel {
	public static class Base {
		public final String id;

		public Base(String id) {
			this.id = id;
		}
	}

	public static class Struct extends Base {
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

	public static class Content extends Base {
		public final String struct;
		public final String sid;
		public final String span;
		public final HSIEForm expr;

		public Content(String id, Struct inside, HSIEForm expr) {
			super(id);
			this.expr = expr;
			struct = inside.id;
			sid = "sid" + inside.nextId();
			span = "span" + inside.nextId();
		}
	}
	
	private int nextId = 1;
	final public List<Base> contents = new ArrayList<Base>();
	public final String prefix;
	public final ListMapMap<String, String, String> fields = new ListMapMap<String, String, String>();
//	public final Map<String, Map<String, List<String>>> fields = new HashMap<String, Map<String, List<String>>>();

	public TemplateAbstractModel(String prefix) {
		this.prefix = prefix;
	}

	public Struct createStruct() {
		Struct s = new Struct("struct_" + nextId++);
		contents.add(s);
		return s;
	}

	public Content createContent(Struct inside, HSIEForm expr) {
		String name = "content_" + nextId++;
		Content s = new Content(name, inside, expr);
		contents.add(s);
		for (Object x : expr.externals) {
			if (x instanceof CardMember) {
				CardMember y = (CardMember)x;
				fields.add(y.var, "assign", y.card+".prototype._"+ name);
			}
		}
		return s;
	}
}
