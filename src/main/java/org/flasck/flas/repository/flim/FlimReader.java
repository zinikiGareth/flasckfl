package org.flasck.flas.repository.flim;

import java.io.File;

import org.flasck.flas.repository.Repository;
import org.zinutils.utils.FileUtils;

public class FlimReader {

	public FlimReader(Repository repository) {
		// TODO Auto-generated constructor stub
	}

	public void read(File flimdir) {
		FileUtils.assertDirectory(flimdir);
	}

}
