package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.HandlerName;

public interface HandlerNameProvider {
	HandlerName handlerName(String baseName);
}
