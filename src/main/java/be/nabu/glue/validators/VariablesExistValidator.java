/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.glue.validators;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.nabu.glue.api.AssignmentExecutor;
import be.nabu.glue.api.ExecutionContext;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.CallLocation;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.impl.executors.EvaluateExecutor;
import be.nabu.glue.core.impl.executors.ForEachExecutor;
import be.nabu.glue.core.impl.methods.GlueValidationImpl;
import be.nabu.glue.impl.SimpleCallLocation;
import be.nabu.libs.evaluator.QueryPart;
import be.nabu.libs.evaluator.QueryPart.Type;
import be.nabu.libs.evaluator.api.Operation;
import be.nabu.libs.evaluator.api.OperationProvider.OperationType;
import be.nabu.libs.validator.api.Validator;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class VariablesExistValidator implements Validator<Script> {

	@Override
	public Class<Script> getValueClass() {
		return Script.class;
	}

	@Override
	public List<GlueValidation> validate(Script script) {
		List<GlueValidation> messages = new ArrayList<GlueValidation>();
		try {
			validate(script, script.getRoot(), new HashSet<String>(), messages);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return messages;
	}
	
	private void validate(Script script, ExecutorGroup group, Set<String> variables, List<GlueValidation> validations) {
		for (Executor child : group.getChildren()) {
			// first check the evaluation part to see if it references non-existent variables
			if (child instanceof EvaluateExecutor) {
				validate(script, child, ((EvaluateExecutor) child).getOperation(), variables, validations);
			}
			// make sure newly defined variables are added to the list
			if (child instanceof AssignmentExecutor) {
				String variableName = ((AssignmentExecutor) child).getVariableName();
				if (variableName != null) {
					variables.add(variableName);
				}
			}
			else if (child instanceof ForEachExecutor) {
				String variableName = ((ForEachExecutor) child).getTemporaryVariable();
				String indexName = ((ForEachExecutor) child).getTemporaryIndex();
				variables.add(variableName == null ? "$variable" : variableName);
				variables.add(indexName == null ? "$index" : indexName);
			}
			// recurse
			if (child instanceof ExecutorGroup) {
				validate(script, (ExecutorGroup) child, variables, validations);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void validate(Script script, Executor executor, Operation<ExecutionContext> operation, Set<String> variables, List<GlueValidation> validations) {
		if (operation.getType() == OperationType.VARIABLE) {
			// if it's not an operation, it must be coming from the pipeline
			if (operation.getParts().get(0).getType() != Type.OPERATION) {
				boolean variableFound = false;
				String wrongCaseMatch = null;
				for (String variable : variables) {
					if (variable.equals(operation.getParts().get(0).getContent().toString())) {
						variableFound = true;
						break;
					}
					if (variable.equalsIgnoreCase(operation.getParts().get(0).getContent().toString())) {
						wrongCaseMatch = variable;
					}
				}
				if (!variableFound) {
					if (wrongCaseMatch != null) {
						validations.add(new GlueValidationImpl(Severity.ERROR, "Variable '" + operation.getParts().get(0).getContent().toString() + "' has wrong case, expecting '" + wrongCaseMatch + "'", "Checking existence of variables", 
							Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
					}
					else {
						// the non-existence of a variable might be on purpose (e.g. when dynamically building arrays, injecting values, eval(),...) so only set it to warning
						validations.add(new GlueValidationImpl(Severity.WARNING, "Variable '" + operation.getParts().get(0).getContent().toString() + "' does not exist", "Checking existence of variables", 
							Arrays.asList(new CallLocation[] { new SimpleCallLocation(script, executor) }), executor));
					}
				}
			}
		}
		for (QueryPart part : operation.getParts()) {
			if (part.getType() == Type.OPERATION) {
				Operation<ExecutionContext> content = (Operation<ExecutionContext>) part.getContent();
				if (content.getType() == OperationType.CLASSIC && content.getParts().size() == 3 && content.getParts().get(1).getType() == Type.NAMING && content.getParts().get(1).getContent().equals(":")) {
					if (content.getParts().get(2).getType() == Type.OPERATION) {
						validate(script, executor, (Operation<ExecutionContext>) content.getParts().get(2).getContent(), variables, validations);
					}
				}
				else {
					validate(script, executor, content, variables, validations);
				}
			}
		}
	}
}
