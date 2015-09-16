package be.nabu.glue.validators;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.glue.api.AssignmentExecutor;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.CallLocation;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.impl.SimpleCallLocation;
import be.nabu.glue.impl.methods.GlueValidationImpl;
import be.nabu.libs.validator.api.Validator;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class VariableNamingValidator implements Validator<Script> {

	private String regex;
	private String description;

	public VariableNamingValidator(String description, String regex) {
		this.description = description == null ? "The variable '${variableName}' does not conform to the regex: " + regex : description;
		this.regex = regex;
	}
	
	@Override
	public Class<Script> getValueClass() {
		return Script.class;
	}

	@Override
	public List<GlueValidation> validate(Script script) {
		List<GlueValidation> messages = new ArrayList<GlueValidation>();
		try {
			validate(script, script.getRoot(), messages);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return messages;
	}
	
	private void validate(Script script, ExecutorGroup group, List<GlueValidation> messages) {
		for (Executor child : group.getChildren()) {
			if (child instanceof AssignmentExecutor) {
				String variableName = ((AssignmentExecutor) child).getVariableName();
				if (variableName != null && !variableName.matches(regex)) {
					messages.add(new GlueValidationImpl(Severity.WARNING, description.replace("${variableName}", variableName), "Validate variable name", 
						Arrays.asList(new CallLocation [] { new SimpleCallLocation(script, child) }), child));
				}
			}
			if (child instanceof ExecutorGroup) {
				validate(script, (ExecutorGroup) child, messages);
			}
		}
	}
}
