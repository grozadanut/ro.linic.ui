package ro.linic.ui.base.services.binding;

import static ro.linic.util.commons.StringUtils.isEmpty;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class NotEmptyValidator implements IValidator<String> {
	final private String message;

	public NotEmptyValidator(final String message) {
		this.message = message;
	}

	@Override
	public IStatus validate(final String value) {
		return isEmpty(value) ? ValidationStatus.error(message) : Status.OK_STATUS;
	}
}
