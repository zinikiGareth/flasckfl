package org.flasck.flas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;

/** The objective of this class is to resolve all of the names of all of the
 * items in all of the expressions so that it is all unambiguous
 *
 * <p>
 * &copy; 2015 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class Rewriter {
	private ErrorResult errors;

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
				return nested.resolve(name);
		}

		protected abstract String makeName(String name);

		public String makeAbsoluteName(String name) {
			return makeName(name);
		}

		/* Hard get a resolved name from the scope */
		public Object get(String resolvedName) {
			return nested.get(resolvedName);
		}
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
//			throw new UtilException("Cannot have names in this scope");
			return scope.resolve(name);
		}

		/* Hard get a resolved name from the scope */
		public Object get(String resolvedName) {
			return scope.getResolved(resolvedName);
		}
	}
	
	class CardContext extends NamingContext {
		public Map<String, Integer> globals = new TreeMap<String, Integer>();
		private final String prefix;
		private final Scope innerScope;

		CardContext(RootContext scope, CardDefinition cd) {
			super(scope);
			this.prefix = scope.resolve(cd.name);
			this.innerScope = cd.innerScope();
			if (cd.state != null) {
				for (StructField sf : cd.state.fields)
					add(sf.name);
			}
			for (ContractImplements ci : cd.contracts) {
				add(ci.referAsVar);
			}
			int pos = 0;
			for (HandlerImplements hi : cd.handlers) {
				globals.put(basename(hi.type), pos++);
			}
		}

		@Override
		public String resolve(String name) {
			if (innerScope.contains(name)) {
				Object defn = innerScope.get(name);
				if (defn instanceof EventHandlerDefinition)
					return prefix + ".prototype." + name;
				return prefix + "." + name;
			}
			if (globals.containsKey(name))
				return prefix + "._H" + globals.get(name);
			else
				return super.resolve(name);
		}
		
		@Override
		public String makeAbsoluteName(String name) {
			return prefix + "." + name;
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
		private final String myname;
		protected final Set<String> locals = new HashSet<String>();

		FunctionContext(NamingContext cx, Scope scope, String myname, int cs) {
			super(cx);
			this.myname = cx.makeAbsoluteName(myname) +"_"+cs;
			if (scope != null) {
				for (String k : scope.keys())
					add(k);
			}
		}

		@Override
		public void add(String s) {
			NamingContext nc = this;
			while (nc != null && nc instanceof FunctionContext) {
				if (nc.defines.contains(s))
					throw new UtilException("Cannot define variable " + s + " multiple times in nested scopes");
				nc = nc.nested;
			}
			super.add(s);
		}
		
		public String resolveWithLocal(String name, boolean direct) {
			if (locals.contains(name)) {
				if (direct)
					return name;
				else
					return "_scoped." + name;
			} else if (defines.contains(name))
				return makeName(name);
			else if (nested instanceof FunctionContext)
				return ((FunctionContext)nested).resolveWithLocal(name, false);
			else
				return nested.resolve(name);
		}
		
		@Override
		public String resolve(String name) {
			return resolveWithLocal(name, true);
		}
		
		@Override
		protected String makeName(String name) {
			return myname + "." + name;
		}
	}

	public Rewriter(ErrorResult errors) {
		this.errors = errors;
	}
	
	public Scope rewrite(Scope scope) {
		Scope newScope = new Scope(scope.outer);
		rewriteScope(new RootContext(scope), scope, newScope);
		return newScope;
	}

	protected void rewriteScope(NamingContext cx, Scope from, Scope into) {
		for (Entry<String, Entry<String, Object>> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition) {
				if (!(cx instanceof RootContext))
					throw new UtilException("Cannot have card in nested scope");
				CardDefinition cd = (CardDefinition) val;
				CardContext c2 = new CardContext((RootContext) cx, cd);
				if (cd.state != null) {
					List<StructField> l = new ArrayList<StructField>(cd.state.fields);
					cd.state.fields.clear();
					for (StructField sf : l)
						cd.state.fields.add(rewrite(cx, sf));
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
				into.define(x.getKey(), name, cd);
			} else if (val instanceof FunctionDefinition) {
				FunctionDefinition nv = rewrite(cx, (FunctionDefinition)val);
				into.define(x.getKey(), nv.name, nv);
			} else {
//				System.out.println("Can't rewrite " + name + " of type " + val.getClass());
				into.define(x.getKey(), name, val);
			}
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
		HandlerImplements ret = new HandlerImplements(cx.nested.resolve(hi.type), hi.boundVars);
		NamingContext c2 = new HandlerContext(cx);
		c2.defines.addAll(hi.boundVars);
		rewrite(c2, ret, hi);
		return ret;
	}

	private void rewrite(NamingContext scope, Implements into, Implements orig) {
//		System.out.println("Rewriting " + orig.type + " to " + into.type);
		for (MethodDefinition m : orig.methods) {
			into.methods.add(rewrite(scope, m));
		}
	}

	public FunctionDefinition rewriteFunction(Scope scope, CardDefinition cd, FunctionDefinition f) {
		NamingContext cx = new CardContext(new RootContext(scope), cd);
		return rewrite(cx, f);
	}

	public EventHandlerDefinition rewriteEventHandler(Scope scope, CardDefinition cd, EventHandlerDefinition ehd) {
		NamingContext cx = new CardContext(new RootContext(scope), cd);
		return rewrite(cx, ehd);
	}
	
	private FunctionDefinition rewrite(NamingContext cx, FunctionDefinition f) {
		List<FunctionCaseDefn> list = new ArrayList<FunctionCaseDefn>();
		int cs = 0;
		for (FunctionCaseDefn c : f.cases) {
			list.add(rewrite(new FunctionContext(cx, c.innerScope(), f.name, cs), c));
			cs++;
		}
		FunctionDefinition ret = new FunctionDefinition(cx.makeAbsoluteName(f.name), f.nargs, list);
		return ret;
	}

	private MethodDefinition rewrite(NamingContext scope, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionContext(scope, null, m.intro.name, cs), c));
			cs++;
		}
		return new MethodDefinition(m.intro, list);
	}

	private EventHandlerDefinition rewrite(NamingContext scope, EventHandlerDefinition ehd) {
		List<EventCaseDefn> list = new ArrayList<EventCaseDefn>();
		int cs = 0;
		String rw = scope.makeAbsoluteName("prototype."+ehd.intro.name);
		for (EventCaseDefn c : ehd.cases) {
			list.add(rewrite(new FunctionContext(scope, null, rw, cs), c));
			cs++;
		}
		return new EventHandlerDefinition(new FunctionIntro(rw, ehd.intro.args), list);
	}

	private FunctionCaseDefn rewrite(FunctionContext cx, FunctionCaseDefn c) {
		c.intro.gatherVars(cx.locals);
		FunctionIntro intro = rewrite(cx, c.intro);
		FunctionCaseDefn ret = new FunctionCaseDefn(c.innerScope().outer, intro.name, intro.args, rewriteExpr(cx, c.expr));
		rewriteScope(cx, c.innerScope(), ret.innerScope());
		return ret;
	}

	private MethodCaseDefn rewrite(FunctionContext cx, MethodCaseDefn c) {
		MethodCaseDefn ret = new MethodCaseDefn(rewrite(cx, c.intro));
		c.intro.gatherVars(cx.locals);
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private EventCaseDefn rewrite(FunctionContext cx, EventCaseDefn c) {
		EventCaseDefn ret = new EventCaseDefn(rewrite(cx, c.intro));
		c.intro.gatherVars(cx.locals);
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private FunctionIntro rewrite(NamingContext scope, FunctionIntro intro) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(scope, o));
		}
		return new FunctionIntro(scope.makeName(intro.name), args);
	}

	private Object rewritePattern(NamingContext scope, Object o) {
		if (o instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) o;
			return new TypedPattern(scope.resolve(tp.type), tp.var);
		} else if (o instanceof VarPattern) {
			return o;
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
		if (expr == null)
			return null;
		try {
			if (expr instanceof ItemExpr) {
				ItemExpr ie = (ItemExpr) expr;
//				System.out.println("Want to rewrite " + ie.tok);
				Object ret;
				if (ie.tok.type == ExprToken.NUMBER || ie.tok.type == ExprToken.STRING)
					ret = ie;
				else if (ie.tok.type == ExprToken.IDENTIFIER || ie.tok.type == ExprToken.SYMBOL || ie.tok.type == ExprToken.PUNC) {
					String rwTo = scope.resolve(ie.tok.text);
					ret = new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, rwTo));
					if (rwTo.contains("._H")) {
	//					System.out.println("_H thing: " + rwTo);
						ret = new ApplyExpr(ret, new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "_card")));
					}
				} else
					throw new UtilException("Cannot handle " + ie.tok);
//				System.out.println("Rewritten to " + ret);
				return ret;
			} else if (expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) expr;
				if (ae.fn instanceof ItemExpr && ((ItemExpr)ae.fn).tok.text.equals(".")) {
					ItemExpr ie = (ItemExpr)ae.args.get(1);
					if (ie.tok.type != ExprToken.IDENTIFIER) throw new UtilException("unhandled case");
	//				System.out.println("Considering . with " + ae.args.get(0).getClass());
					if (ae.args.get(0) instanceof ItemExpr) {
						String pkg = ((ItemExpr)ae.args.get(0)).tok.text;
	//					System.out.println("is: " + scope.get(pkg));
						if (scope.get(pkg) instanceof PackageDefn) {
	//						System.out.println("pkg " + pkg);
							return ItemExpr.id(pkg + "." + ie.tok.text);
						}
					}
					Object applyFn = rewriteExpr(scope, ae.args.get(0));
	
					return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "FLEval.field")), applyFn, new ItemExpr(new ExprToken(ExprToken.STRING, ie.tok.text)));
				}
				List<Object> args = new ArrayList<Object>();
				for (Object o : ae.args)
					args.add(rewriteExpr(scope, o));
				return new ApplyExpr(rewriteExpr(scope, ae.fn), args);
			}
			System.out.println("Can't rewrite expr " + expr + " of type " + expr.getClass());
			return expr;
		} catch (ResolutionException ex) {
			errors.message((Block)null, ex.getMessage());
			return null;
		}
	}

	private TypeReference rewriteType(NamingContext scope, Object type) {
		if (type instanceof TypeReference) {
			TypeReference tr = (TypeReference) type;
			TypeReference ret = new TypeReference(scope.resolve(tr.name));
			for (Object o : tr.args)
				ret.args.add(rewriteType(scope, o));
			return ret;
		}
		System.out.println("Can't rewrite type " + type + " of type " + type.getClass());
		return (TypeReference) type;
	}
}
