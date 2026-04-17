package ro.linic.ui.pos.driver.zfplab.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.services.ECRDriver;
import ro.linic.ui.pos.driver.zfplab.Messages;
import ro.linic.ui.pos.driver.zfplab.internal.FP;
import ro.linic.ui.pos.driver.zfplab.preferences.PreferenceKey;

@Component
public class ZFPLabECRDriver implements ECRDriver {

	@Override
	public boolean isECRSupported(final String ecrModel) {
		return ECR_MODEL_TREMOL.equalsIgnoreCase(ecrModel);
	}

	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final Map<PaymentType, BigDecimal> payments,
			final Optional<String> taxId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reportZ() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportX() {
		// TODO Auto-generated method stub
	}

	@Override
	public void reportD() {
		// TODO Auto-generated method stub

	}

	@Override
	public CompletableFuture<Result> reportMF(final LocalDateTime reportStart, final LocalDateTime reportEnd,
			final String chosenDirectory) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public CompletableFuture<Result> readReceipts(final LocalDateTime reportStart, final LocalDateTime reportEnd) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public void cancelReceipt() {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		
		final String serverAddress = prefs.get(PreferenceKey.SERVER_ADDRESS, PreferenceKey.SERVER_ADDRESS_DEF);
		final String deviceIp = prefs.get(PreferenceKey.ECR_IP, null);
		final int devicePort = prefs.getInt(PreferenceKey.ECR_PORT, PreferenceKey.ECR_PORT_DEF);
		final String devicePassword = prefs.get(PreferenceKey.ECR_PASSWORD, PreferenceKey.ECR_PASSWORD_DEF);
		
		if (deviceIp == null)
			new RuntimeException(Messages.ErrorECRDriver_SetIp);
		
		final FP fp = new FP();
        fp.ServerAddress = serverAddress;
        try {
			fp.ServerCloseDeviceConnection();
			fp.ServerSetDeviceTcpSettings(deviceIp, devicePort, devicePassword);
			fp.CancelFiscReceipt();
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
