package org.flasck.flas.compiler;

import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;

public interface HSIEFormGenerator {

	public void generate(HSIEForm form);

	TemplateGenerator templateGenerator();
}
