package ro.linic.ui.legacy.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ro.linic.ui.camel.core.service.CamelRouteBuilder;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.services.ReceiptLineUpdater;
import ro.linic.ui.pos.base.services.ReceiptLoader;
import ro.linic.ui.pos.cloud.model.CloudReceipt;
import ro.linic.ui.pos.cloud.services.RemoteSyncer;

@Component
public class RemoteSyncCamelRoute extends RouteBuilder implements CamelRouteBuilder {
	@Reference private RemoteSyncer remoteSyncer;
	@Reference private ReceiptLineUpdater receiptLineUpdater;
	@Reference private ReceiptLoader receiptLoader;
	
	@Override
	public RouteBuilder getRouteBuilder() {
		return this;
	}

	@Override
	public void configure() throws Exception {
		from("direct:syncReceipts")
		.bean(receiptLoader, "findClosed")
		.bean(this, "routeReceipts");
		
		// run every 5 minutes: 5 * 60 * 1000 = 300000 ms
		from("scheduler://global?delay=300000")
		.to("direct:syncReceipts");
	}
	
	public Collection<IStatus> routeReceipts(final Collection<CloudReceipt> receipts) {
		/**
		 * if (receipt.closed && receipt.unsynced)
		 * 	sync remote
		 * if (receipt.closed && receipt.synced && receipt.creationTime before today)
		 * 	remove local
		 */
        return receipts.stream()
        		.filter(CloudReceipt::closed)
        		.map(receipt -> {
        			if (!receipt.synced())
        				return remoteSyncer.syncReceipts(List.of(receipt), null); // unsynced
        			else if (receipt.getCreationTime().isBefore(Instant.now().minus(1, ChronoUnit.DAYS)))
        				return receiptLineUpdater.delete(receipt.getLines().stream()
        						.map(ReceiptLine::getId)
        						.collect(Collectors.toSet())); // synced, closed yesterday or before
        			return ValidationStatus.OK_STATUS; // synced, closed today
        		})
        		.collect(Collectors.toList());
    }
}
