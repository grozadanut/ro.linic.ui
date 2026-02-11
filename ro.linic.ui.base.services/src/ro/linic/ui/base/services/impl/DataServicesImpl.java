package ro.linic.ui.base.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.GenericDataHolder;

@Component
public class DataServicesImpl implements DataServices {
	private Map<String, GenericDataHolder> holders = new HashMap<>();
	
	@Override
	public synchronized GenericDataHolder holder(final String name) {
		return holders.computeIfAbsent(name, k -> new GenericDataHolderImpl());
	}
}
