package ro.linic.ui.legacy.inport;

public class ImportParseException extends Exception {

	private static final long serialVersionUID = 1L;

	public ImportParseException() {
		super();
	}

	public ImportParseException(final String arg0, final Throwable arg1, final boolean arg2, final boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public ImportParseException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

	public ImportParseException(final String arg0) {
		super(arg0);
	}

	public ImportParseException(final Throwable arg0) {
		super(arg0);
	}

}
