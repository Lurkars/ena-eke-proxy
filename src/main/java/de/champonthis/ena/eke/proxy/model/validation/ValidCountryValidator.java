/**
 * 
 */
package de.champonthis.ena.eke.proxy.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Lurkars
 *
 */
public class ValidCountryValidator implements ConstraintValidator<ValidCountry, String> {

	@Override
	public boolean isValid(String country, ConstraintValidatorContext constraintValidatorContext) {
		return CountryValidator.isValidCountryCode(country);
	}
}
