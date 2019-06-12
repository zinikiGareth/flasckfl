package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;

// TODO: think more clearly about the roles of IScope and Scope
// rename one or both
// consider also what should be where and what we should pass around
@Deprecated
public interface IScope extends Iterable<Scope.ScopeEntry> {
	ScopeEntry get(String cardType);
	void define(ErrorReporter er, String simpleName, Locatable defn);
	NameOfThing name();
	int caseName(String name);
	boolean contains(String key);
	String fullName(String name);
	int size();
}
