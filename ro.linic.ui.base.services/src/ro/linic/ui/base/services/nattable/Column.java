package ro.linic.ui.base.services.nattable;

public record Column(int index, String property, String name, int size) {
	public Column withSize(final int size) {
		return new Column(index, property, name, size);
	}
}
