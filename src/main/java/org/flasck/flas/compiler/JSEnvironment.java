package org.flasck.flas.compiler;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The idea here is to create a set of "package" files in memory with abstract constructs.
 * We then have a method to iterate over all of them and turn that into JS files in one go.
 * 
 * @author gareth
 */
public class JSEnvironment implements JSStorage {
	// The idea is that there is one file per package
	private final Map<String, JSFile> files = new TreeMap<String, JSFile>();

	public Iterable<File> files() {
		return files.values().stream().map(jsf -> jsf.file()).collect(Collectors.toList());
	}

	@Override
	public JSClassCreator newClass(String pkg, String clz) {
		return null;
	}
}
