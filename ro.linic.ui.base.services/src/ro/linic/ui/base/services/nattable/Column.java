package ro.linic.ui.base.services.nattable;

public record Column(int index, String property, String name, int size, String tooltip, boolean hidden) {
	public Column(final int index, final String property, final String name, final int size) {
		this(index, property, name, size, null, false);
	}
	
	public Column(final int index, final String property, final String name, final int size, final String tooltip) {
		this(index, property, name, size, tooltip, false);
	}
	
	public Column(final int index, final String property, final String name, final int size, final boolean hidden) {
		this(index, property, name, size, null, hidden);
	}

	public Column withSize(final int size) {
		return new Column(index, property, name, size);
	}
}
