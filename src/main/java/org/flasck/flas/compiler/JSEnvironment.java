package org.flasck.flas.compiler;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class JSEnvironment {
	// The idea is that there is one file per package
	private final Map<String, JSFile> files = new TreeMap<String, JSFile>();

	public Iterable<File> files() {
		return files.values().stream().map(jsf -> jsf.file()).collect(Collectors.toList());
	}
}
