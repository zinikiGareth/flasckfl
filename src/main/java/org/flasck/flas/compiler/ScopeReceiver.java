package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.Scope;

public interface ScopeReceiver {
	public void provideScope(Scope scope);
}
