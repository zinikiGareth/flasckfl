package org.flasck.flas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;

public class Rewriter {
	private abstract class NamingContext {
		protected final Set<String> defines = new HashSet<String>();
		protected final NamingContext nested;
		
		public NamingContext(NamingContext inner) {
			this.nested = inner;
		}

		public void add(String s) {
			defines.add(s);
		}
		
		public String resolve(String name) {
			if (defines.contains(name)) {
				return this.makeName(name);
			} else
				return ((NamingContext)nested).resolve(name);
		}

		protected abstract String makeName(String name);
	}
	
	class RootContext extends NamingContext {
		private Scope scope;

		public RootContext(Scope scope) {
			super(null);
			this.scope = scope;
		}

		@Override
		public void add(String s) {
			throw new UtilException("Cannot add names to this scope");
		}

		@Override
		public String resolve(String name) {
			return scope.resolve(name);
		}

		@Override
		protected String makeName(String name) {
			throw new UtilException("Cannot have names in this scope");
		}
		
	}
	
	class CardContext extends NamingContext {
		CardContext(RootContext scope) {
			super(scope);
		}

		@Override
		protected String makeName(String name) {
			return "_card."+name;
		}
	}
	
	class HandlerContext extends NamingContext {
		HandlerContext(CardContext card) {
			super(card);
		}

		@Override
		protected String makeName(String name) {
			return "_handler."+name;
		}
	}
	
	class FunctionContext extends NamingContext {
		FunctionContext(NamingContext cx) {
			super(cx);
		}

		@Override
		public void add(String s) {
			NamingContext nc = this;
			while (nc != null && nc instanceof FunctionContext) {
				if (nc.defines.contains(s))
					throw new UtilException("Cannot define variable " + s + " multiple times in nested scopes");
				nc = nc.nested;
			}
		}
		
		@Override
		protected String makeName(String name) {
			return name;
		}
	}
	
	public void rewrite(Scope scope) {
		RootContext cx = new RootContext(scope);
		for (Entry<String, Object> x : scope) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof CardDefinition) {
				CardContext c2 = new CardContext(cx);
				CardDefinition cd = (CardDefinition) val;
				// TODO: Gather locally defined things 
				if (cd.state != null) {
					List<StructField> l = new ArrayList<StructField>(cd.state.fields);
					cd.state.fields.clear();
					for (StructField sf : l) {
						cd.state.fields.add(rewrite(cx, sf));
						c2.add(sf.name);
					}
				}
				for (ContractImplements ci : cd.contracts) {
					c2.add(ci.referAsVar);
				}
				for (HandlerImplements hi : cd.handlers) {
					c2.add(basename(hi.type));
				}
				if (cd.template != null) {
					System.out.println("Don't rewrite template yet");
				}
				List<ContractImplements> l = new ArrayList<ContractImplements>(cd.contracts);
				cd.contracts.clear();
				for (ContractImplements ci : l) {
					cd.contracts.add(rewriteCI(c2, ci));
				}
				List<HandlerImplements> ll = new ArrayList<HandlerImplements>(cd.handlers);
				cd.handlers.clear();
				for (HandlerImplements hi : ll) {
					cd.handlers.add(rewriteHI(c2, hi));
				}
			} else
				System.out.println("Can't rewrite " + name + " of type " + val.getClass());
		}
	}

	public static String basename(String type) {
		return type.substring(type.lastIndexOf(".")+1);
	}

	private ContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		ContractImplements ret = new ContractImplements(cx.resolve(ci.type), ci.referAsVar);
		rewrite(cx, ret, ci);
		return ret;
	}

	private HandlerImplements rewriteHI(CardContext cx, HandlerImplements hi) {
		HandlerImplements ret = new HandlerImplements(cx.resolve(hi.type), hi.boundVars);
		NamingContext c2 = new HandlerContext(cx);
		c2.defines.addAll(hi.boundVars);
		rewrite(c2, ret, hi);
		return ret;
	}

	private void rewrite(NamingContext scope, Implements into, Implements orig) {
		System.out.println("Rewriting " + orig.type + " to " + into.type);
		for (MethodDefinition m : orig.methods) {
			into.methods.add(rewrite(scope, m));
		}
	}

	private MethodDefinition rewrite(NamingContext scope, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(scope, c));
		}
		return new MethodDefinition(m.intro, list);
	}

	private MethodCaseDefn rewrite(NamingContext cx, MethodCaseDefn c) {
		MethodCaseDefn ret = new MethodCaseDefn(rewrite(cx, c.intro));
		NamingContext c2 = new FunctionContext(cx);
		gatherVars(c2.defines, c.intro.args);
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(c2, mm));
		return ret;
	}

	private FunctionIntro rewrite(NamingContext scope, FunctionIntro intro) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(scope, o));
		}
		return new FunctionIntro(intro.name, args);
	}

	private Object rewritePattern(NamingContext scope, Object o) {
		if (o instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) o;
			return new TypedPattern(scope.resolve(tp.type), tp.var);
		} else {
			System.out.println("Couldn't rewrite pattern " + o.getClass());
			return o;
		}
	}

	private MethodMessage rewrite(NamingContext cx, MethodMessage mm) {
		List<String> newSlot = null;
		if (mm.slot != null && !mm.slot.isEmpty()) {
			newSlot = new ArrayList<String>();
			newSlot.add(cx.resolve(mm.slot.get(0)));
			for (int i=1;i<mm.slot.size();i++)
				newSlot.add(mm.slot.get(i));
		}
		return new MethodMessage(newSlot, rewriteExpr(cx, mm.expr));
	}

	private StructField rewrite(NamingContext scope, StructField sf) {
		return new StructField(rewriteType(scope, sf.type), sf.name, rewriteExpr(scope, sf.init));
	}

	private Object rewriteExpr(NamingContext scope, Object expr) {
		if (expr instanceof ItemExpr) {
			ItemExpr ie = (ItemExpr) expr;
			if (ie.tok.type == ExprToken.NUMBER)
				return ie;
			else if (ie.tok.type == ExprToken.IDENTIFIER)
				return new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, scope.resolve(ie.tok.text)));
			else
				return ie;  // symbol or punc
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			if (ae.fn instanceof ItemExpr && ((ItemExpr)ae.fn).tok.text.equals(".")) {
				return new ApplyExpr(ae.fn, rewriteExpr(scope, ae.args.get(0)), ae.args.get(1));
			}
			List<Object> args = new ArrayList<Object>();
			for (Object o : ae.args)
				args.add(rewriteExpr(scope, o));
			return new ApplyExpr(rewriteExpr(scope, ae.fn), args);
		}
		System.out.println("Can't rewrite expr " + expr + " of type " + expr.getClass());
		return expr;
	}

	private Object rewriteType(NamingContext scope, Object type) {
		if (type instanceof TypeReference) {
			TypeReference tr = (TypeReference) type;
			TypeReference ret = new TypeReference(scope.resolve(tr.name));
			for (Object o : tr.args)
				ret.args.add(rewriteType(scope, o));
			return ret;
		}
		System.out.println("Can't rewrite type " + type + " of type " + type.getClass());
		return type;
	}
	
	private void gatherVars(Set<String> defines, List<Object> args) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern)
				defines.add(((VarPattern)arg).var);
			else if (arg instanceof ConstructorMatch)
				gatherCtor(defines, (ConstructorMatch) arg);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern)
				defines.add(((TypedPattern)arg).var);
			else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(Set<String> defines, ConstructorMatch cm) {
		for (Field x : cm.args) {
			if (x.patt instanceof VarPattern)
				defines.add(((VarPattern)x.patt).var);
			else if (x.patt instanceof ConstructorMatch)
				gatherCtor(defines, (ConstructorMatch)x.patt);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not gathering vars from " + x.patt.getClass());
		}
	}
}
