package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.tc3.NamedType;

public interface EventHolder extends NamedType, StateHolder {
	List<Template> templates();
}
