package ro.linic.ui.pos.base.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.preferences.PreferenceKey;
import ro.linic.ui.pos.base.services.ECRDriver;
import ro.linic.ui.pos.base.services.ECRDriver.PaymentType;
import ro.linic.ui.pos.base.services.ECRDriver.Result;
import ro.linic.ui.pos.base.services.ECRService;

@Component
public class ECRServiceImpl implements ECRService {
	private static final long RESULT_READ_TIMEOUT_S = 60;
	
	private List<ECRDriver> drivers = new ArrayList<>();
	
	@Reference(
            service = ECRDriver.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetDriver"
    )
    private void setDriver(final ECRDriver driver) {
        drivers.add(driver);
	}

	@SuppressWarnings("unused")
	private void unsetDriver(final ECRDriver driver) {
		drivers.remove(driver);
	}
	
	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId) {
		return findDriver()
				.map(driver -> driver.printReceipt(receipt, paymentType, taxId))
				.map(cf -> cf.completeOnTimeout(Result.error(Messages.ECRServiceImpl_Timeout), RESULT_READ_TIMEOUT_S, TimeUnit.SECONDS))
				.orElse(CompletableFuture.completedFuture(Result.error(Messages.ECRServiceImpl_DriverNotFound)));
	}

	@Override
	public void reportZ() {
		findDriver().ifPresent(ECRDriver::reportZ);
	}

	@Override
	public void reportX() {
		findDriver().ifPresent(ECRDriver::reportX);
	}

	@Override
	public void reportD() {
		findDriver().ifPresent(ECRDriver::reportD);
	}
	
	@Override
	public CompletableFuture<Result> reportMF(final LocalDateTime reportStart, final LocalDateTime reportEnd,
			final String chosenDirectory) {
		return findDriver()
				.map(driver -> driver.reportMF(reportStart, reportEnd, chosenDirectory))
				.orElse(CompletableFuture.completedFuture(Result.error(Messages.ECRServiceImpl_DriverNotFound)));
	}
	
	@Override
	public void cancelReceipt() {
		findDriver().ifPresent(ECRDriver::cancelReceipt);
	}
	
	private Optional<ECRDriver> findDriver() {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		final String ecrModel = prefs.get(PreferenceKey.ECR_MODEL, PreferenceKey.ECR_MODEL_DEF);
		
		return drivers.stream()
				.filter(d -> d.isECRSupported(ecrModel))
				.findFirst();
	}
}
