package org.flasck.flas.compiler;

import java.util.List;

public interface ScriptCompiler {

	List<Class<?>> createJVM(String pkg, String flas);

}
