package ro.linic.ui.pos.base.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.services.ECRDriver.Result;

public interface ECRService {
	CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId, List<String> freeText);
	CompletableFuture<Result> printReceipt(Receipt receipt, Map<PaymentType, BigDecimal> payments, Optional<String> taxId, List<String> freeText);
	CompletableFuture<Result> printNonFiscalReceipt(List<String> lines);
	public void reportZ();
	public void reportX();
	public void reportD();
	/**
	 * Download the MF report from the ECR
	 * 
	 * @param reportStart report query start time
	 * @param reportEnd report query end time
	 * @param chosenDirectory the location where you want to save the report
	 * @return the path of the file that shows the result of the execution
	 */
	public CompletableFuture<Result> reportMF(LocalDateTime reportStart, LocalDateTime reportEnd, final String chosenDirectory);
	public void cancelReceipt();
	/**
	 * Read Fiscal Receipts from the printer.
	 * 
	 * @param reportStart the start of the period you want to read receipts
	 * @param reportEnd the end of the period you want to read receipts
	 * @return the result code
	 */
	public CompletableFuture<Result> readReceipts(LocalDateTime reportStart, LocalDateTime reportEnd);
	
	default CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType) {
		return printReceipt(receipt, paymentType, Optional.empty(), List.of());
	}
	
	default CompletableFuture<Result> printReceipt(final Receipt receipt, final Map<PaymentType, BigDecimal> payments) {
		return printReceipt(receipt, payments, Optional.empty(), List.of());
	}
}
