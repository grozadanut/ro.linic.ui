package ro.linic.ui.p2.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Widget;

import ro.linic.ui.p2.internal.ui.ColocatedRepositoryTracker;
import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.ProvUIProvisioningListener;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * A RepositorySelectionGroup is a reusable UI component that displays available
 * repositories and allows the user to select them.
 *
 * @since 3.5
 */
public class RepositorySelectionGroup {

	private static final String SITE_NONE = ProvUIMessages.AvailableIUsPage_NoSites;
	private static final String SITE_ALL = ProvUIMessages.AvailableIUsPage_AllSites;
	private static final String SITE_LOCAL = ProvUIMessages.AvailableIUsPage_LocalSites;
	private static final int INDEX_SITE_NONE = 0;
	private static final int INDEX_SITE_ALL = 1;
	private static final int DEC_MARGIN_WIDTH = 2;
	private static final String LINKACTION = "linkAction"; //$NON-NLS-1$

	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=245569
	private static final int COUNT_VISIBLE_ITEMS = 20;
	IWizardContainer container;
	ProvisioningUI ui;
	IUViewQueryContext queryContext;

	ListenerList<IRepositorySelectionListener> listeners = new ListenerList<>();

	Combo repoCombo;
	Link repoManipulatorLink;
	ControlDecoration repoDec;
	ComboAutoCompleteField repoAutoComplete;
	ProvUIProvisioningListener comboRepoListener;
	IRepositoryManipulationHook repositoryManipulationHook;

	Image info, warning, error;
	URI[] comboRepos; // the URIs shown in the combo, kept in sync with combo items
	HashMap<String, URI> disabledRepoProposals = new HashMap<>(); // proposal string -> disabled URI

	public RepositorySelectionGroup(final ProvisioningUI ui, final IWizardContainer container, final Composite parent,
			final IUViewQueryContext queryContext) {
		this.container = container;
		this.queryContext = queryContext;
		this.ui = ui;
		createControl(parent);
	}

	public Control getDefaultFocusControl() {
		return repoCombo;
	}

	public void addRepositorySelectionListener(final IRepositorySelectionListener listener) {
		listeners.add(listener);
	}

	protected void createControl(final Composite parent) {
		final RepositoryTracker tracker = ui.getRepositoryTracker();
		// Get the possible field error indicators
		info = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
				.getImage();
		warning = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING)
				.getImage();
		error = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();

		// Combo that filters sites
		final Composite comboComposite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.marginTop = 0;
		layout.marginBottom = IDialogConstants.VERTICAL_SPACING;
		layout.numColumns = 4;
		layout.marginWidth = 0;
		comboComposite.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		comboComposite.setLayoutData(gd);
		comboComposite.setFont(parent.getFont());

		final Label label = new Label(comboComposite, SWT.NONE);
		label.setText(ProvUIMessages.AvailableIUsPage_RepoFilterLabel);
		label.setFont(comboComposite.getFont());

		repoCombo = new Combo(comboComposite, SWT.DROP_DOWN);
		repoCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				repoComboSelectionChanged();
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				repoComboSelectionChanged();
			}

		});
		// Auto complete - install before our own key listeners, so that auto complete
		// gets first shot.
		repoAutoComplete = new ComboAutoCompleteField(repoCombo);
		repoCombo.setVisibleItemCount(COUNT_VISIBLE_ITEMS);
		repoCombo.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				addRepository(false);
		}));

		// We don't ever want this to be interpreted as a default
		// button event
		repoCombo.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				e.doit = false;
			}
		});

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		// breathing room for info dec
		gd.horizontalIndent = DEC_MARGIN_WIDTH * 2;
		repoCombo.setLayoutData(gd);
		repoCombo.setFont(comboComposite.getFont());
		repoCombo.addModifyListener(event -> {
			URI location = null;
			IStatus status = null;
			final String text = repoCombo.getText().trim();
			if (!text.isEmpty()) {
				final int index = getComboIndex(text);
				// only validate text that doesn't match existing text in combo
				if (index < 0) {
					location = tracker.locationFromString(repoCombo.getText());
					if (location == null) {
						status = tracker.getInvalidLocationStatus(repoCombo.getText());
					} else {
						status = tracker.validateRepositoryLocation(ui.getSession(), location, false,
								new NullProgressMonitor());
					}
				} else {
					// user typed or pasted an existing location. Select it.
					repoComboSelectionChanged();
				}
			}
			setRepoComboDecoration(status);
		});

		// Clear default text when user clicks in the combo box.
		repoCombo.addMouseListener(MouseListener.mouseDownAdapter(e -> {
			if (repoCombo.getText().equals(SITE_NONE))
				repoCombo.setText(""); //$NON-NLS-1$
		}));

		// Restore default text when focus is lost from the combo box.
		repoCombo.addFocusListener(FocusListener.focusLostAdapter(e -> {
			if (repoCombo.getText().isEmpty())
				fillRepoCombo(SITE_NONE);
		}));

		repoDec = new ControlDecoration(repoCombo, SWT.LEFT | SWT.TOP);
		repoDec.setMarginWidth(DEC_MARGIN_WIDTH);

		final DropTarget target = new DropTarget(repoCombo, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance() });
		target.addDropListener(new URLDropAdapter(true) {
			@Override
			protected void handleDrop(final String urlText, final DropTargetEvent event) {
				repoCombo.setText(urlText);
				event.detail = DND.DROP_LINK;
				addRepository(false);
			}
		});

		final Button button = new Button(comboComposite, SWT.PUSH);
		button.setText(ProvUIMessages.AvailableIUsPage_AddButton);
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				addRepository(true);
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				addRepository(true);
			}
		});
		setButtonLayoutData(button);
		button.setFont(comboComposite.getFont());

		createRepoManipulatorButton(comboComposite);

		addComboProvisioningListeners();
		parent.addDisposeListener(e -> removeProvisioningListeners());
	}

	private void createRepoManipulatorButton(final Composite comboComposite) {
		final Button repoManipulatorButton = new Button(comboComposite, SWT.PUSH);
		repoManipulatorButton.setText(ProvUIMessages.RepositoryManipulationPage_Manage);
		setButtonLayoutData(repoManipulatorButton);
		repoManipulatorButton.setFont(comboComposite.getFont());

		repoManipulatorButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			openRepoManipulatorUi();
		}));
	}

	private void openRepoManipulatorUi() {
		if (repositoryManipulationHook != null) {
			repositoryManipulationHook.preManipulateRepositories();
		}
		ui.manipulateRepositories(repoCombo.getShell());

		if (repositoryManipulationHook != null) {
			repositoryManipulationHook.postManipulateRepositories();
		}
	}

	private void setButtonLayoutData(final Button button) {
		final GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		final GC gc = new GC(button);
		gc.setFont(JFaceResources.getDialogFont());
		final FontMetrics fm = gc.getFontMetrics();
		gc.dispose();
		final int widthHint = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.BUTTON_WIDTH);
		final Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
	}

	public void setRepositorySelection(final int scope, final URI location) {
		switch (scope) {
		case AvailableIUGroup.AVAILABLE_ALL:
			fillRepoCombo(SITE_ALL);
			break;
		case AvailableIUGroup.AVAILABLE_LOCAL:
			fillRepoCombo(SITE_LOCAL);
			break;
		case AvailableIUGroup.AVAILABLE_SPECIFIED:
			fillRepoCombo(getSiteString(location));
			break;
		default:
			fillRepoCombo(SITE_NONE);
		}
		setRepoComboDecoration(null);
	}

	public void setRepositoryManipulationHook(final IRepositoryManipulationHook hook) {
		this.repositoryManipulationHook = hook;
	}

	protected void setRepoComboDecoration(final IStatus status) {
		if (status == null || status.isOK() || status.getSeverity() == IStatus.CANCEL) {
			repoDec.setShowOnlyOnFocus(true);
			repoDec.setDescriptionText(ProvUIMessages.AvailableIUsPage_RepoFilterInstructions);
			repoDec.setImage(info);
			// We may have been previously showing an error or warning
			// hover. We will need to dismiss it, but if there is no text
			// typed, don't do this, so that the user gets the info cue
			if (repoCombo.getText().length() > 0)
				repoDec.showHoverText(null);
			return;
		}
		Image image;
		if (status.getSeverity() == IStatus.WARNING)
			image = warning;
		else if (status.getSeverity() == IStatus.ERROR)
			image = error;
		else
			image = info;
		repoDec.setImage(image);
		repoDec.setDescriptionText(status.getMessage());
		repoDec.setShowOnlyOnFocus(false);
		// use a delay to show the validation method because the very next
		// selection or keystroke might fix it
		repoCombo.getDisplay().timerExec(500, () -> {
			if (repoDec != null && repoDec.getImage() != info)
				repoDec.showHoverText(status.getMessage());
		});

	}

	/*
	 * Fill the repo combo and use the specified string as the selection. If the
	 * selection is null, then the current selection should be preserved if
	 * applicable.
	 */
	void fillRepoCombo(final String selection) {
		final RepositoryTracker tracker = ui.getRepositoryTracker();
		final URI[] sites = tracker.getKnownRepositories(ui.getSession());
		final boolean hasLocalSites = getLocalSites().length > 0;
		final String[] items;
		if (hasLocalSites) {
			// None, All, repo1, repo2....repo n, Local
			comboRepos = new URI[sites.length + 3];
			items = new String[sites.length + 3];
		} else {
			// None, All, repo1, repo2....repo n
			comboRepos = new URI[sites.length + 2];
			items = new String[sites.length + 2];
		}
		items[INDEX_SITE_NONE] = SITE_NONE;
		items[INDEX_SITE_ALL] = SITE_ALL;
		for (int i = 0; i < sites.length; i++) {
			items[i + 2] = getSiteString(sites[i]);
			comboRepos[i + 2] = sites[i];
		}
		if (hasLocalSites)
			items[items.length - 1] = SITE_LOCAL;
		if (sites.length > 0)
			sortRepoItems(items, comboRepos, hasLocalSites);
		final Runnable runnable = () -> {
			if (repoCombo == null || repoCombo.isDisposed())
				return;
			String repoToSelect = selection;
			if (repoToSelect == null) {
				// If the combo is open and something is selected, use that index if we
				// weren't given a string to select.
				final int selIndex = repoCombo.getSelectionIndex();
				if (selIndex >= 0)
					repoToSelect = repoCombo.getItem(selIndex);
				else
					repoToSelect = repoCombo.getText();
			}
			repoCombo.setItems(items);
			repoAutoComplete.setProposalStrings(getComboProposals());
			boolean selected = false;
			for (int i = 0; i < items.length; i++)
				if (items[i].equals(repoToSelect)) {
					selected = true;
					if (repoCombo.getListVisible())
						repoCombo.select(i);
					repoCombo.setText(repoToSelect);
					break;
				}
			if (!selected) {
				if (repoCombo.getListVisible())
					repoCombo.select(INDEX_SITE_NONE);
				repoCombo.setText(SITE_NONE);
			}
			repoComboSelectionChanged();
		};
		if (Display.getCurrent() == null)
			repoCombo.getDisplay().asyncExec(runnable);
		else
			runnable.run();
	}

	String getSiteString(final URI uri) {
		final String nickname = getMetadataRepositoryManager().getRepositoryProperty(uri, IRepository.PROP_NICKNAME);
		if (nickname != null && nickname.length() > 0)
			return NLS.bind(ProvUIMessages.AvailableIUsPage_NameWithLocation, new Object[] { nickname,
					ProvUIMessages.RepositorySelectionGroup_NameAndLocationSeparator, URIUtil.toUnencodedString(uri) });
		return URIUtil.toUnencodedString(uri);
	}

	IAction getLinkAction(final Widget widget) {
		final Object data = widget.getData(LINKACTION);
		if (data == null || !(data instanceof IAction)) {
			return null;
		}
		return (IAction) data;
	}

	private void sortRepoItems(final String[] strings, final URI[] locations, final boolean hasLocalSites) {
		final int sortStart = 2;
		final int sortEnd = hasLocalSites ? strings.length - 2 : strings.length - 1;
		if (sortStart >= sortEnd)
			return;
		final HashMap<URI, String> uriToString = new HashMap<>();
		for (int i = sortStart; i <= sortEnd; i++) {
			uriToString.put(locations[i], strings[i]);
		}
		final Collator collator = Collator.getInstance(Locale.getDefault());
		final Comparator<String> stringComparator = (a, b) -> collator.compare(a, b);
		final Comparator<URI> uriComparator = (a, b) -> collator.compare(uriToString.get(a), uriToString.get(b));

		Arrays.sort(strings, sortStart, sortEnd, stringComparator);
		Arrays.sort(locations, sortStart, sortEnd, uriComparator);
	}

	private URI[] getLocalSites() {
		// use our current visibility flags plus the local filter
		final int flags = ui.getRepositoryTracker().getMetadataRepositoryFlags() | IRepositoryManager.REPOSITORIES_LOCAL;
		return getMetadataRepositoryManager().getKnownRepositories(flags);
	}

	String[] getComboProposals() {
		final int flags = ui.getRepositoryTracker().getMetadataRepositoryFlags() | IRepositoryManager.REPOSITORIES_DISABLED;
		final String[] items = repoCombo.getItems();
		// Clear any previously remembered disabled repos
		disabledRepoProposals = new HashMap<>();
		final URI[] disabled = getMetadataRepositoryManager().getKnownRepositories(flags);
		final String[] disabledItems = new String[disabled.length];
		for (int i = 0; i < disabledItems.length; i++) {
			disabledItems[i] = getSiteString(disabled[i]);
			disabledRepoProposals.put(disabledItems[i], disabled[i]);
		}
		final String[] both = new String[items.length + disabledItems.length];
		System.arraycopy(items, 0, both, 0, items.length);
		System.arraycopy(disabledItems, 0, both, items.length, disabledItems.length);
		return both;
	}

	int getComboIndex(String repoText) {
		// Callers have typically done this already, but just in case
		repoText = repoText.trim();
		// First look for exact match to the combo string.
		// This includes the name, etc.
		if (repoText.length() > 0) {
			final String[] items = repoCombo.getItems();
			for (int i = 0; i < items.length; i++)
				if (repoText.equals(items[i])) {
					return i;
				}
		}
		// Look for URI match - the user may have pasted or dragged
		// in a location that matches one we already know about, even
		// if the text does not match completely. (slashes, no name, etc.)
		try {
			final URI location = URIUtil.fromString(repoText);
			for (int i = 0; i < comboRepos.length; i++)
				if (URIUtil.sameURI(location, comboRepos[i])) {
					return i;
				}
		} catch (final URISyntaxException e) {
			// never mind
		}

		// Special case. The user has typed a URI with a trailing slash.
		// Make a URI without the trailing slash and see if it matches
		// a location we know about.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=268580
		final int length = repoText.length();
		if (length > 0 && repoText.charAt(length - 1) == '/') {
			return getComboIndex(repoText.substring(0, length - 1));
		}
		return -1;
	}

	void addComboProvisioningListeners() {
		// We need to monitor repository events so that we can adjust the repo combo.
		comboRepoListener = new ProvUIProvisioningListener(getClass().getName(),
				ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, ui.getOperationRunner()) {
			@Override
			protected void repositoryAdded(final RepositoryEvent e) {
				fillRepoCombo(getSiteString(e.getRepositoryLocation()));
			}

			@Override
			protected void repositoryRemoved(final RepositoryEvent e) {
				fillRepoCombo(null);
			}

			@Override
			protected void refreshAll() {
				fillRepoCombo(null);
			}
		};
		ProvUI.getProvisioningEventBus(ui.getSession()).addListener(comboRepoListener);
	}

	void removeProvisioningListeners() {
		if (comboRepoListener != null) {
			ProvUI.getProvisioningEventBus(ui.getSession()).removeListener(comboRepoListener);
			comboRepoListener = null;
		}

	}

	/*
	 * Add a repository using the text in the combo or launch a dialog if the text
	 * represents an already known repo.
	 */
	void addRepository(final boolean alwaysPrompt) {
		final RepositoryTracker manipulator = ui.getRepositoryTracker();
		final String selectedRepo = repoCombo.getText().trim();
		final int selectionIndex = getComboIndex(selectedRepo);
		final boolean isNewText = selectionIndex < 0;
		// If we are adding something already in the combo, just
		// select that item.
		if (!alwaysPrompt && !isNewText && selectionIndex != repoCombo.getSelectionIndex()) {
			repoComboSelectionChanged();
		} else if (alwaysPrompt) {
			final AddRepositoryDialog dialog = new AddRepositoryDialog(repoCombo.getShell(), ui) {

				@Override
				protected String getInitialLocationText() {
					if (isNewText) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
						// we need to ensure any embedded nickname is stripped out
						final URI loc = manipulator.locationFromString(selectedRepo);
						return loc.toString();
					}
					return super.getInitialLocationText();
				}

				@Override
				protected String getInitialNameText() {
					if (isNewText) {
						final URI loc = manipulator.locationFromString(selectedRepo);
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
						if (loc != null && manipulator instanceof ColocatedRepositoryTracker) {
							final String parsedNickname = ((ColocatedRepositoryTracker) manipulator).getParsedNickname(loc);
							if (parsedNickname != null)
								return parsedNickname;
						}
					}
					return super.getInitialNameText();
				}

			};
			dialog.setTitle(ProvUIMessages.AddRepositoryDialog_Title);
			dialog.open();
			final URI location = dialog.getAddedLocation();
			if (location != null)
				fillRepoCombo(getSiteString(location));
		} else if (isNewText) {
			try {
				container.run(false, false, monitor -> {
					URI location;
					IStatus status;
					// This might be a disabled repo. If so, no need to validate further.
					if (disabledRepoProposals.containsKey(selectedRepo)) {
						location = disabledRepoProposals.get(selectedRepo);
						status = Status.OK_STATUS;
					} else {
						location = manipulator.locationFromString(selectedRepo);
						if (location == null)
							status = manipulator.getInvalidLocationStatus(selectedRepo);
						else {
							status = manipulator.validateRepositoryLocation(ui.getSession(), location, false, monitor);
						}
					}
					if (status.isOK() && location != null) {
						String nick = null;
						if (manipulator instanceof ColocatedRepositoryTracker)
							nick = ((ColocatedRepositoryTracker) manipulator).getParsedNickname(location);
						manipulator.addRepository(location, nick, ui.getSession());
						fillRepoCombo(getSiteString(location));
					}
					setRepoComboDecoration(status);
				});
			} catch (final InvocationTargetException e) {
				// ignore
			} catch (final InterruptedException e) {
				// ignore
			}
		}
	}

	public ProvisioningContext getProvisioningContext() {
		final int siteSel = getComboIndex(repoCombo.getText().trim());
		if (siteSel < 0 || siteSel == INDEX_SITE_ALL || siteSel == INDEX_SITE_NONE)
			return new ProvisioningContext(ui.getSession().getProvisioningAgent());
		final URI[] locals = getLocalSites();
		// If there are local sites, the last item in the combo is "Local Sites Only"
		// Use all local sites in this case
		// We have to set metadata repositories and artifact repositories in the
		// provisioning context because the artifact repositories are used for
		// sizing.
		if (locals.length > 0 && siteSel == repoCombo.getItemCount() - 1) {
			final ProvisioningContext context = new ProvisioningContext(ui.getSession().getProvisioningAgent());
			context.setMetadataRepositories(locals);
			context.setArtifactRepositories(locals);
			return context;
		}
		// A single site is selected.
		final ProvisioningContext context = new ProvisioningContext(ui.getSession().getProvisioningAgent());
		context.setMetadataRepositories(new URI[] { comboRepos[siteSel] });
		context.setArtifactRepositories(new URI[] { comboRepos[siteSel] });
		return context;
	}

	void repoComboSelectionChanged() {
		int repoChoice = -1;
		URI repoLocation = null;

		int selection = -1;
		if (repoCombo.getListVisible()) {
			selection = repoCombo.getSelectionIndex();
		} else {
			selection = getComboIndex(repoCombo.getText().trim());
		}
		final int localIndex = getLocalSites().length == 0 ? repoCombo.getItemCount() : repoCombo.getItemCount() - 1;
		if (comboRepos == null || selection < 0) {
			selection = INDEX_SITE_NONE;
		}

		if (selection == INDEX_SITE_NONE) {
			repoChoice = AvailableIUGroup.AVAILABLE_NONE;
		} else if (selection == INDEX_SITE_ALL) {
			repoChoice = AvailableIUGroup.AVAILABLE_ALL;
		} else if (selection >= localIndex) {
			repoChoice = AvailableIUGroup.AVAILABLE_LOCAL;
		} else {
			repoChoice = AvailableIUGroup.AVAILABLE_SPECIFIED;
			repoLocation = comboRepos[selection];
		}

		for (final IRepositorySelectionListener listener : listeners) {
			listener.repositorySelectionChanged(repoChoice, repoLocation);
		}
	}

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}
}
