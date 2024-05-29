package ro.linic.ui.pos.base.tests.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import ro.linic.ui.pos.base.services.ProductDataHolder;

class ProductDataHolderIntegrationTest {

	@Test
    public void assertServiceAccessWithOSGiContextWorks() {
		final ProductDataHolder taskService = getService(ProductDataHolder.class);
        assertNotNull(taskService, "No ProductDataHolder found");
    }

    static <T> T getService(final Class<T> clazz) {
        final Bundle bundle = FrameworkUtil.getBundle(ProductDataHolderIntegrationTest.class);
        if (bundle != null) {
            final ServiceTracker<T, T> st =
                new ServiceTracker<T, T>(
                    bundle.getBundleContext(), clazz, null);
            st.open();
            if (st != null) {
                try {
                    // give the runtime some time to startup
                    return st.waitForService(500);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
