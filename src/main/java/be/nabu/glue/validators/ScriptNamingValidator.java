package be.nabu.glue.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.CallLocation;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.impl.methods.GlueValidationImpl;
import be.nabu.glue.impl.SimpleCallLocation;
import be.nabu.libs.validator.api.Validator;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class ScriptNamingValidator implements Validator<Script> {

	private String description;
	private String regex;

	public ScriptNamingValidator(String description, String regex) {
		this.description = description == null ? "The script '${scriptName}' does not conform to the regex: " + regex : description;
		this.regex = regex;
	}
	
	@Override
	public Class<Script> getValueClass() {
		return Script.class;
	}

	@Override
	public List<GlueValidation> validate(Script script) {
		List<GlueValidation> messages = new ArrayList<GlueValidation>();
		if (script.getName() != null) {
			String fullName = (script.getNamespace() == null ? "" : script.getNamespace() + ".") + script.getName();
			// each part of the namespace must match the regex
			// the namespace is "." separated until it gets to the final part: the actual name
			if (regex != null && !fullName.matches("(" + regex + "\\.)*(" + regex + ")")) {
				messages.add(new GlueValidationImpl(Severity.WARNING, description.replace("${scriptName}", fullName), "Validate script name", Arrays.asList(new CallLocation [] { new SimpleCallLocation(script, null) }), null));
			}
		}
		return messages;
	}

}
