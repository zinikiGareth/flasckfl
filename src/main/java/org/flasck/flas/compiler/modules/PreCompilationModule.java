package org.flasck.flas.compiler.modules;

import org.flasck.flas.Configuration;

public interface PreCompilationModule {
	/**
	 * Take actions prior to the main compilation beginning.  These actions may constitute
	 * the entire operation for some combination of options.  In this case, return false.
	 * 
	 * @param config the Configuration object
	 * @return true if compilation should continue; false if completed now
	 * @throws Exception 
	 */
	boolean preCompilation(Configuration config) throws Exception;
}
