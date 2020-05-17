package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.FieldType;
import org.ziniki.splitter.NoMetaKeyException;
import org.zinutils.collections.CollectionUtils;

public class TypeChecker extends LeafAdapter {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private ArrayList<String> currentTemplates;
	private ArrayList<String> referencedTemplates;
	private List<Template> allTemplates;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		new ObjectDefnChecker(errors, repository, sv, obj);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		new SingleFunctionChecker(errors, sv, repository, meth);
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		new SingleFunctionChecker(errors, sv, repository, meth);
	}
	
	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
		new GroupChecker(errors, repository, sv, new FunctionGroupTCState(repository, grp));
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		currentTemplates = new ArrayList<>();
		referencedTemplates = new ArrayList<>();
		allTemplates = cd.templates;
	}

	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		// for cards, check that the templates form a mutually-referring set
		if (!isFirst && referencedTemplates != null && !referencedTemplates.contains(t.name().baseName()))
			errors.message(t.location(), "template " + t.name().baseName() + " has not been referenced yet");
		if (currentTemplates != null)
			currentTemplates.add(t.name().baseName());
		new TemplateChecker(errors, repository, sv, t, allTemplates, referencedTemplates);
	}
	
	@Override
	public void leaveCardDefn(CardDefinition s) {
		currentTemplates = null;
		referencedTemplates = null;
		allTemplates = null;
	}

	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		new UDDChecker(errors, repository, sv);
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new UTAChecker(errors, repository, sv, a);
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect e) {
		new ExpectChecker(errors, repository, sv, e);
	}
	
	@Override
	public void visitUnitTestSend(UnitTestSend s) {
		new UTSendChecker(errors, repository, sv, s);
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
		CardData webInfo = template.webinfo();
		if (webInfo == null) {
			// we failed to find the card's webinfo ... that should generate its own error
			return;
		}
		try {
			List<FieldType> types = new ArrayList<>();
			FieldType curr = FieldType.CARD;
			Template ct = template;
			for (int i=0;i<tz.fields.size();i++) {
				Object idx = tz.fields.get(i);
				if (idx instanceof Integer) {
					if (curr != FieldType.CONTAINER) {
						errors.message(tz.location, "can only use indices to index containers");
						return;
					}
					// I feel we ought to do something here, but I'm not sure what
					// If nothing else, we ought to not allow it to go around to another index
					types.add(FieldType.ITEM);
				} else {
					curr = webInfo.get((String)idx);
					if (curr == FieldType.CONTAINER) {
						ct = findCBO(card.templates, ct, tz, (String)idx);
						if (ct == null) // will have produced an error
							return;
						webInfo = ct.webinfo();
					}
					types.add(curr);
				}
			}
			if (curr != FieldType.CONTENT && curr != FieldType.STYLE && (!allowContainer || curr != FieldType.CONTAINER)) {
				errors.message(tz.location, "element " + curr + " '" + tz + "' is not a valid " + type + " target");
				return;
			}
			tz.bindTypes(types);
		} catch (NoMetaKeyException ex) {
			errors.message(tz.location, "there is no target '" + tz + "' on the card");
		}
	}
	
	private Template findCBO(List<Template> allTemplates, Template ct, TargetZone tz, String idx) {
		Set<Template> sendsTo = new HashSet<>();
		List<Type> types = new ArrayList<>();
		for (TemplateBinding b : ct.bindings()) {
			if (!b.assignsTo.text.equals(idx))
				continue;
			for (TemplateBindingOption cb : b.conditionalBindings) {
				types.add(notList(ExpressionChecker.check(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), false, cb.expr)));
				if (cb.sendsTo != null)
					sendsTo.add(cb.sendsTo.template());
			}
			if (b.defaultBinding != null) {
				types.add(notList(ExpressionChecker.check(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), false, b.defaultBinding.expr)));
				if (b.defaultBinding.sendsTo != null) {
					sendsTo.add(b.defaultBinding.sendsTo.template());
				}
			}
		}
		if (sendsTo.isEmpty()) {
			if (types.size() == 1) {
				return selectTemplateFromCollectionBasedOnOperatingType(errors, tz.location, allTemplates, types.get(0));
			}
			errors.message(tz.location, "template " + ct.name().uniqueName() + " does not send to for " + idx);
			return null;
		}
		if (sendsTo.size() > 1) {
			errors.message(tz.location, idx + " is ambiguous for template " + ct.name().uniqueName());
			return null;
		}
		return CollectionUtils.nth(sendsTo, 0);
	}

	private Type notList(Type t) {
		if (TypeHelpers.isList(t))
			return TypeHelpers.extractListPoly(t);
		else
			return t;
	}

	public static Template selectTemplateFromCollectionBasedOnOperatingType(ErrorReporter errors, InputPosition pos, Iterable<Template> allTemplates, Type ty) {
		Template ret = null;
		for (Template t : allTemplates) {
			if (t.nestingChain() == null)
				continue;
			if (t.nestingChain().iterator().next().type().incorporates(pos, ty)) {
				// TODO: check we can handle rest of chain
				
				if (ret != null) {
					errors.message(pos, "ambiguous templates for " + ty.signature());
				}
				ret = t;
			}
		}
		if (ret == null) {
			errors.message(pos, "there is no compatible template for " + ty.signature());
		}
		return ret;
	}
}
