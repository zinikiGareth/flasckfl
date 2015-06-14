package org.flasck.flas.rewriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.EventHandlerInContext;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
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
// and ultimately pull these two together
public class Rewriter {
	private final ErrorResult errors;
	public final Map<String, StructDefn> structs = new TreeMap<String, StructDefn>();
	public final Map<String, TypeDefn> types = new TreeMap<String, TypeDefn>();
	public final Map<String, ContractDecl> contracts = new TreeMap<String, ContractDecl>();
	public final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<Template> templates = new ArrayList<Template>();
	public final Map<String, ContractImplements> cardImplements = new TreeMap<String, ContractImplements>();
	public final Map<String, HandlerImplements> cardHandlers = new TreeMap<String, HandlerImplements>();
	public final List<MethodInContext> methods = new ArrayList<MethodInContext>();
	public final List<EventHandlerInContext> eventHandlers = new ArrayList<EventHandlerInContext>();
	public final Map<String, FunctionDefinition> functions = new TreeMap<String, FunctionDefinition>();

	private abstract class NamingContext {
		protected final NamingContext nested;
		
		public NamingContext(NamingContext inner) {
			this.nested = inner;
		}

		public abstract Object resolve(InputPosition location, String name);
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
		public Object resolve(InputPosition location, String name) {
			if (biscope.contains(name))
				return new AbsoluteVar(biscope.getEntry(name));
			if (name.contains(".")) {
				// try and resolve through a sequence of packages
				String tmp = name;
				int idx;
				Scope scope = biscope;
				while ((idx = tmp.indexOf('.')) != -1) {
					String pkg = tmp.substring(0, idx);
					Object o = scope.get(pkg);
					if (o == null || !(o instanceof PackageDefn))
						break;
					tmp = tmp.substring(idx+1);
					scope = ((PackageDefn)o).innerScope();
				}
				if (!tmp.contains(".")) {
					ScopeEntry o = scope.getEntry(tmp);
					if (o != null)
						return new AbsoluteVar(o);
				}
			}
			throw new ResolutionException(location, name);
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
		public Object resolve(InputPosition location, String name) {
			if (pkg.innerScope().contains(name))
				return new AbsoluteVar(pkg.innerScope().getEntry(name));
			return nested.resolve(location, name);
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
				statics.put(State.simpleName(hi.type), pos++);
			}
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (members.contains(name))
				return new CardMember(location, prefix, name);
			if (statics.containsKey(name))
				return new ObjectReference(prefix, "_H" + statics.get(name));
			if (innerScope.contains(name))
				return new CardFunction(prefix, name);
			return nested.resolve(location, name);
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
		public Object resolve(InputPosition location, String name) {
			if (hi.boundVars.contains(name))
				return new HandlerLambda(location, ((CardContext)nested).prefix + "._H" + cs, name);
			return nested.resolve(location, name);
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
		private final boolean fromMethod;

		FunctionCaseContext(NamingContext cx, String myname, int cs, Set<String> locals, Scope inner, boolean fromMethod) {
			super(cx);
			this.myname = myname +"_"+cs;
			this.bound = locals;
			this.inner = inner;
			this.fromMethod = fromMethod;
		}

		public Object resolve(InputPosition location, String name) {
			if (bound.contains(name))
				return new LocalVar(myname, name);
			if (inner.contains(name))
				return new AbsoluteVar(inner.getEntry(name));
			Object res = nested.resolve(location, name);
			if (res instanceof ObjectReference)
				return new ObjectReference((ObjectReference)res, fromMethod);
			if (res instanceof CardFunction)
				return new CardFunction((CardFunction)res, fromMethod);
			return res;
		}
	}

	public Rewriter(ErrorResult errors) {
		this.errors = errors;
	}
	
	public void rewrite(ScopeEntry pkgEntry) {
		PackageDefn pkg = (PackageDefn) pkgEntry.getValue();
		rewriteScope(new PackageContext(new RootContext(pkgEntry.scope()), pkg), pkg.innerScope());
	}

	protected void rewriteScope(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition)
				rewriteCard(cx, (CardDefinition)val);
			else if (val instanceof FunctionDefinition)
				functions.put(name, rewrite(cx, (FunctionDefinition)val));
			else if (val instanceof EventHandlerDefinition)
				eventHandlers.add(new EventHandlerInContext(from, name, rewrite(cx, (EventHandlerDefinition)val)));
			else if (val instanceof StructDefn) {
				structs.put(name, (StructDefn)val);
			} else if (val instanceof TypeDefn) {
				types.put(name, (TypeDefn)val);
			} else if (val instanceof ContractDecl) {
				contracts.put(name, (ContractDecl)val);
			} else
				throw new UtilException("Cannot handle " + val.getClass());
		}
	}

	private void rewriteCard(NamingContext cx, CardDefinition cd) {
		if (!(cx instanceof PackageContext))
			throw new UtilException("Cannot have card in nested scope");
		CardContext c2 = new CardContext((PackageContext) cx, cd);
		CardGrouping grp = new CardGrouping();
		cards.put(cd.name, grp);
		StructDefn sd = new StructDefn(cd.name, false);
		if (cd.state != null) {
			for (StructField sf : cd.state.fields) {
				sd.fields.add(rewrite(cx, sf));
				grp.inits.put(sf.name, rewriteExpr(cx, sf.init));
			}
		}
		
		int pos = 0;
		for (ContractImplements ci : cd.contracts) {
			ContractImplements rw = rewriteCI(c2, ci);
			String myname = cd.name +"._C" + pos;
			grp.contracts.add(new ContractGrouping(rw.type, myname, rw.referAsVar));
			cardImplements.put(myname, rw);
			if (rw.referAsVar != null)
				sd.fields.add(new StructField(new TypeReference(null, rw.type, null), rw.referAsVar));

			for (MethodDefinition m : ci.methods)
				methods.add(new MethodInContext(cd.innerScope(), m.intro.name, "_C"+pos, HSIEForm.Type.CONTRACT, rewrite(c2, m)));

			pos++;
		}
		structs.put(cd.name, sd);
		if (cd.template != null)
			templates.add(rewrite(c2, cd.template));
		
		pos = 0;
		for (HandlerImplements hi : cd.handlers) {
			HandlerImplements rw = rewriteHI(c2, hi, pos);
			String hiName = cd.name +"._H"+pos;
			cardHandlers.put(hiName, rw);
			if (!rw.boundVars.isEmpty()) {
//				System.out.println("Creating class for handler " + hiName);
				StructDefn hsd = new StructDefn(hiName, false);
				// Doing this seems clever, but I'm not really sure that it is
				// We need to make sure that in doing this, everything typechecks to the same set of variables, whereas we normally insert fresh variables every time we use the type
				for (int i=0;i<rw.boundVars.size();i++)
					hsd.args.add("A"+i);
				int j=0;
				for (String s : hi.boundVars) {
					hsd.fields.add(new StructField(new TypeReference(null, null, "A"+j), s));
					j++;
				}
				structs.put(hiName, hsd);
			}
			HandlerContext hc = new HandlerContext(c2, hi, pos);
			for (MethodDefinition m : hi.methods)
				methods.add(new MethodInContext(cd.innerScope(), m.intro.name, "_H"+pos, HSIEForm.Type.HANDLER, rewrite(hc, m)));
			pos++;
		}
		
		rewriteScope(c2, cd.fnScope);
	}

	private Template rewrite(CardContext cx, Template template) {
		// Again, the need for a scope seems dodgy if we've rewritten ...
		return new Template(template.prefix, rewrite(cx, template.topLine), template.scope);
	}

	private TemplateLine rewrite(CardContext cx, TemplateLine tl) {
		List<Object> contents = new ArrayList<Object>();
		List<Object> attrs = new ArrayList<Object>();
		List<Object> formats = new ArrayList<Object>();
		for (Object o : tl.contents) {
			if (o instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken) o;
				if (tt.type == TemplateToken.STRING || tt.type == TemplateToken.DIV)
					contents.add(tt);
				else if (tt.type == TemplateToken.IDENTIFIER)
					contents.add(rewriteExpr(cx, ItemExpr.from(new ExprToken(ExprToken.IDENTIFIER, tt.text))));
				else
					throw new UtilException("Content type not handled: " + tt);
			} else if (o instanceof StringLiteral || o instanceof NumericLiteral) {
				contents.add(o);
			} else if (o instanceof ApplyExpr) {
				contents.add(rewriteExpr(cx, o));
			} else 
				throw new UtilException("Content type not handled: " + o.getClass());
		}
		for (Object o : tl.attrs) {
			if (o instanceof TemplateExplicitAttr)
				attrs.add(o);
			else
				throw new UtilException("Attr type not handled: " + o.getClass());
		}
		for (Object o : tl.formats) {
			if (o instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken) o;
				if (tt.type == TemplateToken.STRING)
					formats.add(tt);
				else
					throw new UtilException("Format type not handled: " + tt);
			} else if (o instanceof ApplyExpr) {
				formats.add(rewriteExpr(cx, o));
			} else 
				throw new UtilException("Format type not handled: " + o.getClass());
		}
		TemplateLine ret = new TemplateLine(contents, tl.customTag, tl.customTagVar, attrs, formats);
		for (EventHandler h : tl.handlers) {
			ret.handlers.add(new EventHandler(h.action, rewriteExpr(cx, h.expr)));
		}
		for (TemplateLine i : tl.nested)
			ret.nested.add(rewrite(cx, i));
		return ret;
	}

	private ContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		try {
			Object av = cx.nested.resolve(ci.typeLocation, ci.type);
			if (av == null || !(av instanceof AbsoluteVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + ci.type);
				return ci;
			}
			return new ContractImplements(ci.typeLocation, ((AbsoluteVar)av).id, ci.vlocation, ci.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private HandlerImplements rewriteHI(CardContext cx, HandlerImplements hi, int cs) {
		try {
			Object av = cx.nested.resolve(hi.typeLocation, hi.type);
			if (av == null || !(av instanceof AbsoluteVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + hi.type);
				return hi;
			}
			HandlerImplements ret = new HandlerImplements(hi.typeLocation, ((AbsoluteVar)av).id, hi.boundVars);
			return ret;
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	public FunctionDefinition rewrite(NamingContext cx, FunctionDefinition f) {
//		System.out.println("Rewriting " + f.name);
		List<FunctionCaseDefn> list = new ArrayList<FunctionCaseDefn>();
		int cs = 0;
		for (FunctionCaseDefn c : f.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, f.name, cs, c.intro.allVars(), c.innerScope(), false), c));
			cs++;
		}
//		System.out.println("rewritten to " + list.get(0).expr);
		FunctionDefinition ret = new FunctionDefinition(f.mytype, f.name, f.nargs, list);
		return ret;
	}

	private MethodDefinition rewrite(NamingContext cx, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, m.intro.allVars(), c.innerScope(), true), c));
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
			list.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name, cs, locals, c.innerScope(), false), c));
			cs++;
		}
		return new EventHandlerDefinition(ehd.intro, list);
	}

	private FunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c) {
		FunctionIntro intro = rewrite(cx, c.intro);
		FunctionCaseDefn ret = new FunctionCaseDefn(c.innerScope().outer, intro.name, intro.args, rewriteExpr(cx, c.expr));
		rewriteScope(cx, c.innerScope());
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
			Object type = scope.resolve(tp.typeLocation, tp.type);
			if (!(type instanceof AbsoluteVar))
				errors.message((Block)null, "could not handle " + type);
			return new TypedPattern(tp.typeLocation, ((AbsoluteVar)type).id, tp.varLocation, tp.var);
		} else if (o instanceof VarPattern) {
			return o;
		} else {
//			System.out.println("Couldn't rewrite pattern " + o.getClass());
			return o;
		}
	}

	private MethodMessage rewrite(NamingContext cx, MethodMessage mm) {
		List<LocatedToken> newSlot = null;
		if (mm.slot != null && !mm.slot.isEmpty()) {
			newSlot = new ArrayList<LocatedToken>();
			LocatedToken slot = mm.slot.get(0);
			Object r = cx.resolve(slot.location, slot.text);
			if (!(r instanceof CardMember))
				errors.message((Block)null, mm.slot.get(0) + " needs to be a state member");
			else {
				CardMember cm = (CardMember)r;
				newSlot.add(new LocatedToken(cm.location, cm.var));
			}
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
				InputPosition location;
				if (expr instanceof UnresolvedOperator) {
					UnresolvedOperator up = (UnresolvedOperator) expr;
					s = up.op;
					location = up.location;  
				} else if (expr instanceof UnresolvedVar) {
					UnresolvedVar uv = (UnresolvedVar) expr;
					s = uv.var;
					location = uv.location;  
				} else
					throw new UtilException("Huh?");
				Object ret = cx.resolve(location, s);
				if (ret instanceof AbsoluteVar || ret instanceof LocalVar || ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof HandlerLambda)
					return ret;
				else
					throw new UtilException("cannot handle " + ret.getClass());
			} else if (expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) expr;
				if (ae.fn instanceof UnresolvedOperator && ((UnresolvedOperator)ae.fn).op.equals(".")) {
					UnresolvedVar field = (UnresolvedVar)ae.args.get(1);
					// The case where we have an absolute var by package name
					// Does this need to be here as well as in RootScope?
					if (!(ae.args.get(0) instanceof ApplyExpr)) {
						UnresolvedVar uv0 = (UnresolvedVar)ae.args.get(0);
						Object pkgEntry = cx.resolve(uv0.location, uv0.var);
						if (pkgEntry instanceof AbsoluteVar) {
							Object o = ((AbsoluteVar)pkgEntry).defn;
							if (o instanceof PackageDefn)
								return new AbsoluteVar(((PackageDefn)o).innerScope().getEntry(field.var));
						}
					}
					
					// expr . field
					Object applyFn = rewriteExpr(cx, ae.args.get(0));
	
					return new ApplyExpr(cx.resolve(null, "."), applyFn, new StringLiteral(field.var));
				}
				List<Object> args = new ArrayList<Object>();
				for (Object o : ae.args)
					args.add(rewriteExpr(cx, o));
				return new ApplyExpr(rewriteExpr(cx, ae.fn), args);
			}
			System.out.println("Can't rewrite expr " + expr + " of type " + expr.getClass());
			return expr;
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private TypeReference rewriteType(NamingContext scope, Object type) {
		if (type instanceof TypeReference) {
			TypeReference tr = (TypeReference) type;
			Object r = scope.resolve(tr.location, tr.name);
			if (!(r instanceof AbsoluteVar)) {
				errors.message((Block)null, tr.name + " is not a type definition");
				return null;
			}
			AbsoluteVar av = (AbsoluteVar)r;
			TypeReference ret = new TypeReference(tr.location, av.id, null);
			for (Object o : tr.args)
				ret.args.add(rewriteType(scope, o));
			return ret;
		}
		System.out.println("Can't rewrite type " + type + " of type " + type.getClass());
		return (TypeReference) type;
	}

	public void dump() {
		try {
			PrintWriter pw = new PrintWriter(System.out);
			for (Entry<String, StructDefn> x : structs.entrySet())
				System.out.println("Struct " + x.getKey());
			for (Entry<String, CardGrouping> x : cards.entrySet())
				System.out.println("Card " + x.getKey());
			for (Entry<String, HandlerImplements> x : cardHandlers.entrySet())
				System.out.println("Handler " + x.getKey());
			for (Entry<String, ContractImplements> x : cardImplements.entrySet())
				System.out.println("Impl " + x.getKey());
			for (Entry<String, FunctionDefinition> x : functions.entrySet()) {
				x.getValue().dumpTo(pw);
			}
			pw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
