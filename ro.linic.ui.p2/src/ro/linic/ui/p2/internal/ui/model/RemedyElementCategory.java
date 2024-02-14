package ro.linic.ui.p2.internal.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.operations.RemedyIUDetail;

public class RemedyElementCategory {

	private String name;
	private List<RemedyIUDetail> elements;

	public RemedyElementCategory(final String name) {
		this.name = name;
		elements = new ArrayList<>();
	}

	public List<RemedyIUDetail> getElements() {
		return elements;
	}

	public String getName() {
		return name;
	}

	public void add(final RemedyIUDetail element) {
		elements.add(element);
	}

}
