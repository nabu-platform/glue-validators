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
