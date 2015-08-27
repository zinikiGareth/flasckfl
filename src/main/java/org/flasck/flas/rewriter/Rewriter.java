package org.flasck.flas.rewriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.PackageFinder;
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
import org.flasck.flas.parsedForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.EventHandlerInContext;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.IterVar;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.D3Thing;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
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
	private final PackageFinder pkgFinder;
	public final Map<String, StructDefn> structs = new TreeMap<String, StructDefn>();
	public final Map<String, UnionTypeDefn> types = new TreeMap<String, UnionTypeDefn>();
	public final Map<String, ContractDecl> contracts = new TreeMap<String, ContractDecl>();
	public final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<Template> templates = new ArrayList<Template>();
	public final List<D3Invoke> d3s = new ArrayList<D3Invoke>();
	public final Map<String, ContractImplements> cardImplements = new TreeMap<String, ContractImplements>();
	public final Map<String, ContractService> cardServices = new TreeMap<String, ContractService>();
	public final Map<String, HandlerImplements> cardHandlers = new TreeMap<String, HandlerImplements>();
	public final List<MethodInContext> methods = new ArrayList<MethodInContext>();
	public final List<EventHandlerInContext> eventHandlers = new ArrayList<EventHandlerInContext>();
	public final Map<String, FunctionDefinition> functions = new TreeMap<String, FunctionDefinition>();

	public abstract class NamingContext {
		protected final NamingContext nested;
		
		public NamingContext(NamingContext inner) {
			this.nested = inner;
		}

		public abstract Object resolve(InputPosition location, String name);
	}

	/** The Root Context exists exactly one time to include the BuiltinScope and nothing else
	 */
	public class RootContext extends NamingContext {
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
				return new AbsoluteVar(location, biscope.getEntry(name));
			if (name.contains(".")) {
				// try and resolve through a sequence of packages
				String tmp = name;
				int idx;
				Scope scope = biscope;
				while ((idx = tmp.indexOf('.')) != -1) {
					String pkg = tmp.substring(0, idx);
					Object o = scope.get(pkg);
					if (o == null)
						break;
					if  (!(o instanceof PackageDefn)) {
						errors.message(location, "The name " + pkg + " is not a package and thus cannot contain " + name);
						return null;
					}
					tmp = tmp.substring(idx+1);
					scope = ((PackageDefn)o).innerScope();
				}
				if (tmp.contains(".")) { // we don't yet have the scope
					idx = name.lastIndexOf(".");
					String pkgName = name.substring(0, idx);
					if (pkgFinder != null)
						scope = pkgFinder.loadFlim(biscope, pkgName);
					if (scope == null)
						throw new ResolutionException(location, name);
					tmp = name.substring(idx+1);
				}
				ScopeEntry o = scope.getEntry(tmp);
				if (o != null) {
					Object defn = o.getValue();
					if (defn instanceof ContractDecl)
						contracts.put(name, (ContractDecl) defn);
					return new AbsoluteVar(location, o);
				}
			}
			throw new ResolutionException(location, name);
		}
	}

	/** The Package Context represents one package which must exist exactly in the builtin scope
	 */
	public class PackageContext extends NamingContext {
		private PackageDefn pkg;

		public PackageContext(NamingContext cx, PackageDefn pkg) {
			super(cx);
			this.pkg = pkg;
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (pkg.innerScope().contains(name))
				return new AbsoluteVar(location, pkg.innerScope().getEntry(name));
			return nested.resolve(location, name);
		}
	}

	/** The Card Context can only be found directly in a Package Context 
	 */
	public class CardContext extends NamingContext {
		private final String prefix;
		private final Set<String> members = new TreeSet<String>();
		private final Map<String, ObjectReference> statics = new TreeMap<String, ObjectReference>();
		private final Scope innerScope;

		public CardContext(PackageContext cx, CardDefinition cd) {
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
			for (ContractService ci : cd.services) {
				if (ci.referAsVar != null)
					members.add(ci.referAsVar);
			}
			for (HandlerImplements hi : cd.handlers) {
				statics.put(State.simpleName(hi.name), new ObjectReference(hi.location(), prefix, hi.name));
			}
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (members.contains(name))
				return new CardMember(location, prefix, name);
			if (statics.containsKey(name))
				return statics.get(name);
			if (innerScope.contains(name))
				return new CardFunction(location, prefix, name);
			return nested.resolve(location, name);
		}
	}

	/** The Handler Context can only be in a Card Context
	 */
	class HandlerContext extends NamingContext {
		private final HandlerImplements hi;

		HandlerContext(CardContext card, HandlerImplements hi) {
			super(card);
			this.hi = hi;
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			for (Object o : hi.boundVars)
				if (((HandlerLambda)o).var.equals(name))
					return o;
			return nested.resolve(location, name);
		}
	}

	public class TemplateContext extends NamingContext {
		private final TemplateListVar listVar;

		public TemplateContext(CardContext cx) {
			super(cx);
			listVar = null;
		}
		
		public TemplateContext(TemplateContext cx, TemplateListVar tlv) {
			super(cx);
			this.listVar = tlv;
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			if (listVar != null && listVar.name.equals(name))
				return listVar;
			return nested.resolve(location, name);
		}

	}

	public class D3Context extends NamingContext {
		private final IterVar iterVar;

		public D3Context(TemplateContext cx, InputPosition location, String iv) {
			super(cx);
			this.iterVar = new IterVar(location, ((CardContext)cx.nested).prefix, iv);
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			if (iterVar != null && iterVar.var.equals(name))
				return iterVar;
			return nested.resolve(location, name);
		}

	}


	// I think I still need ImplementsContext, MethodContext and EventHandlerContext
	// BUT I think the latter two can just be FunctionContext & ImplementsContext is dull
	
	/** A function context can appear in lots of places, including inside other functions
	 */
	class FunctionCaseContext extends NamingContext {
		private final String myname;
		protected final Map<String, LocalVar> bound;
		private final Scope inner;
		private final boolean fromMethod;

		FunctionCaseContext(NamingContext cx, String myname, int cs, Map<String, LocalVar> locals, Scope inner, boolean fromMethod) {
			super(cx);
			this.myname = myname +"_"+cs;
			this.bound = locals;
			this.inner = inner;
			this.fromMethod = fromMethod;
		}

		public Object resolve(InputPosition location, String name) {
			if (bound.containsKey(name))
				return bound.get(name); // a local var
			if (inner.contains(name))
				return new AbsoluteVar(inner.getEntry(name));
			Object res = nested.resolve(location, name);
			if (res instanceof ObjectReference)
				return new ObjectReference(location, (ObjectReference)res, fromMethod);
			if (res instanceof CardFunction)
				return new CardFunction(location, (CardFunction)res, fromMethod);
			return res;
		}
	}

	public Rewriter(ErrorResult errors, PackageFinder finder) {
		this.errors = errors;
		this.pkgFinder = finder;
	}
	
	public void rewrite(ScopeEntry pkgEntry) {
		PackageDefn pkg = (PackageDefn) pkgEntry.getValue();
		rewriteScope(figureBaseContext(pkgEntry), pkg.innerScope());
	}
	
	public NamingContext figureBaseContext(ScopeEntry pkgEntry) {
		PackageDefn pkg = (PackageDefn) pkgEntry.getValue();
		Scope s = pkgEntry.scope();
		if (s.outerEntry == null)
			return new PackageContext(new RootContext(s), pkg);
		return new PackageContext(figureBaseContext(s.outerEntry), pkg);
	}

	protected void rewriteScope(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof PackageDefn)
				rewriteScope(cx, ((PackageDefn)val).innerScope());
			else if (val instanceof CardDefinition)
				rewriteCard(cx, (CardDefinition)val);
			else if (val instanceof FunctionDefinition)
				functions.put(name, rewrite(cx, (FunctionDefinition)val));
			else if (val instanceof EventHandlerDefinition)
				eventHandlers.add(new EventHandlerInContext(from, name, rewrite(cx, (EventHandlerDefinition)val)));
			else if (val instanceof StructDefn) {
				structs.put(name, rewrite(cx, (StructDefn)val));
			} else if (val instanceof UnionTypeDefn) {
				types.put(name, (UnionTypeDefn)val);
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
		StructDefn sd = new StructDefn(cd.location, cd.name, false);
		CardGrouping grp = new CardGrouping(sd);
		cards.put(cd.name, grp);
		if (cd.state != null) {
			for (StructField sf : cd.state.fields) {
				sd.addField(new StructField(rewrite(cx, sf.type), sf.name, rewriteExpr(cx, sf.init)));
				grp.inits.put(sf.name, rewriteExpr(cx, sf.init));
			}
		}
		
		int pos = 0;
		for (ContractImplements ci : cd.contracts) {
			ContractImplements rw = rewriteCI(c2, ci);
			if (rw == null)
				continue;
			String myname = cd.name +"._C" + pos;
			grp.contracts.add(new ContractGrouping(rw.name(), myname, rw.referAsVar));
			cardImplements.put(myname, rw);
			if (rw.referAsVar != null)
				sd.addField(new StructField(rw, rw.referAsVar));

			for (MethodDefinition m : ci.methods) {
				MethodDefinition rwm = rewrite(c2, m);
				methods.add(new MethodInContext(cd.innerScope(), ci.location(), ci.name(), m.intro.name, HSIEForm.Type.CONTRACT, rwm));
				rw.methods.add(rwm);
			}
			
			pos++;
		}
		
		pos=0;
		for (ContractService cs : cd.services) {
			ContractService rw = rewriteCS(c2, cs);
			String myname = cd.name +"._S" + pos;
			grp.services.add(new ServiceGrouping(rw.name(), myname, rw.referAsVar));
			cardServices.put(myname, rw);
			if (rw.referAsVar != null)
				sd.fields.add(new StructField(rw, rw.referAsVar));

			for (MethodDefinition m : cs.methods)
				methods.add(new MethodInContext(cd.innerScope(), null, cs.name(), m.intro.name, HSIEForm.Type.SERVICE, rewrite(c2, m)));

			pos++;
		}

		if (cd.template != null)
			templates.add(rewrite(new TemplateContext(c2), cd.template));
		
		for (HandlerImplements hi : cd.handlers) {
			HandlerImplements rw = rewriteHI(c2, hi, pos);
			if (rw == null)
				continue;
			String hiName = cd.name +"."+hi.name;
			cardHandlers.put(hiName, rw);
			List<Type> args = new ArrayList<Type>();
			//				System.out.println("Creating class for handler " + hiName);
			// Using polymorphic vars with random names here seems clever, but I'm not really sure that it is
			// We need to make sure that in doing this, everything typechecks to the same set of variables, whereas we normally insert fresh variables every time we use the type
			for (int i=0;i<rw.boundVars.size();i++)
				args.add(Type.polyvar(null, "A"+i));
			StructDefn hsd = new StructDefn(hi.location(), hiName, false, args);
			int j=0;
			for (Object s : rw.boundVars) {
				hsd.fields.add(new StructField(Type.polyvar(rw.location(), "A"+j), ((HandlerLambda)s).var));
				j++;
			}
			structs.put(hiName, hsd);
			HandlerContext hc = new HandlerContext(c2, rw);
			for (MethodDefinition m : hi.methods)
				methods.add(new MethodInContext(cd.innerScope(), null, hi.name(), m.intro.name, HSIEForm.Type.HANDLER, rewrite(hc, m)));
			
			grp.handlers.add(new HandlerGrouping(cd.name + "." + rw.name));
		}
		
		rewriteScope(c2, cd.fnScope);
	}

	private Template rewrite(TemplateContext cx, Template template) {
		try {
			// Again, the need for a scope seems dodgy if we've rewritten ...
			return new Template(template.prefix, rewrite(cx, template.content), template.scope);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private TemplateLine rewrite(TemplateContext cx, TemplateLine tl) {
		List<Object> attrs = new ArrayList<Object>();
		List<Object> formats = new ArrayList<Object>();
		if (tl instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) tl;
			for (Object o : tf.formats) {
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
		}
		if (tl instanceof ContentString) {
			return tl;
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr)tl;
			return new ContentExpr(rewriteExpr(cx, ce.expr), ce.editable(), formats);
		} else if (tl instanceof CardReference) {
			CardReference cr = (CardReference) tl;
			Object cardName = cr.explicitCard == null ? null : cx.resolve(cr.location, (String)cr.explicitCard);
			Object yoyoName = cr.yoyoVar == null ? null : cx.resolve(cr.location, (String)cr.yoyoVar);
			return new CardReference(cr.location, cardName, yoyoName);
		} else if (tl instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) tl;
			for (Object o : td.attrs) {
				if (o instanceof TemplateExplicitAttr) {
					TemplateExplicitAttr tea = (TemplateExplicitAttr) o;
					Object value = tea.value;
					if (tea.type == TemplateToken.IDENTIFIER) // any type of expression
						value = rewriteExpr(cx, value);
					attrs.add(new TemplateExplicitAttr(tea.location, tea.attr, tea.type, value));
				} else
					throw new UtilException("Attr type not handled: " + o.getClass());
			}
			TemplateDiv ret = new TemplateDiv(td.customTag, td.customTagVar, attrs, formats);
			for (TemplateLine i : td.nested)
				ret.nested.add(rewrite(cx, i));
			for (EventHandler h : td.handlers) {
				ret.handlers.add(new EventHandler(h.action, rewriteExpr(cx, h.expr)));
			}
			return ret;
		} else if (tl instanceof TemplateList) {
			TemplateList ul = (TemplateList)tl;
			Object rlistVar = cx.resolve(ul.listLoc, (String) ul.listVar);
			TemplateListVar tlv = new TemplateListVar(ul.listLoc, (String) ul.iterVar);
			TemplateList rul = new TemplateList(ul.listLoc, rlistVar, tlv, null, null, formats);
			cx = new TemplateContext(cx, tlv);
			rul.template = rewrite(cx, ul.template);
			return rul;
		} else if (tl instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases)tl;
			TemplateCases ret = new TemplateCases(tc.loc, rewriteExpr(cx, tc.switchOn));
			for (TemplateOr tor : tc.cases)
				ret.addCase(rewrite(cx, tor));
			return ret;
		} else if (tl instanceof D3Invoke) {
			D3Invoke prev = (D3Invoke) tl;
			D3Context c2 = new D3Context(cx, prev.d3.dloc, prev.d3.iter);
			List<D3PatternBlock> patterns = new ArrayList<D3PatternBlock>();
			for (D3PatternBlock p : prev.d3.patterns) {
				D3PatternBlock rp = new D3PatternBlock(p.pattern);
				patterns.add(rp);
				for (D3Section s : p.sections.values()) {
					D3Section rs = new D3Section(s.location, s.name);
					rp.sections.put(s.name, rs);
					for (MethodMessage mm : s.actions)
						rs.actions.add(rewrite(c2, mm));
					for (PropertyDefn prop : s.properties.values())
						rs.properties.put(prop.name, new PropertyDefn(prop.location, prop.name, rewriteExpr(c2, prop.value)));
				}
			}
			D3Thing rwD3 = new D3Thing(prev.d3.prefix, prev.d3.name, prev.d3.dloc, rewriteExpr(c2, prev.d3.data), prev.d3.iter, patterns);
			D3Invoke rw = new D3Invoke(prev.scope, rwD3);
			d3s.add(rw);
			return rw;
		} else 
			throw new UtilException("Content type not handled: " + tl.getClass());
	}

	private TemplateOr rewrite(TemplateContext cx, TemplateOr tor) {
		return new TemplateOr(tor.location(), rewriteExpr(cx, tor.cond), rewrite(cx, tor.template));
	}

	private ContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		try {
			Object av = cx.nested.resolve(ci.location(), ci.name());
			if (av == null || !(av instanceof AbsoluteVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + ci.name());
				return ci;
			}
			return new ContractImplements(ci.location(), ((AbsoluteVar)av).id, ci.varLocation, ci.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private ContractService rewriteCS(CardContext cx, ContractService cs) {
		try {
			Object av = cx.nested.resolve(cs.location(), cs.name());
			if (av == null || !(av instanceof AbsoluteVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + cs.name());
				return cs;
			}
			return new ContractService(cs.location(), ((AbsoluteVar)av).id, cs.vlocation, cs.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private HandlerImplements rewriteHI(CardContext cx, HandlerImplements hi, int cs) {
		try {
			Object av = cx.nested.resolve(hi.location(), hi.name());
			if (av == null || !(av instanceof AbsoluteVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + hi.name());
				return hi;
			}
			List<Object> bvs = new ArrayList<Object>();
			for (Object o : hi.boundVars) {
				HandlerLambda hl;
				if (o instanceof VarPattern) {
					VarPattern vp = (VarPattern) o;
					hl = new HandlerLambda(vp.varLoc, hi.name, null, vp.var);
				} else if (o instanceof TypedPattern) {
					TypedPattern vp = (TypedPattern) o;
					Object type = cx.resolve(vp.typeLocation, vp.type);
					if (type instanceof AbsoluteVar && ((AbsoluteVar)type).defn instanceof Type) {
						hl = new HandlerLambda(vp.varLocation, hi.name, (Type) ((AbsoluteVar)type).defn, vp.var);
					} else {
						errors.message(vp.typeLocation, vp.type + " is not a type");
						continue;
					}
				} else
					throw new UtilException("Can't handle pattern " + o + " as a handler lambda");
				bvs.add(hl);
			}
			HandlerImplements ret = new HandlerImplements(hi.location(), hi.name, ((AbsoluteVar)av).id, bvs);
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
			list.add(rewrite(new FunctionCaseContext(cx, f.name, cs, c.intro.allVars(errors, cx, f.name + "_" + cs), c.innerScope(), false), c));
			cs++;
		}
//		System.out.println("rewritten to " + list.get(0).expr);
		FunctionDefinition ret = new FunctionDefinition(f.location, f.mytype, f.name, f.nargs, list);
		return ret;
	}

	private MethodDefinition rewrite(NamingContext cx, MethodDefinition m) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, m.intro.allVars(errors, cx, m.intro.name + "_" + cs), c.innerScope(), true), c));
			cs++;
		}
		return new MethodDefinition(rewrite(cx, m.intro), list);
	}

	private EventHandlerDefinition rewrite(NamingContext cx, EventHandlerDefinition ehd) {
		List<EventCaseDefn> list = new ArrayList<EventCaseDefn>();
		int cs = 0;
		for (EventCaseDefn c : ehd.cases) {
			Map<String, LocalVar> locals = new HashMap<String, LocalVar>();
			ehd.intro.gatherVars(errors, cx, ehd.intro.name, locals);
			list.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name +"_" + cs, cs, locals, c.innerScope(), false), c));
			cs++;
		}
		return new EventHandlerDefinition(rewrite(cx, ehd.intro), list);
	}

	private StructDefn rewrite(NamingContext cx, StructDefn sd) {
		StructDefn ret = new StructDefn(sd.location(), sd.name(), sd.generate, (List<Type>)sd.polys());
		for (StructField sf : sd.fields) {
			StructField rsf = new StructField(rewrite(cx, sf.type), sf.name, rewriteExpr(cx, sf.init));
			ret.addField(rsf);
		}
		return ret;
	}

	private FunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c) {
		FunctionIntro intro = rewrite(cx, c.intro);
		Object expr = rewriteExpr(cx, c.expr);
		if (expr == null)
			return null;
		FunctionCaseDefn ret = new FunctionCaseDefn(c.innerScope().outer, intro.location, intro.name, intro.args, expr);
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
		return new FunctionIntro(intro.location, intro.name, args);
	}

	private Object rewritePattern(NamingContext scope, Object o) {
		if (o instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) o;
			try {
				Object type = scope.resolve(tp.typeLocation, tp.type);
				if (!(type instanceof AbsoluteVar))
					errors.message(tp.typeLocation, "could not handle " + type);
				return new TypedPattern(tp.typeLocation, ((AbsoluteVar)type).id, tp.varLocation, tp.var);
			} catch (ResolutionException ex) {
				errors.message(tp.typeLocation, "no such type: " + ex.name);
				return null;
			}
		} else if (o instanceof VarPattern) {
			return o;
		} else {
//			System.out.println("Couldn't rewrite pattern " + o.getClass());
			return o;
		}
	}

	public MethodMessage rewrite(NamingContext cx, MethodMessage mm) {
		List<Locatable> newSlot = null;
		if (mm.slot != null && !mm.slot.isEmpty()) {
			newSlot = new ArrayList<Locatable>();
			LocatedToken slot = (LocatedToken) mm.slot.get(0);
			Locatable r = (Locatable) cx.resolve(slot.location, slot.text);
			newSlot.add(r);
			for (int i=1;i<mm.slot.size();i++)
				newSlot.add(mm.slot.get(i));
		}
		return new MethodMessage(newSlot, rewriteExpr(cx, mm.expr));
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
				if (ret instanceof AbsoluteVar || ret instanceof LocalVar || ret instanceof IterVar || ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof HandlerLambda || ret instanceof TemplateListVar)
					return ret;
				else
					throw new UtilException("cannot handle " + ret.getClass());
			} else if (expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) expr;
				if (ae.fn instanceof UnresolvedOperator && ((UnresolvedOperator)ae.fn).op.equals(".")) {
					String fname;
					InputPosition loc;
					if (ae.args.get(1) instanceof ApplyExpr) { // The field starts with a capital
						System.out.println("Capital");
						ApplyExpr inner = (ApplyExpr) ae.args.get(1);
						fname = ((UnresolvedVar)inner.fn).var;
						loc = ((UnresolvedVar)inner.fn).location;
					} else {
						UnresolvedVar field = (UnresolvedVar)ae.args.get(1);
						fname = field.var;
						loc = field.location;
					}
					// The case where we have an absolute var by package name
					// Does this need to be here as well as in RootScope?
					if (!(ae.args.get(0) instanceof ApplyExpr)) {
						UnresolvedVar uv0 = (UnresolvedVar)ae.args.get(0);
						Object pkgEntry = cx.resolve(uv0.location, uv0.var);
						if (pkgEntry instanceof AbsoluteVar) {
							Object o = ((AbsoluteVar)pkgEntry).defn;
							if (o instanceof PackageDefn)
								return new AbsoluteVar(((PackageDefn)o).innerScope().getEntry(fname));
						}
					}
					
					// expr . field
					Object applyFn = rewriteExpr(cx, ae.args.get(0));
	
					return new ApplyExpr(ae.location, cx.resolve(ae.location, "."), applyFn, new StringLiteral(loc, fname));
				}
				List<Object> args = new ArrayList<Object>();
				for (Object o : ae.args)
					args.add(rewriteExpr(cx, o));
				return new ApplyExpr(ae.location, rewriteExpr(cx, ae.fn), args);
			} else if (expr instanceof IfExpr) {
				IfExpr ie = (IfExpr)expr;
				return new IfExpr(rewriteExpr(cx, ie.guard), rewriteExpr(cx, ie.ifExpr), rewriteExpr(cx, ie.elseExpr));
			} else
				throw new UtilException("Can't rewrite expr " + expr + " of type " + expr.getClass());
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private Type rewrite(NamingContext cx, Type type) {
		if (type.iam != WhatAmI.REFERENCE)
			return type;
		try {
			Object r = cx.resolve(type.location(), type.name());
			if (!(r instanceof AbsoluteVar)) {
				errors.message(type.location(), type.name() + " is not a type definition");
				return null;
			}
			Type ret = (Type) ((AbsoluteVar)r).defn;
			if (ret == null) {
				errors.message(type.location(), "there is no definition in var for " + type.name());
				return null;
			}
			if (ret.hasPolys() && !type.hasPolys()) {
				errors.message(type.location(), "cannot use " + ret.name() + " without specifying polymorphic arguments");
				return null;
			} else if (!ret.hasPolys() && type.hasPolys()) {
				errors.message(type.location(), "cannot use polymorphic arguments to type " + ret.name());
				return null;
			} else if (ret.hasPolys() && type.hasPolys()) {
				// check and instantiate
				if (type.polys().size() != ret.polys().size()) {
					errors.message(type.location(), "incorrect number of polymorphic arguments to type " + ret.name());
					return null;
				} else {
					List<Type> rwp = new ArrayList<Type>();
					for (Type p : type.polys())
						rwp.add(rewrite(cx, p));
					return ret.instance(type.location(), rwp);
				}
			} else
				return ret;
		} catch (ResolutionException ex) {
			errors.message(type.location(), ex.getMessage());
			return null;
		}
	}

	public void dump() {
		try {
			PrintWriter pw = new PrintWriter(System.out);
			for (Entry<String, StructDefn> x : structs.entrySet())
				System.out.println("Struct " + x.getKey());
			for (Entry<String, CardGrouping> x : cards.entrySet())
				System.out.println("Card " + x.getKey());
			for (Entry<String, ContractImplements> x : cardImplements.entrySet())
				System.out.println("Impl " + x.getKey());
			for (Entry<String, ContractService> x : cardServices.entrySet())
				System.out.println("Service " + x.getKey());
			for (Entry<String, HandlerImplements> x : cardHandlers.entrySet())
				System.out.println("Handler " + x.getKey());
			for (Entry<String, FunctionDefinition> x : functions.entrySet()) {
				x.getValue().dumpTo(pw);
			}
			pw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
