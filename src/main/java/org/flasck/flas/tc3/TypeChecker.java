package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventHolder;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TargetZone.Qualifier;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestClose;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestExpectCancel;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.parsedForm.ut.UnitTestInput;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.FieldType;
import org.ziniki.splitter.NoMetaKeyException;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class TypeChecker extends LeafAdapter {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final ErrorMark mark;
	private String utName;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
		mark = errors.mark();
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
		new ObjectDefnChecker(errors, repository, sv, obj.name().uniqueName(), obj.templates, false);
	}

	@Override
	public void visitCardDefn(CardDefinition cd) {
		new ObjectDefnChecker(errors, repository, sv, cd.name().uniqueName(), cd.templates, true);
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
		new GroupChecker(errors, repository, sv, new FunctionGroupTCState(repository, grp), mark);
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		utName = e.name.uniqueName();
	}
	
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
		new UDDChecker(errors, repository, sv, utName);
	}

	@Override
	public void visitUnitTestInvoke(UnitTestInvoke uti) {
		new UTIChecker(errors, repository, sv, utName);
	}
	
	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		new UTAChecker(errors, repository, sv, utName);
	}
	
	@Override
	public void visitUnitTestIdentical(UnitTestIdentical a) {
		new UTAChecker(errors, repository, sv, utName);
	}

	@Override
	public void visitUnitTestShove(UnitTestShove s) {
		new ShoveChecker(errors, repository, sv, utName);
	}

	@Override
	public void visitUnitTestExpect(UnitTestExpect e) {
		new ExpectChecker(errors, repository, sv, utName, e);
	}

	@Override
	public void visitUnitTestClose(UnitTestClose utc) {
		new CloseCardChecker(errors, repository, sv, utName, utc);
	}

	@Override
	public void visitUnitTestExpectCancel(UnitTestExpectCancel e) {
		new ExpectCancelChecker(errors, repository, sv, utName, e);
	}

	@Override
	public void visitUnitTestSend(UnitTestSend s) {
		new UTSendChecker(errors, repository, sv, utName, s);
	}

	@Override
	public void visitUnitTestEvent(UnitTestEvent e) {
		new UTEventChecker(errors, repository, sv, utName, e);
	}
	
	@Override
	public void visitUnitTestInput(UnitTestInput e) {
		new UTInputChecker(errors, repository, sv, utName, e);
	}
	
	@Override
	public void leaveUnitTestMatch(UnitTestMatch m) {
		resolveTargetZone(errors, repository, m.card.defn(), m.targetZone, "match", "match text in", true);
	}

	public static void resolveTargetZone(ErrorReporter errors, RepositoryReader repository, RepositoryEntry re, TargetZone tz, String type, String msg, boolean allowContainer) {
		if (tz.isWholeCard()) {
			// it's aimed at the whole card
			tz.bindTypes(new ArrayList<>());
			return;
		}
		UnitDataDeclaration udd = (UnitDataDeclaration) re;
		EventHolder card = (EventHolder) udd.ofType.namedDefn();
		if (card.templates().isEmpty()) {
			errors.message(tz.location, "cannot " + msg + " card with no templates");
			return;
		}
		// TODO: I think all (or most) of this should be extracted to
		// "getValidEventTarget" or something very similar
		Template template = card.templates().get(0);
		CardData webInfo = template.webinfo();
		if (webInfo == null) {
			// we failed to find the card's webinfo ... that should generate its own error
			return;
		}
		try {
			List<FieldType> types = new ArrayList<>();
			FieldType curr = FieldType.CARD;
			TemplateOrError ct = new TemplateOrError(template, webInfo);
			for (int i = 0; i < tz.fields.size(); i++) {
				Object part = tz.fields.get(i);
				if (part instanceof Integer) {
					if (curr != FieldType.CONTAINER) {
						errors.message(tz.location, "can only use indices to index containers");
						return;
					}
					// I feel we ought to do something here, but I'm not sure what
					// If nothing else, we ought to not allow it to go around to another index
					types.add(FieldType.ITEM);
				} else if (part instanceof Qualifier) {
					if (!ct.hasError()) 
						throw new NotImplementedException("the user has provided an explicit resolution but it isn't needed - we should check here");
					Qualifier q = (Qualifier) part;
					String tn = q.qualifyingTemplate;
					Type ty = ct.forType();
					if (!(ty instanceof UnionTypeDefn))
						throw new NotImplementedException("we are kind of assuming here that this is a Union, but if not ...");
					UnionTypeDefn u = (UnionTypeDefn) ty;
					Template chosen = null;
					for (Template t : card.templates()) {
						if (t.name().baseName().equals(tn)) {
							chosen = t;
						}
					}
					if (chosen == null) {
						errors.message(q.location(), "there is no template called '" + tn + "'");
						return;
					}
					if (chosen.nestingChain() == null) {
						errors.message(q.location(), "the template '" + tn + "' does not have a nesting chain, cannot be used as a qualifier");
						return;
					}
					Type tnc = chosen.nestingChain().iterator().next().type();
					if (!(tnc instanceof StructDefn) || !(u.hasCase((StructDefn) tnc))) {
						errors.message(q.location, "template '" + tn + "' expects type " + tnc + " which is not part of the union " + u);
						return;
					}
					ct = new TemplateOrError(chosen, chosen.webinfo());
					// I'm not 100% sure where this comes from in this case
					types.add(FieldType.CONTAINER);
				} else if (part instanceof String) {
					curr = ct.webInfo().get((String) part);
					if (curr == FieldType.CONTAINER) {
						ct = findCBO(errors, repository, card.templates(), ct.template(), tz, (String) part);
						if (ct == null) // will have produced an error
							return;
					}
					types.add(curr);
				} else {
					throw new HaventConsideredThisException("part is a " + part.getClass());
				}
			}
			if (curr != FieldType.CONTENT && curr != FieldType.IMAGE && curr != FieldType.LINK && curr != FieldType.STYLE && (!allowContainer || curr != FieldType.CONTAINER)) {
				errors.message(tz.location, "element " + curr + " '" + tz + "' is not a valid " + type + " target");
				return;
			}
			tz.bindTypes(types);
		} catch (NoMetaKeyException ex) {
			errors.message(tz.location, "there is no target '" + tz + "' on the card");
		}
	}

	private static TemplateOrError findCBO(ErrorReporter errors, RepositoryReader repository, List<Template> allTemplates, Template ct, TargetZone tz, String slot) {
		Set<Template> sendsTo = new HashSet<>();
		List<Type> types = new ArrayList<>();
		for (TemplateBinding b : ct.bindings()) {
			if (!b.assignsTo.text.equals(slot))
				continue;
			for (TemplateBindingOption cb : b.conditionalBindings) {
				types.add(notList(ExpressionChecker.check(errors, repository,
						new FunctionGroupTCState(repository, new DependencyGroup()), ct.name().uniqueName(), false, cb.expr)));
				if (cb.sendsTo != null)
					sendsTo.add(cb.sendsTo.template());
			}
			if (b.defaultBinding != null) {
				types.add(notList(ExpressionChecker.check(errors, repository,
						new FunctionGroupTCState(repository, new DependencyGroup()), ct.name().uniqueName(), false, b.defaultBinding.expr)));
				if (b.defaultBinding.sendsTo != null) {
					sendsTo.add(b.defaultBinding.sendsTo.template());
				}
			}
		}
		if (sendsTo.isEmpty()) {
			if (types.size() == 1) {
				Template ret = selectTemplateFromCollectionBasedOnOperatingType(errors, tz.location, allTemplates, types.get(0));
				if (ret == null) {
					return new TemplateOrError(new FLASError(tz.location, "could not find a template for " + types.get(0).signature()), types.get(0));
				}
				return new TemplateOrError(ret, ret.webinfo());
			} else {
				errors.message(tz.location, "template " + ct.name().uniqueName() + " does not send to for " + slot);
				return null;
			}
		}
		if (sendsTo.size() > 1) {
			errors.message(tz.location, slot + " is ambiguous for template " + ct.name().uniqueName());
			return null;
		}
		Template ret = CollectionUtils.nth(sendsTo, 0);
		return new TemplateOrError(ret, ret.webinfo());
	}

	private static Type notList(Type t) {
		if (TypeHelpers.isListLike(t))
			return TypeHelpers.extractListPoly(t);
		else
			return t;
	}

	public static Template selectTemplateFromCollectionBasedOnOperatingType(ErrorReporter errors, InputPosition pos, Iterable<Template> allTemplates, Type forType) {
		Template ret = null;
		for (Template t : allTemplates) {
			if (t.nestingChain() == null)
				continue;
			if (t.nestingChain().iterator().next().type().incorporates(pos, forType)) {
				// TODO: check we can handle rest of chain

				if (ret != null) {
					errors.message(pos, "ambiguous templates for " + forType.signature());
				}
				ret = t;
			}
		}
		return ret;
	}

	public static PosType instantiateFreshPolys(Expr tmp, CurrentTCState state, Map<String, UnifiableType> uts,	PosType post, boolean nested) {
		InputPosition pos = post.pos;
		Type type = post.type;
		logger.info("instantiating fresh polys in " + type + " with a current map of " + uts);
		if (type instanceof PolyType) {
			PolyType pt = (PolyType) type;
			if (state.hasPoly(pt))
				return logit(new PosType(pos, state.getPoly(pt)));
			else if (uts.containsKey(pt.shortName()))
				return logit(new PosType(pos, uts.get(pt.shortName())));
			else {
				UnifiableType ret = state.createUT(null, "instantiating " + tmp + "." + pt.shortName());
				uts.put(pt.shortName(), ret);
				return logit(new PosType(pos, ret));
			}
		} else if (type instanceof Apply) {
			Apply a = (Apply) type;
			List<Type> types = new ArrayList<>();
			for (Type t : a.tys)
				types.add(instantiateFreshPolys(tmp, state, uts, new PosType(pos, t), true).type);
			return logit(new PosType(pos, new Apply(types).withHandler(a.withHandler())));
		} else if (type instanceof PolyHolder && ((PolyHolder) type).hasPolys()) {
			PolyHolder sd = (PolyHolder) type;
			List<Type> polys = new ArrayList<>();
			for (Type t : sd.polys())
				polys.add(instantiateFreshPolys(tmp, state, uts, new PosType(pos, t), true).type);
			PolyInstance pi = new PolyInstance(pos, sd, polys);
			if (!nested && type instanceof FieldsDefn) {
				List<Type> types = new ArrayList<>();
				for (StructField sf : ((FieldsDefn) type).fields)
					types.add(instantiateFreshPolys(tmp, state, uts, new PosType(pos, sf.type.namedDefn()), true).type);
				if (types.isEmpty())
					return logit(new PosType(pos, pi));
				else
					return logit(new PosType(pos, new Apply(types, pi)));
			} else {
				return new PosType(pos, pi);
			}
		} else if (type instanceof PolyInstance) {
			PolyInstance inst = (PolyInstance) type;
			List<Type> polys = new ArrayList<>();
			for (Type t : inst.polys())
				polys.add(instantiateFreshPolys(tmp, state, uts, new PosType(pos, t), true).type);
			PolyInstance pi = new PolyInstance(pos, inst.struct(), polys);
			if (type instanceof FieldsDefn) {
				List<Type> types = new ArrayList<>();
				for (StructField sf : ((FieldsDefn) type).fields)
					types.add(instantiateFreshPolys(tmp, state, uts, new PosType(pos, sf.type.namedDefn()), true).type);
				if (types.isEmpty())
					return logit(new PosType(pos, pi));
				else
					return logit(new PosType(pos, new Apply(types, pi)));
			} else {
				return logit(new PosType(pos, pi));
			}
		} else
			return logit(post);
	}

	private static PosType logit(PosType ret) {
		logger.info("instantiated type is " + ret);
		return ret;
	}
}
