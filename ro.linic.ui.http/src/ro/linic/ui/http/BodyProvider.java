package ro.linic.ui.http;

import java.io.FileNotFoundException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@FunctionalInterface
public interface BodyProvider {
	static final Logger log = Logger.getLogger(BodyProvider.class.getName());
	
	BodyPublisher get();
	
	public static BodyProvider empty() {
        return () -> BodyPublishers.noBody();
    }
	
	public static BodyProvider of(final String body) {
        return () -> BodyPublishers.ofString(body);
    }
	
	public static BodyProvider of(final Path filepath) {
        return () -> {
			try {
				return BodyPublishers.ofFile(filepath);
			} catch (final FileNotFoundException e) {
				log.log(Level.SEVERE, "File exception", e);
				return BodyPublishers.noBody();
			}
		};
    }
	
	public static BodyProvider of(final byte[] body) {
		return () -> BodyPublishers.ofByteArray(body);
    }
}