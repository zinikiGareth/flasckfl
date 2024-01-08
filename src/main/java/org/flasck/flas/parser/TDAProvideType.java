package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.TypeReference;

@FunctionalInterface
public interface TDAProvideType {
	void provide(TypeReference ty);
}
