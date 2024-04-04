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

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ro.linic.ui.security.services.AuthenticationSession;
import ro.linic.ui.workbench.services.LinicWorkbench;

@Component(property = EventConstants.EVENT_TOPIC + "=" + UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class LinicWorkbenchImpl implements LinicWorkbench, EventHandler {
	private static final String DATA = "-data"; //$NON-NLS-1$
	
	private IWorkbench workbench;
	private AuthenticationSession authSession;
	
	private boolean restartWhenReady = false;
	
	@Override
	public void switchWorkspace(final String workspaceLoc) {
		if (replaceProgramArgument(DATA, workspaceLoc)) {
			authSession.storeSession();
			lazyRestart();
		}
	}
	
	@Override
	public void lazyRestart() {
		if (workbench != null)
			Display.getDefault().execute(() -> workbench.restart());
		else
			restartWhenReady = true;
	}
	
	@Override
    public void handleEvent(final Event event) {
		// APP_STARTUP_COMPLETE
		if (restartWhenReady)
			Job.create("Restart", (ICoreRunnable) monitor -> Display.getDefault().syncExec(() -> workbench.restart()))
			.schedule(1);
		restartWhenReady = false;
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
	
	@Reference(
            service = IWorkbench.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC
    )
    private void setWorkbench(final IWorkbench workbench) {
        this.workbench = workbench;
	}

	@SuppressWarnings("unused")
	private void unsetWorkbench(final IWorkbench workbench) {
		this.workbench = null;
	}
}
