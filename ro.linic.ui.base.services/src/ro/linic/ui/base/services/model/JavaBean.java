package ro.linic.ui.base.services.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class JavaBean {
	private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
		changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}
}
