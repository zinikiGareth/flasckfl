package org.flasck.flas.resolver;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public interface NestingChain extends Iterable<TemplateNestingChain.Link> {
	RepositoryEntry resolve(RepositoryResolver resolver, UnresolvedVar var);
	void addInferred(InputPosition loc, Type ty);
	void declare(TypeReference typeReference, VarName nameVar);
	List<TypeReference> types();
	void resolvedTypes();
}
