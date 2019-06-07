package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.HandlerName;

public interface HandlerNameProvider {
	HandlerName provide(String baseName);
}
