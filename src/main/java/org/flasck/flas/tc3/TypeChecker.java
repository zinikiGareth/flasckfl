package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

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
}
