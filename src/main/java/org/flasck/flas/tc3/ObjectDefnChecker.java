package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class ObjectDefnChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final String fnCxt;
	private final List<Template> allTemplates;
	private final boolean checkReferencing;
	private ExprResult result;
	private final List<String> currentTemplates = new ArrayList<String>();
	private final List<String> referencedTemplates = new ArrayList<String>();

	public ObjectDefnChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, List<Template> templates, boolean checkReferencing) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		allTemplates = templates;
		this.checkReferencing = checkReferencing;
		sv.push(this);
	}

	@Override
	public void visitStructField(StructField sf) {
		result = null;
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}
	
	@Override
	public void leaveStructField(StructField sf) {
		if (result == null)
			return;
		
		if (!sf.type().incorporates(result.pos, result.type))
			errors.message(result.pos, "cannot initialize " + sf.name +" from " + result.type.signature());
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		// for cards, check that the templates form a mutually-referring set
		if (!isFirst && checkReferencing && !referencedTemplates.contains(t.name().baseName()))
			errors.message(t.location(), "template " + t.name().baseName() + " has not been referenced yet");
		currentTemplates.add(t.name().baseName());
		new TemplateChecker(errors, repository, sv, t, allTemplates, referencedTemplates);
	}
	
	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
		sv.result(null);
	}

	@Override
	public void leaveCardDefn(CardDefinition obj) {
		sv.result(null);
	}

	@Override
	public void result(Object r) {
		result = (ExprResult) r;
	}
}
