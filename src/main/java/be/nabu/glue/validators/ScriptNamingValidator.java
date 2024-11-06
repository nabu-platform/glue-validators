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
