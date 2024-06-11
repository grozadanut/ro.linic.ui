package ro.linic.ui.base.services.binding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class NotNullValidator implements IValidator<Object> {
	final private String message;

	public NotNullValidator(final String message) {
		this.message = message;
	}

	@Override
	public IStatus validate(final Object value) {
		return value == null ? ValidationStatus.error(message) : Status.OK_STATUS;
	}
}
