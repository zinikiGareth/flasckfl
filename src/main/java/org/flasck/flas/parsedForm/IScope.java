package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;

// TODO: think more clearly about the roles of IScope and Scope
// rename one or both
// consider also what should be where and what we should pass around
public interface IScope extends Iterable<Scope.ScopeEntry> {
	ScopeEntry get(String cardType);
	void define(String simpleName, Object defn);
	NameOfThing name();
	int caseName(String name);
	boolean contains(String key);
	String fullName(String name);
}
