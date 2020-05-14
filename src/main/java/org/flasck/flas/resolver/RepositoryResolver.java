package org.flasck.flas.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeHelpers;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.CardType;
import org.ziniki.splitter.FieldType;
import org.ziniki.splitter.NoMetaDataException;
import org.ziniki.splitter.NoMetaKeyException;
import org.zinutils.exceptions.NotImplementedException;

public class RepositoryResolver extends LeafAdapter implements Resolver {
//	public class BindingInfo {
//		private final TemplateBinding b;
//		private final FieldType type;
//
//		public BindingInfo(TemplateBinding b, FieldType type) {
//			this.b = b;
//			this.type = type;
//		}
//	}

	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final List<NameOfThing> scopeStack = new ArrayList<>();
	private NameOfThing scope;
	private Implements currentlyImplementing;
	private Template currentTemplate;
	private UnresolvedVar currShoveExpr;
	private Set<String> currentBindings;
//	private BindingInfo currentBinding;
	private ArrayList<String> currentTemplates;
	private ArrayList<String> referencedTemplates;
	private NestingChain templateNestingChain;

	public RepositoryResolver(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
	}

	@Override
	public void resolveAll() {
		repository.traverse(this);
	}

	public void currentScope(NameOfThing scope) {
		this.scope = scope;
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		scopeStack.add(0, scope);
		this.scope = fn.name();
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		scopeStack.add(0, scope);
		this.scope = fi.name();
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		scopeStack.add(0, scope);
		this.scope = meth.name();
		if (currentlyImplementing != null && currentlyImplementing.actualType() != null) {
			ContractDecl cd = currentlyImplementing.actualType();
			ContractMethodDecl cm = cd.getMethod(meth.name().name);
			if (cm != null) {
				if (meth.argCount() < cm.args.size()) {
					InputPosition loc = meth.location();
					if (!meth.args().isEmpty())
						loc = meth.args().get(meth.args().size()-1).location();
					errors.message(loc, "insufficient arguments provided to contract method '" + meth.name().name + "'");
				} else if (meth.argCount() > cm.args.size()) {
					InputPosition loc = meth.args().get(cm.args.size()).location();
					errors.message(loc, "excess arguments provided to contract method '" + meth.name().name + "'");
				}
				meth.bindFromContract(cm);
			} else
				errors.message(meth.location(), "there is no method '" + meth.name().name + "' on '" + cd.name().uniqueName() + "'");
		}
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor ctor) {
		scopeStack.add(0, scope);
		this.scope = ctor.name();
	}
	
	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
		RepositoryEntry defn = find(scope, p.ctor);
		if (defn == null) {
			errors.message(p.location, "cannot find type '" + p.ctor + "'");
			return;
		} else if (!(defn instanceof StructDefn)) {
			errors.message(p.location, p.ctor + " is not a struct defn");
			return;
		} else
			p.bind((StructDefn) defn);
	}
	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveObjectCtor(ObjectCtor oa) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void visitTuple(TupleAssignment ta) {
		scopeStack.add(0, scope);
		this.scope = ta.scopePackage();
	}

	@Override
	public void leaveTuple(TupleAssignment e) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitStructDefn(StructDefn sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		scopeStack.add(0, scope);
		this.scope = cd.name();
		currentTemplates = new ArrayList<>();
		referencedTemplates = new ArrayList<>();
	}
	
	@Override
	public void visitAgentDefn(AgentDefinition sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitServiceDefn(ServiceDefinition sd) {
		scopeStack.add(0, scope);
		this.scope = sd.name();
	}
	
	@Override
	public void visitImplements(ImplementsContract ic) {
		scopeStack.add(0, scope);
		this.scope = ic.name();
		currentlyImplementing = ic;
	}
	
	@Override
	public void visitProvides(Provides p) {
		scopeStack.add(0, scope);
		this.scope = p.name();
		currentlyImplementing = p;
	}
	
	@Override
	public void visitHandlerImplements(HandlerImplements hi, StateHolder sh) {
		scopeStack.add(0, scope);
		this.scope = hi.name();
		currentlyImplementing = hi;
	}

	@Override
	public void leaveProvides(Provides p) {
		currentlyImplementing = null;
		ContractDecl d = p.actualType();
		if (d == null) {
			if (p.implementsType().defn() != null)
				errors.message(p.implementsType().location(), p.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.SERVICE) {
			errors.message(p.implementsType().location(), "cannot provide non-service contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : p.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(p.location(), "must implement required contract method " + cmd.name.uniqueName());
		}
	}

	@Override
	public void leaveImplements(ImplementsContract ic) {
		currentlyImplementing = null;
		ContractDecl d = ic.actualType();
		if (d == null) {
			if (ic.implementsType().defn() != null)
				errors.message(ic.implementsType().location(), ic.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.CONTRACT) {
			errors.message(ic.implementsType().location(), "cannot implement " + d.type.toString().toLowerCase() + " contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : ic.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(ic.location(), "must implement required contract method " + cmd.name.uniqueName());
		}
	}
	
	@Override
	public void leaveHandlerImplements(HandlerImplements hi) {
		currentlyImplementing = null;
		ContractDecl d = hi.actualType();
		if (d == null) {
			if (hi.implementsType().defn() != null)
				errors.message(hi.implementsType().location(), hi.implementsType().name() + " is not a contract");
			return;
		}
		if (d.type != ContractType.HANDLER) {
			errors.message(hi.implementsType().location(), "handler cannot implement " + (d.type == ContractType.SERVICE?"service":"non-handler") + " contract");
			return;
		}
		cmds:
		for (ContractMethodDecl cmd : d.methods) {
			if (!cmd.required)
				continue;
			for (ObjectMethod m : hi.implementationMethods)
				if (m.name().name.equals(cmd.name.name))
					continue cmds;
			errors.message(hi.location(), "must implement required contract method " + cmd.name.uniqueName());
		}

	}

	@Override
	public void leaveStructDefn(StructDefn sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveObjectDefn(ObjectDefn sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveCardDefn(CardDefinition cd) {
		this.scope = scopeStack.remove(0);
		currentTemplates = null;
		referencedTemplates = null;
	}

	@Override
	public void leaveAgentDefn(AgentDefinition sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void leaveServiceDefn(ServiceDefinition sd) {
		this.scope = scopeStack.remove(0);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		RepositoryEntry defn = null;
		if (templateNestingChain != null) {
			defn = templateNestingChain.resolve(this, var);
		}
		if (defn == null)
			defn = find(scope, var.var);
		if (defn == null) {
			errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		var.bind(defn);
	}
	
	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		final RepositoryEntry defn = find(scope, operator.op);
		if (defn == null) {
			errors.message(operator.location, "cannot resolve '" + operator.op + "'");
			return;
		}
		operator.bind(defn);
	}

	@Override
	public void visitTypeReference(TypeReference ref) {
		String tn = ref.name();
		final RepositoryEntry defn = find(scope, tn);
		if (defn == null) {
			errors.message(ref.location(), "cannot resolve '" + tn + "'");
			return;
		} else if (!(defn instanceof NamedType)) {
			errors.message(ref.location(), defn.name().uniqueName() + " is not a type");
			return;
		}
		
		NamedType nt = (NamedType) defn;
		if (nt instanceof PolyHolder) {
			PolyHolder ph = (PolyHolder)nt;
			if (ph.hasPolys()) {
				List<PolyType> nd = ph.polys();
				List<TypeReference> nu = ref.polys();
				int ndp = nd.size();
				if (ndp != nu.size()) {
					errors.message(ref.location(), "expected " + ndp + " poly vars");
					return;
				}
				ErrorMark mark = errors.mark();
				List<Type> bound = new ArrayList<>();
				for (TypeReference tr : nu) {
					visitTypeReference(tr);
					if (mark.hasMoreNow())
						return;
					bound.add(tr.defn());
				}
				// it needs to bind to a polyinstance
				ref.bind(new PolyInstance(ref.location(), (NamedType) defn, bound));
				return;
			}
		}
		
		ref.bind((NamedType) defn);
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		scopeStack.add(0, scope);
		scope = cd.name();
	}
	
	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		for (TypedPattern tp : cmd.args) {
			if (tp.type.defn() instanceof ContractDecl)
				errors.message(tp.typeLocation, "method arguments may not be contracts");
		}
		if (cmd.handler != null) {
			if (!(cmd.handler.type.defn() instanceof ContractDecl))
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
			else if (((ContractDecl)cmd.handler.type.defn()).type != ContractType.HANDLER)
				errors.message(cmd.handler.typeLocation, "method handler must be a handler contract");
		}
		cmd.bindType();
	}
	
	@Override
	public void leaveContractDecl(ContractDecl cd) {
		this.scope = scopeStack.remove(0);
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		currentTemplate = t;
		currentBindings = new TreeSet<>();
		if (!isFirst && !referencedTemplates.contains(t.name().baseName()))
			errors.message(t.location(), "template " + t.name().baseName() + " has not been referenced yet");
		currentTemplates.add(t.name().baseName());
	}
	
	// TODO: this increasingly assumes that the templateReference being referred to is only the one in the definition
	// If that is the case, it should probably be renamed
	// If it is not, I think there are bugs ...
	@Override
	public void visitTemplateReference(TemplateReference refersTo, boolean isFirst, boolean isDefining) {
		TemplateName name = refersTo.name;
		RepositoryEntry t1 = repository.get(name.uniqueName());
		if (t1 == null || !(t1 instanceof Template)) {
			// if this isn't defined, I think that should have been reported elsewhere
			return;
		}
		Template template = (Template) t1;
		CardData webInfo = null;
		try {
			webInfo = repository.findWeb(name.baseName());
		} catch (NoMetaDataException ex) {
			// webInfo will be null and be caught below
		}
		if (webInfo == null) {
			errors.message(name.location(), "there is no web template defined for " + name.baseName());
			return;
		}
		if (isFirst && webInfo.type() != CardType.CARD) {
			errors.message(name.location(), "first web template must be a card template, not item " + name.baseName());
			return;
		} else if (!isFirst && webInfo.type() != CardType.ITEM) {
			errors.message(name.location(), "secondary web templates must be item templates, not card " + name.baseName());
			return;
		}
		if (!isFirst && isDefining) {
			// we want to add the item type onto the resolution chain
			// Note that it's not a strict error for it not to be there, but if it's not, you will get undefined errors if you assume it is
			// Note that this really should be a chain:
			// for templates nested inside templates, each one is in the list
			// it should also allow a var
			// basically this type is not good enough, but get there through tests
			templateNestingChain = template.nestingChain();
		}
		refersTo.bindTo(template, webInfo);
	}

	@Override
	public void leaveTemplateReference(TemplateReference refersTo, boolean isFirst, boolean isDefining) {
		if (refersTo.template() != null && refersTo.template().nestingChain() != null)
			refersTo.template().nestingChain().resolvedTypes();
	}

	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		CardData ce = currentTemplate.defines.defn();
		if (ce == null) { // an undefined template should already have been reported ...
			return;
		}
		String slot = b.assignsTo.text;
		InputPosition slotLoc = b.assignsTo.location();
		if (!ce.hasField(slot)) {
			errors.message(slotLoc, "there is no slot " + slot + " in " + currentTemplate.defines.name.baseName());
			return;
		}
		if (currentBindings.contains(slot)) {
			errors.message(slotLoc, "cannot bind to " + slot + " multiple times");
			return;
		}
		currentBindings.add(slot);
		FieldType type = ce.get(slot);
		switch (type) {
		case STYLE:
			if (b.doesAssignment())
				errors.message(slotLoc, "style field cannot be assigned to");
			break;
		case CONTENT:
			if (!b.doesAssignment())
				errors.message(slotLoc, "content field must be assigned to");
			break;
		case CONTAINER:
			if (!b.doesAssignment())
				errors.message(slotLoc, "container field must be assigned to");
			break;
		case PUNNET:
			if (!b.doesAssignment())
				errors.message(slotLoc, "container field must be assigned to");
			break;
		case CARD:
		case ITEM:
			errors.message(slotLoc, "cannot add bindings for field of type " + type.toString().toLowerCase());
			break;
		}
		b.assignsTo.fieldType(type);
//		currentBinding = new BindingInfo(b, type);
	}
	
	@Override
	public void visitTemplateBindingOption(TemplateBindingOption option) {
		if (option.sendsTo != null) {
			RepositoryEntry defn = find(scope, option.sendsTo.name.baseName());
			if (defn == null)
				errors.message(option.sendsTo.location(), "template " + option.sendsTo.name.baseName() + " is not defined");
			else if (!(defn instanceof Template))
				errors.message(option.sendsTo.location(), "cannot send to " + option.sendsTo.name.baseName() + " which is not a template");
			else {
				Template template = (Template)defn;
				CardData card = template.defines.defn();
				if (card != null && card.type() != CardType.ITEM)
					errors.message(option.sendsTo.location(), "cannot send to " + option.sendsTo.name.baseName() + " which is not an item template");
				option.sendsTo.bindTo(template, card);
			}
			referencedTemplates.add(option.sendsTo.name.baseName());
		}
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		CardData ce = currentTemplate.defines.defn();
		if (ce == null) { // an undefined template should already have been reported ...
			return;
		}
		TemplateField fld = tso.styleField;
		if (!ce.hasField(fld.text)) {
			// I _think_ it should already have been reported, but if there are no errors report it anyway ...
			if (!errors.hasErrors())
				errors.message(fld.location(), "there is no slot " + fld.text + " in " + currentTemplate.defines.name.baseName());
			return;
		}
		fld.fieldType(ce.get(fld.text));
	}
	
	@Override
	public void visitTemplateEvent(TemplateEvent te) {
		RepositoryEntry defn = find(scope, te.handler);
		boolean isEH = false;
		if (defn == null) {
			errors.message(te.location(), "cannot resolve '" + te.handler + "'");
			return;
		}
		if (defn instanceof ObjectMethod) {
			ObjectMethod om = (ObjectMethod) defn;
			if (om.isEvent()) {
				isEH = true;
				te.bindHandler(om);
			}
		}
		if (!isEH) {
			errors.message(te.location(), defn.name().uniqueName() + " is not an event handler");
			return;
		}
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		if (option.sendsTo != null) {
			RepositoryEntry template = repository.get(option.sendsTo.name.uniqueName());
			if (template instanceof Template) {
				Type ty = null;
				if (option.expr instanceof UnresolvedVar) {
					RepositoryEntry defn;
					defn = ((UnresolvedVar)option.expr).defn();
					if (defn instanceof StructField) {
						Type st = ((StructField)defn).type();
						if (st instanceof PolyInstance) {
							PolyInstance pi = (PolyInstance)st;
							NamedType pis = pi.struct();
							if (pis.equals(LoadBuiltins.list))
								st = pi.getPolys().get(0);
						}
						if (st instanceof StructDefn)
							ty = st;
						else if (st instanceof Primitive)
							ty = st;
						else // TODO: there may be other cases
							errors.message(option.expr.location(), "expected a struct value, not " + st.signature());
					} else if (defn instanceof TemplateNestedField) {
						TemplateNestedField tnf = (TemplateNestedField) defn;
						ty = tnf.type();
						if (ty == null) {
							errors.message(option.expr.location(), "cannot infer types here; explicitly type chained element " + ((UnresolvedVar)option.expr).var);
						} else if (TypeHelpers.isList(ty)) {
							ty = TypeHelpers.extractListPoly(ty);
						}
							 
					} else
						throw new NotImplementedException("not handling " + defn.getClass());
				}
				if (ty != null)
					((Template)template).canUse(ty);
			}
		}
	}

	@Override
	public void leaveTemplateBinding(TemplateBinding b) {
//		this.currentBinding = null;
	}
	
	@Override
	public void leaveTemplate(Template t) {
		currentTemplate = null;
		templateNestingChain = null;
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		scopeStack.add(0, scope);
		this.scope = e.name;
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		scopeStack.add(0, scope);
		this.scope = udd.name;
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		this.scope = scopeStack.remove(0);
		checkValidityOfUDDConstruction(udd);
	}
	
	@Override
	public void visitUnitTestShove(UnitTestShove s) {
		currShoveExpr = null;
	}
	
	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		if (currShoveExpr == null) {
			visitUnresolvedVar(v, 0);
			currShoveExpr = v;
		} else if (currShoveExpr.defn() instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) currShoveExpr.defn();
			NamedType ty = udd.ofType.defn();
			if (ty instanceof StateHolder) {
				StateHolder st = (StateHolder) ty;
				if (st.state() == null) {
					errors.message(v.location, v.var + " does not have state");
					return;
				}
				StructField f = st.state().findField(v.var);
				if (f == null) {
					errors.message(v.location, "there is no field " + v.var + " in " + ty.name().uniqueName());
					return;
				}
				v.bind(f);
				currShoveExpr = v;
			} else {
				errors.message(v.location, "cannot shove into " + v.var);
			}
		} else {
			// I'm sure there are some legitimate cases here, but we should probably also recognize that others are sensible compile-time errors
			throw new NotImplementedException("Cannot handle shove of var " + currShoveExpr.defn());
		}
	}

	@Override
	public void leaveUnitTestEvent(UnitTestEvent e) {
		resolveTargetZone(e.card.defn(), e.targetZone, "event", false);
	}

	@Override
	public void leaveUnitTestMatch(UnitTestMatch m) {
		resolveTargetZone(m.card.defn(), m.targetZone, "match", true);
	}
	
	private void resolveTargetZone(RepositoryEntry re, TargetZone tz, String type, boolean allowContainer) {
		if (tz.isWholeCard()) {
			// it's aimed at the whole card
			tz.bindTypes(new ArrayList<>());
			return;
		}
		UnitDataDeclaration udd = (UnitDataDeclaration) re;
		CardDefinition card = (CardDefinition)udd.ofType.defn();
		if (card.templates.isEmpty()) {
			errors.message(tz.location, "cannot send " + type + " to card with no templates");
			return;
		}
		// TODO: I think all (or most) of this should be extracted to "getValidEventTarget" or something very similar
		Template template = card.templates.get(0);
		CardData webInfo = template.defines.defn();
		if (webInfo == null) {
			// we failed to find the card's webinfo ... that should generate its own error
			return;
		}
		try {
			List<FieldType> types = new ArrayList<>();
			// TODO: need to traverse the fields
			String key = (String) tz.fields.get(0);
			FieldType fieldType = webInfo.get(key);
			if (fieldType != FieldType.CONTENT && fieldType != FieldType.STYLE && (!allowContainer || fieldType != FieldType.CONTAINER)) {
				errors.message(tz.location, "element " + fieldType + " '" + key + "' is not a valid " + type + " target");
				return;
			}
			types.add(fieldType);
			tz.bindTypes(types);
		} catch (NoMetaKeyException ex) {
			errors.message(tz.location, "there is no target '" + tz + "' on the card");
		}
	}
	
	@Override
	public void visitUnitTestSend(UnitTestSend s) {
		scopeStack.add(0, scope);
//		RepositoryEntry defn = find(scope, s.contract.name());
//		if (defn != null)
//			this.scope = defn.name();
		// otherwise it is left as it is, which will fail, but errors will occur when the traverser vistri
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		this.scope = scopeStack.remove(0);
	}
	
	private void checkValidityOfUDDConstruction(UnitDataDeclaration udd) {
		NamedType defn = udd.ofType.defn();
		if (defn == null) {
			if (!errors.hasErrors())
				throw new RuntimeException("the UDD type did not get resolved");
			else
				return;
		}
		if (defn instanceof ContractDecl) {
			if (udd.expr != null || !udd.fields.isEmpty()) {
				errors.message(udd.location(), "a contract data declaration may not be initialized");
			}
		} else if (defn instanceof StructDefn) {
			StructDefn sd = (StructDefn) defn;
			if (udd.expr == null && udd.fields.isEmpty() && sd.argCount() != 0) {
				errors.message(udd.location(), "either an expression or at least one field assignment must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) defn;
			if (udd.expr == null && udd.fields.isEmpty() && hi.argCount() != 0) {
				errors.message(udd.name.location, "an expression must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof ObjectDefn) {
			if (udd.expr == null) {
				errors.message(udd.name.location, "an expression must be specified for " + defn.name().uniqueName());
			}
		} else if (defn instanceof CardDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "cards may not be initialized");
			}
		} else if (defn instanceof AgentDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "agents may not be initialized");
			}
		} else if (defn instanceof ServiceDefinition) {
			if (udd.expr != null) {
				errors.message(udd.location(), "services may not be initialized");
			}
		} else
			throw new RuntimeException("udd not handled: " + defn.getClass());
	}
	
	@Override
	public void leaveUnitTest(UnitTestCase e) {
		this.scope = scopeStack.remove(0);
	}

	private RepositoryEntry find(NameOfThing s, String var) {
		if (s == null) {
			return repository.get(var);
		}
		String name = s.uniqueName() + "." + var;
		final RepositoryEntry defn = repository.get(name);
		if (defn != null)
			return defn;
		else
			return find(s.container(), var);
	}

}
