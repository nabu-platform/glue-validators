package be.nabu.glue.validators;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.glue.api.ExecutionContext;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.MethodDescription;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.CallLocation;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.api.MethodProvider;
import be.nabu.glue.core.impl.executors.EvaluateExecutor;
import be.nabu.glue.core.impl.methods.GlueValidationImpl;
import be.nabu.glue.impl.SimpleCallLocation;
import be.nabu.libs.evaluator.QueryPart;
import be.nabu.libs.evaluator.QueryPart.Type;
import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.api.OperationProvider.OperationType;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.libs.validator.api.Validator;

public class MethodsExistValidator implements Validator<Script> {

	private MethodProvider[] methodProviders;
	
	public MethodsExistValidator(MethodProvider...methodProviders) {
		this.methodProviders = methodProviders;
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
	
	private void validate(Script script, ExecutorGroup group, List<GlueValidation> validations) {
		for (Executor child : group.getChildren()) {
			// first check the evaluation part to see if it references non-existent variables
			if (child instanceof EvaluateExecutor) {
				validate(script, child, ((EvaluateExecutor) child).getOperation(), validations);
			}
			// recurse
			if (child instanceof ExecutorGroup) {
				validate(script, (ExecutorGroup) child, validations);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void validate(Script script, Executor executor, Operation<ExecutionContext> operation, List<GlueValidation> validations) {
		if (operation.getType() == OperationType.METHOD) {
			String methodName = operation.getParts().get(0).getContent().toString();
			List<MethodDescription> matches = new ArrayList<MethodDescription>();
			List<MethodDescription> wrongCaseMatches = new ArrayList<MethodDescription>();
			for (MethodProvider provider : methodProviders) {
				for (MethodDescription description : provider.getAvailableMethods()) {
					if (description.getNamespace() != null) {
						String fullName = description.getNamespace() + "." + description.getName();
						if (fullName.equals(methodName)) {
							matches.add(description);
							continue;
						}
						else if (fullName.equalsIgnoreCase(methodName)) {
							wrongCaseMatches.add(description);
						}
					}
					if (description.getName().equals(methodName)) {
						matches.add(description);
					}
					else if (description.getName().equalsIgnoreCase(methodName)) {
						wrongCaseMatches.add(description);
					}
				}
			}
			if (matches.isEmpty()) {
				if (wrongCaseMatches.isEmpty()) {
					validations.add(new GlueValidationImpl(Severity.ERROR, "Method '" + methodName + "' does not exist", "Checking existence of methods", 
						Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
				}
				else if (wrongCaseMatches.size() == 1) {
					validations.add(new GlueValidationImpl(Severity.ERROR, "Method '" + methodName + "' has wrong case, expecting '" + wrongCaseMatches.get(0).getName() + "'", "Checking existence of methods", 
						Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
				}
				else {
					validations.add(new GlueValidationImpl(Severity.ERROR, "Method '" + methodName + "' has wrong case, multiple case matches", "Checking existence of methods", 
						Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
				}
			}
			else if (matches.size() > 1) {
				validations.add(new GlueValidationImpl(Severity.ERROR, "Method '" + methodName + "' has " + matches.size() + " matches, please use namespace to differentiate", "Checking existence of methods", 
					Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
			}
		}
		for (QueryPart part : operation.getParts()) {
			if (part.getType() == Type.OPERATION) {
				validate(script, executor, (Operation<ExecutionContext>) part.getContent(), validations);
			}
		}
	}
}
