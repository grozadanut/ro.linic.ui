package ro.linic.ui.pos.base.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ro.linic.ui.pos.base.model.Receipt;
import ro.linic.ui.pos.base.services.ECRDriver.PaymentType;
import ro.linic.ui.pos.base.services.ECRDriver.Result;

public interface ECRService {
	CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId);
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
	
	default CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType) {
		return printReceipt(receipt, paymentType, Optional.empty());
	}
}
