package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain.Link;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.CardType;
import org.ziniki.splitter.FieldType;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;

public class TemplateChecker extends LeafAdapter implements ResultAware {
	public enum Mode {
		COND, BINDEXPR, STYLEEXPR
	}

	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final Template currentTemplate;
	private Mode mode;
	private InputPosition eloc;
	private ExprResult exprType;
	private List<String> referencedTemplates;
	private List<Template> allTemplates;
	private final String fnCxt;
	private FunctionGroupTCState state;

	public TemplateChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, Template t, List<Template> allTemplates, List<String> referencedTemplates) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.currentTemplate = t;
		this.allTemplates = allTemplates;
		this.referencedTemplates = referencedTemplates;
		this.fnCxt = t.name().uniqueName();
		state = new FunctionGroupTCState(repository, new DependencyGroup());
		sv.push(this);
	}

	@Override
	public void visitTemplateBindingCondition(Expr cond) {
		mode = Mode.COND;
		eloc = cond.location();
	}
	
	@Override
	public void visitTemplateBindingExpr(Expr expr) {
		mode = Mode.BINDEXPR;
		eloc = expr.location();
	}
	
	@Override
	public void visitTemplateStyleCond(Expr cond) {
		mode = Mode.COND;
		eloc = cond.location();
	}

	@Override
	public void visitTemplateStyleExpr(Expr style) {
		mode = Mode.STYLEEXPR;
		eloc = style.location();
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (mode == null)
			throw new NotImplementedException("was not in a mode capable of handling expr");
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, true));
	}
	
	@Override
	public void result(Object r) {
		ExprResult ty = (ExprResult) r;
		if (mode == Mode.COND) {
			if (!ty.type.equals(LoadBuiltins.bool) && !ty.type.equals(LoadBuiltins.trueT) && !ty.type.equals(LoadBuiltins.falseT))
				errors.message(eloc, "conditions must be Boolean");
		} else if (mode == Mode.STYLEEXPR) {
			// I think it's also OK for style expressions to be lists, but we don't have that case yet :-)
			if (!ty.type.equals(LoadBuiltins.string) && !TypeHelpers.isListString(ty.type))
				errors.message(eloc, "styles must be strings or lists of strings");
		} else {
			exprType = ty;
		}
		mode = null;
	}
	
	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		FieldType dest = option.assignsTo.type();
		InputPosition pos = exprType.location();
		Type etype = exprType.type;
		if (etype instanceof ErrorType)
			return;
		switch (dest) {
		case CONTENT:
			if (option.sendsTo != null) {
				if (referencedTemplates != null)
					referencedTemplates.add(option.sendsTo.name.baseName());
			}
			if (etype instanceof ObjectDefn) {
				if (option.sendsTo == null || option.sendsTo.template() == null) {
					errors.message(pos, "must use templates to render object " + etype.signature());
					break;
				}
				NameOfThing tdb = option.sendsTo.template().name();
				NameOfThing obj = ((ObjectDefn)etype).name();
				if (!obj.equals(tdb.container())) {
					errors.message(pos, "cannot use template '" + tdb.uniqueName() + "' to render object of type '" + obj.uniqueName());
					break;
				}
			} else if (option.sendsTo == null) {
				String msg = "cannot render compound object in field " + option.assignsTo.text;
				if (etype instanceof UnifiableType)
					((UnifiableType)etype).requirePrimitive(pos, msg);
				else if (!TypeHelpers.isPrimitive(etype))
					errors.message(pos, msg);
			} else {
				String msg = "cannot specify sendsTo operator when value is a primitive";
				if (etype instanceof UnifiableType)
					((UnifiableType)etype).requireNonPrimitive(pos, msg);
				else if (TypeHelpers.isPrimitive(etype))
					errors.message(option.sendsTo.location(), msg);
			} 
			break;
		case CONTAINER:
			if (option.sendsTo != null) {
				CardData card = option.sendsTo.template().webinfo();
				if (card != null && card.type() != CardType.ITEM) {
					errors.message(option.sendsTo.location(), "cannot send to " + option.sendsTo.name.baseName() + " which is not an item template");
					break;
				}
				if (referencedTemplates != null)
					referencedTemplates.add(option.sendsTo.name.baseName());
			}
			if (etype instanceof UnifiableType) {
				((UnifiableType)etype).callOnResolved(res -> processContainerType(pos, option, res));
				return;
			} else if (etype instanceof Apply) {
				throw new NotImplementedException("you should have thrown an error before getting here: " + etype.signature());
			}
			processContainerType(pos, option, etype);
			break;
		case PUNNET:
			// basically you should have to use a Crobag, but it may be possible to have one card or even a list of cards
			// but any such thing would have to be a "card handle" and I don't think we have a type for that
			if (!LoadBuiltins.crobag.equals(etype)) {
				errors.message(pos, "cannot render " + etype.signature() + " in punnet");
				break;
			}
			break;
		default:
			errors.message(option.assignsTo.location(), "cannot handle dest type " + dest);
			break;
		}
	}

	private void processContainerType(InputPosition pos, TemplateBindingOption option, Type etype) {
		// REFACTOR !!!!!!!
		if (etype instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) etype;
			if (od.name().uniqueName().equals("Crobag")) {
				// Copied and hacked from below
				System.out.println("need to handle crobags in a similar fashion to lists");
				etype = repository.get("org.zinapps.samples.earthquakes.Earthquake");
				Map<NamedType, Template> mapping = new HashMap<>();
				Template which = TypeChecker.selectTemplateFromCollectionBasedOnOperatingType(errors, pos, allTemplates, etype);
				if (which == null) {
					errors.message(pos, "there is no compatible template for " + etype.signature());
				} else {
					referencedTemplates.add(which.name().baseName());
					mapping.put((StructDefn) etype, which);
				}
				option.attachMapping(mapping);
				return;
			}
			if (option.sendsTo == null || option.sendsTo.template() == null) {
				errors.message(pos, "must use templates to render object " + etype.signature());
				return;
			}
			NameOfThing tdb = option.sendsTo.template().name();
			NameOfThing obj = ((ObjectDefn)etype).name();
			if (!obj.equals(tdb.container())) {
				errors.message(pos, "cannot use template '" + tdb.uniqueName() + "' to render object of type '" + obj.uniqueName());
				return;
			}
		}
		if (TypeHelpers.isPrimitive(etype)) {
			String msg = "cannot render primitive object in container " + option.assignsTo.text;
			errors.message(pos, msg);
			return;
		}
		if (option.sendsTo != null && !TypeHelpers.isList(etype)) {
			errors.message(option.sendsTo.location(), "cannot specify sendsTo operator for a single item when target is a container");
			return;
		}
		// GP 2020-09-25: since Lists are a union, surely these two cases are back to front?
		if (option.sendsTo == null && etype instanceof UnionTypeDefn) {
			Map<NamedType, Template> mapping = new HashMap<>();
			distributeUnion(pos, etype, mapping);
			option.attachMapping(mapping);
		} else if (option.sendsTo == null && TypeHelpers.isList(etype)) {
			/* In this case, we have specified a list of items we want rendered into a container, 
			 * but we haven't specified how we want it done.  In that case, the items must be of a type
			 * that we can identify and must either be STRUCT or UNION.
			 * We can then go through each one and find the appropriate template to use, checking that it is compatible,
			 * marking it as used, and recording the fact that we want to do this for this template (or something)
			 */
			etype = TypeHelpers.extractListPoly(etype);
			if (etype instanceof PolyInstance)
				etype = ((PolyInstance)etype).struct();
			Map<NamedType, Template> mapping = new HashMap<>();
			if (etype instanceof StructDefn) {
				Template which = TypeChecker.selectTemplateFromCollectionBasedOnOperatingType(errors, pos, allTemplates, etype);
				if (which == null) {
					errors.message(pos, "there is no compatible template for " + etype.signature());
				} else {
					referencedTemplates.add(which.name().baseName());
					mapping.put((StructDefn) etype, which);
				}
			} else if (etype instanceof UnionTypeDefn) {
				distributeUnion(pos, etype, mapping);
			} else if (etype instanceof UnifiableType) {
				errors.message(pos, "the compiler is not clever enough to do the analysis required to figure out which template to send elements to; please specify a template or make your types clearer");
			} else {
				// TODO: note that this could also be a PolyInstance of one of these ...
				throw new HaventConsideredThisException("must be struct or union, not " + etype);
			}
			option.attachMapping(mapping);
		} else if (option.sendsTo != null) { // need to test that we have compatible chains
			if (TypeHelpers.isList(etype)) {
				etype = TypeHelpers.extractListPoly(etype);
			}
			// I think by this point the fact that we are here means this must all have resolved
			Template t = option.sendsTo.template();
			NestingChain chain = t.nestingChain();
			Iterator<Link> it = chain.iterator();
			pos = option.sendsTo.location();
			{
				Link first = it.next();
				Type lp = null;
				if (TypeHelpers.isList(first.type()))
					lp = TypeHelpers.extractListPoly(first.type());
				// first.actualType must match what we have in mind
				if (!first.type().incorporates(pos, etype) && !(lp != null && lp.incorporates(pos, etype))) {
					errors.message(pos, "template '" + t.name().uniqueName() + "' requires " + first.type().signature() + " not " + etype.signature());
					return;
				}
			}
			// we must have all its remaining context types in our context
			List<Integer> posns = new ArrayList<>();
			checkContextVars:
			while (it.hasNext()) {
				Link contextVar = it.next();
				if (currentTemplate.nestingChain() != null) {
					int mp = 0;
					for (Link mine : currentTemplate.nestingChain()) {
						if (mine.type().incorporates(pos, contextVar.type())) {
							posns.add(mp);
							continue checkContextVars;
						}
						mp++;
					}
				}
				String cv = contextVar.name().var;
				if (cv != null)
					errors.message(pos, "cannot provide required context var " + cv + " required by " + option.sendsTo.name.uniqueName());
				else
					errors.message(pos, "cannot provide a template context var of type " + contextVar.type().signature() + " required by " + option.sendsTo.name.uniqueName());
			}
			option.sendsTo.bindPosns(posns);
		} else {
			errors.message(pos, "cannot identify a suitable template to format value into container " + option.assignsTo.text);
		}
	}

	private void distributeUnion(InputPosition pos, Type etype, Map<NamedType, Template> mapping) {
		Template which = TypeChecker.selectTemplateFromCollectionBasedOnOperatingType(errors, pos, allTemplates, etype);
		if (which != null) {
			referencedTemplates.add(which.name().baseName());
			mapping.put((NamedType) etype, which);
		} else {
			List<String> missing = new ArrayList<>();
			for (TypeReference ty : ((UnionTypeDefn)etype).cases) {
				NamedType td = ty.defn();
				if (td instanceof PolyInstance)
					td = ((PolyInstance)td).struct();
				StructDefn sd = (StructDefn)td;
				Template wh2 = TypeChecker.selectTemplateFromCollectionBasedOnOperatingType(errors, pos, allTemplates, sd);
				if (wh2 == null) {
					missing.add(sd.signature());
				} else {
					referencedTemplates.add(wh2.name().baseName());
					mapping.put(sd, wh2);
				}
			}
			if (!missing.isEmpty())
				errors.message(pos, "cannot distribute across " + etype.signature() + " because not all members have defined cases; missing cases for: " + String.join(", ", missing));
		}
	}

	@Override
	public void leaveTemplate(Template t) {
		state.groupDone(errors, new HashMap<>(), new HashMap<>());
		sv.result(null);
	}
}
