/**
 * 
 */
package de.champonthis.ena.eke.proxy.model.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Lurkars
 *
 */
public class ValidCountriesValidator implements ConstraintValidator<ValidCountries, Set<String>> {

	  @Override
	  public boolean isValid(Set<String> countries, ConstraintValidatorContext constraintValidatorContext) {
	    return CountryValidator.isValidCountryCodes(countries);
	  }

}
