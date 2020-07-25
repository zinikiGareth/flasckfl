package org.flasck.flas.grammar;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class SentenceData {
	public final File file;
	public final Set<String> productionsUsed;
	public final Map<String, String> matchers;

	public SentenceData(Set<String> productionsUsed, Map<String, String> matchers, File file) {
		this.file = file;
		this.productionsUsed = productionsUsed;
		this.matchers = matchers;
	}
	
	@Override
	public String toString() {
		return file + "\n" + productionsUsed + "\n" + matchers;
	}
}
