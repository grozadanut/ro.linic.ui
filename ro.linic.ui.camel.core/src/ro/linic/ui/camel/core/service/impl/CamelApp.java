package ro.linic.ui.camel.core.service.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import ro.linic.ui.camel.core.service.CamelRouteBuilder;
import ro.linic.ui.camel.core.service.CamelService;

@Component(immediate = true)
public class CamelApp implements CamelService {
	private DefaultCamelContext camel;
	
	@Activate
	void activate() {
		camel = new DefaultCamelContext();
		// start is not blocking
		camel.start();
	}

	@Deactivate
	void deactivate() throws Exception {
		camel.close();
	}

	@Reference(
			service = CamelRouteBuilder.class,
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC,
			unbind = "unsetRoute"
			)
	void setRoute(final CamelRouteBuilder routeBuilder) throws Exception {
		camel.addRoutes(routeBuilder.getRouteBuilder());
	}

	void unsetRoute(final CamelRouteBuilder routeBuilder) throws Exception {
		camel.removeRouteDefinitions(routeBuilder.getRouteBuilder().getRoutes().getRoutes());
	}

	@Override
	public CamelContext get() {
		return camel;
	}
}
