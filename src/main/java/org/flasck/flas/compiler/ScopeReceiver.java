package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.IScope;

public interface ScopeReceiver {
	public void provideScope(IScope scope);
}
