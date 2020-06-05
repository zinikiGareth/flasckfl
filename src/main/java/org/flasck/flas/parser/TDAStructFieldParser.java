package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAStructFieldParser implements TDAParsing {
	private final ErrorReporter errors;
	private final StructFieldConsumer builder;
	private final FieldsType fieldsType;
	private final boolean createAsAccessors;

	public TDAStructFieldParser(ErrorReporter errors, StructFieldConsumer builder, FieldsType fieldsType, boolean createAsAccessors) {
		this.errors = errors;
		this.builder = builder;
		this.fieldsType = fieldsType;
		this.createAsAccessors = createAsAccessors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		TypeReference type = null;
		if (fieldsType != FieldsType.WRAPS) {
			type = (TypeReference) new TypeExprParser().tryParsing(toks);
			if (type == null) {
				errors.message(toks, "field must have a valid type definition");
				return new IgnoreNestedParser();
			}
		}
		ValidIdentifierToken field = VarNameToken.from(toks);
		if (field == null) {
			errors.message(toks, "field must have a valid field name");
			return new IgnoreNestedParser();
		}
		if (field.text.equals("id")) {
			errors.message(toks, "'id' is a reserved field name");
			return new IgnoreNestedParser();
		}
		ReturnParser ret = new ReturnParser();
		if (!toks.hasMoreContent()) {
			if (fieldsType == FieldsType.WRAPS) {
				errors.message(toks, "wraps fields must have initializers");
				return new IgnoreNestedParser();
			}
			builder.addField(new StructField(field.location, builder.holder(), createAsAccessors, type, field.text));
			ret.noNest(errors);
		} else {
			if (fieldsType == FieldsType.ENVELOPE) {
				errors.message(toks, "envelope fields may not have initializers");
				return new IgnoreNestedParser();
			}
			toks.skipWS();
			InputPosition assOp = toks.realinfo();
			String op = toks.getTo(2);
			if (!"<-".equals(op)) {
				errors.message(toks, "expected <- or end of line");
				return new IgnoreNestedParser();
			}
			assOp.endAt(toks.at());
			TypeReference ft = type;
			new TDAExpressionParser(errors, expr -> {
				if (errors.hasErrors()) {
					ret.ignore();
				} else {
					ret.noNest(errors);
					builder.addField(new StructField(field.location, assOp, builder.holder(), createAsAccessors, ft, field.text, expr));
				}
			}).tryParsing(toks);
			if (errors.hasErrors())
				return new IgnoreNestedParser();
		}
		return ret.get();
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
