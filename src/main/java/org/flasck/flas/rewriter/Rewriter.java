package org.flasck.flas.rewriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.SpecialFormat;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.flim.PackageFinder;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstPattern;
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
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.IterVar;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
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
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.EventHandlerInContext;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.MethodInContext;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWContentString;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWD3Invoke;
import org.flasck.flas.rewrittenForm.RWD3PatternBlock;
import org.flasck.flas.rewrittenForm.RWD3Section;
import org.flasck.flas.rewrittenForm.RWD3Thing;
import org.flasck.flas.rewrittenForm.RWEventCaseDefn;
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.rewrittenForm.RWEventHandlerDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWHandlerLambda;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWPropertyDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTemplate;
import org.flasck.flas.rewrittenForm.RWTemplateCases;
import org.flasck.flas.rewrittenForm.RWTemplateDiv;
import org.flasck.flas.rewrittenForm.RWTemplateFormatEvents;
import org.flasck.flas.rewrittenForm.RWTemplateList;
import org.flasck.flas.rewrittenForm.RWTemplateListVar;
import org.flasck.flas.rewrittenForm.RWTemplateOr;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.Type.WhatAmI;
import org.flasck.flas.typechecker.TypeOfSomethingElse;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	static final Logger logger = LoggerFactory.getLogger("Rewriter");
	private final ErrorResult errors;
	private final PackageFinder pkgFinder;
	public final Map<String, RWStructDefn> structs = new TreeMap<String, RWStructDefn>();
	public final Map<String, RWUnionTypeDefn> types = new TreeMap<String, RWUnionTypeDefn>();
	public final Map<String, RWContractDecl> contracts = new TreeMap<String, RWContractDecl>();
	public final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<RWTemplate> templates = new ArrayList<RWTemplate>();
	public final List<RWD3Invoke> d3s = new ArrayList<RWD3Invoke>();
	public final Map<String, RWContractImplements> cardImplements = new TreeMap<String, RWContractImplements>();
	public final Map<String, RWContractService> cardServices = new TreeMap<String, RWContractService>();
	public final Map<String, RWHandlerImplements> callbackHandlers = new TreeMap<String, RWHandlerImplements>();
	public final List<MethodInContext> methods = new ArrayList<MethodInContext>();
	public final List<EventHandlerInContext> eventHandlers = new ArrayList<EventHandlerInContext>();
	public final Map<String, MethodInContext> standalone = new TreeMap<String, MethodInContext>();
	public final Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>();

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
				// TODO: need to be sure to consider "the current package"
				int idx = name.lastIndexOf(".");
				String pkgName = name.substring(0, idx);
				ImportPackage ip = pkgFinder.loadFlim(errors, pkgName);
				if (ip == null)
					throw new ResolutionException(location, 0, pkgName);
				Map.Entry<String, Object> defn = ip.getEntry(name);
				if (defn != null) {
					// Now, do I really want to wrap this up or just return it?
					// I think I do need to wrap it up in order to get the "location" here ...
					return new PackageVar(location, defn.getKey(), defn.getValue());
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
			if (pkg.innerScope().contains(name)) {
				String id = pkg.name + "." + name;
				System.out.println("Resolving " + name + " to " + id);
				Object val;
				if (structs.containsKey(id))
					val = structs.get(id);
				else if (contracts.containsKey(id))
					val = contracts.get(id);
				else if (callbackHandlers.containsKey(id))
					val = callbackHandlers.get(id);
				else
					throw new UtilException("Can't identify " + id);
				return new PackageVar(location, id, val);
			}
			return nested.resolve(location, name);
		}
	}

	public class StructDefnContext extends NamingContext {
		private final List<Type> polys;

		public StructDefnContext(NamingContext cx, List<Type> polys) {
			super(cx);
			this.polys = polys;
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			for (Type t : polys) {
				if (t.name().equals(name))
					return t;
			}
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
		private final RWHandlerImplements hi;

		HandlerContext(NamingContext cx, RWHandlerImplements hi) {
			super(cx);
			this.hi = hi;
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			for (Object o : hi.boundVars)
				if (((RWHandlerLambda)o).var.equals(name))
					return o;
			Object ret = nested.resolve(location, name);
			if (ret instanceof ScopedVar) {
				InputPosition loc = ((ScopedVar) ret).location();
				Type type = new TypeOfSomethingElse(loc, ((ScopedVar)ret).id);
				RWHandlerLambda hl = new RWHandlerLambda(loc, hi.hiName, type, name);
				hi.addScoped(hl, (ScopedVar) ret);
				return hl;
			}
			return ret;
		}
	}

	public class TemplateContext extends NamingContext {
		private final RWTemplateListVar listVar;

		public TemplateContext(CardContext cx) {
			super(cx);
			listVar = null;
		}
		
		public TemplateContext(TemplateContext cx, RWTemplateListVar tlv) {
			super(cx);
			if (tlv != null && tlv.name == null)
				throw new UtilException("Shouldn't happen");
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
			if (name.equals("dragOrder") || name.equals("dropTarget") || name.equals("rawHTML"))
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

	public void rewriteScope(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			if (name.equals("Nil"))
			logger.info("Rewriting " + name);
			Object val = x.getValue().getValue();
			if (val instanceof PackageDefn) {
				logger.info("Choosing not to follow " + name + " down into a nested package");
//				rewriteScope(cx, ((PackageDefn)val).innerScope());
			} else if (val instanceof CardDefinition)
				rewriteCard(cx, (CardDefinition)val);
			else if (val instanceof FunctionDefinition)
				functions.put(name, rewrite(cx, (FunctionDefinition)val));
			else if (val instanceof MethodDefinition) {
				MethodInContext mic = rewriteStandaloneMethod(cx, from, (MethodDefinition)val, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE);
				standalone.put(mic.name, mic);
			} else if (val instanceof EventHandlerDefinition)
				eventHandlers.add(new EventHandlerInContext(name, rewrite(cx, (EventHandlerDefinition)val)));
			else if (val instanceof StructDefn) {
				structs.put(name, rewrite(cx, (StructDefn)val));
			} else if (val instanceof UnionTypeDefn) {
				types.put(name, rewrite(cx, (UnionTypeDefn)val));
			} else if (val instanceof ContractDecl) {
				contracts.put(name, rewrite(cx, (ContractDecl)val));
			} else if (val instanceof HandlerImplements) {
				callbackHandlers.put(name, rewriteHI(cx, (HandlerImplements)val, from));
			} else if (val instanceof RWStructDefn) { // already rewritten struct - from builtin or FLIM
				structs.put(name, (RWStructDefn) val);
			} else if (val instanceof Type) {
				logger.warn("Not doing anything with Type " + name + ": " + val.getClass()); // don't do anything - is that OK? 
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	private void rewriteCard(NamingContext cx, CardDefinition cd) {
		if (!(cx instanceof PackageContext))
			throw new UtilException("Cannot have card in nested scope");
		CardContext c2 = new CardContext((PackageContext) cx, cd);
		RWStructDefn sd = new RWStructDefn(cd.location, cd.name, false);
		CardGrouping grp = new CardGrouping(sd);
		cards.put(cd.name, grp);
		if (cd.state != null) {
			for (StructField sf : cd.state.fields) {
				sd.addField(new RWStructField(sf.loc, false, rewrite(cx, sf.type, false), sf.name, rewriteExpr(cx, sf.init)));
				grp.inits.put(sf.name, rewriteExpr(cx, sf.init));
			}
		}
		
		int pos = 0;
		for (ContractImplements ci : cd.contracts) {
			RWContractImplements rw = rewriteCI(c2, ci);
			if (rw == null)
				continue;
			String myname = cd.name +"._C" + pos;
			grp.contracts.add(new ContractGrouping(rw.name(), myname, rw.referAsVar));
			cardImplements.put(myname, rw);
			if (rw.referAsVar != null)
				sd.addField(new RWStructField(rw.location(), false, rw, rw.referAsVar));

			for (MethodDefinition m : ci.methods) {
				RWMethodDefinition rwm = rewrite(c2, m, true);
				methods.add(new MethodInContext(this, cx, MethodInContext.DOWN, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.CONTRACT, rwm));
				rw.methods.add(rwm);
			}
			
			pos++;
		}
		
		pos=0;
		for (ContractService cs : cd.services) {
			RWContractService rw = rewriteCS(c2, cs);
			if (rw == null)
				continue;
			String myname = cd.name +"._S" + pos;
			grp.services.add(new ServiceGrouping(rw.name(), myname, rw.referAsVar));
			cardServices.put(myname, rw);
			if (rw.referAsVar != null)
				sd.fields.add(new RWStructField(rw.vlocation, false, rw, rw.referAsVar));

			for (MethodDefinition m : cs.methods)
				methods.add(new MethodInContext(this, cx, MethodInContext.UP, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.SERVICE, rewrite(c2, m, true)));

			pos++;
		}

		if (cd.template != null)
			templates.add(rewrite(new TemplateContext(c2), cd.template));
		
		for (HandlerImplements hi : cd.handlers) {
			RWHandlerImplements rw = rewriteHI(c2, hi, cd.innerScope());
			if (rw != null)
				grp.handlers.add(new HandlerGrouping(rw.hiName, rw));
		}
		
		grp.platforms.putAll(cd.platforms);
		rewriteScope(c2, cd.fnScope);
	}

	private RWTemplate rewrite(TemplateContext cx, Template template) {
		try {
			// Again, the need for a scope seems dodgy if we've rewritten ...
			return new RWTemplate(template.prefix, rewrite(cx, template.content));
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
			return rewriteEventHandlers(cx, new RWContentString(cs.text, formats), ((TemplateFormatEvents)tl).handlers);
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr)tl;
			boolean rawHTML = false;
			for (SpecialFormat tt : specials) {
				if (tt.name.equals("rawHTML")) {
					rawHTML = true;
				} else
					errors.message(tt.location(), "Cannot handle special format " + tt.name);
			}
			return rewriteEventHandlers(cx, new RWContentExpr(rewriteExpr(cx, ce.expr), ce.editable(), rawHTML, formats), ((TemplateFormatEvents)tl).handlers);
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
			RWTemplateDiv ret = new RWTemplateDiv(td.customTag, td.customTagVar, attrs, formats);
			for (TemplateLine i : td.nested)
				ret.nested.add(rewrite(cx, i));
			rewriteEventHandlers(cx, ret, td.handlers);
			ret.droppables = droppables;
			return ret;
		} else if (tl instanceof TemplateList) {
			TemplateList ul = (TemplateList)tl;
			TemplateListVar tlv = ul.iterVar != null ? new TemplateListVar(ul.listLoc, (String) ul.iterVar) : null;
			RWTemplateListVar rwv = tlv == null ? null : new RWTemplateListVar(tlv.location, tlv.name);
			boolean supportDragOrdering = false;
			for (SpecialFormat tt : specials) {
				if (tt.name.equals("dragOrder")) {
					supportDragOrdering = true;
				} else
					errors.message(tt.location(), "Cannot handle special format " + tt.name);
			}
			RWTemplateList rul = new RWTemplateList(ul.listLoc, rewriteExpr(cx, ul.listVar), ul.iterLoc, rwv, ul.customTag, ul.customTagVar, formats, supportDragOrdering);
			cx = new TemplateContext(cx, rwv);
			rul.template = rewrite(cx, ul.template);
			return rul;
		} else if (tl instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases)tl;
			RWTemplateCases ret = new RWTemplateCases(tc.loc, rewriteExpr(cx, tc.switchOn));
			for (TemplateOr tor : tc.cases)
				ret.addCase(rewrite(cx, tor));
			return ret;
		} else if (tl instanceof D3Invoke) {
			D3Invoke prev = (D3Invoke) tl;
			D3Context c2 = new D3Context(cx, prev.d3.dloc, prev.d3.iter);
			List<RWD3PatternBlock> patterns = new ArrayList<RWD3PatternBlock>();
			for (D3PatternBlock p : prev.d3.patterns) {
				RWD3PatternBlock rp = new RWD3PatternBlock(p.pattern);
				patterns.add(rp);
				for (D3Section s : p.sections.values()) {
					RWD3Section rs = new RWD3Section(s.location, s.name);
					rp.sections.put(s.name, rs);
					for (MethodMessage mm : s.actions)
						rs.actions.add(rewrite(c2, mm));
					for (PropertyDefn prop : s.properties.values())
						rs.properties.put(prop.name, new RWPropertyDefn(prop.location, prop.name, rewriteExpr(c2, prop.value)));
				}
			}
			RWD3Thing rwD3 = new RWD3Thing(prev.d3.prefix, prev.d3.name, prev.d3.dloc, rewriteExpr(c2, prev.d3.data), prev.d3.iter, patterns);
			RWD3Invoke rw = new RWD3Invoke(rwD3);
			d3s.add(rw);
			return rw;
		} else 
			throw new UtilException("Content type not handled: " + (tl == null?"null":tl.getClass()));
	}

	private TemplateLine rewriteEventHandlers(TemplateContext cx, RWTemplateFormatEvents ret, List<EventHandler> handlers) {
		// It may or may not be the same array ... copy it to be sure ...
		handlers = new ArrayList<EventHandler>(handlers);
		ret.handlers.clear();
		for (EventHandler h : handlers) {
			ret.handlers.add(new RWEventHandler(h.action, rewriteExpr(cx, h.expr)));
		}
		return ret;
	}

	private RWTemplateOr rewrite(TemplateContext cx, TemplateOr tor) {
		return new RWTemplateOr(tor.location(), rewriteExpr(cx, tor.cond), rewrite(cx, tor.template));
	}

	private RWContractDecl rewrite(NamingContext cx, ContractDecl ctr) {
		RWContractDecl ret = new RWContractDecl(ctr.kw, ctr.location(), ctr.name());
		for (ContractMethodDecl cmd : ctr.methods) {
			ret.addMethod(rewrite(cx, cmd));
		}
		return ret;
	}

	private RWContractMethodDecl rewrite(NamingContext cx, ContractMethodDecl cmd) {
		List<Object> args = new ArrayList<Object>();
		List<Type> targs = new ArrayList<Type>(); 
		for (Object o : cmd.args) {
			args.add(rewritePattern(cx, o));
			if (o instanceof TypedPattern) {
				targs.add(rewrite(cx, ((TypedPattern)o).type, false));
			}
		}
		targs.add(typeFrom(cx.resolve(cmd.location(), "Message")));
		return new RWContractMethodDecl(cmd.location(), cmd.required, cmd.dir, cmd.name, args, Type.function(cmd.location(), targs));
	}

	private Type typeFrom(Object resolve) {
		if (resolve == null)
			return null;
		else if (resolve instanceof Type)
			return (Type) resolve;
		else if (resolve instanceof PackageVar)
			return (Type) ((PackageVar)resolve).defn;
		else
			throw new UtilException("Cannot extract a type from " + resolve);
	}

	private RWContractImplements rewriteCI(CardContext cx, ContractImplements ci) {
		try {
			Object av = cx.nested.resolve(ci.location(), ci.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + ci.name());
				return null;
			}
			return new RWContractImplements(ci.kw, ci.location(), ((PackageVar)av).id, ci.varLocation, ci.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private RWContractService rewriteCS(CardContext cx, ContractService cs) {
		try {
			Object av = cx.nested.resolve(cs.location(), cs.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + cs.name());
				return null;
			}
			return new RWContractService(cs.kw, cs.location(), ((PackageVar)av).id, cs.vlocation, cs.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private RWHandlerImplements rewriteHI(NamingContext cx, HandlerImplements hi, Scope scope) {
		try {
			Type any = (Type) ((PackageVar)cx.nested.resolve(hi.location(), "Any")).defn;
			Object av = cx.nested.resolve(hi.location(), hi.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message((Block)null, "cannot find a valid definition of contract " + hi.name());
				return null;
			}
			PackageVar ctr = (PackageVar) av;
//			String rwname = cx.prefix + "." + hi.name;
			String rwname = hi.hiName;
			List<RWHandlerLambda> bvs = new ArrayList<RWHandlerLambda>();
			for (Object o : hi.boundVars) {
				RWHandlerLambda hl;
				if (o instanceof VarPattern) {
					VarPattern vp = (VarPattern) o;
					hl = new RWHandlerLambda(vp.varLoc, rwname, any, vp.var);
				} else if (o instanceof TypedPattern) {
					TypedPattern vp = (TypedPattern) o;
					hl = new RWHandlerLambda(vp.varLocation, rwname, rewrite(cx, vp.type, false), vp.var);
				} else
					throw new UtilException("Can't handle pattern " + o + " as a handler lambda");
				bvs.add(hl);
			}
			RWHandlerImplements ret = new RWHandlerImplements(hi.kw, hi.location(), rwname, ctr.id, hi.inCard, bvs);
			callbackHandlers.put(ret.hiName, ret);
			HandlerContext hc = new HandlerContext(cx, ret);
			for (MethodDefinition m : hi.methods) {
				RWMethodDefinition rm = rewrite(hc, m, true);
				ret.methods.add(rm);
				methods.add(new MethodInContext(this, cx, MethodInContext.DOWN, ret.location(), ret.name(), m.intro.name, HSIEForm.CodeType.HANDLER, rm));
			}
			RWStructDefn hsd = new RWStructDefn(hi.location(), ret.hiName, false);
			for (Object s : ret.boundVars) {
				RWHandlerLambda hl = (RWHandlerLambda) s;
				hsd.fields.add(new RWStructField(hl.location, false, hl.type, hl.var));
			}
			structs.put(ret.hiName, hsd);
			return ret;
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	public RWFunctionDefinition rewrite(NamingContext cx, FunctionDefinition f) {
//		System.out.println("Rewriting " + f.name);
		List<RWFunctionCaseDefn> list = new ArrayList<RWFunctionCaseDefn>();
		int cs = 0;
		RWFunctionIntro fi = null;
		for (FunctionCaseDefn c : f.cases) {
			Map<String, LocalVar> vars = allVars(errors, this, cx, f.name + "_" + cs, c.intro);
			FunctionCaseContext fccx = new FunctionCaseContext(cx, f.name, cs, vars, c.innerScope(), false);
			RWFunctionCaseDefn rwc = rewrite(fccx, c, vars);
			if (fi == null)
				fi = rwc.intro;
			list.add(rwc);
			cs++;
		}
//		System.out.println("rewritten to " + list.get(0).expr);
		RWFunctionDefinition ret = new RWFunctionDefinition(f.location, f.mytype, fi, list);
		return ret;
	}

	private MethodInContext rewriteStandaloneMethod(NamingContext cx, Scope from, MethodDefinition m, HSIEForm.CodeType codeType) {
		RWMethodDefinition rw = rewrite(cx, m, false);
		return new MethodInContext(this, cx, MethodInContext.STANDALONE, rw.location(), null, rw.intro.name, codeType, rw);
	}
	
	private RWMethodDefinition rewrite(NamingContext cx, MethodDefinition m, boolean fromHandler) {
		List<RWMethodCaseDefn> list = new ArrayList<RWMethodCaseDefn>();
		int cs = 0;
		Map<String, LocalVar> vars = allVars(errors, this, cx, m.intro.name + "_" + cs, m.intro);
		for (MethodCaseDefn c : m.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, vars, c.innerScope(), fromHandler), c, vars));
			cs++;
		}
		return new RWMethodDefinition(rewrite(cx, m.intro, vars), list);
	}

	private RWEventHandlerDefinition rewrite(NamingContext cx, EventHandlerDefinition ehd) {
		List<RWEventCaseDefn> list = new ArrayList<RWEventCaseDefn>();
		int cs = 0;
		Map<String, LocalVar> locals = new HashMap<String, LocalVar>();
		gatherVars(errors, this, cx, ehd.intro.name, locals, ehd.intro);
		for (EventCaseDefn c : ehd.cases) {
			list.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name +"_" + cs, cs, locals, c.innerScope(), false), c, locals));
			cs++;
		}
		return new RWEventHandlerDefinition(rewrite(cx, ehd.intro, locals), list);
	}

	private RWStructDefn rewrite(NamingContext cx, StructDefn sd) {
		RWStructDefn ret = new RWStructDefn(sd.location(), sd.name(), sd.generate, rewritePolys(sd.polys()));
		for (StructField sf : sd.fields) {
			// TODO: it's not clear that the expression needs this rewritten context
			StructDefnContext sx = new StructDefnContext(cx, ret.polys());
			RWStructField rsf = new RWStructField(sf.loc, false, rewrite(sx, sf.type, false), sf.name, rewriteExpr(sx, sf.init));
			ret.addField(rsf);
		}
		return ret;
	}

	private RWUnionTypeDefn rewrite(NamingContext cx, UnionTypeDefn u) {
		RWUnionTypeDefn ret = new RWUnionTypeDefn(u.location(), u.generate, u.name(), rewritePolys(u.polys()));
		for (TypeReference c : u.cases) {
			ret.addCase(rewrite(cx, c, true));
		}
		return ret;
	}

	protected List<Type> rewritePolys(List<TypeReference> polys) {
		List<Type> pts = new ArrayList<Type>(); // poly vars
		if (polys != null)
			for (TypeReference r : polys)
				pts.add(Type.polyvar(r.location(), r.name()));
		return pts;
	}


	private RWFunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c, Map<String, LocalVar> vars) {
		RWFunctionIntro intro = rewrite(cx, c.intro, vars);
		Object expr = rewriteExpr(cx, c.expr);
		if (expr == null)
			return null;
		RWFunctionCaseDefn ret = new RWFunctionCaseDefn(intro, expr);
		rewriteScope(new NestedScopeContext(cx), c.innerScope());
		return ret;
	}

	private RWMethodCaseDefn rewrite(FunctionCaseContext cx, MethodCaseDefn c, Map<String, LocalVar> vars) {
		RWMethodCaseDefn ret = new RWMethodCaseDefn(rewrite(cx, c.intro, vars));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private RWEventCaseDefn rewrite(FunctionCaseContext cx, EventCaseDefn c, Map<String, LocalVar> vars) {
		RWEventCaseDefn ret = new RWEventCaseDefn(c.kw, rewrite(cx, c.intro, vars));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private RWFunctionIntro rewrite(NamingContext cx, FunctionIntro intro, Map<String, LocalVar> vars) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(cx, o));
		}
		return new RWFunctionIntro(intro.location, intro.name, args, vars);
	}

	public Object rewritePattern(NamingContext cx, Object o) {
		try {
			if (o instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o;
				return new RWTypedPattern(tp.typeLocation, rewrite(cx, tp.type, false), tp.varLocation, tp.var);
			} else if (o instanceof VarPattern) {
				VarPattern vp = (VarPattern) o;
				return new RWVarPattern(vp.location(), vp.var);
			} else if (o instanceof ConstructorMatch) {
				ConstructorMatch cm = (ConstructorMatch) o;
				Object type = cx.resolve(cm.location, cm.ctor);
				if (!(type instanceof PackageVar))
					errors.message(cm.location, "could not handle " + type);
				RWConstructorMatch ret = new RWConstructorMatch(cm.location, (PackageVar)type);
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

	public RWMethodMessage rewrite(NamingContext cx, MethodMessage mm) {
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
		return new RWMethodMessage(newSlot, rewriteExpr(cx, mm.expr));
	}

	private Object rewriteExpr(NamingContext cx, Object expr) {
		if (expr == null)
			return null;
		try {
			if (expr instanceof NumericLiteral || expr instanceof StringLiteral)
				return expr;
			else if (expr instanceof PackageVar || expr instanceof LocalVar || expr instanceof ScopedVar || expr instanceof CardMember)
				return expr;
			else if (expr instanceof PackageVar) {
				System.out.println("expr = " + expr);
				return null;
			} else if (expr instanceof UnresolvedOperator || expr instanceof UnresolvedVar) {
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
				if (ret instanceof PackageVar || ret instanceof ScopedVar || ret instanceof LocalVar || ret instanceof IterVar || ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof RWHandlerLambda || ret instanceof RWTemplateListVar || ret instanceof SpecialFormat)
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
									return new PackageVar(uv0.location, ((PackageDefn)o).innerScope().getEntry(fname));
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

	public Type rewrite(NamingContext cx, TypeReference type, boolean allowPolys) {
		try {
			Type ret;
			ret = null;
			try {
				Object r = cx.resolve(type.location(), type.name());
				if (r == null) {
					errors.message(type.location(), "there is no definition in var for " + type.name());
					return null;
				} else if (r instanceof Type)
					ret = (Type)r;
				else if (r instanceof PackageVar)
					ret = (Type) ((PackageVar)r).defn;
				else {
					errors.message(type.location(), type.name() + " is not a type definition");
					return null;
				}
			} catch (ResolutionException ex) {
				if (allowPolys)
					return Type.polyvar(type.location(), type.name());
				throw ex;
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
					for (TypeReference p : type.polys())
						rwp.add(rewrite(cx, p, true));
					if (ret.iam != WhatAmI.INSTANCE)
						ret = ret.instance(type.location(), rwp);
				}
			}
			int k = -1;
			List<Type> fnargs = new ArrayList<Type>();
			if (ret.iam == WhatAmI.FUNCTION)
				k = ret.arity() + 1;
			else if (ret.iam == WhatAmI.TUPLE)
				k = ret.width();
			else if (ret instanceof ContractDecl) {
				return rewrite(cx, ((ContractDecl)ret));
			}
			else
				return ret;
			for (int i=0;i<k;i++) {
				// TODO: big-divide: obviously, ret should have its args already rewritten
				// Is this supposed to be rewriting our args? or what
//				fnargs.add(rewrite(cx, ret.arg(i), allowPolys));
				throw new UtilException("big-divide not-understood case");
			}
			if (ret.iam == WhatAmI.FUNCTION)
				return Type.function(ret.location(), fnargs);
			else
				return Type.tuple(ret.location(), fnargs);
		} catch (ResolutionException ex) {
			errors.message(type.location(), ex.getMessage());
			return null;
		}
	}

	
	public Map<String, LocalVar> allVars(ErrorResult errors, Rewriter rewriter, Rewriter.NamingContext cx, String definedBy, FunctionIntro fi) {
		Map<String, LocalVar> ret = new TreeMap<>();
		gatherVars(errors, rewriter, cx, definedBy, ret, fi);
		return ret;
	}
	
	public void gatherVars(ErrorResult errors, Rewriter rewriter, Rewriter.NamingContext cx, String definedBy, Map<String, LocalVar> into, FunctionIntro fi) {
		for (int i=0;i<fi.args.size();i++) {
			Object arg = fi.args.get(i);
			if (arg instanceof VarPattern) {
				VarPattern vp = (VarPattern)arg;
				into.put(vp.var, new LocalVar(definedBy, vp.varLoc, vp.var, null, null));
			} else if (arg instanceof ConstructorMatch)
				gatherCtor(errors, cx, definedBy, into, (ConstructorMatch) arg);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)arg;
				Type t = null;
				if (cx != null) { // the DependencyAnalyzer can pass in null for the NamingContext 'coz it only wants the var names
					try {
						t = rewriter.rewrite(cx, tp.type, false);
					} catch (ResolutionException ex) {
						throw new UtilException("Need to consider if " + tp.type + " might be a polymorphic var");
					}
				}
				into.put(tp.var, new LocalVar(definedBy, tp.varLocation, tp.var, tp.typeLocation, t));
			} else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(ErrorResult errors, NamingContext cx, String definedBy, Map<String, LocalVar> into, ConstructorMatch cm) {
		// NOTE: I am deliberately NOT returning any errors here because I figure this should already have been checked for validity somewhere else
		// But this (albeit, defensively) assumes that cm.ctor is a struct defn and that it has the defined fields 
		for (Field x : cm.args) {
			if (x.patt instanceof VarPattern) {
				VarPattern vp = (VarPattern)x.patt;
				// TODO: it should theoretically be possible to infer the type of this field by looking at the StructField associated with the StructDefn associated with cm.ctor, and we have a resolving context
				Type t = null;
				if (cx != null) {
					Object sd = cx.resolve(cm.location, cm.ctor);
					if (sd instanceof PackageVar && ((PackageVar)sd).defn instanceof RWStructDefn) {
						RWStructDefn sdf = (RWStructDefn) ((PackageVar)sd).defn;
						RWStructField sf = sdf.findField(x.field);
						if (sf != null) {
							t = sf.type;
						}
					}
				}
				into.put(vp.var, new LocalVar(definedBy, vp.varLoc, vp.var, vp.varLoc, t));
			} else if (x.patt instanceof ConstructorMatch)
				gatherCtor(errors, cx, definedBy, into, (ConstructorMatch)x.patt);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not gathering vars from " + x.patt.getClass());
		}
	}

	public void dump() {
		try {
			PrintWriter pw = new PrintWriter(System.out);
			for (Entry<String, RWStructDefn> x : structs.entrySet())
				System.out.println("Struct " + x.getKey());
			for (Entry<String, CardGrouping> x : cards.entrySet())
				System.out.println("Card " + x.getKey());
			for (Entry<String, RWContractImplements> x : cardImplements.entrySet())
				System.out.println("Impl " + x.getKey());
			for (Entry<String, RWContractService> x : cardServices.entrySet())
				System.out.println("Service " + x.getKey());
			for (Entry<String, RWHandlerImplements> x : callbackHandlers.entrySet())
				System.out.println("Handler " + x.getKey());
			for (Entry<String, RWFunctionDefinition> x : functions.entrySet()) {
				x.getValue().dumpTo(pw);
			}
			pw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
