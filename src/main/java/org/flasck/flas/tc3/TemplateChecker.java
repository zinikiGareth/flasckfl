package org.flasck.flas.tc3;

import java.util.Iterator;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.resolver.NestingChain;
import org.flasck.flas.resolver.TemplateNestingChain.Link;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.ziniki.splitter.FieldType;
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

	public TemplateChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, Template t) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.currentTemplate = t;
		sv.push(this);
	}

//	@Override
//	public void visitTemplateReference(TemplateReference refersTo, boolean isFirst, boolean isDefining) {
//		if (isFirst)
//			return;
//		if (isDefining) // only interested the first time through
//			return;
//	}
	
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
		FunctionGroupTCState state = new FunctionGroupTCState(repository, new DependencyGroup());
		sv.push(new ExpressionChecker(errors, repository, state, sv, true));
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
		switch (dest) {
		case CONTENT:
			if (etype == LoadBuiltins.template)
				; // It's fine to use an object template to render into a string cell
			else if (etype instanceof ObjectDefn) {
				errors.message(pos, "must use templates to render object " + etype.signature());
			} else if (!TypeHelpers.isPrimitive(etype) && option.sendsTo == null) {
				errors.message(pos, "cannot render compound object in field " + option.assignsTo.text);
			} else if (TypeHelpers.isPrimitive(etype) && option.sendsTo != null) {
				errors.message(option.sendsTo.location(), "cannot specify sendsTo operator when value is a primitive");
			}
			break;
		case CONTAINER:
			if (etype == LoadBuiltins.template)
				break; // It's fine to use an object template to render into a string cell
			else if (etype instanceof ObjectDefn) {
				errors.message(pos, "must use templates to render object " + etype.signature());
				break;
			}
			if (TypeHelpers.isPrimitive(etype)) {
				errors.message(pos, "cannot render primitive object in container " + option.assignsTo.text);
				break;
			}
			if (option.sendsTo != null && !TypeHelpers.isList(etype)) {
				errors.message(option.sendsTo.location(), "cannot specify sendsTo operator for a single item when target is a container");
				break;
			}
			if (option.sendsTo != null) { // need to test that we have compatible chains
				if (TypeHelpers.isList(etype)) {
					etype = TypeHelpers.extractListPoly(etype);
				}
				// I think by this point the fact that we are here means this must all have resolved
				Template t = option.sendsTo.template();
				NestingChain chain = t.nestingChain();
				Iterator<Link> it = chain.iterator();
				pos = option.sendsTo.location(); 
				Link first = it.next();
				// first.actualType must match what we have in mind
				if (!first.type().incorporates(pos, etype)) {
					errors.message(pos, "template '" + t.name().uniqueName() + "' requires " + first.type().signature() + " not " + etype.signature());
					break;
				}
				// we must have all its remaining context types in our context
				checkContextVars:
				while (it.hasNext()) {
					Link contextVar = it.next();
					if (currentTemplate.nestingChain() != null) {
						for (Link mine : currentTemplate.nestingChain()) {
							if (mine.type().incorporates(pos, contextVar.type()))
								continue checkContextVars;
						}
					}
					String cv = contextVar.name().var;
					if (cv != null)
						errors.message(pos, "cannot provide required context var " + cv + " required by " + option.sendsTo.name.uniqueName());
					else
						errors.message(pos, "cannot provide a template context var of type " + contextVar.type().signature() + " required by " + option.sendsTo.name.uniqueName());
				}
			}
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

	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
