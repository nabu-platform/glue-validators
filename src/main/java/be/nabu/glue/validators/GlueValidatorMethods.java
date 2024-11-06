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
import java.util.List;

import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.evaluator.annotations.MethodProviderClass;
import be.nabu.libs.validator.MultipleValidator;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.Validator;

@MethodProviderClass(namespace = "validation")
public class GlueValidatorMethods {
	
	public static ScriptRepository getRepository() {
		return ScriptRuntime.getRuntime().getScript().getRepository();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Validator<T> combine(Validator<T>...validators) {
		return new MultipleValidator<T>(validators);
	}
	
	public static Validator<Script> createVariableNameValidator(String description, String regex) {
		return new VariableNamingValidator(description, regex);
	}
	
	public static Validator<Script> createScriptNameValidator(String description, String regex) {
		return new ScriptNamingValidator(description, regex);
	}
	
	@SuppressWarnings("unchecked")
	public static List<? extends Validation<?>> validate(Script script, Validator<Script>...validators) {
		List<Validation<?>> messages = new ArrayList<Validation<?>>();
		for (Validator<Script> validator : validators) {
			List<? extends Validation<?>> validations = validator.validate(script);
			if (validations != null) {
				messages.addAll(validations);
			}
		}
		return messages;
	}
	
}
