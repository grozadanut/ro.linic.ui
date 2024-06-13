package ro.linic.ui.base.services.nattable;

public record UpdateCommand(Class modelClass, Object model, String updatedProperty, Object newValue) {

}
