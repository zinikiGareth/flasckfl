package org.flasck.flas.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.jvm.ziniki.ContentObject;
import org.flasck.jvm.ziniki.FileContentObject;
import org.flasck.jvm.ziniki.PackageSources;
import org.zinutils.utils.FileNameComparator;
import org.zinutils.utils.FileUtils;

public class FileBasedSources implements PackageSources {
	private final File dir;
	private final List<ContentObject> fls = new ArrayList<>();
	private final List<ContentObject> uts = new ArrayList<>();
	private final List<ContentObject> fas = new ArrayList<>();
	private final List<ContentObject> sts = new ArrayList<>();

	public FileBasedSources(File dir) {
		this.dir = dir;
		collect(fls, dir, "*.fl");
		collect(uts, dir, "*.ut");
		collect(fas, dir, "*.fa");
		collect(sts, dir, "*.st");
	}

	@Override
	public String getPackageName() {
		return dir.getName();
	}

	private void collect(List<ContentObject> into, File dir, String patt) {
		List<File> files = FileUtils.findFilesMatching(dir, patt);
		files.sort(new FileNameComparator());
		for (File f : files) {
			into.add(new FileContentObject(f));
		}
	}

	@Override
	public List<ContentObject> sources() {
		return fls;
	}

	@Override
	public List<ContentObject> unitTests() {
		return uts;
	}

	@Override
	public List<ContentObject> assemblies() {
		return fas;
	}

	@Override
	public List<ContentObject> systemTests() {
		return sts;
	}
}
