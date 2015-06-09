package org.flasck.flas.rewriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectRelative;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
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
		protected final NamingContext nested;
		
		public NamingContext(NamingContext inner) {
			this.nested = inner;
		}

		public abstract Object resolve(String name);
	}

	/** The Root Context exists exactly one time to include the BuiltinScope and nothing else
	 */
	class RootContext extends NamingContext {
		private final Scope biscope;

		public RootContext(Scope biscope) {
			super(null);
			if (biscope == null)
				throw new UtilException("but no");
			this.biscope = biscope;
		}

		@Override
		public Object resolve(String name) {
			if (biscope.contains(name))
				return new AbsoluteVar(biscope.getEntry(name));
			throw new ResolutionException(name);
		}
	}

	/** The Package Context represents one package which must exist exactly in the builtin scope
	 */
	class PackageContext extends NamingContext {
		private PackageDefn pkg;

		public PackageContext(RootContext cx, PackageDefn pkg) {
			super(cx);
			this.pkg = pkg;
		}

		@Override
		public Object resolve(String name) {
			if (pkg.innerScope().contains(name))
				return new AbsoluteVar(pkg.innerScope().getEntry(name));
			return nested.resolve(name);
		}
	}

	/** The Card Context can only be found directly in a Package Context 
	 */
	class CardContext extends NamingContext {
		private final String prefix;
		private final Set<String> members = new TreeSet<String>();
		private final Map<String, Integer> statics = new TreeMap<String, Integer>();
		private final Scope innerScope;

		CardContext(PackageContext cx, CardDefinition cd) {
			super(cx);
			this.prefix = cd.name;
			this.innerScope = cd.innerScope();
			if (cd.state != null) {
				for (StructField sf : cd.state.fields)
					members.add(sf.name);
			}
			for (ContractImplements ci : cd.contracts) {
				if (ci.referAsVar != null)
					members.add(ci.referAsVar);
			}
			int pos = 0;
			for (HandlerImplements hi : cd.handlers) {
				statics.put(basename(hi.type), pos++);
			}
		}

		@Override
		public Object resolve(String name) {
			if (members.contains(name))
				return new CardMember(prefix, name);
			if (statics.containsKey(name))
				return new ObjectRelative(prefix, "_H" + statics.get(name));
			if (innerScope.contains(name))
				return new ObjectRelative(prefix, name);
			return nested.resolve(name);
		}
	}

	/** The Handler Context can only be in a Card Context
	 */
	class HandlerContext extends NamingContext {
		private final HandlerImplements hi;
		private final int cs;

		HandlerContext(CardContext card, HandlerImplements hi, int cs) {
			super(card);
			this.hi = hi;
			this.cs = cs;
		}
		
		@Override
		public Object resolve(String name) {
			if (hi.boundVars.contains(name))
				return new HandlerLambda(((CardContext)nested).prefix + "._H" + cs, name);
			return nested.resolve(name);
		}
	}
	
	// I think I still need ImplementsContext, MethodContext and EventHandlerContext
	// BUT I think the latter two can just be FunctionContext & ImplementsContext is dull
	
	/** A function context can appear in lots of places, including inside other functions
	 */
	class FunctionCaseContext extends NamingContext {
		private final String myname;
		protected final Set<String> bound;
		private final Scope inner;

		FunctionCaseContext(NamingContext cx, String myname, int cs, Set<String> locals, Scope inner) {
			super(cx);
			this.myname = myname +"_"+cs;
			this.bound = locals;
			this.inner = inner;
		}

		public Object resolve(String name) {
			if (bound.contains(name))
				return new LocalVar(myname, name);
			if (inner.contains(name))
				return new AbsoluteVar(inner.getEntry(name));
			return nested.resolve(name);
		}
	}

	public Rewriter(ErrorResult errors) {
		this.errors = errors;
	}
	
	public void rewrite(ScopeEntry pkgEntry) {
		PackageDefn pkg = (PackageDefn) pkgEntry.getValue();
		PackageDefn newPkg = new PackageDefn(pkg);
		rewriteScope(new PackageContext(new RootContext(pkgEntry.scope()), pkg), pkg.innerScope(), newPkg.innerScope());
		newPkg.replaceOther();
	}

	protected void rewriteScope(NamingContext cx, Scope from, Scope into) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition)
				rewriteCard(cx, into, (CardDefinition)val);
			else if (val instanceof FunctionDefinition)
				into.define(x.getKey(), ((FunctionDefinition)val).name, rewrite(cx, (FunctionDefinition)val));
			else if (val instanceof EventHandlerDefinition)
				into.define(x.getKey(), ((EventHandlerDefinition)val).intro.name, rewrite(cx, (EventHandlerDefinition)val));
			else {
//				System.out.println("Don't do anything to rewrite " + name + " of type " + val.getClass());
				into.define(x.getKey(), name, val);
			}
		}
	}

	private CardDefinition rewriteCard(NamingContext cx, Scope into, CardDefinition cd) {
		if (!(cx instanceof PackageContext))
			throw new UtilException("Cannot have card in nested scope");
		CardContext c2 = new CardContext((PackageContext) cx, cd);
		CardDefinition ret = new CardDefinition(into, cd.name);
		if (cd.state != null) {
			ret.state = new StateDefinition();
			for (StructField sf : cd.state.fields)
				ret.state.fields.add(rewrite(cx, sf));
		}
		ret.template = cd.template;
		for (ContractImplements ci : cd.contracts) {
			ret.contracts.add(rewriteCI(c2, ci));
		}
		int pos = 0;
		for (HandlerImplements hi : cd.handlers) {
			ret.handlers.add(rewriteHI(c2, hi, pos++));
		}
		rewriteScope(c2, cd.fnScope, ret.fnScope);
		return ret;
	}

	public static String basename(String type) {
		return type.substring(type.lastIndexOf(".")+1);
	}

	private ContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		ContractImplements ret = new ContractImplements(ci.type, ci.referAsVar);
		rewrite(cx, ret, ci);
		return ret;
	}

	private HandlerImplements rewriteHI(CardContext cx, HandlerImplements hi, int cs) {
		HandlerImplements ret = new HandlerImplements(hi.type, hi.boundVars);
		NamingContext c2 = new HandlerContext(cx, hi, cs);
		rewrite(c2, ret, hi);
		return ret;
	}

	private void rewrite(NamingContext scope, Implements into, Implements orig) {
//		System.out.println("Rewriting " + orig.type + " to " + into.type);
		for (MethodDefinition m : orig.methods) {
			into.methods.add(rewrite(scope, m));
		}
	}

//	public FunctionDefinition rewriteFunction(Scope scope, CardDefinition cd, FunctionDefinition f) {
//		NamingContext cx = new CardContext(new RootContext(scope), cd);
//		return rewrite(cx, f);
//	}
//
//	public EventHandlerDefinition rewriteEventHandler(Scope scope, CardDefinition cd, EventHandlerDefinition ehd) {
//		NamingContext cx = new CardContext(new RootContext(scope), cd);
//		return rewrite(cx, ehd);
//	}
//	
	private FunctionDefinition rewrite(NamingContext cx, FunctionDefinition f) {
		System.out.println("Rewriting " + f.name);
		List<FunctionCaseDefn> list = new ArrayList<FunctionCaseDefn>();
		int cs = 0;
		for (FunctionCaseDefn c : f.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, f.name, cs, c.intro.allVars(), c.innerScope()), c));
			cs++;
		}
		System.out.println("rewritten to " + list.get(0).expr);
		FunctionDefinition ret = new FunctionDefinition(f.name, f.nargs, list);
		return ret;
	}

	private MethodDefinition rewrite(NamingContext cx, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, m.intro.allVars(), c.innerScope()), c));
			cs++;
		}
		return new MethodDefinition(m.intro, list);
	}

	private EventHandlerDefinition rewrite(NamingContext cx, EventHandlerDefinition ehd) {
		List<EventCaseDefn> list = new ArrayList<EventCaseDefn>();
		int cs = 0;
		for (EventCaseDefn c : ehd.cases) {
			Set<String> locals = new HashSet<String>();
			ehd.intro.gatherVars(locals);
			list.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name, cs, locals, c.innerScope()), c));
			cs++;
		}
		return new EventHandlerDefinition(ehd.intro, list);
	}

	private FunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c) {
		FunctionIntro intro = rewrite(cx, c.intro);
		FunctionCaseDefn ret = new FunctionCaseDefn(c.innerScope().outer, intro.name, intro.args, rewriteExpr(cx, c.expr));
		rewriteScope(cx, c.innerScope(), ret.innerScope());
		return ret;
	}

	private MethodCaseDefn rewrite(FunctionCaseContext cx, MethodCaseDefn c) {
		MethodCaseDefn ret = new MethodCaseDefn(rewrite(cx, c.intro));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private EventCaseDefn rewrite(FunctionCaseContext cx, EventCaseDefn c) {
		EventCaseDefn ret = new EventCaseDefn(rewrite(cx, c.intro));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private FunctionIntro rewrite(NamingContext cx, FunctionIntro intro) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(cx, o));
		}
		return new FunctionIntro(intro.name, args);
	}

	private Object rewritePattern(NamingContext scope, Object o) {
		if (o instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) o;
			Object type = scope.resolve(tp.type);
			if (!(type instanceof AbsoluteVar))
				errors.message((Block)null, "could not handle " + type);
			return new TypedPattern(((AbsoluteVar)type).id, tp.var);
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
			Object r = cx.resolve(mm.slot.get(0));
			if (!(r instanceof CardMember))
				errors.message((Block)null, mm.slot.get(0) + " needs to be a state member");
			else
				newSlot.add(((CardMember)r).var);
			for (int i=1;i<mm.slot.size();i++)
				newSlot.add(mm.slot.get(i));
		}
		return new MethodMessage(newSlot, rewriteExpr(cx, mm.expr));
	}

	private StructField rewrite(NamingContext scope, StructField sf) {
		return new StructField(rewriteType(scope, sf.type), sf.name, rewriteExpr(scope, sf.init));
	}

	private Object rewriteExpr(NamingContext cx, Object expr) {
		if (expr == null)
			return null;
		try {
			if (expr instanceof NumericLiteral || expr instanceof StringLiteral || expr instanceof AbsoluteVar)
				return expr;
			else if (expr instanceof UnresolvedOperator || expr instanceof UnresolvedVar) {
				String s;
				if (expr instanceof UnresolvedOperator)
					 s = ((UnresolvedOperator) expr).op;
				else if (expr instanceof UnresolvedVar)
					s = ((UnresolvedVar) expr).var;
				else
					throw new UtilException("Huh?");
				Object ret = cx.resolve(s);
				if (ret instanceof AbsoluteVar || ret instanceof LocalVar || ret instanceof CardMember || ret instanceof ObjectRelative || ret instanceof HandlerLambda)
					return ret;
				else
					throw new UtilException("cannot handle " + ret.getClass());
//				Object ret = ItemExpr.from(new ExprToken(ExprToken.IDENTIFIER, rwTo));
//				if (rwTo.contains("._H")) {
////					System.out.println("_H thing: " + rwTo);
//					ret = new ApplyExpr(ret, ItemExpr.from(new ExprToken(ExprToken.IDENTIFIER, "_card")));
//				}
//				return ret;
			} else if (expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) expr;
				if (ae.fn instanceof UnresolvedOperator && ((UnresolvedOperator)ae.fn).op.equals(".")) {
					UnresolvedVar field = (UnresolvedVar)ae.args.get(1);
					// The case where we have an absolute var by package name
					if (!(ae.args.get(0) instanceof ApplyExpr)) {
						String pkgVar = ((UnresolvedVar)ae.args.get(0)).var;
						Object pkgEntry = cx.resolve(pkgVar);
						if (pkgEntry instanceof AbsoluteVar) {
							Object o = ((AbsoluteVar)pkgEntry).defn;
							if (o instanceof PackageDefn)
								return new AbsoluteVar(((PackageDefn)o).innerScope().getEntry(field.var));
						}
					}
					
					// expr . field
					Object applyFn = rewriteExpr(cx, ae.args.get(0));
	
					return new ApplyExpr(cx.resolve("."), applyFn, new StringLiteral(field.var));
				}
				List<Object> args = new ArrayList<Object>();
				for (Object o : ae.args)
					args.add(rewriteExpr(cx, o));
				return new ApplyExpr(rewriteExpr(cx, ae.fn), args);
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
			Object r = scope.resolve(tr.name);
			if (!(r instanceof AbsoluteVar)) {
				errors.message((Block)null, tr.name + " is not a type definition");
				return null;
			}
			TypeReference ret = new TypeReference(((AbsoluteVar)r).id);
			for (Object o : tr.args)
				ret.args.add(rewriteType(scope, o));
			return ret;
		}
		System.out.println("Can't rewrite type " + type + " of type " + type.getClass());
		return (TypeReference) type;
	}
}
