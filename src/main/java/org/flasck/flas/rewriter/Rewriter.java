package org.flasck.flas.rewriter;

import java.io.File;
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
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.CastExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.SpecialFormat;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.Template;
import org.flasck.flas.commonBase.template.TemplateCardReference;
import org.flasck.flas.commonBase.template.TemplateCases;
import org.flasck.flas.commonBase.template.TemplateExplicitAttr;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateLine;
import org.flasck.flas.commonBase.template.TemplateList;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.commonBase.template.TemplateOr;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.flim.PackageFinder;
import org.flasck.flas.parsedForm.CardDefinition;
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
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.EventHandlerInContext;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
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
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWPropertyDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTemplateDiv;
import org.flasck.flas.rewrittenForm.RWTemplateFormatEvents;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
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
	public final Map<String, Type> builtins = new TreeMap<String, Type>();
	public final Map<String, RWStructDefn> structs = new TreeMap<String, RWStructDefn>();
	public final Map<String, RWObjectDefn> objects = new TreeMap<String, RWObjectDefn>();
	public final Map<String, RWUnionTypeDefn> types = new TreeMap<String, RWUnionTypeDefn>();
	public final Map<String, RWContractDecl> contracts = new TreeMap<String, RWContractDecl>();
	public final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<Template> templates = new ArrayList<Template>();
	public final List<RWD3Invoke> d3s = new ArrayList<RWD3Invoke>();
	public final Map<String, RWContractImplements> cardImplements = new TreeMap<String, RWContractImplements>();
	public final Map<String, RWContractService> cardServices = new TreeMap<String, RWContractService>();
	public final Map<String, RWHandlerImplements> callbackHandlers = new TreeMap<String, RWHandlerImplements>();
	public final Map<String, MethodInContext> methods = new TreeMap<String, MethodInContext>();
	public final Map<String, EventHandlerInContext> eventHandlers = new TreeMap<String, EventHandlerInContext>();
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
		public RootContext() {
			super(null);
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			// TODO: I think these should possibly just keep on having their "simple" names and let JSOUT handle the rename
			if (name.equals("."))
				return new PackageVar(location, "FLEval.field", null);
			if (name.equals("()"))
				return new PackageVar(location, "FLEval.tuple", null);
			if (name.equals("let"))
				return new PackageVar(location, "let", null);
			Object val = getMe(location, name);
			if (val != null)
				return val;
			if (name.contains(".")) {
				int idx = name.lastIndexOf(".");
				String pkgName = name.substring(0, idx);
				pkgFinder.loadFlim(errors, pkgName);
				val = getMe(location, name);
				if (val != null)
					return val;
			}
			throw new ResolutionException(location, name);
		}
	}

	/** The Package Context represents one package which must exist exactly in the builtin scope
	 */
	public class PackageContext extends NamingContext {
		private final String pkgName;
		private final Scope scope;

		public PackageContext(NamingContext cx, String pkgName, Scope scope) {
			super(cx);
			this.pkgName = pkgName;
			this.scope = scope;
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (scope.contains(name)) {
				return getMe(location, pkgName + "." + name);
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
						members.put(sf.name, resolveType(cx, sf.type));
					} catch (ResolutionException ex) {
						errors.message(ex.location, ex.getMessage());
					}
				}
			}
			for (ContractImplements ci : cd.contracts) {
				if (ci.referAsVar != null)
					members.put(ci.referAsVar, (Type)getObject(cx.resolve(ci.location(), ci.name())));
			}
			for (ContractService cs : cd.services) {
				if (cs.referAsVar != null)
					members.put(cs.referAsVar, (Type)getObject(cx.resolve(cs.location(), cs.name())));
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
				if (((HandlerLambda)o).var.equals(name))
					return o;
			Object ret = nested.resolve(location, name);
			if (ret instanceof VarNestedFromOuterFunctionScope) {
				InputPosition loc = ((VarNestedFromOuterFunctionScope) ret).location();
				Type type = new TypeOfSomethingElse(loc, ((VarNestedFromOuterFunctionScope)ret).id);
				HandlerLambda hl = new HandlerLambda(loc, hi.hiName, type, name);
				hi.addScoped(hl, (VarNestedFromOuterFunctionScope) ret);
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
		private final String name;

		FunctionCaseContext(NamingContext cx, String myname, int cs, Map<String, LocalVar> locals, Scope inner, boolean fromMethod) {
			super(cx);
			this.name = myname + "_" + cs;
			this.bound = locals;
			this.inner = inner;
			this.fromMethod = fromMethod;
		}

		public String name() {
			return name;
		}
		
		public Object resolve(InputPosition location, String name) {
			if (bound.containsKey(name))
				return bound.get(name); // a local var
			if (inner.contains(name)) {
				ScopeEntry tmp = inner.getEntry(name);
				Object mf = tmp.getValue();
				Object defn;
				if (mf instanceof FunctionDefinition)
					defn = functions.get(((FunctionDefinition) mf).name);
				else if (mf instanceof MethodDefinition)
					defn = standalone.get(((MethodDefinition) mf).intro.name);
				else if (mf instanceof HandlerImplements)
					defn = Rewriter.this.callbackHandlers.get(((HandlerImplements)mf).hiName);
				else
					throw new UtilException("Cannot handle " + mf.getClass());
				return new VarNestedFromOuterFunctionScope(tmp.location(), tmp.getKey(), defn, true);
			}
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
				return new VarNestedFromOuterFunctionScope(lv.location(), lv.uniqueName(), lv, false);
			} else if (ret instanceof VarNestedFromOuterFunctionScope) {
				return ((VarNestedFromOuterFunctionScope)ret).notLocal();
			} else
				return ret;
		}

	}

	public Rewriter(ErrorResult errors, List<File> pkgdirs, ImportPackage rootPkg) {
		this.errors = errors;
		this.pkgFinder = new PackageFinder(this, pkgdirs, rootPkg);
		importPackage1(rootPkg);
		importPackage2(rootPkg);
	}

	public void importPackage1(ImportPackage pkg) {
		for (Entry<String, Object> x : pkg) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof RWStructDefn) {
//					System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				structs.put(name, (RWStructDefn) val);
			} else if (val instanceof RWObjectDefn) {
//					System.out.println("Adding type for " + x.getValue().getKey() + " => " + val);
				objects.put(name, (RWObjectDefn) val);
			} else if (val instanceof RWUnionTypeDefn) {
				types.put(name, (RWUnionTypeDefn) val);
			} else if (val instanceof RWContractDecl) {
				contracts.put(name, (RWContractDecl) val);
			} else if (val instanceof CardGrouping) {
				cards.put(name, (CardGrouping)val);
			} else if (val instanceof CardDefinition || val instanceof ContractDecl) {
//					System.out.println("Not adding anything for " + x.getValue().getKey() + " " + val);
			} else if (val == null) {
//					System.out.println("Cannot add type for " + x.getValue().getKey() + " as it is null");
			} else if (val instanceof Type) {
				Type ty = (Type) val;
				if (ty.iam == WhatAmI.BUILTIN)
					builtins.put(name, ty);
				else
					throw new UtilException("Cannot handle type of kind " + ty.iam);
			}
		}
	}
	
	public void importPackage2(ImportPackage pkg) {
		for (Entry<String, Object> x : pkg) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof RWFunctionDefinition) {
				functions.put(name, (RWFunctionDefinition)val);
			}
		}
	}

	public void rewritePackageScope(String inPkg, final Scope scope) {
		PackageContext cx = new PackageContext(new RootContext(), inPkg, scope);
		pass1(cx, scope);
		if (errors.hasErrors())
			return;
		pass2(cx, scope);
		if (errors.hasErrors())
			return;
		pass3(cx, scope);
	}
	
	// Introduce new Definitions which we might reference with minimal amount of info
	public void pass1(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition) {
				CardDefinition cd = (CardDefinition) val;
				RWStructDefn sd = new RWStructDefn(cd.location, cd.name, false);
				CardGrouping grp = new CardGrouping(sd);
				cards.put(cd.name, grp);
				pass1(null, cd.fnScope);
			} else if (val instanceof FunctionDefinition) {
				FunctionDefinition f = (FunctionDefinition) val;
				RWFunctionDefinition ret = new RWFunctionDefinition(f.location, f.mytype, f.name, f.nargs, true);
				functions.put(name, ret);
				for (FunctionCaseDefn c : f.cases)
					pass1(cx, c.innerScope());
			} else if (val instanceof MethodDefinition) {
				MethodDefinition m = (MethodDefinition) val;
				RWMethodDefinition rw = new RWMethodDefinition(m.location(), m.intro.name, m.intro.args.size());
				List<Object> enc = new ArrayList<>();
				gatherEnclosing(enc, cx, from);
				MethodInContext mic = new MethodInContext(this, cx, MethodInContext.STANDALONE, rw.location(), null, rw.name(), cx.hasCard()?CodeType.CARD:CodeType.STANDALONE, rw, enc);
				
				// I am not convinced that this should be per-method, and not per-case, but that's the way it seems to be
				// we should test this by having complicated scoping things and seeing if it works
				standalone.put(mic.name, mic);
				for (MethodCaseDefn c : m.cases) {
					pass1(cx, c.innerScope());
				}
			} else if (val instanceof EventHandlerDefinition) {
				EventHandlerDefinition ehd = (EventHandlerDefinition) val;
				RWEventHandlerDefinition rw = new RWEventHandlerDefinition(ehd.location(), ehd.intro.name, ehd.intro.args.size());
				EventHandlerInContext ehic = new EventHandlerInContext(name, rw);
				eventHandlers.put(ehic.name, ehic);
				for (EventCaseDefn c : ehd.cases)
					pass1(cx, c.innerScope());
			} else if (val instanceof StructDefn) {
				StructDefn sd = (StructDefn) val;
				structs.put(name, new RWStructDefn(sd.location(), sd.name(), sd.generate, rewritePolys(sd.polys())));
			} else if (val instanceof UnionTypeDefn) {
				UnionTypeDefn ud = (UnionTypeDefn) val;
				types.put(name, new RWUnionTypeDefn(ud.location(), ud.generate, ud.name(), rewritePolys(ud.polys())));
			} else if (val instanceof ContractDecl) {
				ContractDecl ctr = (ContractDecl)val;
				RWContractDecl ret = new RWContractDecl(ctr.kw, ctr.location(), ctr.name(), true);
				contracts.put(name, ret);
			} else if (val instanceof ObjectDefn) {
				ObjectDefn od = (ObjectDefn)val;
				RWObjectDefn ret = new RWObjectDefn(od.location(), od.name(), od.generate, rewritePolys(od.polys()));
				objects.put(name, ret);
			} else if (val instanceof HandlerImplements) {
				HandlerImplements hi = (HandlerImplements) val;
				pass1HI(cx, hi);
				for (MethodDefinition m : hi.methods)
					for (MethodCaseDefn c : m.cases)
						pass1(cx, c.innerScope());
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	// Fill in definitions as much as we can from just here
	public void pass2(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition) {
				try {
					CardDefinition cd = (CardDefinition) val;
					pass2(new CardContext((PackageContext) cx, cd), cd.innerScope());
				} catch (ResolutionException ex) {
					errors.message(ex.location, ex.getMessage());
				}
			} else if (val instanceof FunctionDefinition) {
				FunctionDefinition fd = (FunctionDefinition) val;
				int cs = 0;
				for (FunctionCaseDefn c : fd.cases) {
					FunctionCaseContext fccx = new FunctionCaseContext(cx, fd.name, cs, null, c.innerScope(), false);
					pass2(fccx, c.innerScope());
					cs++;
				}
			} else if (val instanceof MethodDefinition) {
			} else if (val instanceof EventHandlerDefinition) {
				// Nothing to do in pass2 ... was set up in pass1 and will be resolved in pass3
			} else if (val instanceof StructDefn) {
				rewrite(cx, (StructDefn)val);
			} else if (val instanceof UnionTypeDefn) {
				rewrite(cx, (UnionTypeDefn)val);
			} else if (val instanceof ContractDecl) {
				rewrite(cx, (ContractDecl)val);
			} else if (val instanceof HandlerImplements) {
				pass2HI(cx, (HandlerImplements) val);
			} else if (val instanceof ObjectDefn) {
				; // we should probably rewrite the fields portion
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	// Resolve things that still need doing & handle nested contexts
	public void pass3(NamingContext cx, Scope from) {
		for (Entry<String, ScopeEntry> x : from) {
			String name = x.getValue().getKey();
			Object val = x.getValue().getValue();
			if (val instanceof CardDefinition)
				rewriteCard(cx, (CardDefinition)val);
			else if (val instanceof FunctionDefinition)
				rewrite(cx, (FunctionDefinition)val);
			else if (val instanceof MethodDefinition) {
				rewriteStandaloneMethod(cx, from, (MethodDefinition)val, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE);
			} else if (val instanceof EventHandlerDefinition)
				rewrite(cx, (EventHandlerDefinition)val);
			else if (val instanceof StructDefn || val instanceof UnionTypeDefn || val instanceof ContractDecl) {
				// these all got sorted out already in the first two passes
			} else if (val instanceof ObjectDefn) {
				// we should probably rewrite the methods now
			} else if (val instanceof HandlerImplements) {
				rewriteHI(cx, (HandlerImplements)val, from);
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	private void rewriteCard(NamingContext cx, CardDefinition cd) {
		if (!(cx instanceof PackageContext))
			throw new UtilException("Cannot have card in nested scope: " + cx.getClass());
		CardContext c2 = new CardContext((PackageContext) cx, cd);
		CardGrouping grp = cards.get(cd.name);
		RWStructDefn sd = grp.struct;
//		RWStructDefn sd = new RWStructDefn(cd.location, cd.name, false);
//		CardGrouping grp = new CardGrouping(sd);
//		cards.put(cd.name, grp);
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
				RWMethodDefinition rwm = new RWMethodDefinition(m.location(), m.intro.name, m.intro.args.size());
				List<Object> enc = new ArrayList<>();
				// I don't think there can be
//				gatherEnclosing(enc, cx, from);
				MethodInContext mic = new MethodInContext(this, cx, MethodInContext.DOWN, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.CONTRACT, rwm, enc);
				rewriteMethodCases(c2, m, true, mic, false);
				methods.put(m.intro.name, mic);
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

			for (MethodDefinition m : cs.methods) {
				RWMethodDefinition rwm = new RWMethodDefinition(m.intro.location, m.intro.name, m.intro.args.size());
				List<Object> enc = new ArrayList<>();
				// I don't think there can be
//				gatherEnclosing(enc, cx, from);
				MethodInContext mic = new MethodInContext(this, cx, MethodInContext.UP, rw.location(), rw.name(), m.intro.name, HSIEForm.CodeType.SERVICE, rwm, enc);
				rewriteMethodCases(c2, m, true, mic, false);
				methods.put(m.intro.name, mic);
			}

			pos++;
		}

		if (cd.template != null)
			templates.add(rewrite(new TemplateContext(c2), cd.template));
		
		for (HandlerImplements hi : cd.handlers) {
			RWHandlerImplements rw = pass1HI(c2, hi);
			if (rw != null) {
				rewriteHI(c2, hi, cd.innerScope());
				grp.handlers.add(new HandlerGrouping(rw.hiName, rw));
			}
		}
		
		grp.platforms.putAll(cd.platforms);
		pass3(c2, cd.fnScope);
	}

	private Template rewrite(TemplateContext cx, Template template) {
		try {
			return new Template(template.prefix, rewrite(cx, template.content));
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
		} else if (tl instanceof TemplateCardReference) {
			TemplateCardReference cr = (TemplateCardReference) tl;
			Object cardName = cr.explicitCard == null ? null : cx.resolve(cr.location, (String)cr.explicitCard);
			Object yoyoName = cr.yoyoVar == null ? null : cx.resolve(cr.location, (String)cr.yoyoVar);
			return new TemplateCardReference(cr.location, cardName, yoyoName);
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
			TemplateListVar rwv = tlv == null ? null : new TemplateListVar(tlv.location, tlv.name);
			boolean supportDragOrdering = false;
			for (SpecialFormat tt : specials) {
				if (tt.name.equals("dragOrder")) {
					supportDragOrdering = true;
				} else
					errors.message(tt.location(), "Cannot handle special format " + tt.name);
			}
			TemplateList rul = new TemplateList(ul.listLoc, rewriteExpr(cx, ul.listVar), ul.iterLoc, rwv, ul.customTag, ul.customTagVar, formats, supportDragOrdering);
			cx = new TemplateContext(cx, rwv);
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

	private TemplateOr rewrite(TemplateContext cx, TemplateOr tor) {
		return new TemplateOr(tor.location(), rewriteExpr(cx, tor.cond), rewrite(cx, tor.template));
	}

	private void rewrite(NamingContext cx, ContractDecl ctr) {
		RWContractDecl ret = contracts.get(ctr.name());
		for (ContractMethodDecl cmd : ctr.methods) {
			ret.addMethod(rewrite(cx, ctr.name(), cmd));
		}
	}

	private RWContractMethodDecl rewrite(NamingContext cx, String name, ContractMethodDecl cmd) {
		List<Object> args = new ArrayList<Object>();
		List<Type> targs = new ArrayList<Type>(); 
		for (Object o : cmd.args) {
			args.add(rewritePattern(cx, name + "." + cmd.name, o));
			if (o instanceof TypedPattern) {
				targs.add(rewrite(cx, ((TypedPattern)o).type, false));
			} else if (o instanceof ConstructorMatch) { // we can get this instead of a typed patter
				ConstructorMatch cm = (ConstructorMatch)o;
				targs.add(rewrite(cx, new TypeReference(cm.location, cm.ctor), false));
			} else
				throw new UtilException("Unexpected pattern " + o.getClass());
		}
		targs.add(typeFrom(cx.resolve(cmd.location(), "Send")));
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

	private RWHandlerImplements pass1HI(NamingContext cx, HandlerImplements hi) {
		Type any = (Type) getObject(cx.nested.resolve(hi.location(), "Any"));
		Object av = cx.resolve(hi.location(), hi.name());
		if (av == null || !(av instanceof PackageVar)) {
			errors.message((Block)null, "cannot find a valid definition of contract " + hi.name());
			return null;
		}
		PackageVar ctr = (PackageVar) av;
		final String rwname = hi.hiName;
		List<HandlerLambda> bvs = new ArrayList<HandlerLambda>();
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
		RWHandlerImplements rw = new RWHandlerImplements(hi.kw, hi.location(), hi.hiName, ctr.id, hi.inCard, bvs);
		callbackHandlers.put(hi.hiName, rw);
		return rw;
	}

	private void pass2HI(NamingContext cx, HandlerImplements hi) {
		RWHandlerImplements ret = callbackHandlers.get(hi.hiName);
		if (ret == null)
			return; // presumably it failed in pass1
	}

	private void rewriteHI(NamingContext cx, HandlerImplements hi, Scope scope) {
		try {
			RWHandlerImplements ret = callbackHandlers.get(hi.hiName);
			if (ret == null)
				return; // presumably it failed in pass1
			HandlerContext hc = new HandlerContext(cx, ret);
			for (MethodDefinition m : hi.methods) {
				RWMethodDefinition rm = new RWMethodDefinition(m.intro.location, m.intro.name, m.intro.args.size());
				List<Object> enc = new ArrayList<>();
				gatherEnclosing(enc, cx, scope);
				MethodInContext mic = new MethodInContext(this, cx, MethodInContext.DOWN, ret.location(), ret.name(), m.intro.name, HSIEForm.CodeType.HANDLER, rm, enc);
				rewriteMethodCases(hc, m, true, mic, false);
				ret.methods.add(rm);
				methods.put(m.intro.name, mic);
			}

			// Create a struct to store the state.  It feels weird creating a struct in pass3, but we don't creating the bound vars for scoped/lambdas
			// until just above, so we have to wait ...
			
			// I don't want to have two arrays with the same named entry, so add a random thing to the end of the struct
			String sdname = hi.hiName+"$struct";
			RWStructDefn hsd = new RWStructDefn(hi.location(), sdname, false);
			for (Object s : ret.boundVars) {
				HandlerLambda hl = (HandlerLambda) s;
				hsd.fields.add(new RWStructField(hl.location, false, hl.type, hl.var));
			}
			structs.put(sdname, hsd);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return;
		}
	}

	private Object getObject(Object o) {
		if (o instanceof PackageVar) {
			return ((PackageVar)o).defn; 
		} else
			return o;
	}

	public void rewrite(NamingContext cx, FunctionDefinition f) {
		RWFunctionDefinition ret = functions.get(f.name);
		for (FunctionCaseDefn c : f.cases) {
			final Map<String, LocalVar> vars = new HashMap<>();
			gatherVars(errors, this, cx, c.caseName(), vars, c.intro);
			FunctionCaseContext fccx = new FunctionCaseContext(cx, f.name, ret.cases.size(), vars, c.innerScope(), false);
			RWFunctionCaseDefn rwc = rewrite(fccx, c, ret.cases.size(), vars);
			ret.cases.add(rwc);
		}
	}

	private void rewriteStandaloneMethod(NamingContext cx, Scope from, MethodDefinition m, HSIEForm.CodeType codeType) {
		rewriteMethodCases(cx, m, false, standalone.get(m.intro.name), true);
	}
	
	private void rewriteMethodCases(NamingContext cx, MethodDefinition m, boolean fromHandler, MethodInContext mic, boolean useCases) {
		int cs = 0;
		for (MethodCaseDefn c : m.cases) {
			Map<String, LocalVar> vars = new HashMap<>();
			String name = useCases ? m.intro.name + "_" + cs : m.intro.name;
			gatherVars(errors, this, cx, name, vars, m.intro);
			mic.method.cases.add(rewrite(new FunctionCaseContext(cx, m.intro.name, cs, vars, c.innerScope(), fromHandler), c, vars));
			cs++;
		}
	}

	private void rewrite(NamingContext cx, EventHandlerDefinition ehd) {
		EventHandlerInContext ehic = eventHandlers.get(ehd.intro.name);
		RWEventHandlerDefinition rw = ehic.handler;
		int cs = 0;
		for (EventCaseDefn c : ehd.cases) {
			Map<String, LocalVar> vars = new HashMap<>();
			gatherVars(errors, this, cx, rw.name(), vars, ehd.intro);
			rw.cases.add(rewrite(new FunctionCaseContext(cx, ehd.intro.name +"_" + cs, cs, vars, c.innerScope(), false), c, vars));
			cs++;
		}
	}

	private void rewrite(NamingContext cx, StructDefn sd) {
		RWStructDefn ret = structs.get(sd.name());
		for (StructField sf : sd.fields) {
			// TODO: it's not clear that the expression needs this rewritten context
			StructDefnContext sx = new StructDefnContext(cx, ret.polys());
			RWStructField rsf = new RWStructField(sf.loc, false, rewrite(sx, sf.type, false), sf.name, rewriteExpr(sx, sf.init));
			ret.addField(rsf);
		}
	}

	private void rewrite(NamingContext cx, UnionTypeDefn u) {
		RWUnionTypeDefn ret = types.get(u.name());
		for (TypeReference c : u.cases) {
			ret.addCase(rewrite(cx, c, true));
		}
	}

	protected List<Type> rewritePolys(List<TypeReference> polys) {
		List<Type> pts = new ArrayList<Type>(); // poly vars
		if (polys != null)
			for (TypeReference r : polys)
				pts.add(Type.polyvar(r.location(), r.name()));
		return pts;
	}

	private RWFunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c, int csNo, Map<String, LocalVar> vars) {
		RWFunctionIntro intro = rewrite(cx, c.intro, cx.name(), vars);
		Object expr = rewriteExpr(cx, c.expr);
		if (expr == null)
			return null;
		// TODO: big-divide: I feel this should be in pass1
		RWFunctionCaseDefn ret = new RWFunctionCaseDefn(intro, csNo, expr);
		pass3(new NestedScopeContext(cx), c.innerScope());
		return ret;
	}

	private RWMethodCaseDefn rewrite(FunctionCaseContext cx, MethodCaseDefn c, Map<String, LocalVar> vars) {
		RWMethodCaseDefn ret = new RWMethodCaseDefn(rewrite(cx, c.intro, c.caseName(), vars));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private RWEventCaseDefn rewrite(FunctionCaseContext cx, EventCaseDefn c, Map<String, LocalVar> vars) {
		RWEventCaseDefn ret = new RWEventCaseDefn(c.kw, rewrite(cx, c.intro, c.intro.name, vars));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private RWFunctionIntro rewrite(NamingContext cx, FunctionIntro intro, String csName, Map<String, LocalVar> vars) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(cx, csName, o));
		}
		return new RWFunctionIntro(intro.location, intro.name, args, vars);
	}

	public Object rewritePattern(NamingContext cx, String name, Object o) {
		try {
			if (o instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o;
				return new RWTypedPattern(tp.typeLocation, rewrite(cx, tp.type, false), tp.varLocation, name + "." + tp.var);
			} else if (o instanceof VarPattern) {
				VarPattern vp = (VarPattern) o;
				return new RWVarPattern(vp.location(), name + "." + vp.var);
			} else if (o instanceof ConstructorMatch) {
				ConstructorMatch cm = (ConstructorMatch) o;
				Object type = cx.resolve(cm.location, cm.ctor);
				if (!(type instanceof PackageVar))
					errors.message(cm.location, "could not handle " + type);
				RWConstructorMatch ret = new RWConstructorMatch(cm.location, (PackageVar)type);
				for (Field x : cm.args)
					ret.args.add(ret.new Field(x.field, rewritePattern(cx, name, x.patt)));
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
				errors.message(ex.location, ex.getMessage());
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
			else if (expr instanceof PackageVar || expr instanceof LocalVar || expr instanceof VarNestedFromOuterFunctionScope || expr instanceof CardMember)
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
				if (ret == null)
					ret = cx.resolve(location, s); // debug
				if (ret instanceof PackageVar || ret instanceof VarNestedFromOuterFunctionScope || ret instanceof LocalVar || ret instanceof IterVar || ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof HandlerLambda || ret instanceof TemplateListVar || ret instanceof SpecialFormat)
					return ret;
				else
					throw new UtilException("cannot handle id " + s + ": " + (ret == null ? "null": ret.getClass()));
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
						if (pv.defn == null) {
							return cx.resolve(ae.location, pv.id +"." + fname);
						}
					}
					if (aefn instanceof UnresolvedVar) {
						UnresolvedVar uv0 = (UnresolvedVar)aefn;
						try {
							Object pkgEntry = cx.resolve(uv0.location, uv0.var);
							if (pkgEntry instanceof PackageVar) {
//								PackageVar pv = (PackageVar)pkgEntry;
//								Object o = pv.defn;
								// TODO: big-divide: I may be making it so that this is no longer possible; not sure
//								if (o instanceof PackageDefn)
//									return getMe(uv0.location, pv.id + "." + fname);
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
			Type ret = null;
			try {
				Object r = resolveType(cx, type);
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
//			List<Type> fnargs = new ArrayList<Type>();
//			// There seems something very wrong here to me ... if we ever get here :-)  Shouldn't fnargs be filled with something?
//			if (ret.iam == WhatAmI.TUPLE)
//				return Type.tuple(ret.location(), fnargs);
//			if (ret.iam == WhatAmI.FUNCTION)
//				return Type.function(ret.location(), fnargs);
//			else
			return ret;
		} catch (ResolutionException ex) {
			errors.message(type.location(), ex.getMessage());
			return null;
		}
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

	protected Object doIhave(InputPosition location, String id) {
		if (builtins.containsKey(id))
			return builtins.get(id);
		else if (types.containsKey(id))
			return types.get(id);
		else if (structs.containsKey(id))
			return structs.get(id);
		else if (contracts.containsKey(id))
			return contracts.get(id);
		else if (objects.containsKey(id))
			return objects.get(id);
		else if (callbackHandlers.containsKey(id))
			return callbackHandlers.get(id);
		else if (functions.containsKey(id))
			return functions.get(id);
		else if (cards.containsKey(id))
			return cards.get(id);
		else
			return null;
	}
	
	public Object getMe(InputPosition location, String id) {
		Object val = doIhave(location, id);
		if (val == null) {
			return null;
		}
		return new PackageVar(location, id, val);
	}

	private Type resolveType(NamingContext cx, TypeReference type) {
		if (type instanceof FunctionTypeReference) {
			FunctionTypeReference ftr = (FunctionTypeReference) type;
			List<Type> list = new ArrayList<>();
			for (TypeReference tr : ftr.args)
				list.add(resolveType(cx, tr));
			return Type.function(type.location(), list);
		}
		return (Type)getObject(cx.resolve(type.location(), type.name()));
	}
	
	private void gatherEnclosing(List<Object> enclosingPatterns, NamingContext cx, Scope s) {
		if (s == null)
			return;
		if (s.container != null) {
			gatherEnclosing(enclosingPatterns, cx, s.outer);
			Object ctr = s.container;
			if (ctr instanceof FunctionCaseDefn) {
				FunctionCaseDefn fn = (FunctionCaseDefn)ctr;
				for (Object o : fn.intro.args) {
					enclosingPatterns.add(rewritePattern(cx, fn.caseName(), o));
				}
			}
		}
	}
}