package ro.linic.ui.pos.driver.zfplab.services.impl;

import static ro.flexbiz.util.commons.NumberUtils.smallerThan;
import static ro.flexbiz.util.commons.PresentationUtils.safeString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import ro.flexbiz.util.commons.NumberUtils;
import ro.flexbiz.util.commons.StringUtils;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.services.ECRDriver;
import ro.linic.ui.pos.driver.zfplab.Messages;
import ro.linic.ui.pos.driver.zfplab.internal.FP;
import ro.linic.ui.pos.driver.zfplab.internal.OptionCustomerReceiptPrintType;
import ro.linic.ui.pos.driver.zfplab.internal.OptionDisplay;
import ro.linic.ui.pos.driver.zfplab.internal.OptionFiscalReceiptPrintType;
import ro.linic.ui.pos.driver.zfplab.internal.OptionPaymentType;
import ro.linic.ui.pos.driver.zfplab.internal.OptionPrinting;
import ro.linic.ui.pos.driver.zfplab.internal.OptionVATClass;
import ro.linic.ui.pos.driver.zfplab.internal.OptionZeroing;
import ro.linic.ui.pos.driver.zfplab.preferences.PreferenceKey;

@Component
public class ZFPLabECRDriver implements ECRDriver {
	private static final ILog log = UIUtils.logger(ZFPLabECRDriver.class);
	
	@Override
	public boolean isECRSupported(final String ecrModel) {
		return ECR_MODEL_TREMOL.equalsIgnoreCase(ecrModel);
	}

	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId) {
		if (receipt == null || receipt.getLines().isEmpty())
			return CompletableFuture.completedFuture(Result.ok());
		
		if (NumberUtils.smallerThan(receipt.total(), BigDecimal.ZERO))
			return CompletableFuture.completedFuture(Result.ok());

		try {
			switch(paymentType) {
			case CASH:
				return CompletableFuture.completedFuture(printReceiptCash(receipt, taxId));
			case CARD:
				return CompletableFuture.completedFuture(printReceiptCard(receipt, taxId));
			default:
				return CompletableFuture.failedFuture(new IllegalArgumentException(NLS.bind(Messages.ECRDriver_IllegalPaymentType, paymentType)));
			}
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
			return CompletableFuture.completedFuture(Result.error(e.getMessage()));
		}
	}
	
	@Override
	public CompletableFuture<Result> printReceipt(final Receipt receipt, final Map<PaymentType, BigDecimal> payments,
			final Optional<String> taxId) {
		if (receipt == null || receipt.getLines().isEmpty())
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(receipt.total(), BigDecimal.ZERO))
			return CompletableFuture.completedFuture(Result.ok());
		
		if (smallerThan(payments.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO), receipt.total()))
			return CompletableFuture.failedFuture(new IllegalArgumentException(Messages.ECRDriver_PaymentSmallerThanTotalErr));

		try {
			final FP fp = openDeviceConnection();
			addSaleLines(fp, receipt, taxId);
			/* payments
			 */
			for (final Entry<PaymentType, BigDecimal> paymentEntry : payments.entrySet()) {
				fp.Payment(mapPaymentType(paymentEntry.getKey()), paymentEntry.getValue().doubleValue());
			}
			
			// close receipt
			fp.CloseReceipt();
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
			return CompletableFuture.completedFuture(Result.error(e.getMessage()));
		}
		return CompletableFuture.completedFuture(Result.ok());
	}

	private OptionPaymentType mapPaymentType(final PaymentType paymentType) {
		/*
		 * Payment_0("0"), // NUMERAR
	     * Payment_1("1"), // CARD
	     * Payment_2("2"), 
	     * Payment_3("3"), // TICHETE
	     * Payment_4("4"), // BONURI
	     * Payment_5("5"), // VOUCHER
	     * Payment_6("6"), // CREDIT
	     * Payment_7("7"), // MODERNE
	     * Payment_8("8"), // ALTE
	     * Payment_9("9");
		 */
		switch (paymentType) {
		case CASH: return OptionPaymentType.Payment_0;
		case CARD: return OptionPaymentType.Payment_1;
		case CREDIT: return OptionPaymentType.Payment_6;
		case MEAL_TICKET: return OptionPaymentType.Payment_3;
		case VALUE_TICKET: return OptionPaymentType.Payment_4;
		case VOUCHER: return OptionPaymentType.Payment_5;
		case MODERN_PAYMENT: return OptionPaymentType.Payment_7;
		case OTHER: return OptionPaymentType.Payment_8;
		default:
			throw new IllegalArgumentException(NLS.bind(Messages.ECRDriver_PaymentTypeError, paymentType));
		}
	}
	
	private Result printReceiptCash(final Receipt receipt, final Optional<String> taxId) throws Exception
	{
		final FP fp = openDeviceConnection();
		addSaleLines(fp, receipt, taxId);
		
		// close receipt
		fp.CashPayCloseReceipt();
		fp.ServerCloseDeviceConnection();
		return Result.ok();
	}
	
	private Result printReceiptCard(final Receipt receipt, final Optional<String> taxId) throws Exception
	{
		final FP fp = openDeviceConnection();
		addSaleLines(fp, receipt, taxId);
		
		// close receipt
		fp.PayExactSum(OptionPaymentType.Payment_1);
		fp.CloseReceipt();
		fp.ServerCloseDeviceConnection();
		return Result.ok();
	}
	
	private void addSaleLines(final FP fp, final Receipt receipt, final Optional<String> taxId) throws Exception
	{
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
		
		final double operatorNumber = prefs.getDouble(PreferenceKey.OPERATOR, PreferenceKey.OPERATOR_DEF);
		final String operatorPassword = prefs.get(PreferenceKey.OPERATOR_PASSWORD, PreferenceKey.OPERATOR_PASSWORD_DEF);
		
		// Open fiscal receipt
		if (taxId.isPresent() && StringUtils.notEmpty(taxId.get()))
			fp.OpenSpecialDocumentReceipt(operatorNumber, operatorPassword, OptionCustomerReceiptPrintType.Step_by_step_printing,
					taxId.get(), null, null, null, null, null, null);
		else
			fp.OpenReceipt(operatorNumber, operatorPassword, OptionFiscalReceiptPrintType.Step_by_step_printing);
		
		// sales
		for (final ReceiptLine line : receipt.getLines()) {
			final OptionVATClass vatClass = switch (safeString(line.getTaxCode(), prefs.get(PreferenceKey.ECR_TAX_CODE, PreferenceKey.ECR_TAX_CODE_DEF)).toUpperCase()) {
			case "1":
			case "A":
				yield OptionVATClass.VAT_Class_A;
			case "2":
			case "B":
				yield OptionVATClass.VAT_Class_B;
			case "3":
			case "C":
				yield OptionVATClass.VAT_Class_C;
			case "4":
			case "D":
				yield OptionVATClass.VAT_Class_D;
			case "5":
			case "E":
				yield OptionVATClass.VAT_Class_E;
			case "6":
			case "F":
				yield OptionVATClass.Alte_taxe;
			default:
				throw new IllegalArgumentException("Unexpected value: " + safeString(line.getTaxCode(), prefs.get(PreferenceKey.ECR_TAX_CODE, PreferenceKey.ECR_TAX_CODE_DEF)));
			};
		
			// Parse the department number (e.g. 1)
			final int depCode = NumberUtils.parseToInt(safeString(line.getDepartmentCode(),
					prefs.get(PreferenceKey.ECR_DEPT, PreferenceKey.ECR_DEPT_DEF)));
			final Double lineDiscount = line.getAllowanceCharge() != null ?
					line.getAllowanceCharge().amountWithSign().doubleValue() : null;
			final String pluNameWithUom = StringUtils.truncate(line.getName(), 30) + "\u0060" + StringUtils.truncate(line.getUom(), 3);
			fp.SellPLUwithSpecifiedVATfromDep(pluNameWithUom, vatClass, line.getPrice().doubleValue(), line.getQuantity().doubleValue(),
					null, lineDiscount, null, null, depCode, null, null);
		}
		
		// Subtotal
		fp.Subtotal(OptionPrinting.Yes, OptionDisplay.No, receipt.getAllowanceCharge() != null ?
				receipt.getAllowanceCharge().amountWithSign().doubleValue() : null, null);
	}

	@Override
	public void reportZ() {
		try {
			final Bundle bundle = FrameworkUtil.getBundle(getClass());
			final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
			
			final FP fp = openDeviceConnection();
        	if (prefs.getBoolean(PreferenceKey.REPORT_Z_AND_D, PreferenceKey.REPORT_Z_AND_D_DEF))
        		fp.PrintDepartmentReport(OptionZeroing.Zeroing);
			fp.PrintDailyReport(OptionZeroing.Zeroing);
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reportX() {
		try {
        	final FP fp = openDeviceConnection();
			fp.PrintDailyReport(OptionZeroing.Not_zeroing);
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reportD() {
		try {
        	final FP fp = openDeviceConnection();
			fp.PrintDepartmentReport(OptionZeroing.Not_zeroing);
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
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
        try {
        	final FP fp = openDeviceConnection();
			fp.CancelReceipt();
			fp.ServerCloseDeviceConnection();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private FP openDeviceConnection() throws Exception {
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
        fp.ServerCloseDeviceConnection();
        fp.ServerSetDeviceTcpSettings(deviceIp, devicePort, devicePassword);
        return fp;
	}
}
