package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;

import org.flasck.jvm.ziniki.ContentObject;

public interface JSUploader {
	ContentObject uploadJs(File f);
}
