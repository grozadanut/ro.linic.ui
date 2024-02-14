package ro.linic.ui.p2.internal.ui.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;

import ro.linic.ui.p2.internal.ui.model.CategoryElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElementWrapper;

/**
 * A collector that converts IU's to category elements as it accepts them.
 * It can be configured so that it is never empty.
 *
 * @since 3.4
 */
public class CategoryElementWrapper extends QueriedElementWrapper {

	// Used to track nested categories
	private Set<String> referredIUs = new HashSet<>();

	public CategoryElementWrapper(final IEclipseContext ctx, final IQueryable<?> queryable, final Object parent) {
		super(ctx, queryable, parent);
	}

	@Override
	protected boolean shouldWrap(final Object match) {
		if (match instanceof IInstallableUnit) {
			final IInstallableUnit iu = (IInstallableUnit) match;
			final Collection<IRequirement> requirements = iu.getRequirements();
			for (final IRequirement requirement : requirements) {
				if (requirement instanceof IRequiredCapability) {
					if (((IRequiredCapability) requirement).getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
						referredIUs.add(((IRequiredCapability) requirement).getName());
					}
				}
			}

			final Iterator<?> iter = super.getCollection().iterator();
			// Don't add the same category IU twice.
			while (iter.hasNext()) {
				final CategoryElement element = (CategoryElement) iter.next();
				if (element.shouldMerge(iu)) {
					element.mergeIU(iu);
					return false;
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public Collection<?> getElements(final Collector<?> collector) {
		if (collector.isEmpty())
			return super.getElements(collector);
		final Collection<?> results = super.getElements(collector);
		cleanList();
		return results;
	}

	@Override
	protected Object wrap(final Object item) {
		final IInstallableUnit iu = (IInstallableUnit) item;
		return super.wrap(new CategoryElement(ctx, parent, iu));
	}

	private void cleanList() {
		removeNestedCategories();
	}

	private void removeNestedCategories() {
		final CategoryElement[] categoryIUs = getCollection().toArray(new CategoryElement[getCollection().size()]);
		// If any other element refers to a category element, remove it from the list
		for (int i = 0; i < categoryIUs.length; i++) {
			if (referredIUs.contains(categoryIUs[i].getIU().getId())) {
				getCollection().remove(categoryIUs[i]);
			}
		}
	}
}
