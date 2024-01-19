package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAStructFieldParser implements TDAParsing {
	private final ErrorReporter errors;
	private final FieldsDefn inside;
	private final StructFieldConsumer builder;
	private final FieldsType fieldsType;
	private final boolean createAsAccessors;

	public TDAStructFieldParser(ErrorReporter errors, FieldsDefn inside, StructFieldConsumer builder, FieldsType fieldsType, boolean createAsAccessors) {
		this.errors = errors;
		this.inside = inside;
		this.builder = builder;
		this.fieldsType = fieldsType;
		this.createAsAccessors = createAsAccessors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		final InputPosition startLine = toks.realinfo();
		TypeReference type = null;
		if (fieldsType != FieldsType.WRAPS) {
			TypeExprParser parser = new TypeExprParser(errors);
			List<TypeReference> types = new ArrayList<>();
			TDAProvideType pt = ty -> types.add(ty);
			parser.tryParsing(toks, pt);
			if (types.isEmpty()) {
				errors.message(toks, "field must have a valid type definition");
				return new IgnoreNestedParser(errors);
			}
			type = types.get(0);
		}
		ValidIdentifierToken field = VarNameToken.from(errors, toks);
		if (field == null) {
			errors.message(toks, "field must have a valid field name");
			return new IgnoreNestedParser(errors);
		}
		if (field.text.equals("id")) {
			errors.message(toks, "'id' is a reserved field name");
			return new IgnoreNestedParser(errors);
		}
		ReturnParser ret = new ReturnParser(errors);
		if (!toks.hasMoreContent(errors)) {
			if (fieldsType == FieldsType.WRAPS) {
				errors.message(toks, "wraps fields must have initializers");
				return new IgnoreNestedParser(errors);
			}
			errors.logReduction("struct-field-no-initialization", type.location(), toks.realinfo());
			builder.addField(new StructField(field.location, builder.holder(), createAsAccessors, true, type, field.text));
			ret.noNest(errors);
		} else {
			if (fieldsType == FieldsType.ENVELOPE) {
				errors.message(toks, "envelope fields may not have initializers");
				return new IgnoreNestedParser(errors);
			}
			toks.skipWS(errors);
			ExprToken arrow = ExprToken.from(errors, toks);
			if (!"<-".equals(arrow.text)) {
				errors.message(toks, "expected <- or end of line");
				return new IgnoreNestedParser(errors);
			}
			TypeReference ft = type;
			new TDAExpressionParser(errors, expr -> {
				if (errors.hasErrors()) {
					ret.ignore();
				} else {
					ret.noNest(errors);
					errors.logReduction("struct-field-with-initialization", startLine, toks.realinfo());
					builder.addField(new StructField(field.location, arrow.location, builder.holder(), createAsAccessors, true, ft, field.text, expr));
				}
			}).tryParsing(toks);
			if (toks.hasMoreContent(errors))
				errors.message(toks, "invalid tokens after expression");
			if (errors.hasErrors())
				return new IgnoreNestedParser(errors);
		}
		return ret.get();
	}

	@Override
	public void scopeComplete(InputPosition location) {
		errors.logReduction("fields-defn-with-fields", inside.kw, location);
	}
}
