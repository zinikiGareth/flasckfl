package org.flasck.flas.htmlzip;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SplitZip {
	public static void main(String[] argv) throws IOException {
		SplitZip sz = new SplitZip();
		final BuilderSink builder = new BuilderSink();
		MultiSink sink = new MultiSink(new StdoutSink(), builder);
		final File inf = new File("/Users/gareth/Downloads/expensesdemo.webflow.zip");
//		final File inf = new File("/Users/gareth/Downloads/demokratizatsiya.webflow.zip");
		sz.split(sink, inf);
		builder.dump();
	}
	
	public void split(Sink sink, File fromZip) throws IOException {
		Splitter splitter = new Splitter(sink);
		ZipFile zf = null;
		try {
			zf = new ZipFile(fromZip);
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = entries.nextElement();
				if (ze.isDirectory())
					continue;
				splitter.extract(zf, ze);
			}
		} finally {
			if (zf != null)
				try { zf.close(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}
}
