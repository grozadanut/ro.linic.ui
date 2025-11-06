package ro.linic.ui.base.services.nattable;

public record Column(int index, String property, String name, int size, String tooltip) {
	public Column(final int index, final String property, final String name, final int size) {
		this(index, property, name, size, null);
	}

	public Column withSize(final int size) {
		return new Column(index, property, name, size);
	}
}
