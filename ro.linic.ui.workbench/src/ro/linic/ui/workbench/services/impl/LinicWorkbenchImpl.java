package ro.linic.ui.workbench.services.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import ro.linic.ui.security.services.AuthenticationSession;
import ro.linic.ui.workbench.services.LinicWorkbench;

@Component
public class LinicWorkbenchImpl implements LinicWorkbench {
	private static final String DATA = "-data"; //$NON-NLS-1$
	
	private AuthenticationSession authSession;
	
	@Reference(
            service = AuthenticationSession.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC
    )
    private void setAuthSession(final AuthenticationSession authSession) {
        this.authSession = authSession;
	}

	@SuppressWarnings("unused")
	private void unsetAuthSession(final AuthenticationSession authSession) {
		this.authSession = null;
	}
	
	@Override
	public void switchWorkspace(final String workspaceLoc) {
		if (replaceProgramArgument(DATA, workspaceLoc))
			authSession.restartPreservingSession();
	}
	
	@Override
	public boolean replaceProgramArgument(final String key, final String value) {
		// find files matched `ini` file extension
		try (final Stream<Path> walk = Files.walk(Paths.get(Platform.getInstallLocation().getURL().toURI()), 1)) {
			final Optional<Path> foundIni = walk
					.filter(p -> !Files.isDirectory(p)) // not a directory
					.filter(p -> p.toString().toLowerCase().endsWith(".ini"))
					.findFirst();
			
			if (foundIni.isEmpty())
				return false;
			
			final List<String> outLines = new ArrayList<>();
			final Path iniFilePath = foundIni.get();
			boolean foundKey = false;
			boolean replaced = false;
			
			for (final String line : Files.readAllLines(iniFilePath)) {
				if (foundKey) {
					if (line.equalsIgnoreCase(value))
						return false;
					outLines.add(value);
					foundKey = false;
					replaced = true;
					continue;
				}
				
				if (line.equalsIgnoreCase(key))
					foundKey = true;

				outLines.add(line);
			}
			
			if (!replaced) {
				outLines.add(key);
				outLines.add(value);
			}
			
			Files.write(iniFilePath, outLines);
			return true;
			
		} catch (IOException | URISyntaxException e) {
			Platform.getLog(getClass()).error(e.getMessage(), e);
			return false;
		}
	}
}
