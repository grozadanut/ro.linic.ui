package ro.linic.ui.pos.base.tests.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static ro.flexbiz.util.commons.NumberUtils.add;
import static ro.flexbiz.util.commons.NumberUtils.multiply;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.Test;

import ro.linic.ui.pos.base.Messages;
import ro.linic.ui.pos.base.model.AllowanceCharge;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.model.ReceiptLine;
import ro.linic.ui.pos.base.services.impl.DudeECRDriver;

public class DudeECRDriverTest {
	@Test
    void printSimpleReceiptTest() {
		final AtomicReference<String> result = new AtomicReference<String>();
		
		final DudeECRDriver driver = new DudeECRDriver();
		driver.setCommandSender(commands -> {
			result.set(commands.toString());
			return Optional.empty();
		});
		driver.setPrefs(mock(IEclipsePreferences.class));
		
		final ReceiptLine line = new ReceiptLine();
		line.setName("product");
		line.setUom("pcs");
		line.setPrice(new BigDecimal("10"));
		line.setQuantity(new BigDecimal("21"));
		line.setDepartmentCode("2");
		line.setTaxCode("3");
		line.setAllowanceCharge(new AllowanceCharge(true, new BigDecimal("2"))); // charge
		line.setTotal(add(multiply(line.getQuantity(), line.getPrice()),
				Optional.ofNullable(line.getAllowanceCharge())
				.map(AllowanceCharge::amountWithSign)
				.orElse(null)));
		
		final Receipt receipt = new Receipt();
		receipt.setLines(List.of(line));
		receipt.setAllowanceCharge(new AllowanceCharge(false, new BigDecimal("9"))); // allowance
		
		driver.printReceipt(receipt , PaymentType.CASH, Optional.of("RO123456"));
		
		assertEquals(result.get().replaceAll("\\s", ""), """
				48,null[\\t]null[\\t]null[\\t]I[\\t]RO123456[\\t]
				49,product[\\t]3[\\t]10[\\t]21[\\t]3[\\t]2[\\t]2[\\t]pcs[\\t]
				51,1[\\t]1[\\t]4[\\t]9[\\t]
				53,0[\\t][\\t]
				56
				47, MULTUMIM ! [\\t]
				35, VA MAI ASTEPTAM [\\t]
				""".replaceAll("\\s", ""));
	}
	
	@Test
    void givenOnePaymentType_whenCloseReceiptNewApi_shouldWorkAsOldApi() {
		final AtomicReference<String> result = new AtomicReference<String>();
		
		final DudeECRDriver driver = new DudeECRDriver();
		driver.setCommandSender(commands -> {
			result.set(commands.toString());
			return Optional.empty();
		});
		driver.setPrefs(mock(IEclipsePreferences.class));
		
		final ReceiptLine line = new ReceiptLine();
		line.setName("product");
		line.setUom("pcs");
		line.setPrice(new BigDecimal("10"));
		line.setQuantity(new BigDecimal("21"));
		line.setDepartmentCode("2");
		line.setTaxCode("3");
		line.setAllowanceCharge(new AllowanceCharge(true, new BigDecimal("2"))); // charge
		line.setTotal(add(multiply(line.getQuantity(), line.getPrice()),
				Optional.ofNullable(line.getAllowanceCharge())
				.map(AllowanceCharge::amountWithSign)
				.orElse(null)));
		
		final Receipt receipt = new Receipt();
		receipt.setLines(List.of(line));
		receipt.setAllowanceCharge(new AllowanceCharge(false, new BigDecimal("9"))); // allowance
		
		driver.printReceipt(receipt, Map.of(PaymentType.CASH, receipt.total()), Optional.of("RO123456"));
		
		assertEquals(result.get().replaceAll("\\s", ""), """
				48,null[\\t]null[\\t]null[\\t]I[\\t]RO123456[\\t]
				49,product[\\t]3[\\t]10[\\t]21[\\t]3[\\t]2[\\t]2[\\t]pcs[\\t]
				51,1[\\t]1[\\t]4[\\t]9[\\t]
				53,0[\\t]203[\\t]
				56
				47, MULTUMIM ! [\\t]
				35, VA MAI ASTEPTAM [\\t]
				""".replaceAll("\\s", ""));
	}
	
	@Test
    void givenMultiplePaymentTypes_whenCloseReceipt_shouldPayMultipleLines() {
		final AtomicReference<String> result = new AtomicReference<String>();
		
		final DudeECRDriver driver = new DudeECRDriver();
		driver.setCommandSender(commands -> {
			result.set(commands.toString());
			return Optional.empty();
		});
		driver.setPrefs(mock(IEclipsePreferences.class));
		
		final ReceiptLine line = new ReceiptLine();
		line.setName("product");
		line.setUom("pcs");
		line.setPrice(new BigDecimal("10"));
		line.setQuantity(new BigDecimal("21"));
		line.setDepartmentCode("2");
		line.setTaxCode("3");
		line.setAllowanceCharge(new AllowanceCharge(true, new BigDecimal("2"))); // charge
		line.setTotal(add(multiply(line.getQuantity(), line.getPrice()),
				Optional.ofNullable(line.getAllowanceCharge())
				.map(AllowanceCharge::amountWithSign)
				.orElse(null)));
		
		final Receipt receipt = new Receipt();
		receipt.setLines(List.of(line));
		receipt.setAllowanceCharge(new AllowanceCharge(false, new BigDecimal("9"))); // allowance
		
		// receipt total is: 10*21 = 210+2-9 = 203
		driver.printReceipt(receipt, Map.of(
				PaymentType.CASH, new BigDecimal("1"),
				PaymentType.CARD, new BigDecimal("2"),
				PaymentType.CREDIT, new BigDecimal("3"),
				PaymentType.MEAL_TICKET, new BigDecimal("4"),
				PaymentType.VALUE_TICKET, new BigDecimal("5"),
				PaymentType.VOUCHER, new BigDecimal("6"),
				PaymentType.MODERN_PAYMENT, new BigDecimal("7"),
				PaymentType.OTHER, new BigDecimal("175")), Optional.of("RO123456"));
		
		assertEquals(result.get().replaceAll("\\s", ""), """
				48,null[\\t]null[\\t]null[\\t]I[\\t]RO123456[\\t]
				49,product[\\t]3[\\t]10[\\t]21[\\t]3[\\t]2[\\t]2[\\t]pcs[\\t]
				51,1[\\t]1[\\t]4[\\t]9[\\t]
				53,0[\\t]1[\\t]
				53,1[\\t]2[\\t]
				53,2[\\t]3[\\t]
				53,3[\\t]4[\\t]
				53,4[\\t]5[\\t]
				53,5[\\t]6[\\t]
				53,6[\\t]7[\\t]
				53,8[\\t]175[\\t]
				56
				47, MULTUMIM ! [\\t]
				35, VA MAI ASTEPTAM [\\t]
				""".replaceAll("\\s", ""));
	}
	
	@Test
    void givenMultiplePaymentTypes_whenPaymentSmallerThanReceipt_shouldThrowException() throws InterruptedException, ExecutionException {
		final AtomicReference<String> result = new AtomicReference<String>();
		
		final DudeECRDriver driver = new DudeECRDriver();
		driver.setCommandSender(commands -> {
			result.set(commands.toString());
			return Optional.empty();
		});
		driver.setPrefs(mock(IEclipsePreferences.class));
		
		final ReceiptLine line = new ReceiptLine();
		line.setName("product");
		line.setUom("pcs");
		line.setPrice(new BigDecimal("10"));
		line.setQuantity(new BigDecimal("21"));
		line.setDepartmentCode("2");
		line.setTaxCode("3");
		line.setAllowanceCharge(new AllowanceCharge(true, new BigDecimal("2"))); // charge
		line.setTotal(add(multiply(line.getQuantity(), line.getPrice()),
				Optional.ofNullable(line.getAllowanceCharge())
				.map(AllowanceCharge::amountWithSign)
				.orElse(null)));
		
		final Receipt receipt = new Receipt();
		receipt.setLines(List.of(line));
		receipt.setAllowanceCharge(new AllowanceCharge(false, new BigDecimal("9"))); // allowance
		
		// receipt total is: 10*21 = 210+2-9 = 203
		final ExecutionException ex = assertThrows(ExecutionException.class, () -> driver.printReceipt(receipt, Map.of(
				PaymentType.CASH, new BigDecimal("1"),
				PaymentType.CARD, new BigDecimal("2")), Optional.of("RO123456")).get());
		
		assertEquals(ex.getMessage(), "java.lang.IllegalArgumentException: " + Messages.ECRDriver_PaymentSmallerThanTotalErr);
	}
}
