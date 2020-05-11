package org.flasck.flas.tc3;

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
	private Mode mode;
	private InputPosition eloc;
	private ExprResult exprType;

	public TemplateChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, Template t) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
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
			if (!ty.type.equals(LoadBuiltins.string) && !isListString(ty.type))
				errors.message(eloc, "styles must be strings or lists of strings");
		} else
			exprType = ty;
		mode = null;
	}
	
	// I feel that this may be useful enough to possibly be more central
	private boolean isListString(Type type) {
		if (!(type instanceof PolyInstance))
			return false;
		
		PolyInstance pi = (PolyInstance) type;
		if (!pi.struct().equals(LoadBuiltins.list))
			return false;
		
		Type ty = pi.getPolys().get(0);
		if (!ty.equals(LoadBuiltins.string))
			return false;
		
		return true;
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		FieldType dest = option.assignsTo.type();
		switch (dest) {
		case CONTENT:
			if (exprType.type == LoadBuiltins.template)
				; // It's fine to use an object template to render into a string cell
			else if (exprType.type instanceof ObjectDefn) {
				errors.message(exprType.location(), "must use templates to render object " + exprType.type.signature());
			} else if (!isPrimitive(exprType.type) && option.sendsTo == null) {
				errors.message(exprType.location(), "cannot render compound object in field " + option.assignsTo.text);
			} else if (isPrimitive(exprType.type) && option.sendsTo != null) {
				errors.message(option.sendsTo.location(), "cannot specify sendsTo operator when value is a primitive");
			}
			break;
		case CONTAINER:
			if (exprType.type == LoadBuiltins.template)
				break; // It's fine to use an object template to render into a string cell
			else if (exprType.type instanceof ObjectDefn) {
				errors.message(exprType.location(), "must use templates to render object " + exprType.type.signature());
				break;
			}
			if (isPrimitive(exprType.type)) {
				errors.message(exprType.location(), "cannot render primitive object in container " + option.assignsTo.text);
			}
			if (option.sendsTo != null) {
				errors.message(option.sendsTo.location(), "cannot specify sendsTo operator when target is a container");
			}
			break;
		case PUNNET:
			// basically you should have to use a Crobag, but it may be possible to have one card or even a list of cards
			// but any such thing would have to be a "card handle" and I don't think we have a type for that
			if (!LoadBuiltins.crobag.equals(exprType.type))
				errors.message(exprType.location(), "cannot render " + exprType.type.signature() + " in punnet");
			break;
		default:
			errors.message(option.assignsTo.location(), "cannot handle dest type " + dest);
			break;
		}
	}
	
	private boolean isPrimitive(Type type) {
		if (type instanceof Primitive)
			return true;
		else
			return false;
	}

	@Override
	public void leaveTemplate(Template t) {
		sv.result(null);
	}
}
