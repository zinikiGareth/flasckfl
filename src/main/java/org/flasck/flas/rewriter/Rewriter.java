package org.flasck.flas.rewriter;

import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.BooleanLiteral;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.SpecialFormat;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.D3Thing;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.HandlerGrouping;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWCastExpr;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractMethodDecl;
import org.flasck.flas.rewrittenForm.RWContractService;
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
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWTemplate;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.TypeCheckStringable;
import org.flasck.flas.testrunner.UnitTests;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeOfSomethingElse;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.types.TypeWithNameAndPolys;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Indenter;

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
public class Rewriter implements CodeGenRegistry {
	static final Logger logger = LoggerFactory.getLogger("Rewriter");
	private ErrorResult errors;
	public final Map<String, PrimitiveType> primitives = new TreeMap<>();
	private final Map<String, Expr> constants = new TreeMap<>();
	private final Map<String, RWStructDefn> structs = new TreeMap<String, RWStructDefn>();
	public final Map<String, RWObjectDefn> objects = new TreeMap<String, RWObjectDefn>();
	public final Map<String, RWUnionTypeDefn> types = new TreeMap<String, RWUnionTypeDefn>();
	public final Map<String, RWContractDecl> contracts = new TreeMap<String, RWContractDecl>();
	// I'm not 100% sure we need both of these, but it seems we need more info for "generating" cards than we do for "referencing" cards on import ...
	private final Map<String, CardGrouping> cards = new TreeMap<String, CardGrouping>();
	public final List<RWTemplate> templates = new ArrayList<RWTemplate>();
	public final List<RWD3Thing> d3s = new ArrayList<RWD3Thing>();
	public final Map<CSName, RWContractImplements> cardImplements = new TreeMap<CSName, RWContractImplements>();
	public final Map<CSName, RWContractService> cardServices = new TreeMap<CSName, RWContractService>();
	public final Map<String, RWHandlerImplements> callbackHandlers = new TreeMap<String, RWHandlerImplements>();
	public final Map<String, RWMethodDefinition> methods = new TreeMap<String, RWMethodDefinition>();
	public final Map<String, RWEventHandlerDefinition> eventHandlers = new TreeMap<String, RWEventHandlerDefinition>();
	public final Map<String, RWMethodDefinition> standalone = new TreeMap<String, RWMethodDefinition>();
	public final Map<String, RWFunctionDefinition> functions = new TreeMap<String, RWFunctionDefinition>();
	public final Map<String, Type> fnArgs = new TreeMap<String, Type>();
	private final List<RepoVisitor> generators = new ArrayList<>();

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

		public CardName cardNameIfAny() {
			if (nested != null)
				return nested.cardNameIfAny();
			return null;
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
			throw new ResolutionException(location, name);
		}
	}

	/** The Package Context represents one package which must exist exactly in the builtin scope
	 */
	public class PackageContext extends NamingContext {
		private final PackageName pkgName;
		private final IScope scope;

		public PackageContext(NamingContext cx, PackageName pkgName, IScope scope) {
			super(cx);
			this.pkgName = pkgName;
			this.scope = scope;
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (scope.contains(name)) {
				return getMe(location, new SolidName(pkgName, name));
			}
			return nested.resolve(location, name);
		}

		public String toString() {
			throw new UtilException("Yo!");
		}
	}

	public class StructDefnContext extends NamingContext {
		private final List<PolyVar> polys;

		public StructDefnContext(NamingContext cx, List<PolyVar> polys) {
			super(cx);
			this.polys = polys;
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			for (PolyVar t : polys) {
				if (t.nameAsString().equals(name))
					return t;
			}
			return nested.resolve(location, name);
		}

	}

	/** The Card Context can only be found directly in a Package Context 
	 */
	public class CardContext extends NamingContext {
		private final CardName cardName;
		private final Map<String, Type> members = new TreeMap<String, Type>();
		private final Map<String, ObjectReference> statics = new TreeMap<String, ObjectReference>();
		private final List<RWEventHandler> areaActions;
		private final IScope innerScope;
		private int fnIdx = 0;

		public CardContext(PackageContext cx, CardName name, List<RWEventHandler> areaActions, CardDefinition cd, boolean doAll) {
			super(cx);
			this.cardName = name;
			this.areaActions = areaActions;
			this.innerScope = null; //cd.innerScope();
			if (!doAll)
				return;
			if (cd.state != null) {
				for (StructField sf : cd.state.fields) {
					try {
						members.put(sf.name, rewrite(cx, sf.type, true));
					} catch (ResolutionException ex) {
						errors.message(ex.location, ex.getMessage());
					}
				}
			}
			for (ContractImplements ci : cd.contracts) {
				if (ci.referAsVar != null) {
					RWContractImplements t = null;
					for (RWContractImplements x : cardImplements.values())
						if (ci.referAsVar.equals(x.referAsVar))
							t = x;
					if (t == null)
						throw new UtilException("No ci for " + ci.referAsVar);
					members.put(ci.referAsVar, t);
				}
			}
			for (ContractService cs : cd.services) {
				if (cs.referAsVar != null) {
					RWContractService t = null;
					for (RWContractService x : cardServices.values())
						if (cs.referAsVar.equals(x.referAsVar))
							t = x;
					if (t == null)
						throw new UtilException("No cs for " + cs.referAsVar);
					members.put(cs.referAsVar, t);
				}
			}
			for (HandlerImplements hi : cd.handlers) {
				statics.put(hi.baseName, new ObjectReference(hi.location(), hi.handlerName));
			}
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (members.containsKey(name))
				return new CardMember(location, cardName, name, members.get(name));
			if (statics.containsKey(name))
				return statics.get(name);
			if (innerScope.contains(name))
				return new CardFunction(location, cardName, name);
			return nested.resolve(location, name);
		}

		@Override
		public boolean hasCard() {
			return true;
		}

		@Override
		public CardName cardNameIfAny() {
			return cardName;
		}

		public FunctionName nextFunction(InputPosition loc, String type, HSIEForm.CodeType kind, AreaName areaName) {
			if (kind == CodeType.AREA)
				return FunctionName.areaMethod(loc, areaName, nextName(type));
			else if (kind == CodeType.CARD) 
				return FunctionName.functionInCardContext(loc, cardName, nextName(type));
			else if (kind == CodeType.EVENT) 
				return FunctionName.eventTrampoline(loc, cardName, nextName(type));
			else
				throw new NotImplementedException("nextFunction of type " + kind);
		}

		public String nextName(String type) {
			return type +"_"+(fnIdx++);
		}
	}
	
	class ObjectContext extends NamingContext {
		private final Map<String, Type> polys = new TreeMap<String, Type>();
		private final Map<String, Type> members = new TreeMap<String, Type>();
		private final SolidName objName;

		public ObjectContext(NamingContext cx, ObjectDefn od, List<PolyVar> polys) {
			super(cx);
			objName = od.name();
			for (PolyVar v : polys)
				this.polys.put(v.nameAsString(), v);
//			for (StructField sf : od.fields) {
//				try {
//					members.put(sf.name, rewrite(this, sf.type, true));
//				} catch (ResolutionException ex) {
//					errors.message(ex.location, ex.getMessage());
//				}
//			}
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			if (members.containsKey(name))
				return new CardMember(location, objName, name, members.get(name));
			else if (polys.containsKey(name))
				return polys.get(name);
			return nested.resolve(location, name);
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
			if (ret instanceof ScopedVar) {
				InputPosition loc = ((ScopedVar) ret).location();
				TypeOfSomethingElse type = new TypeOfSomethingElse(loc, ((ScopedVar)ret).id);
				HandlerLambda hl = new HandlerLambda(loc, hi.handlerName, type, name);
				hi.addScoped(hl, (ScopedVar) ret);
				return hl;
			}
			return ret;
		}
	}

	public class TemplateContext extends NamingContext {
		private final String tlvSimpleName;
		private final TemplateListVar listVar;
		private int nextAreaNo;

		public TemplateContext(CardContext cx) {
			super(cx);
			this.listVar = null;
			this.tlvSimpleName = null;
			this.nextAreaNo = 1;
		}
		
		public TemplateContext(TemplateContext cx, AreaName areaName) {
			super(cx);
			this.listVar = null;
			this.tlvSimpleName = null;
			this.nextAreaNo = -1;
		}

		public TemplateContext(TemplateContext cx, AreaName areaName, String tlvSimpleName, TemplateListVar tlv) {
			super(cx);
			if (tlv != null && tlv.simpleName == null)
				throw new UtilException("Shouldn't happen");
			this.tlvSimpleName = tlvSimpleName;
			this.listVar = tlv;
			this.nextAreaNo = -1;
		}

		public CardContext cardContext() {
			if (nested instanceof CardContext) {
				return (CardContext)nested;
			} else if (nested instanceof TemplateContext)
				return ((TemplateContext)nested).cardContext();
			else
				throw new UtilException("Cannot handle " + nested.getClass());
		}

		public CardName cardName() {
			return cardContext().cardName;
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			if (listVar != null && tlvSimpleName.equals(name))
				return listVar;
			return nested.resolve(location, name);
		}

		public FunctionName nextFunction(InputPosition loc, AreaName aname, String type, CodeType from) {
			if (nested instanceof CardContext) {
				return ((CardContext)nested).nextFunction(loc, type, from, aname);
			} else if (nested instanceof TemplateContext)
				return ((TemplateContext)nested).nextFunction(loc, aname, type, from);
			else
				throw new UtilException("Cannot handle " + nested.getClass());
		}

		public AreaName nextArea() {
			if (nextAreaNo == -1)
				return ((TemplateContext)nested).nextArea();
//			String cn = cardNameAsString();
//			int idx = cn.lastIndexOf(".");
//			if (cn.charAt(idx+1) != '_')
//				cn = cn.substring(0, idx+1) + "_" + cn.substring(idx+1); 
			return new AreaName(cardName(), "B"+(nextAreaNo++));
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

		public D3Context(TemplateContext cx, InputPosition location, VarName iv) {
			super(cx);
			this.iterVar = new IterVar(location, ((TemplateContext)cx.nested).cardName(), iv.var);
		}
		
		@Override
		public Object resolve(InputPosition location, String name) {
			if (iterVar != null && iterVar.var.equals(name))
				return iterVar;
			return nested.resolve(location, name);
		}

		public AreaName nextArea() {
			return ((TemplateContext)nested).nextArea();
		}

	}

	// This is for functions to use in Pass1 to identify scoping context
	public class Pass1ScopeContext extends NamingContext {
		public Pass1ScopeContext(NamingContext cx) {
			super(cx);
		}

		@Override
		public Object resolve(InputPosition location, String name) {
			// Because this is a partial context, we don't know anything yet
			return nested.resolve(location, name);
		}
	}


	// I think I still need ImplementsContext, MethodContext and EventHandlerContext
	// BUT I think the latter two can just be FunctionContext & ImplementsContext is dull
	
	/** A function context can appear in lots of places, including inside other functions
	 */
	class FunctionCaseContext extends NamingContext {
		protected final Map<String, LocalVar> bound;
		private final IScope inner;
		private final boolean fromMethod;
		private final FunctionName funcName;
		private final FunctionName caseName;

		FunctionCaseContext(NamingContext cx, FunctionName funcName, FunctionName caseName, Map<String, LocalVar> locals, IScope inner, boolean fromMethod) {
			super(cx);
			this.funcName = funcName;
			this.caseName = caseName;
			this.bound = locals;
			this.inner = inner;
			this.fromMethod = fromMethod;
		}

		public FunctionName name() {
			return caseName;
		}
		
		public Object resolve(InputPosition location, String name) {
			if (bound != null && bound.containsKey(name))
				return bound.get(name); // a local var
			if (inner.contains(name)) {
				VarName vn = new VarName(location, inner.name(), name);
				String full = vn.uniqueName(); // inner.fullName(name);
				Locatable defn = functions.get(full);
				if (defn == null)
					defn = standalone.get(full);
				if (defn == null)
					defn = callbackHandlers.get(full);
				if (defn == null)
					throw new UtilException("Scope has definition of " + name + " as " + full + " but it is not a function, method or handler");
				return new ScopedVar(defn.location(), vn, defn, funcName);
			}
			Object res = nested.resolve(location, name);
			if (res instanceof ObjectReference)
				return new ObjectReference(location, (ObjectReference)res, fromMethod);
			if (res instanceof CardFunction)
				return new CardFunction(location, (CardFunction)res, fromMethod);
			if (res instanceof LocalVar) {
				LocalVar lv = (LocalVar) res;
				return new ScopedVar(lv.varLoc, lv.var, lv, lv.fnName);
			}
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
				return new ScopedVar(lv.location(), lv.var, lv, lv.fnName);
			} else
				return ret;
		}

	}

	public Rewriter(ErrorResult errors, List<File> pkgdirs) {
	}

	public void rewritePackageScope(String priorPackage, IScope priorScope, String inPkg, final Scope scope) {
		NamingContext rc = new RootContext();
		if (priorPackage != null) {
			rc = new PackageContext(rc, new PackageName(priorPackage), priorScope);
		//	rc = doRewriting(rc, prior.getPackage().uniqueName(), prior.getScope());
		}
		doRewriting(rc, inPkg, scope);
	}

	private PackageContext doRewriting(NamingContext rc, String inPkg, final Scope scope) {
		PackageContext cx = new PackageContext(rc, new PackageName(inPkg), scope);
		pass1(cx, scope);
		if (errors.hasErrors())
			return null;
		pass2(cx, scope);
		if (errors.hasErrors())
			return null;
		pass3(cx, scope);
		return cx;
	}
	
	// Introduce new Definitions which we might reference with minimal amount of info
	public void pass1(NamingContext cx, IScope from) {
		String prev = null;
		for (ScopeEntry x : from) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof CardDefinition) {
				CardDefinition cd = (CardDefinition) val;
				CardGrouping cg = createCard((PackageContext)cx, cd);
				final CardContext ic = new CardContext((PackageContext) cx, cg.getName(), cg.areaActions, cd, false);
				pass1(ic, null /*cd.fnScope*/);
			} else if (val instanceof FunctionCaseDefn) {
				FunctionCaseDefn c = (FunctionCaseDefn) val;
				String fn = c.functionName().uniqueName();
				if (functions.containsKey(fn)) {
					RWFunctionDefinition ret = functions.get(fn);
					if (prev != null && !prev.equals(name))
						errors.message(c.location(), "split function definition: " + fn);
					if (ret.mytype != c.mytype())
						errors.message(c.location(), "mismatched kinds in function " + fn);
					if (ret.nargs != c.nargs())
						errors.message(c.location(), "inconsistent argument counts in function " + fn);
				} else {
					RWFunctionDefinition ret = new RWFunctionDefinition(c.intro.name(),c.nargs(), true);
					functions.put(name, ret);
				}
				pass1(new Pass1ScopeContext(cx), null /*c.innerScope()*/);
			} else if (val instanceof MethodCaseDefn) {
				MethodCaseDefn m = (MethodCaseDefn) val;
				String mn = m.methodName().uniqueName();
				if (methods.containsKey(mn)) {
					RWMethodDefinition ret = methods.get(mn);
					if (prev != null && !prev.equals(name))
						errors.message(m.location(), "split function definition: " + mn);
					if (ret.nargs() != m.nargs())
						errors.message(m.location(), "inconsistent argument counts in function " + mn);
				} else {
					RWMethodDefinition rw = new RWMethodDefinition(m.location(), null, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE, RWMethodDefinition.STANDALONE, m.location(), m.intro.name(), m.intro.args.size());
					standalone.put(rw.name().uniqueName(), rw);
				}
				pass1(cx, m.innerScope());
			} else if (val instanceof EventCaseDefn) {
				EventCaseDefn ehd = (EventCaseDefn) val;
				String mn = ehd.methodName().uniqueName();
				if (eventHandlers.containsKey(mn)) {
					RWEventHandlerDefinition rw = eventHandlers.get(mn);
					if (prev != null && !prev.equals(name))
						errors.message(ehd.location(), "split function definition: " + mn);
//					if (ret.mytype != m.mytype())
//						errors.message(m.location(), "mismatched kinds in function " + mn);
					if (rw.nargs() != ehd.intro.args.size())
						errors.message(ehd.location(), "inconsistent argument counts in function " + mn);
				} else {
					RWEventHandlerDefinition rw = new RWEventHandlerDefinition(ehd.intro.name(), ehd.intro.args.size());
					eventHandlers.put(rw.name().uniqueName(), rw);
				}
				pass1(cx, ehd.innerScope());
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
//				for (ObjectMethod om : od.ctors) {
//					MethodCaseDefn m = om.getMethod();
//					RWMethodDefinition rw = new RWMethodDefinition(m.location(), null, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE, RWMethodDefinition.STANDALONE, m.location(), m.intro.name(), m.intro.args.size());
//					List<RWTypedPattern> handler = new ArrayList<>();
//					ret.addConstructor(new RWObjectMethod(rw, deriveType(cx, m.location(), m.intro.args, null, null, handler)));
//				}
//				for (ObjectMethod om : od.methods) {
//					MethodCaseDefn m = om.getMethod();
//					RWMethodDefinition rw = new RWMethodDefinition(m.location(), null, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE, RWMethodDefinition.STANDALONE, m.location(), m.intro.name(), m.intro.args.size());
//					List<RWTypedPattern> handler = new ArrayList<>();
//					ret.addMethod(new RWObjectMethod(rw, deriveType(cx, m.location(), m.intro.args, null, null, handler)));
//				}
			} else if (val instanceof StructDefn) {
				StructDefn sd = (StructDefn) val;
				structs.put(name, new RWStructDefn(sd.location(), sd.type, sd.name, sd.generate, rewritePolys(sd.polys())));
			} else if (val instanceof HandlerImplements) {
				// do nothing here
			} else if (val instanceof TupleMember) {
				TupleMember tm = (TupleMember) val;
				if (tm.which == 0) {
					RWFunctionDefinition ret = new RWFunctionDefinition(tm.exprFnName(), 0, true);
					functions.put(tm.exprFnName().uniqueName(), ret);
				}
				RWFunctionDefinition ret = new RWFunctionDefinition(tm.name(), 0, true);
				functions.put(name, ret);
			} else if (val instanceof UnitTests) {
				// Do we want to start again with another package scope?
				pass1(cx, ((UnitTests)val).scope());
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
			prev = name;
		}
	}

	// Fill in definitions as much as we can from just here
	public void pass2(NamingContext cx, IScope from) {
		for (ScopeEntry x : from) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof CardDefinition) {
				try {
					CardDefinition cd = (CardDefinition) val;
					CardGrouping cg = pass2Card(cx, cd);
					if (!errors.hasErrors()) {
						CardContext c2 = new CardContext((PackageContext) cx, cg.getName(), cg.areaActions, cd, true);
						pass2(c2, null /*cd.innerScope() */);
						for (HandlerImplements h : cd.handlers) {
							RWHandlerImplements rw = pass2HI(c2, h);
							if (rw != null)
								cg.handlers.add(new HandlerGrouping(rw.handlerName, rw));
						}
					}
				} catch (ResolutionException ex) {
					errors.message(ex.location, ex.getMessage());
				}
			} else if (val instanceof FunctionCaseDefn) {
				FunctionCaseDefn c = (FunctionCaseDefn) val;
				FunctionCaseContext fccx = new FunctionCaseContext(cx, c.functionName(), c.caseName(), null,null /*c.innerScope()*/, false);
				pass2(fccx, null /*c.innerScope()*/);
			} else if (val instanceof MethodCaseDefn) {
			} else if (val instanceof EventCaseDefn) {
				// Nothing to do in pass2 ... was set up in pass1 and will be resolved in pass3
			} else if (val instanceof StructDefn) {
				rewrite(cx, (StructDefn)val);
			} else if (val instanceof UnionTypeDefn) {
				rewrite(cx, (UnionTypeDefn)val);
			} else if (val instanceof ContractDecl) {
				rewrite(cx, (ContractDecl)val);
			} else if (val instanceof HandlerImplements) {
				pass2HI(cx, (HandlerImplements) val);
			} else if (val instanceof TupleMember) {
				// do nothing here
			} else if (val instanceof ObjectDefn) {
				; // we should probably rewrite the fields portion
			} else if (val instanceof UnitTests) {
				// Do we want to start again with another package scope?
				pass2(cx, ((UnitTests)val).scope());
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	// Resolve things that still need doing & handle nested contexts
	public void pass3(NamingContext cx, IScope from) {
		for (ScopeEntry x : from) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof CardDefinition)
				rewriteCard(cx, (CardDefinition)val);
			else if (val instanceof FunctionCaseDefn)
				rewrite(cx, (FunctionCaseDefn)val);
			else if (val instanceof MethodCaseDefn) {
				rewriteStandaloneMethod(cx, (MethodCaseDefn)val, cx.hasCard()?CodeType.CARD:CodeType.STANDALONE);
			} else if (val instanceof EventCaseDefn)
				rewrite(cx, (EventCaseDefn)val);
			else if (val instanceof StructDefn || val instanceof UnionTypeDefn || val instanceof ContractDecl) {
				// these all got sorted out already in the first two passes
			} else if (val instanceof ObjectDefn) {
				rewriteObject(cx, (ObjectDefn)val);
			} else if (val instanceof HandlerImplements) {
				rewriteHI(cx, (HandlerImplements)val, from);
			} else if (val instanceof TupleMember) {
				rewrite(cx, (TupleMember)val);
			} else if (val instanceof UnitTests) {
				// Do we want to start again with another package scope?
				pass3(cx, ((UnitTests)val).scope());
			} else if (val == null)
				logger.warn("Did you know " + name + " does not have a definition?");
			else
				throw new UtilException("Cannot handle " + name +": " + (val == null?"null":val.getClass()));
		}
	}

	private CardGrouping createCard(PackageContext cx, CardDefinition cd) {
		RWStructDefn sd = new RWStructDefn(cd.location, FieldsDefn.FieldsType.ENTITY, new SolidName(cd.cardName.pkg, cd.cardName.cardName), false);
		CardGrouping grp = new CardGrouping(cd.location, cd.cardName, sd);
		cards.put(cd.cardName.uniqueName(), grp);
		return grp;
	}
	
	private CardGrouping pass2Card(NamingContext cx, CardDefinition cd) {
		CardGrouping grp = cards.get(cd.cardName.uniqueName());
		for (ContractImplements ci : cd.contracts) {
			RWContractImplements rw = rewriteCI(cx, ci);
			if (rw == null)
				continue;
//			grp.contracts.add(new ContractGrouping((SolidName) rw.getTypeName(), ci.getRealName(), rw.referAsVar));
//			cardImplements.put(ci.getRealName(), rw);
			if (rw.referAsVar != null)
				grp.struct.addField(new RWStructField(rw.location(), false, rw, rw.referAsVar));
		}
		
		for (ContractService cs : cd.services) {
			RWContractService rw = rewriteCS(cx, cs);
			if (rw == null)
				continue;
//			grp.services.add(new ServiceGrouping(rw.name(), cs.getRealName(), rw.referAsVar));
//			cardServices.put(cs.getRealName(), rw);
			if (rw.referAsVar != null)
				grp.struct.fields.add(new RWStructField(rw.vlocation, false, rw, rw.referAsVar));
		}
		return grp;
	}

	@SuppressWarnings("null")
	private void rewriteCard(NamingContext cx, CardDefinition cd) {
		if (!(cx instanceof PackageContext))
			throw new UtilException("Cannot have card in nested scope: " + cx.getClass());
		CardGrouping grp = cards.get(cd.cardName.uniqueName());
		RWStructDefn sd = grp.struct;
		CardContext c2 = new CardContext((PackageContext) cx, grp.getName(), grp.areaActions, cd, true);
		if (cd.state != null) {
			for (StructField sf : cd.state.fields) {
				rewriteField(c2, sd, sf);
			}
		}
		
		for (ContractImplements ci : cd.contracts) {
			RWContractImplements rw = null; //cardImplements.get(ci.getRealName());

			for (MethodCaseDefn c : ci.methods) {
				if (methods.containsKey(c.intro.name().uniqueName()))
					throw new UtilException("Error or exception?  I think this is two methods with the same name");
				RWMethodDefinition rwm = new RWMethodDefinition(rw.location(), contracts.get(rw.nameAsString()), HSIEForm.CodeType.CONTRACT, RWMethodDefinition.DOWN, c.location(), c.intro.name(), c.intro.args.size());
				rewriteCase(c2, rwm, c, true, false);
				methods.put(c.intro.name().uniqueName(), rwm);
				rw.methods.add(rwm);
				if (!errors.hasErrors())
					rwm.gatherScopedVars();
			}
		}
		
		for (ContractService cs : cd.services) {
			RWContractService rw = null; //cardServices.get(cs.getRealName());

			for (MethodCaseDefn c : cs.methods) {
				if (methods.containsKey(c.intro.name().uniqueName()))
					throw new UtilException("Error or exception?  I think this is two methods with the same name");
				RWMethodDefinition rwm = new RWMethodDefinition(rw.location(), contracts.get(rw.nameAsString()), HSIEForm.CodeType.SERVICE, RWMethodDefinition.UP, c.intro.location, c.intro.name(), c.intro.args.size());
				rewriteCase(c2, rwm, c, true, false);
				methods.put(c.intro.name().uniqueName(), rwm);
				rwm.gatherScopedVars();
			}
		}

		for (HandlerImplements hi : cd.handlers) {
			rewriteHI(c2, hi, null /* cd.innerScope() */);
		}
		
		grp.platforms.putAll(cd.platforms);
		pass3(c2, null /*cd.fnScope*/);
	}

	private void rewriteObject(NamingContext cx, ObjectDefn od) {
//		RWObjectDefn rw = objects.get(od.name().uniqueName());
//		Object ret = cx.resolve(od.location(), "NilMap");
//		final ObjectContext ox = new ObjectContext(cx, od, rw.polys());
//		for (StructField sf : od.fields) {
//			Type st = rewrite(ox, sf.type, false);
//			rw.addField(new RWStructField(sf.loc, false, st, sf.name, null));
//			if (sf.init != null) {
//				InputPosition loc = ((Locatable)sf.init).location();
//				Object rx = rewriteExpr(ox, sf.init);
//				rx = new AssertTypeExpr(loc, (TypeWithName) st, rx);
//				PackageVar assoc = (PackageVar) cx.resolve(loc, "Assoc");
//				ret = new ApplyExpr(loc, assoc, new StringLiteral(sf.location(), sf.name), rx, ret);
//			}
//		}
//		for (ObjectMethod c : od.ctors) {
//			final FunctionName fnName = c.getMethod().methodName();
//			RWFunctionDefinition fn = new RWFunctionDefinition(fnName, 0, true);
//			RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(fnName.location, fnName, new ArrayList<>(), null), 0, new CreateObject(fnName.location, od.name(), ret));
//			fn.addCase(fcd0);
//			fn.gatherScopedVars();
//			functions.put(fn.uniqueName(), fn);
//		}
//		for (ObjectMethod m : od.methods) {
//			final RWMethodDefinition rwm = rw.getMethod(m.getMethod().methodName());
//			rewriteCase(ox, rwm, m.getMethod(), false, true);
//			methods.put(m.getMethod().methodName().uniqueName(), rwm);
//		}
	}

	public FunctionName makeFn(TemplateContext cx, TemplateFormat tf, AreaName areaName, Object expr) {
		if (expr == null)
			return null;
		FunctionName fnName = cx.nextFunction(tf.kw, areaName, "formats", CodeType.AREA);
		RWFunctionDefinition fn = new RWFunctionDefinition(fnName, 0, true);
		RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(tf.kw, fnName, new ArrayList<>(), null), 0, expr);
		fn.addCase(fcd0);
		fn.gatherScopedVars();
		functions.put(fnName.uniqueName(), fn);
		return fnName;
	}

	private void rewrite(NamingContext cx, ContractDecl ctr) {
		RWContractDecl ret = contracts.get(ctr.name().uniqueName());
		for (ContractMethodDecl cmd : ctr.methods) {
			ret.addMethod(rewriteCMD(cx, ctr.name(), cmd));
		}
	}

	private RWContractMethodDecl rewriteCMD(NamingContext cx, SolidName name, ContractMethodDecl cmd) {
		List<Object> outargs = new ArrayList<Object>();
		List<RWTypedPattern> handlerOut = new ArrayList<>();
		final FunctionType type = deriveType(cx, cmd.location(), cmd.args, cmd.name, outargs, handlerOut);
		return new RWContractMethodDecl(cmd.location(), cmd.required, cmd.dir, cmd.name, outargs, type, handlerOut.get(0));
	}

	private FunctionType deriveType(NamingContext cx, final InputPosition loc, final List<Object> inargs, final FunctionName cn, List<Object> outargs, List<RWTypedPattern> handlerOut) {
		List<Type> targs = new ArrayList<Type>(); 
		for (Object o : inargs) {
			if (outargs != null)
				outargs.add(rewritePattern(cx, cn, o));
			if (o instanceof VarPattern) {
				targs.add((Type) ((PackageVar)cx.resolve(((VarPattern)o).location(), "Any")).defn);
			} else if (o instanceof TypedPattern) {
				targs.add(rewrite(cx, ((TypedPattern)o).type, false));
			} else if (o instanceof ConstructorMatch) { // we can get this instead of a typed patter
				ConstructorMatch cm = (ConstructorMatch)o;
				targs.add(rewrite(cx, new TypeReference(cm.location, cm.ctor), false));
			} else
				throw new UtilException("Unexpected pattern " + o.getClass());
		}
		// TODO: in the fulness of time, the parser will pass down a CMD with a specific handler field
		// When it does, we can remove this code
		RWTypedPattern handler = null;
		if (!targs.isEmpty()) {
			Object finalArg = targs.get(targs.size()-1);
			if (finalArg instanceof RWContractDecl) {
				targs.remove(targs.size()-1);
				if (outargs != null) {
					handler = (RWTypedPattern) outargs.remove(outargs.size()-1);
				} else
					handler = (RWTypedPattern) rewritePattern(cx, cn, inargs.get(inargs.size()-1));
			}
		}
		handlerOut.add(handler);
		targs.add(typeFrom(cx.resolve(loc, "Send")));
		final FunctionType type = new FunctionType(loc, targs);
		return type;
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

	private RWContractImplements rewriteCI(NamingContext cx, ContractImplements ci) {
		try {
			Object av = null; // cx.resolve(ci.location(), ci.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message(ci.location(), "cannot find a valid definition of contract " + ci.name());
				return null;
			}
			@SuppressWarnings("unused")
			RWContractDecl cd = (RWContractDecl) ((PackageVar)av).defn;
			return new RWContractImplements(ci.kw, ci.location(), null /*ci.getRealName()*/, (SolidName) cd.getTypeName(), ci.varLocation, ci.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private RWContractService rewriteCS(NamingContext cx, ContractService cs) {
		try {
			Object av = null; // cx.resolve(cs.location(), cs.name());
			if (av == null || !(av instanceof PackageVar)) {
				errors.message(cs.location(), "cannot find a valid definition of contract " + cs.name());
				return null;
			}
			@SuppressWarnings("unused")
			RWContractDecl cd = (RWContractDecl) ((PackageVar)av).defn;
			return new RWContractService(cs.kw, cs.location(), null /*cs.getRealName()*/, (SolidName) cd.getTypeName(), cs.vlocation, cs.referAsVar);
		} catch (ResolutionException ex) {
			errors.message(ex.location, ex.getMessage());
			return null;
		}
	}

	private RWHandlerImplements pass2HI(NamingContext cx, HandlerImplements hi) {
		TypeWithName any = (TypeWithName) getObject(cx.nested.resolve(hi.location(), "Any"));
		Object av = null;
		try {
			av = nullValue(); // cx.resolve(hi.location(), hi.name());
		} catch (ResolutionException ex) {
			// remains null, see below ...
		}
		if (av == null || !(av instanceof PackageVar)) {
			errors.message(hi.location(), "cannot find a valid definition of contract " + hi.name());
			return null;
		}
		List<HandlerLambda> bvs = new ArrayList<HandlerLambda>();
		for (Object o : hi.boundVars) {
			HandlerLambda hl;
			if (o instanceof VarPattern) {
				VarPattern vp = (VarPattern) o;
				hl = new HandlerLambda(vp.varLoc, hi.handlerName, any, vp.var);
			} else if (o instanceof TypedPattern) {
				TypedPattern vp = (TypedPattern) o;
				hl = new HandlerLambda(vp.var.loc, hi.handlerName, (TypeWithName) rewrite(cx, vp.type, false), vp.var.var);
			} else
				throw new UtilException("Can't handle pattern " + o + " as a handler lambda");
			bvs.add(hl);
		}
		RWContractDecl cd = (RWContractDecl) ((PackageVar) av).defn;
		RWHandlerImplements rw = new RWHandlerImplements(hi.kw, hi.location(), hi.handlerName, cd.getTypeName(), hi.inCard, bvs);
		callbackHandlers.put(hi.handlerName.uniqueName(), rw);
		for (MethodCaseDefn c : hi.methods) {
			pass1(cx, c.innerScope());
		}
		return rw;
	}

	private void rewriteHI(NamingContext cx, HandlerImplements hi, IScope scope) {
		try {
			RWHandlerImplements ret = callbackHandlers.get(hi.handlerName.uniqueName());
			if (ret == null)
				return; // presumably it failed in pass1
			HandlerContext hc = new HandlerContext(cx, ret);
			for (MethodCaseDefn c : hi.methods) {
				if (methods.containsKey(c.intro.name().uniqueName()))
					throw new UtilException("Error or exception?  I think this is two methods with the same name");
				RWMethodDefinition rm = new RWMethodDefinition(ret.location(), contracts.get(ret.nameAsString()), HSIEForm.CodeType.HANDLER, RWMethodDefinition.DOWN, c.intro.location, c.intro.name(), c.intro.args.size());
				rewriteCase(hc, rm, c, true, false);
				ret.methods.add(rm);
				rm.gatherScopedVars();
				methods.put(c.intro.name().uniqueName(), rm);
				Map<String, LocalVar> vars = new TreeMap<>();
				gatherVars(errors, this, hc, rm.name(), rm.name(), vars, c.intro);
				FunctionCaseContext hfc = new FunctionCaseContext(hc, rm.name(), null, vars, null/*c.innerScope()*/, true);
				
				pass3(hfc, c.innerScope());
			}

			// Create a struct to store the state.  It feels weird creating a struct in pass3, but we don't creating the bound vars for scoped/lambdas
			// until just above, so we have to wait ...
			
			// I don't want to have two arrays with the same named entry, so add a random thing to the end of the struct
			SolidName sdname = new SolidName(hi.handlerName.name, hi.handlerName.baseName+"$struct");
			RWStructDefn hsd = new RWStructDefn(hi.location(), FieldsDefn.FieldsType.STRUCT, sdname, false);
			for (Object s : ret.boundVars) {
				HandlerLambda hl = (HandlerLambda) s;
				hsd.fields.add(new RWStructField(hl.location, false, hl.type, hl.var));
			}
			structs.put(sdname.uniqueName(), hsd);
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

	public void rewrite(NamingContext cx, FunctionCaseDefn c) {
		RWFunctionDefinition ret = functions.get(c.functionName().uniqueName());
		final Map<String, LocalVar> vars = new HashMap<>();
		gatherVars(errors, this, cx, c.functionName(), c.caseName(), vars, c.intro);
		FunctionCaseContext fccx = new FunctionCaseContext(cx, c.functionName(), c.caseName(), vars, null /*c.innerScope()*/, false);
		RWFunctionCaseDefn rwc = rewrite(fccx, c, ret.cases.size(), vars);
		if (rwc == null)
			return;
		ret.addCase(rwc);
		ret.gatherScopedVars();
	}

	/** A tuple member is one of a set of values to be assigned to the result
	 * of the same function call.  So basically, we just ignore all but case 0
	 * and then rewrite case 0 to the overall function.
	 * Somebody somewhere needs to do the work to unpack the results - probably
	 * we need to rewrite expressions that involve the tuple members.
	 * @param cx
	 * @param c
	 */
	public void rewrite(NamingContext cx, TupleMember c) {
		final FunctionName cn = c.name();
		if (c.which == 0) {
			// Rewrite the first function into an overall thing
//			final FunctionCaseDefn fcd = new FunctionCaseDefn(c.exprFnName(), new ArrayList<>(), c.ta.expr);
//			fcd.provideCaseName(-1);
//			rewrite(cx, fcd);
		}
		
		// now make this point to a deref of that
		RWFunctionDefinition me = functions.get(cn.uniqueName());
		final RWFunctionIntro intro = new RWFunctionIntro(cn.location, cn, new ArrayList<>(), new HashMap<>());
		final PackageVar pv = new PackageVar(cn.location, new SolidName(null, "(_)"), null);
		final ApplyExpr extractExpr = new ApplyExpr(cn.location, pv, new NumericLiteral(cn.location, c.which), new ApplyExpr(cn.location, new PackageVar(cn.location, c.exprFnName(), null)));
		me.addCase(new RWFunctionCaseDefn(intro, 0, extractExpr));
	}

	private void rewriteStandaloneMethod(NamingContext cx, MethodCaseDefn c, HSIEForm.CodeType codeType) {
		RWMethodDefinition rm = standalone.get(c.intro.name().uniqueName());
		rewriteCase(cx, rm, c, false, true);
		rm.gatherScopedVars();
	}
	
	protected void rewriteCase(NamingContext cx, RWMethodDefinition rm, MethodCaseDefn c, boolean fromHandler, boolean useCases) {
		Map<String, LocalVar> vars = new HashMap<>();
		NameOfThing name = useCases ? c.caseName() : c.methodName();
		gatherVars(errors, this, cx, c.methodName(), name, vars, c.intro);
		rm.cases.add(rewrite(new FunctionCaseContext(cx, c.methodName(), null, vars, null, fromHandler), c, vars));
	}

	private void rewrite(NamingContext cx, EventCaseDefn c) {
		RWEventHandlerDefinition rw = eventHandlers.get(c.intro.name().uniqueName());
		Map<String, LocalVar> vars = new HashMap<>();
		gatherVars(errors, this, cx, rw.name(), rw.name(), vars, c.intro);
		rw.cases.add(rewrite(new FunctionCaseContext(cx, c.methodName(), null, vars, null, false), c, vars));
	}

	private void rewrite(NamingContext cx, StructDefn sd) {
		RWStructDefn ret = structs.get(sd.name().uniqueName());
		if (ret == null)
			throw new UtilException("Struct " + sd.name().uniqueName() + " was not created in pass1");
		for (StructField sf : sd.fields) {
			// TODO: it's not clear that the expression needs this rewritten context
			StructDefnContext sx = new StructDefnContext(cx, ret.polys());
			rewriteField(sx, ret, sf);
		}
	}

	protected void rewriteField(NamingContext sx, RWStructDefn sd, StructField sf) {
		FunctionName fnName = null;
		Type st = rewrite(sx, sf.type, false);
		if (sf.init != null) {
			Object rw = rewriteExpr(sx, sf.init);
			InputPosition loc = ((Locatable)rw).location();
			Object expr = new AssertTypeExpr(loc, (TypeWithName) st, rw);
			fnName = FunctionName.initializer(loc, sd.structName(), "inits_" + sf.name);
			RWFunctionDefinition fn = new RWFunctionDefinition(fnName, 0, true);
			RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(loc, fnName, new ArrayList<>(), null), 0, expr);
			fn.addCase(fcd0);
			fn.gatherScopedVars();
			functions.put(fnName.uniqueName(), fn);
		}
		RWStructField rsf = new RWStructField(sf.loc, false, st, sf.name, fnName);
		sd.addField(rsf);
	}

	private void rewrite(NamingContext cx, UnionTypeDefn u) {
		RWUnionTypeDefn ret = types.get(u.name());
		for (TypeReference c : u.cases) {
			ret.addCase((TypeWithName) rewrite(cx, c, true));
		}
	}

	protected List<PolyVar> rewritePolys(List<PolyType> polys) {
		List<PolyVar> pts = new ArrayList<>();
		if (polys != null)
			for (PolyType r : polys)
				pts.add(new PolyVar(r.location(), r.name()));
		return pts;
	}

	private RWFunctionCaseDefn rewrite(FunctionCaseContext cx, FunctionCaseDefn c, int csNo, Map<String, LocalVar> vars) {
		RWFunctionIntro intro = rewriteFunctionIntro(cx, c.intro, cx.name(), vars);
		Object expr = rewriteExpr(cx, c.expr);
		if (expr == null)
			return null;
		RWFunctionCaseDefn ret = new RWFunctionCaseDefn(intro, csNo, expr);
		pass3(new NestedScopeContext(cx), null /*c.innerScope()*/);
		return ret;
	}

	private RWMethodCaseDefn rewrite(FunctionCaseContext cx, MethodCaseDefn c, Map<String, LocalVar> vars) {
		RWMethodCaseDefn ret = new RWMethodCaseDefn(rewriteFunctionIntro(cx, c.intro, c.caseName(), vars));
		for (MethodMessage mm : c.messages)
			ret.addMessage(rewrite(cx, mm));
		return ret;
	}

	private RWEventCaseDefn rewrite(FunctionCaseContext cx, EventCaseDefn c, Map<String, LocalVar> vars) {
		RWEventCaseDefn ret = new RWEventCaseDefn(c.kw, rewriteFunctionIntro(cx, c.intro, c.intro.name(), vars));
		for (MethodMessage mm : c.messages)
			ret.messages.add(rewrite(cx, mm));
		return ret;
	}

	private RWFunctionIntro rewriteFunctionIntro(NamingContext cx, FunctionIntro intro, NameOfThing csName, Map<String, LocalVar> vars) {
		List<Object> args = new ArrayList<Object>();
		for (Object o : intro.args) {
			args.add(rewritePattern(cx, csName, o));
		}
		return new RWFunctionIntro(intro.location, intro.name(), args, vars);
	}

	private void createD3Methods(D3Context c2, CardName cardName, D3Thing d3) {
		int nextFn = 1;
		// TODO: we should figure out the right positions for everything labelled "hack" here :-)
		InputPosition posn = new InputPosition("d3", 1, 1, null);
		Object init = (PackageVar) c2.resolve(posn, "NilMap");
		PackageVar assoc = (PackageVar) c2.resolve(posn, "Assoc");
		PackageVar cons = (PackageVar) c2.resolve(posn, "Cons");
		PackageVar nil = (PackageVar) c2.resolve(posn, "Nil");
		BuiltinOperation tuple = (BuiltinOperation) c2.resolve(posn, "()");
		RWStructDefn d3Elt = structs.get("D3Element");
//		PackageVar d3Elt = new PackageVar(posn, "D3Element", null);
		ListMap<String, Object> byKey = new ListMap<String, Object>();
		Map<String, InputPosition> sectionLocations = new HashMap<String, InputPosition>();
		for (D3PatternBlock p : sortAlphabetically(d3.patterns)) {
			for (D3Section s : sortAlphabetically(p.sections)) {
				sectionLocations.put(s.name, s.location);
				if (!s.properties.isEmpty()) {
					Object pl = nil; // prepend to an empty list
					for (PropertyDefn prop : sortAlphabetically(s.properties)) {
						Object expr = rewriteExpr(c2, prop.value);
						// TODO: only create functions for things that depend on the class
						// constants can just be used directly
						FunctionLiteral efn = functionWithArgs(cardName, nextFn++, Arrays.asList(new RWTypedPattern(d3.d3.varLoc, d3Elt, d3.d3.varLoc, d3.d3.iterVar)), expr);
						Object pair = new ApplyExpr(prop.location, tuple, new StringLiteral(prop.location, prop.name), efn);
						pl = new ApplyExpr(prop.location, cons, pair, pl);
					}
					byKey.add(s.name, new ApplyExpr(s.location, tuple, p.pattern, pl));
				}
				else if (!s.actions.isEmpty()) { // something like enter, that is a "method"
					FunctionName fn = FunctionName.functionInCardContext(s.location, c2.cardNameIfAny(), "_d3_" + d3.d3.name + "_" + s.name+"_"+p.pattern.text);
					RWFunctionIntro fi = new RWFunctionIntro(s.location, fn, new ArrayList<Object>(), null);
					RWMethodCaseDefn mcd = new RWMethodCaseDefn(fi);
					for (MethodMessage mm : s.actions)
						mcd.addMessage(rewrite(c2, mm));
					RWMethodDefinition method = new RWMethodDefinition(null, null, HSIEForm.CodeType.CARD, RWMethodDefinition.EVENT, fi.location, fi.fnName, fi.args.size());
					method.cases.add(mcd);
					method.gatherScopedVars();
					this.methods.put(method.name().uniqueName(), method);
					byKey.add(s.name, new FunctionLiteral(fi.location, fi.fnName));
				} else { // something like layout, that is just a set of definitions
					// This function is generated over in DomFunctionGenerator, because it "fits" better there ...
				}
			}
		}
		for (Entry<String, List<Object>> k : byKey.entrySet()) {
			String sectionName = k.getKey();
			Object list = nil;
			List<Object> lo = k.getValue();
			for (int i=lo.size()-1;i>=0;i--) {
				Locatable li = (Locatable) lo.get(i);
				list = new ApplyExpr(li.location(), cons, li, list);
			}
			init = new ApplyExpr(((Locatable)list).location(), assoc, new StringLiteral(sectionLocations.get(sectionName), sectionName), list, init);
		}

		Locatable dataExpr = (Locatable) rewriteExpr(c2, d3.d3.expr);
		FunctionLiteral dataFn = functionWithArgs(cardName, nextFn++, new ArrayList<Object>(), dataExpr);
		init = new ApplyExpr(dataExpr.location(), assoc, new StringLiteral(dataExpr.location(), "data"), dataFn, init);

		FunctionName name = FunctionName.functionInCardContext(d3.d3.varLoc, c2.cardNameIfAny(), "_d3init_" + d3.d3.name);
		RWFunctionIntro d3f = new RWFunctionIntro(d3.d3.varLoc, name, new ArrayList<Object>(), null);
		RWFunctionDefinition func = new RWFunctionDefinition(name, 0, true);
		func.addCase(new RWFunctionCaseDefn(d3f, 0, init));
		func.gatherScopedVars();
		functions.put(name.uniqueName(), func);
	}

	private <T extends Comparable<T>> List<T> sortAlphabetically(List<T> items) {
		List<T> ret = new ArrayList<T>();
		ret.addAll(items);
		Collections.sort(ret);
		return ret;
	}

	private FunctionLiteral functionWithArgs(CardName inCard, final int nextFn, List<Object> args, Object expr) {
		String name = "_gen_" + nextFn;

		InputPosition loc = ((Locatable)expr).location(); // may or may not be correct location
		FunctionName fn = FunctionName.functionInCardContext(loc, inCard, name);
		RWFunctionIntro d3f = new RWFunctionIntro(loc, fn, args, null);
		RWFunctionDefinition func = new RWFunctionDefinition(fn, args.size(), true);
		func.addCase(new RWFunctionCaseDefn(d3f, 0, expr));
		functions.put(d3f.fnName.uniqueName(), func);

		return new FunctionLiteral(d3f.location, d3f.fnName);
	}

	public Object rewritePattern(NamingContext cx, NameOfThing name, Object o) {
		try {
			if (o instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) o;
				TypeWithName rt = (TypeWithName) rewrite(cx, tp.type, false);
				fnArgs.put(tp.var.uniqueName(), rt);
				return new RWTypedPattern(tp.typeLocation, rt, tp.var.loc, tp.var);
			} else if (o instanceof VarPattern) {
				VarPattern vp = (VarPattern) o;
				return new RWVarPattern(vp.location(), new VarName(vp.varLoc, name, vp.var));
			} else if (o instanceof ConstructorMatch) {
				ConstructorMatch cm = (ConstructorMatch) o;
				Object type = cx.resolve(cm.location, cm.ctor);
				if (!(type instanceof PackageVar))
					errors.message(cm.location, "could not handle " + type);
				RWConstructorMatch ret = new RWConstructorMatch(cm.location, (PackageVar)type);
				for (Field x : cm.args)
					ret.args.add(ret.new Field(x.location(), x.field, rewritePattern(cx, name, x.patt)));
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

	public Object rewriteExpr(NamingContext cx, Object expr) {
		if (expr == null)
			return null;
		try {
			if (expr instanceof NumericLiteral || expr instanceof StringLiteral || expr instanceof BooleanLiteral || expr instanceof BuiltinOperation)
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
				if (ret == null)
					ret = cx.resolve(location, s); // debug
				if (ret instanceof PackageVar || ret instanceof ScopedVar || ret instanceof LocalVar || ret instanceof IterVar ||
					ret instanceof CardMember || ret instanceof ObjectReference || ret instanceof CardFunction || ret instanceof HandlerLambda ||
					ret instanceof TemplateListVar || ret instanceof SpecialFormat || ret instanceof BooleanLiteral || ret instanceof BuiltinOperation)
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
					boolean rewritten = false;
					if (aefn instanceof ApplyExpr) {
						aefn = rewriteExpr(cx, aefn);
						rewritten = true;
					}
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
							cx.resolve(uv0.location, uv0.var);
						} catch (ResolutionException ex) {
							try {
								return cx.resolve(uv0.location, uv0.var + "." + fname);
							} catch (ResolutionException ex2) {
								// This seems bizarre at best, but it handles the case where we have long package names on import.
								// But it seems that at the very least, we should try resolving it.
								// In doing another case, I came across this being passed downstream to people who didn't want it.
								
								// TODO: improve this logic
								return new UnresolvedVar(uv0.location, uv0.var + "." + fname);
//								throw ex2;
							}
						}
					} 
					
					if (!(aefn instanceof ApplyExpr) && !(aefn instanceof UnresolvedVar))
						throw new UtilException("That case is not handled: " + aefn.getClass());
					
					// expr . field
					Object applyFn = aefn;
					if (!rewritten)
						applyFn = rewriteExpr(cx, aefn);
					if (castTo != null)
						applyFn = new RWCastExpr(castLoc, castTo, applyFn);
	
					if (applyFn instanceof PackageVar && ((PackageVar)applyFn).defn instanceof RWObjectDefn)
						return new ApplyExpr(ae.location, BuiltinOperation.OCTOR.at(ae.location), applyFn, new StringLiteral(loc, fname));
					else
						return new ApplyExpr(ae.location, BuiltinOperation.FIELD.at(ae.location), applyFn, new StringLiteral(loc, fname));
				}
				List<Object> args = new ArrayList<Object>();
				Object head = rewriteExpr(cx, ae.fn);
				if (head instanceof UnresolvedVar) {
					UnresolvedVar uvh = (UnresolvedVar) head;
					throw new ResolutionException(uvh.location(), uvh.var);
				}
				for (Object o : ae.args)
					args.add(rewriteExpr(cx, o));
				return new ApplyExpr(ae.location, head, args);
			} else if (expr instanceof CastExpr) {
				CastExpr ce = (CastExpr) expr;
				Object resolve = cx.resolve(ce.location, (String) ce.castTo);
				return new RWCastExpr(ce.location, resolve, rewriteExpr(cx, ce.expr));
			} else if (expr instanceof IfExpr) {
				IfExpr ie = (IfExpr)expr;
				return new IfExpr((Locatable) rewriteExpr(cx, ie.guard), rewriteExpr(cx, ie.ifExpr), rewriteExpr(cx, ie.elseExpr));
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
					return new PolyVar(type.location(), type.name());
				throw ex;
			}
			if (ret instanceof TypeWithNameAndPolys) {
				TypeWithNameAndPolys tnp = (TypeWithNameAndPolys) ret;
				if (tnp.hasPolys() && !type.hasPolys()) {
					errors.message(type.location(), "cannot use " + tnp.nameAsString() + " without specifying polymorphic arguments");
					return null;
				} else if (!tnp.hasPolys() && type.hasPolys()) {
					errors.message(type.location(), "cannot use polymorphic arguments to type " + tnp.nameAsString());
					return null;
				} else if (tnp.hasPolys() && type.hasPolys()) {
					// check and instantiate
					if (type.polys().size() != tnp.polys().size()) {
						errors.message(type.location(), "incorrect number of polymorphic arguments to type " + tnp.nameAsString());
						return null;
					} else {
						List<Type> rwp = new ArrayList<Type>();
						for (TypeReference p : type.polys())
							rwp.add(rewrite(cx, p, true));
						ret = tnp.instance(type.location(), rwp);
					}
				}
			}
			return ret;
		} catch (ResolutionException ex) {
			errors.message(type.location(), ex.getMessage());
			return null;
		}
	}

	public void gatherVars(ErrorResult errors, Rewriter rewriter, Rewriter.NamingContext cx, FunctionName fnName, NameOfThing caseName, Map<String, LocalVar> into, FunctionIntro fi) {
		for (int i=0;i<fi.args.size();i++) {
			Object arg = fi.args.get(i);
			if (arg instanceof VarPattern) {
				VarPattern vp = (VarPattern)arg;
				into.put(vp.var, new LocalVar(fnName, caseName, vp.varLoc, vp.var, null, null));
			} else if (arg instanceof ConstructorMatch)
				gatherCtor(errors, cx, fnName, caseName, into, (ConstructorMatch) arg);
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
				into.put(tp.var.var, new LocalVar(fnName, caseName, tp.var.loc, tp.var.var, tp.typeLocation, t));
			} else
				throw new UtilException("Not gathering vars from " + arg.getClass());
		}
	}

	private void gatherCtor(ErrorResult errors, NamingContext cx, FunctionName fnName, NameOfThing caseName, Map<String, LocalVar> into, ConstructorMatch cm) {
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
				into.put(vp.var, new LocalVar(fnName, caseName, vp.varLoc, vp.var, vp.varLoc, t));
			} else if (x.patt instanceof ConstructorMatch)
				gatherCtor(errors, cx, fnName, caseName, into, (ConstructorMatch)x.patt);
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
			for (Entry<CSName, RWContractImplements> x : cardImplements.entrySet())
				System.out.println("Impl " + x.getKey().uniqueName());
			for (Entry<CSName, RWContractService> x : cardServices.entrySet())
				System.out.println("Service " + x.getKey().uniqueName());
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
		if (primitives.containsKey(id))
			return primitives.get(id);
		else if (constants.containsKey(id))
			return constants.get(id);
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
		else if (fnArgs.containsKey(id))
			return fnArgs.get(id);
		else
			return null;
	}
	
	public PackageVar getMe(InputPosition location, NameOfThing name) {
		Object val = doIhave(location, name.uniqueName());
		if (val == null) {
			return null;
		}
		if (val instanceof NamedThing)
			name = ((NamedThing)val).getName();
		else
			System.out.println(val.getClass() + " is not a NamedThing");
		return new PackageVar(location, name, val);
	}

	private Type resolveType(NamingContext cx, TypeReference type) {
		if (type instanceof FunctionTypeReference) {
			FunctionTypeReference ftr = (FunctionTypeReference) type;
			List<Type> list = new ArrayList<>();
			for (TypeReference tr : ftr.args)
				list.add(resolveType(cx, tr));
			return new FunctionType(type.location(), list);
		}
		return (Type)getObject(cx.resolve(type.location(), type.name()));
	}
	
	public void writeGeneratableTo(File writeRW) throws FileNotFoundException {
		Indenter pw = new Indenter(writeRW);
		for (RWHandlerImplements h : this.callbackHandlers.values()) {
			writeHandler(pw, h);
		}
		for (RWMethodDefinition m : this.standalone.values()) {
			writeMethod(pw, m);
		}
		writeGeneratableFunctionsTo(pw, functions);
		pw.close();
	}

	private void writeHandler(Indenter pw, RWHandlerImplements h) {
		pw.println("handler " + h.handlerName.uniqueName() + " " + (h.inCard?"*card*":"*function*"));
		for (HandlerLambda v : h.boundVars)
			writeLambda(pw.indent(), v);
		for (RWMethodDefinition m : h.methods)
			writeMethod(pw.indent(), m);
	}

	private void writeLambda(Indenter pw, HandlerLambda v) {
		pw.println("lambda " + v.clzName.uniqueName() + " " + v.var);
	}

	private void writeMethod(Indenter pw, RWMethodDefinition m) {
		pw.println("method " + m.name().uniqueName());
		for (ScopedVar sv : m.scopedVars)
			pw.indent().println("nested " + sv.id.uniqueName() + " " + sv.definedBy.uniqueName());
		for (RWMethodCaseDefn c : m.cases)
			writeMethodCase(pw.indent(), c);
	}

	private void writeMethodCase(Indenter pw, RWMethodCaseDefn c) {
		pw.println("case " + c.intro.fnName.uniqueName());
		for (Object a : c.intro.args)
			writeArgumentTo(pw.indent(), a);
		pw.println("=");
		for (RWMethodMessage m : c.messages)
			writeMessage(pw.indent(), m);
	}

	private void writeMessage(Indenter pw, RWMethodMessage m) {
		if (m.slot != null)
			pw.println(m.slot.toString());
		pw.println("<-");
		writeExpr(pw.indent(), m.expr);
	}

	public void writeGeneratableFunctionsTo(File file, Map<String, RWFunctionDefinition> functions) throws FileNotFoundException {
		Indenter pw = new Indenter(file);
		writeGeneratableFunctionsTo(pw, functions);
		pw.close();
	}

	private void writeGeneratableFunctionsTo(Indenter pw, Map<String, RWFunctionDefinition> functions) {
		for (RWFunctionDefinition fn : functions.values()) {
			if (!fn.generate)
				continue;
			pw.println("function " + fn.uniqueName() + (fn.inCard != null?" " + fn.inCard.uniqueName():"") + " " + fn.nargs);
			for (ScopedVar sv : fn.scopedVars)
				pw.indent().println("nested " + sv.id.uniqueName() + " " + sv.definedBy.uniqueName());
			for (RWFunctionCaseDefn c : fn.cases) {
				Indenter p2 = pw.indent();
				p2.println("case " + c.caseName().uniqueName());
				for (Object a : c.args())
					writeArgumentTo(p2.indent(), a);
				p2.println("=");
				writeExpr(p2.indent(), c.expr);
			}
		}
	}

	private void writeArgumentTo(Indenter pw, Object a) {
		if (a instanceof RWVarPattern) {
			pw.println("var " + ((RWVarPattern)a).var.uniqueName());
		} else if (a instanceof RWTypedPattern) {
			RWTypedPattern tp = (RWTypedPattern)a;
			pw.println("typed " + tp.type + " " + tp.var.uniqueName());
		} else if (a instanceof RWConstructorMatch) {
			RWConstructorMatch cm = (RWConstructorMatch)a;
			pw.println("ctor " + cm.ref);
			for (org.flasck.flas.rewrittenForm.RWConstructorMatch.Field x : cm.args) {
				writeArgumentTo(pw.indent(), x.patt);
			}
		} else if (a instanceof ConstPattern) {
			pw.println("const " + ((ConstPattern)a).value);
		} else
			throw new UtilException("Cannot handle " + a.getClass());
	}

	private void writeExpr(Indenter pw, Object expr) {
		if (expr == null)
			pw.println("null");
		else if (expr instanceof LocalVar || expr instanceof ExternalRef)
			pw.println(expr.getClass().getSimpleName()+"."+expr);
		else if (expr instanceof StringLiteral || expr instanceof NumericLiteral || expr instanceof BooleanLiteral)
			pw.println("" + expr);
		else if (expr instanceof BuiltinOperation)
			pw.println(((BuiltinOperation) expr).opName);
		else if (expr instanceof CardStateRef) {
			CardStateRef csr = (CardStateRef) expr;
			pw.println(csr.toString());
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			pw.println("@");
			writeExpr(pw.indent(), ae.fn);
			for (Object o : ae.args)
				writeExpr(pw.indent(), o);
		}
		else if (expr instanceof SendExpr) {
			SendExpr se = (SendExpr) expr;
			pw.println("<-");
			writeExpr(pw.indent(), se.sender);
			writeExpr(pw.indent(), se.method);
			for (Object o : se.args)
				writeExpr(pw.indent(), o);
			pw.println("=>");
			writeExpr(pw.indent(), se.handler);
		} else if (expr instanceof AssertTypeExpr) {
			AssertTypeExpr se = (AssertTypeExpr) expr;
			pw.println("#" + se.type);
			writeExpr(pw.indent(), se.expr);
		} else if (expr instanceof TypeCheckMessages) {
			TypeCheckMessages se = (TypeCheckMessages) expr;
			pw.println("#tcm");
			writeExpr(pw.indent(), se.expr);
		} else if (expr instanceof IfExpr) {
			IfExpr ie = (IfExpr) expr;
			pw.println("if");
			writeExpr(pw.indent(), ie.guard);
			pw.println("then");
			writeExpr(pw.indent(), ie.ifExpr);
			pw.println("else");
			writeExpr(pw.indent(), ie.elseExpr);
		} else if (expr instanceof TemplateListVar) {
			pw.println(expr.toString());
		} else if (expr instanceof TypeCheckStringable) {
			pw.println("CheckStringable");
			writeExpr(pw.indent(), ((TypeCheckStringable)expr).expr);
		}
		else
			pw.println("?? " + expr.getClass() + ":" + expr);
	}

	@Override
	public void registerCodeGenerator(RepoVisitor gen) {
		generators.add(gen);
	}

	public void visit(RepoVisitor v, boolean visitAll) {
		for (RWStructDefn sd : structs.values()) {
			if (sd.generate || visitAll) {
				v.visitStructDefn(sd);
			}
		}
		
		for (RWObjectDefn od : objects.values()) {
			if (od.generate || visitAll) {
				v.visitObjectDefn(od);
			}
		}
		
		for (RWContractDecl c : contracts.values()) {
			if (c.generate || visitAll)
				v.visitContractDecl(c);
		}

		for (CardGrouping c : cards.values()) {
			v.visitCardGrouping(c);
		}
		
		for (RWContractImplements ci : cardImplements.values()) {
			v.visitContractImpl(ci);
		}

		for (RWContractService cs : cardServices.values()) {
			v.visitServiceImpl(cs);
		}
		
		for (RWHandlerImplements hi : callbackHandlers.values()) {
			v.visitHandlerImpl(hi);
		}
	}

	public void visitGenerators() {
		for (RepoVisitor gen : generators)
			visit(gen, false);
	}

	public void checkCardContractUsage() {
		for (Entry<String, CardGrouping> kv : cards.entrySet()) {
			CardGrouping grp = kv.getValue();
			for (ContractGrouping ctr : grp.contracts) {
				RWContractImplements ci = cardImplements.get(ctr.implName);
				if (ci == null)
					throw new UtilException("Could not find contract implements for " + ctr.implName);
				RWContractDecl cd = contracts.get(ci.nameAsString());
				if (cd == null)
					throw new UtilException("Could not find contract decl for " + ci.nameAsString());
				Set<RWContractMethodDecl> requireds = new TreeSet<RWContractMethodDecl>(); 
				for (RWContractMethodDecl m : cd.methods) {
					if (m.dir.equals(ContractMethodDir.DOWN) && m.required)
						requireds.add(m);
				}
				for (RWMethodDefinition m : ci.methods) {
					boolean haveMethod = false;
					for (RWContractMethodDecl dc : cd.methods) {
						if (dc.dir.equals(ContractMethodDir.DOWN) && (ctr.implName.uniqueName() +"." + dc.name).equals(m.name().uniqueName())) {
							if (dc.args.size() != m.nargs())
								errors.message(m.location(), "incorrect number of arguments in declaration, expected " + dc.args.size());
							requireds.remove(dc);
							haveMethod = true;
							break;
						}
					}
					if (!haveMethod)
						errors.message(m.location(), "cannot implement down method " + m.name().uniqueName() + " because it is not in the contract declaration");
				}
				if (!requireds.isEmpty()) {
					for (RWContractMethodDecl d : requireds)
						errors.message(ci.location(), ci.nameAsString() + " does not implement " + d);
				}
			}
		}
	}
}