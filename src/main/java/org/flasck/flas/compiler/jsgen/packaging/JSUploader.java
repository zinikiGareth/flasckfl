package org.flasck.flas.compiler.jsgen.packaging;

import java.io.IOException;

import org.flasck.jvm.ziniki.ContentObject;

public interface JSUploader {
	void uploadFlim(ContentObject co) throws IOException;
	ContentObject uploadJs(ContentObject co) throws IOException;
}
