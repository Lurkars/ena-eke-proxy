/**
 * 
 */
package de.champonthis.ena.eke.proxy.model.validation;

import java.time.Instant;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Lurkars
 *
 */
public class ValidRollingStartIntervalNumberValidator
		implements ConstraintValidator<ValidRollingStartIntervalNumber, Integer> {

	@Override
	public boolean isValid(Integer rollingStartIntervalNumber, ConstraintValidatorContext constraintValidatorContext) {
		int currentDateTime = Math.toIntExact(Instant.now().getEpochSecond() / 600L);
		return rollingStartIntervalNumber > 0 && rollingStartIntervalNumber < currentDateTime;
	}
}
