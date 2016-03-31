package org.flasck.flas.rewriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.PackageFinder;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
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
import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.ScopedVar;
import org.flasck.flas.parsedForm.SpecialFormat;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.stories.D3Thing;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.typechecker.TypeOfSomethingElse;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
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
	public final Map<String, Object> builtins = new TreeMap<String, Object>();
	public final Map<String, ContractDecl> contracts = new TreeMap<String, ContractDecl>();
	public final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<Template> templates = new ArrayList<Template>();
	public final List<D3Invoke> d3s = new ArrayList<D3Invoke>();
	public final Map<String, ContractImplements> cardImplements = new TreeMap<String, ContractImplements>();
	public final Map<String, ContractService> cardServices = new TreeMap<String, ContractService>();
	public final Map<String, HandlerImplements> callbackHandlers = new TreeMap<String, HandlerImplements>();
	public final List<MethodInContext> methods = new ArrayList<MethodInContext>();
	public final List<EventHandlerInContext> eventHandlers = new ArrayList<EventHandlerInContext>();
	public final Map<String, MethodInContext> standalone = new TreeMap<String, MethodInContext>();
	public final Map<String, FunctionDefinition> functions = new TreeMap<String, FunctionDefinition>();

	public abstract class NamingContext {
		protected final NamingContext nested;
		
		public NamingContext(NamingContext inner) {
			this.nested = inner;
		}

		public abstract Object resolve(InputPosition location, String name);

		public boolean hasCard() {
			if (nested != null)
				return nested.hasCard();
			return false;
		}
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
			if (biscope.contains(name)) {
				return new PackageVar(location, biscope.getEntry(name));
			}
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
					if (pkgFinder != null) {
						scope = pkgFinder.loadFlim(biscope, pkgName);
						if (scope == null)
							throw new ResolutionException(location, 0, pkgName);
					}
					if (scope == null)
						throw new ResolutionException(location, name);
					tmp = name.substring(idx+1);
				}
				ScopeEntry o = scope.getEntry(tmp);
				if (o != null) {
					Object defn = o.getValue();
					if (defn instanceof ContractDecl)
						contracts.put(name, (ContractDecl) defn);
					return new PackageVar(location, o);
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
				return new PackageVar(location, pkg.innerScope().getEntry(name));
			return nested.resolve(location, name);
		}
	}

	/** The Card Context can only be found directly in a Package Context 
	 */
	public class CardContext extends NamingContext {
		private final String prefix;
		private final Map<String, Type> members = new TreeMap<String, Type>();
		private final Map<String, ObjectReference> statics = new TreeMap<String, ObjectReference>();
		private final Scope innerScope;

		public CardContext(PackageContext cx, CardDefinition cd) {
			super(cx);
			this.prefix = cd.name;
			this.innerScope = cd.innerScope();
			if (cd.state != null) {
				for (StructField sf : cd.state.fields) {
					try {
						members.put(sf.name, (Type)((PackageVar)cx.resolve(sf.type.location(), sf.type.name())).defn);
					} catch (ResolutionException ex) {
						errors.message(ex.location, ex.getMessage());
					}
				}
			}
			for (ContractImplements ci : cd.contracts) {
				if (ci.referAsVar != null)
					members.put(ci.referAsVar, ci);
			}
			for (ContractService cs : cd.services) {
				if (cs.referAsVar != null)
					members.put(cs.referAsVar, cs);
			}
			for (HandlerImplements hi : cd.handlers) {
				statics.put(State.simpleName(hi.hiName), new ObjectReference(hi.location(), prefix, hi.hiName));
			}
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (members.containsKey(name))
				return new CardMember(location, prefix, name, members.get(name));
			if (statics.containsKey(name))
				return statics.get(name);
			if (innerScope.contains(name))
				return new CardFunction(location, prefix, name);
			return nested.resolve(location, name);
		}

		@Override
		public boolean hasCard() {
			return true;
		}
	}

	/** The Handler Context can only be in a Card Context
	 */
	class HandlerContext extends NamingContext {
		private final HandlerImplements hi;

		HandlerContext(NamingContext cx, HandlerImplements hi) {
			super(cx);
			this.hi = hi;
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			for (Object o : hi.boundVars)
				if (((HandlerLambda)o).var.equals(name))
					return o;
			Object ret = nested.resolve(location, name);
			if (ret instanceof ScopedVar) {
				InputPosition loc = ((ScopedVar) ret).location();
				Type type = new TypeOfSomethingElse(loc, ((ScopedVar)ret).id);
				HandlerLambda hl = new HandlerLambda(loc, hi.hiName, type, name);
				hi.addScoped(hl, (ScopedVar) ret);
				return hl;
			}
			return ret;
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

	public class FormatContext extends NamingContext {
		public FormatContext(TemplateContext cx) {
			super(cx);
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (name.equals("dragOrder") || name.equals("dropTarget"))
				return new SpecialFormat(location, name);
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
		protected final Map<String, LocalVar> bound;
		private final Scope inner;
		private final boolean fromMethod;

		FunctionCaseContext(NamingContext cx, String myname, int cs, Map<String, LocalVar> locals, Scope inner, boolean fromMethod) {
			super(cx);
			this.bound = locals;
			this.inner = inner;
			this.fromMethod = fromMethod;
		}

		public Object resolve(InputPosition location, String name) {
			if (bound.containsKey(name))
				return bound.get(name); // a local var
			if (inner.contains(name))
				return new ScopedVar(inner.getEntry(name), true);
			Object res = nested.resolve(location, name);
			if (res instanceof ObjectReference)
				return new ObjectReference(location, (ObjectReference)res, fromMethod);
			if (res instanceof CardFunction)
				return new CardFunction(location, (CardFunction)res, fromMethod);
			return res;
		}
	}

	public class NestedScopeContext extends NamingContext {

		public NestedScopeContext(NamingContext inner) {
			super(inner);
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			Object ret = nested.resolve(location, name);
			if (ret instanceof LocalVar) {
				LocalVar lv = (LocalVar) ret;
				return new ScopedVar(lv.location(), lv.var, lv, false);
			} else if (ret instanceof ScopedVar) {
				return ((ScopedVar)ret).notLocal();
			} else
				return ret;
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
			else if (val instanceof MethodDefinition) {
				MethodInContext mic = rewriteStandaloneMethod(cx, from, (MethodDefinition)val, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE);
				standalone.put(mic.name, mic);
			} else if (val instanceof EventHandlerDefinition)
				eventHandlers.add(new EventHandlerInContext(from, name, rewrite(cx, (EventHandlerDefinition)val)));
			else if (val instanceof StructDefn) {
				structs.put(name, rewrite(cx, (StructDefn)val));
			} else if (val instanceof UnionTypeDefn) {
				types.put(name, (UnionTypeDefn)val);
			} else if (val instanceof ContractDecl) {
				contracts.put(name, rewrite(cx, (ContractDecl)val));
			} else if (val instanceof HandlerImplements) {
				callbackHandlers.put(name, rewriteHI(cx, (HandlerImplements)val, from));
			} else if (val instanceof Type) {
				; // don't do anything - is that OK? 
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
				sd.addField(new StructField(sf.loc, false, rewrite(cx, sf.type, false), sf.name, rewriteExpr(cx, sf.init)));
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
				sd.addField(new StructField(rw.location(), false, rw, rw.referAsVar));

			for (MethodDefinition m : ci.methods) {
				MethodDefinition rwm = rewrite(c2, m, true);
				methods.add(new MethodInContext(this, cx, cd.innerScope(), MethodInContext.DOWN, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.CONTRACT, rwm));
				rw.methods.add(rwm);
			}
			
			pos++;
		}
		
		pos=0;
		for (ContractService cs : cd.services) {
			ContractService rw = rewriteCS(c2, cs);
			if (rw == null)
				continue;
			String myname = cd.name +"._S" + pos;
			grp.services.add(new ServiceGrouping(rw.name(), myname, rw.referAsVar));
			cardServices.put(myname, rw);
			if (rw.referAsVar != null)
				sd.fields.add(new StructField(rw.vlocation, false, rw, rw.referAsVar));

			for (MethodDefinition m : cs.methods)
				methods.add(new MethodInContext(this, cx, cd.innerScope(), MethodInContext.UP, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.SERVICE, rewrite(c2, m, true)));

			pos++;
		}

		if (cd.template != null)
			templates.add(rewrite(new TemplateContext(c2), cd.template));
		
		for (HandlerImplements hi : cd.handlers) {
			HandlerImplements rw = rewriteHI(c2, hi, cd.innerScope());
			if (rw != null)
				grp.handlers.add(new HandlerGrouping(rw.hiName, rw));
		}
		
		grp.platforms.putAll(cd.platforms);
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
		if (tl == null)
			return null;
		List<Object> attrs = new ArrayList<Object>();
		List<Object> formats = new ArrayList<Object>();
		List<SpecialFormat> specials = new ArrayList<SpecialFormat>();
		if (tl instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) tl;
			FormatContext fc = new FormatContext(cx);
			for (Object o : tf.formats) {
				if (o instanceof TemplateToken) {
					TemplateToken tt = (TemplateToken) o;
					if (tt.type == TemplateToken.STRING)
						formats.add(tt);
					else if (tt.type == TemplateToken.IDENTIFIER) {
						Object rw = rewriteExpr(fc, ItemExpr.from(new ExprToken(tt.location, ExprToken.IDENTIFIER, tt.text)));
						if (rw instanceof SpecialFormat)
							specials.add((SpecialFormat)rw);
						else
							formats.add(rw);
					}
					else
						throw new UtilException("Format type not handled: " + tt);
				} else if (o instanceof ApplyExpr) {
					Object rw = rewriteExpr(fc, o);
					if (rw instanceof ApplyExpr && ((ApplyExpr)rw).fn instanceof SpecialFormat) {
						ApplyExpr ae = (ApplyExpr)rw;
						SpecialFormat sf = (SpecialFormat) ae.fn;
						sf.args.addAll(ae.args);
						specials.add(sf);
					} else
						formats.add(rw);
				} else 
					throw new UtilException("Format type not handled: " + o.getClass());
			}
		}
		if (tl instanceof ContentString) {
			ContentString cs = (ContentString)tl;
			return rewriteEventHandlers(cx, new ContentString(cs.text, formats), ((TemplateFormatEvents)tl).handlers);
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr)tl;
			return rewriteEventHandlers(cx, new ContentExpr(rewriteExpr(cx, ce.expr), ce.editable(), formats), ((TemplateFormatEvents)tl).handlers);
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
			List<String> droppables = null; 
			for (SpecialFormat tt : specials) {
				if (tt.name.equals("dropTarget")) {
					if (droppables == null)
						droppables = new ArrayList<String>();
					for (Object o : tt.args) {
						if (!(o instanceof StringLiteral))
							errors.message(((Locatable)o).location(), "arguments to dropTarget must be string literals");
						else
							droppables.add(((StringLiteral)o).text);
					}
				} else
					errors.message(tt.location(), "Cannot handle special format " + tt.name);
			}
			TemplateDiv ret = new TemplateDiv(td.customTag, td.customTagVar, attrs, formats);
			for (TemplateLine i : td.nested)
				ret.nested.add(rewrite(cx, i));
			rewriteEventHandlers(cx, ret, td.handlers);
			ret.droppables = droppables;
			return ret;
		} else if (tl instanceof TemplateList) {
			TemplateList ul = (TemplateList)tl;
			TemplateListVar tlv = new TemplateListVar(ul.listLoc, (String) ul.iterVar);
			boolean supportDragOrdering = false;
			for (SpecialFormat tt : specials) {
				if (tt.name.equals("dragOrder")) {
					supportDragOrdering = true;
				} else
					errors.message(tt.location(), "Cannot handle special format " + tt.name);
			}
			TemplateList rul = new TemplateList(ul.listLoc, rewriteExpr(cx, ul.listVar), ul.iterLoc, tlv, ul.customTag, ul.customTagVar, formats, supportDragOrdering);
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
			throw new UtilException("Content type not handled: " + (tl == null?"null":tl.getClass()));
	}

	private TemplateLine rewriteEventHandlers(TemplateContext cx, TemplateFormatEvents ret, List<EventHandler> handlers) {
		// It may or may not be the same array ... copy it to be sure ...
		handlers = new ArrayList<EventHandler>(handlers);
		ret.handlers.clear();
		for (EventHandler h : handlers) {
			ret.handlers.add(new EventHandler(h.action, rewriteExpr(cx, h.expr)));
		}
		return ret;
	}

	private TemplateOr rewrite(TemplateContext cx, TemplateOr tor) {
		return new TemplateOr(tor.location(), rewriteExpr(cx, tor.cond), rewrite(cx, tor.template));
	}

	private ContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		try {
			Object av = cx.nested.resolve(ci.location(), ci.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + ci.name());
				return ci;
			}
			return new ContractImplements(ci.location(), ((PackageVar)av).id, ci.varLocation, ci.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private ContractService rewriteCS(CardContext cx, ContractService cs) {
		try {
			Object av = cx.nested.resolve(cs.location(), cs.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + cs.name());
				return cs;
			}
			return new ContractService(cs.location(), ((PackageVar)av).id, cs.vlocation, cs.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private HandlerImplements rewriteHI(NamingContext cx, HandlerImplements hi, Scope scope) {
		try {
			Type any = (Type) ((PackageVar)cx.nested.resolve(hi.location(), "Any")).defn;
			Object av = cx.nested.resolve(hi.location(), hi.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + hi.name());
				return hi;
			}
			PackageVar ctr = (PackageVar) av;
//			String rwname = cx.prefix + "." + hi.name;
			String rwname = hi.hiName;
			List<Object> bvs = new ArrayList<Object>();
			for (Object o : hi.boundVars) {
				HandlerLambda hl;
				if (o instanceof VarPattern) {
					VarPattern vp = (VarPattern) o;
					hl = new HandlerLambda(vp.varLoc, rwname, any, vp.var);
				} else if (o instanceof TypedPattern) {
					TypedPattern vp = (TypedPattern) o;
					hl = new HandlerLambda(vp.varLocation, rwname, rewrite(cx, vp.type, false), vp.var);
				} else
					throw new UtilException("Can't handle pattern " + o + " as a handler lambda");
				bvs.add(hl);
			}
			HandlerImplements ret = new HandlerImplements(hi.location(), rwname, ctr.id, hi.inCard, bvs);
			callbackHandlers.put(ret.hiName, ret);
			HandlerContext hc = new HandlerContext(cx, ret);
			for (MethodDefinition m : hi.methods) {
				MethodDefinition rm = rewrite(hc, m, true);
				ret.methods.add(rm);
				methods.add(new MethodInContext(this, cx, scope, MethodInContext.DOWN, ret.location(), ret.name(), m.intro.name, HSIEForm.CodeType.HANDLER, rm));
			}
			StructDefn hsd = new StructDefn(hi.location(), ret.hiName, false);
			for (Object s : ret.boundVars) {
				HandlerLambda hl = (HandlerLambda) s;
				hsd.fields.add(new StructField(hl.location, false, hl.type, hl.var));
			}
			structs.put(ret.hiName, hsd);
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
			FunctionCaseContext fccx = new FunctionCaseContext(cx, f.name, cs, c.intro.allVars(errors, this, cx, f.name + "_" + cs), c.innerScope(), false);
			list.add(rewrite(fccx, c));
			cs++;
		}
//		System.out.println("rewritten to " + list.get(0).expr);
		FunctionDefinition ret = new FunctionDefinition(f.location, f.mytype, f.name, f.nargs, list);
		return ret;
	}

	private MethodInContext rewriteStandaloneMethod(NamingContext cx, Scope from, MethodDefinition m, HSIEForm.CodeType codeType) {
		MethodDefinition rw = rewrite(cx, m, false);
		return new MethodInContext(this, cx, from, MethodInContext.STANDALONE, rw.location(), null, rw.intro.name, codeType, rw);
	}
	
	private MethodDefinition rewrite(NamingContext cx, MethodDefinition m, boolean fromHandler) {
		List<MethodCaseDefn> list = new ArrayList<MethodCaseDefn>();
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, m.intro.allVars(errors, this, cx, m.intro.name + "_" + cs), c.innerScope(), fromHandler), c));
			cs++;
		}
		return new MethodDefinition(rewrite(cx, m.intro), list);
	}

	private EventHandlerDefinition rewrite(NamingContext cx, EventHandlerDefinition ehd) {
		List<EventCaseDefn> list = new ArrayList<EventCaseDefn>();
		int cs = 0;
		for (EventCaseDefn c : ehd.cases) {
			Map<String, LocalVar> locals = new HashMap<String, LocalVar>();
			ehd.intro.gatherVars(errors, this, cx, ehd.intro.name, locals);
			list.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name +"_" + cs, cs, locals, c.innerScope(), false), c));
			cs++;
		}
		return new EventHandlerDefinition(rewrite(cx, ehd.intro), list);
	}

	private StructDefn rewrite(NamingContext cx, StructDefn sd) {
		StructDefn ret = new StructDefn(sd.location(), sd.name(), sd.generate, (List<Type>)sd.polys());
		for (StructField sf : sd.fields) {
			StructField rsf = new StructField(sf.loc, false, rewrite(cx, sf.type, false), sf.name, rewriteExpr(cx, sf.init));
			ret.addField(rsf);
		}
		return ret;
	}

	private ContractDecl rewrite(NamingContext cx, ContractDecl ctr) {
		ContractDecl ret = new ContractDecl(ctr.location(), ctr.name());
		for (ContractMethodDecl cmd : ctr.methods) {
			ret.addMethod(rewrite(cx, cmd));
		}
		return ret;
	}

	private ContractMethodDecl rewrite(NamingContext cx, ContractMethodDecl cmd) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : cmd.args) {
			args.add(rewritePattern(cx, o));
		}
		return new ContractMethodDecl(cmd.required, cmd.dir, cmd.name, args, rewrite(cx, cmd.type, false));
	}

	private FunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c) {
		FunctionIntro intro = rewrite(cx, c.intro);
		Object expr = rewriteExpr(cx, c.expr);
		if (expr == null)
			return null;
		FunctionCaseDefn ret = new FunctionCaseDefn(intro.location, intro.name, intro.args, expr);
		rewriteScope(new NestedScopeContext(cx), c.innerScope());
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

	public Object rewritePattern(NamingContext cx, Object o) {
		try {
			if (o instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o;
				return new TypedPattern(tp.typeLocation, rewrite(cx, tp.type, false), tp.varLocation, tp.var);
			} else if (o instanceof VarPattern) {
				return o;
			} else if (o instanceof ConstructorMatch) {
				ConstructorMatch cm = (ConstructorMatch) o;
				Object type = cx.resolve(cm.location, cm.ctor);
				if (!(type instanceof PackageVar))
					errors.message(cm.location, "could not handle " + type);
				ConstructorMatch ret = new ConstructorMatch(cm.location, (PackageVar)type);
				for (Field x : cm.args)
					ret.args.add(ret.new Field(x.field, rewritePattern(cx, x.patt)));
				return ret;
			} else {
				return o;
			}
		} catch (ResolutionException ex) {
			errors.message(ex.location, "no such type: " + ex.name);
			return null;
		}
	}

	public MethodMessage rewrite(NamingContext cx, MethodMessage mm) {
		List<Locatable> newSlot = null;
		if (mm.slot != null && !mm.slot.isEmpty()) {
			newSlot = new ArrayList<Locatable>();
			LocatedToken slot = (LocatedToken) mm.slot.get(0);
			try {
				Locatable r = (Locatable) cx.resolve(slot.location, slot.text);
				newSlot.add(r);
				for (int i=1;i<mm.slot.size();i++)
					newSlot.add(mm.slot.get(i));
			} catch (ResolutionException ex) {
				errors.message(slot.location, ex.getMessage());
				return null;
			}
		}
		return new MethodMessage(newSlot, rewriteExpr(cx, mm.expr));
	}

	private Object rewriteExpr(NamingContext cx, Object expr) {
		if (expr == null)
			return null;
		try {
			if (expr instanceof NumericLiteral || expr instanceof StringLiteral)
				return expr;
			else if (expr instanceof PackageVar || expr instanceof LocalVar || expr instanceof ScopedVar || expr instanceof CardMember)
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
				if (ret instanceof PackageVar || ret instanceof ScopedVar || ret instanceof LocalVar || ret instanceof IterVar || ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof HandlerLambda || ret instanceof TemplateListVar || ret instanceof SpecialFormat)
					return ret;
				else
					throw new UtilException("cannot handle " + ret.getClass());
			} else if (expr instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) expr;
				if (ae.fn instanceof UnresolvedOperator && ((UnresolvedOperator)ae.fn).op.equals(".")) {
					String fname;
					InputPosition loc;
					if (ae.args.get(1) instanceof ApplyExpr) { // The field starts with a capital
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
					Object aefn = ae.args.get(0);
					if (aefn instanceof ApplyExpr)
						aefn = rewriteExpr(cx, aefn);
					if (aefn == null)
						return null;
					Object castTo = null;
					InputPosition castLoc = null;
					while (aefn instanceof CastExpr) {
						CastExpr ce = (CastExpr)aefn;
						if (castTo == null) {
							castLoc = ce.location;
							castTo = cx.resolve(ce.location, (String) ce.castTo);
						}
						aefn = ((CastExpr)aefn).expr;
					}
					if (aefn instanceof PackageVar) {
						PackageVar pv = (PackageVar)aefn;
						Object defn = pv.defn;
						ScopeEntry entry = null;
						if (defn == null)
							;
						else if (defn instanceof PackageDefn) {
							Scope scope = ((PackageDefn)defn).innerScope();
							entry = scope.getEntry(fname);
						} else
							throw new UtilException("Can't handle that " + defn.getClass());
						Object pd;
						String pvn = pv.id + "." + fname;
						if (entry == null) {
							// attempt to force loading of package
							try {
								pd = cx.resolve(pv.location, pvn);
							} catch (ResolutionException ex) {
								return new PackageVar(pv.location, pvn, null);
							}
						} else
							pd = entry.getValue();
						return new PackageVar(pv.location, pvn, pd);
					}
					if (aefn instanceof UnresolvedVar) {
						UnresolvedVar uv0 = (UnresolvedVar)aefn;
						try {
							Object pkgEntry = cx.resolve(uv0.location, uv0.var);
							if (pkgEntry instanceof PackageVar) {
								Object o = ((PackageVar)pkgEntry).defn;
								if (o instanceof PackageDefn)
									return new PackageVar(((PackageDefn)o).innerScope().getEntry(fname));
							}
						} catch (ResolutionException ex) {
							return new PackageVar(uv0.location, uv0.var + "." + fname, null);
						}
					} 
					
					if (!(aefn instanceof ApplyExpr) && !(aefn instanceof UnresolvedVar))
						throw new UtilException("That case is not handled: " + aefn.getClass());
					
					// expr . field
					Object applyFn = rewriteExpr(cx, aefn);
					if (castTo != null)
						applyFn = new CastExpr(castLoc, castTo, applyFn);
	
					return new ApplyExpr(ae.location, cx.resolve(ae.location, "."), applyFn, new StringLiteral(loc, fname));
				}
				List<Object> args = new ArrayList<Object>();
				for (Object o : ae.args)
					args.add(rewriteExpr(cx, o));
				return new ApplyExpr(ae.location, rewriteExpr(cx, ae.fn), args);
			} else if (expr instanceof CastExpr) {
				CastExpr ce = (CastExpr) expr;
				Object resolve = cx.resolve(ce.location, (String) ce.castTo);
				return new CastExpr(ce.location, resolve, rewriteExpr(cx, ce.expr));
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

	public Type rewrite(NamingContext cx, Type type, boolean allowPolys) {
		try {
			Type ret;
			ret = null;
			if (type.iam != WhatAmI.REFERENCE)
				ret = type;
			else {
				try {
					Object r = cx.resolve(type.location(), type.name());
					if (!(r instanceof PackageVar)) {
						errors.message(type.location(), type.name() + " is not a type definition");
						return null;
					}
					ret = (Type) ((PackageVar)r).defn;
					if (ret == null) {
						errors.message(type.location(), "there is no definition in var for " + type.name());
						return null;
					}
				} catch (ResolutionException ex) {
					if (allowPolys)
						return Type.polyvar(type.location(), type.name());
					throw ex;
				}
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
						rwp.add(rewrite(cx, p, true));
					ret = ret.instance(type.location(), rwp);
				}
			}
			int k = -1;
			List<Type> fnargs = new ArrayList<Type>();
			if (ret.iam == WhatAmI.FUNCTION)
				k = ret.arity() + 1;
			else if (ret.iam == WhatAmI.TUPLE)
				k = ret.width();
			else
				return ret;
			for (int i=0;i<k;i++)
				fnargs.add(rewrite(cx, ret.arg(i), allowPolys));
			if (ret.iam == WhatAmI.FUNCTION)
				return Type.function(ret.location(), fnargs);
			else
				return Type.tuple(ret.location(), fnargs);
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
			for (Entry<String, HandlerImplements> x : callbackHandlers.entrySet())
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
