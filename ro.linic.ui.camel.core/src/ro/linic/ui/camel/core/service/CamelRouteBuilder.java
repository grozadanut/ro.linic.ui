package ro.linic.ui.camel.core.service;

import org.apache.camel.builder.RouteBuilder;

/**
 * Plugins that want to register camel routes should implement this service and 
 * return the route in the getRouteBuilder().
 * Example:
 * <pre>
 * import org.apache.camel.builder.RouteBuilder;
 * import org.osgi.service.component.annotations.Component;
 * 
 * import ro.linic.ui.camel.core.service.CamelRouteBuilder;
 * 
 * @Component
 * public class MyRouteBuilder extends RouteBuilder implements CamelRouteBuilder {
 * 
 * @Override
 * public RouteBuilder getRouteBuilder() {
 * 	return this;
 * }
 * 
 * @Override
 * public void configure() {
 *        // here is a sample which processes the input files
 *        // (leaving them in place - see the 'noop' flag)
 *        // then performs content based routing on the message using XPath
 *        from("file:src/data?noop=true")
 *            .choice()
 *                .when(xpath("/person/city = 'London'"))
 *                    .log("UK message")
 *                    .to("file:target/messages/uk")
 *                .otherwise()
 *                    .log("Other message")
 *                    .to("file:target/messages/others");
 *    }
 * }
 * </pre>
 */
public interface CamelRouteBuilder {
	RouteBuilder getRouteBuilder();
}
