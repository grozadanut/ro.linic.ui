package ro.linic.ui.p2.internal.ui.viewers;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.Image;

import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.model.IRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.ProvElement;

/**
 * Label provider for repository elements.  The column structure is
 * assumed to be known by the caller who sets up the columns
 *
 * @since 3.5
 */
public class RepositoryDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {
	public static final int COL_NAME = 0;
	public static final int COL_LOCATION = 1;
	public static final int COL_ENABLEMENT = 2;
	
	private IEclipseContext ctx;

	public RepositoryDetailsLabelProvider(final IEclipseContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public Image getImage(final Object obj) {
		if (obj instanceof ProvElement) {
			return ((ProvElement) obj).getImage(obj);
		}
		if (obj instanceof IArtifactRepository) {
			return ProvUIImages.getImage(ctx, ProvUIImages.IMG_ARTIFACT_REPOSITORY);
		}
		if (obj instanceof IMetadataRepository) {
			return ProvUIImages.getImage(ctx, ProvUIImages.IMG_METADATA_REPOSITORY);
		}
		return null;
	}

	@Override
	public Image getColumnImage(final Object element, final int index) {
		if (index == 0) {
			return getImage(element);
		}
		return null;
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {

		switch (columnIndex) {
			case COL_NAME :
				if (element instanceof IRepositoryElement<?>) {
					final String name = ((IRepositoryElement<?>) element).getName();
					if (name != null) {
						return name;
					}
				}
				if (element instanceof IRepository<?>) {
					final String name = ((IRepository<?>) element).getName();
					if (name != null) {
						return name;
					}
				}
				return ""; //$NON-NLS-1$
			case COL_LOCATION :
				if (element instanceof IRepository<?>) {
					return TextProcessor.process(URIUtil.toUnencodedString(((IRepository<?>) element).getLocation()));
				}
				if (element instanceof IRepositoryElement<?>) {
					return TextProcessor.process(URIUtil.toUnencodedString(((IRepositoryElement<?>) element).getLocation()));
				}
				break;
			case COL_ENABLEMENT :
				if (element instanceof MetadataRepositoryElement)
					return ((MetadataRepositoryElement) element).isEnabled() ? ProvUIMessages.RepositoryDetailsLabelProvider_Enabled : ProvUIMessages.RepositoryDetailsLabelProvider_Disabled;

		}
		return null;
	}

	public String getClipboardText(final Object element, final String columnDelimiter) {
		final StringBuilder result = new StringBuilder();
		result.append(getColumnText(element, COL_NAME));
		result.append(columnDelimiter);
		result.append(getColumnText(element, COL_LOCATION));
		result.append(columnDelimiter);
		result.append(getColumnText(element, COL_ENABLEMENT));
		return result.toString();
	}
}
