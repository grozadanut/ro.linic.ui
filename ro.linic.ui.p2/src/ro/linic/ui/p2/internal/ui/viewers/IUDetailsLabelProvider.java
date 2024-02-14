package ro.linic.ui.p2.internal.ui.viewers;

import java.text.NumberFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IIUElement;
import ro.linic.ui.p2.internal.ui.model.ProvElement;
import ro.linic.ui.p2.org.eclipse.ui.FilteredTree;

/**
 * Label provider for showing IU's in a table.  Clients can configure
 * what is shown in each column.
 *
 * @since 3.4
 */
public class IUDetailsLabelProvider extends ColumnLabelProvider implements ITableLabelProvider, IFontProvider {
	final static int PRIMARY_COLUMN = 0;
	final static String BLANK = ""; //$NON-NLS-1$
	private String toolTipProperty = null;
	private FilteredTree filteredTree;
	private boolean useBoldFont = false;
	private boolean showingId = false;

	private IUColumnConfig[] columnConfig;
	Shell shell;
	HashMap<IIUElement, Job> jobs = new HashMap<>();
	private IEclipseContext ctx;

	public IUDetailsLabelProvider(final IEclipseContext ctx) {
		this(ctx, null, null, null);
	}

	public IUDetailsLabelProvider(final IEclipseContext ctx, final FilteredTree filteredTree, final IUColumnConfig[] columnConfig, final Shell shell) {
		this.ctx = ctx;
		this.filteredTree = filteredTree;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;
		for (final IUColumnConfig config : this.columnConfig)
			if (config.getColumnType() == IUColumnConfig.COLUMN_ID) {
				showingId = true;
				break;
			}
		this.shell = shell;
	}

	@Override
	public String getText(final Object obj) {
		return getColumnText(obj, PRIMARY_COLUMN);
	}

	@Override
	public Image getImage(final Object obj) {
		return getColumnImage(obj, PRIMARY_COLUMN);
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		int columnContent = IUColumnConfig.COLUMN_ID;
		if (columnIndex < columnConfig.length) {
			columnContent = columnConfig[columnIndex].getColumnType();
		}

		final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null) {
			if (columnIndex == 0) {
				if (element instanceof ProvElement)
					return ((ProvElement) element).getLabel(element);
				return element.toString();
			}
			return BLANK;
		}

		switch (columnContent) {
			case IUColumnConfig.COLUMN_ID :
				return iu.getId();
			case IUColumnConfig.COLUMN_NAME :
				// Get the iu name in the current locale
				final String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (name != null)
					return name;
				// If the iu name is not available, we return blank if we know know we are
				// showing id in another column.  Otherwise we return id so the user doesn't
				// see blank iu's.
				if (showingId)
					return BLANK;
				return iu.getId();
			case IUColumnConfig.COLUMN_DESCRIPTION :
				// Get the iu description in the current locale
				final String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
				if (description != null)
					return description;
				return BLANK;
			case IUColumnConfig.COLUMN_VERSION :
				// If it's an element, determine if version should be shown
				if (element instanceof IIUElement) {
					if (((IIUElement) element).shouldShowVersion())
						return iu.getVersion().toString();
					return BLANK;
				}
				// It's a raw IU, return the version
				return iu.getVersion().toString();
			case IUColumnConfig.COLUMN_PROVIDER :
				return iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
			case IUColumnConfig.COLUMN_SIZE :
				if (element instanceof IIUElement && ((IIUElement) element).shouldShowSize())
					return getIUSize((IIUElement) element);
				return BLANK;
		}
		return BLANK;
	}

	@Override
	public Image getColumnImage(final Object element, final int index) {
		if (index == PRIMARY_COLUMN) {
			if (element instanceof ProvElement)
				return ((ProvElement) element).getImage(element);
			if (ProvUI.getAdapter(element, IInstallableUnit.class) != null)
				return ProvUIImages.getImage(ctx, ProvUIImages.IMG_IU);
		}
		return null;
	}

	private String getIUSize(final IIUElement element) {
		final long size = element.getSize();
		// If size is already known, or we already tried
		// to get it, don't try again
		if (size != ProvUI.SIZE_UNKNOWN)
			return getFormattedSize(size);
		if (!jobs.containsKey(element)) {
			final Job resolveJob = new Job(element.getIU().getId()) {

				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					if (shell == null || shell.isDisposed())
						return Status.CANCEL_STATUS;

					element.computeSize(monitor);

					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					// If we still could not compute size, give up
					if (element.getSize() == ProvUI.SIZE_UNKNOWN)
						return Status.OK_STATUS;

					if (shell == null || shell.isDisposed())
						return Status.CANCEL_STATUS;

					shell.getDisplay().asyncExec(() -> {
						if (shell != null && !shell.isDisposed())
							fireLabelProviderChanged(new LabelProviderChangedEvent(IUDetailsLabelProvider.this, element));
					});

					return Status.OK_STATUS;
				}
			};
			jobs.put(element, resolveJob);
			resolveJob.setSystem(true);
			resolveJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(final IJobChangeEvent event) {
					jobs.remove(element);
				}
			});
			resolveJob.schedule();
		}
		return ProvUIMessages.IUDetailsLabelProvider_ComputingSize;
	}

	private String getFormattedSize(final long size) {
		if (size == ProvUI.SIZE_UNKNOWN || size == ProvUI.SIZE_UNAVAILABLE)
			return ProvUIMessages.IUDetailsLabelProvider_Unknown;
		if (size > 1000L) {
			final long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(Long.valueOf(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(Long.valueOf(size)));
	}

	public void setToolTipProperty(final String propertyName) {
		toolTipProperty = propertyName;
	}

	public String getClipboardText(final Object element, final String columnDelimiter) {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < columnConfig.length; i++) {
			if (i != 0)
				result.append(columnDelimiter);
			result.append(getColumnText(element, i));
		}
		return result.toString();
	}

	public void setUseBoldFontForFilteredItems(final boolean useBoldFont) {
		this.useBoldFont = useBoldFont;
	}

	@Override
	public String getToolTipText(final Object element) {
		final IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null || toolTipProperty == null)
			return null;
		return iu.getProperty(toolTipProperty, null);
	}

	@Override
	public Font getFont(final Object element) {
		if (filteredTree != null && useBoldFont) {
			return FilteredTree.getBoldFont(element, filteredTree, filteredTree.getPatternFilter());
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		for (final Job job : jobs.values())
			job.cancel();
	}

}
