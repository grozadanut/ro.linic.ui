package ro.linic.ui.pos.base.services;

import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;

public interface ECRDriver {
	public static final String ECR_MODEL_DATECS = "Datecs";
	public static final String ECR_MODEL_PARTNER = "Partner";
	public static final String ECR_MODEL_TREMOL = "Tremol";
	
	boolean isECRSupported(String ecrModel);
	CompletableFuture<Result> printReceipt(final Receipt receipt, final PaymentType paymentType, final Optional<String> taxId);
	CompletableFuture<Result> printReceipt(Receipt receipt, Map<PaymentType, BigDecimal> payments, Optional<String> taxId);
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
	/**
	 * Read Fiscal Receipts from the printer.
	 * 
	 * @param reportStart the start of the period you want to read receipts
	 * @param reportEnd the end of the period you want to read receipts
	 * @return the result code
	 */
	public CompletableFuture<Result> readReceipts(LocalDateTime reportStart, LocalDateTime reportEnd);
	public void cancelReceipt();
	
	public static class Result {
		private final String error;
		private final String info;
		
		public static Result ok() {
			return new Result(null, "");
		}
		
		public static Result ok(final String info) {
			return new Result(null, info);
		}
		
		public static Result error(final String error) {
			return new Result(error, "");
		}

		private Result(final String error, final String info) {
			this.error = error;
			this.info = info;
		}
		
		public boolean isOk() {
			return isEmpty(error);
		}
		
		public String error() {
			return error;
		}
		
		public String info() {
			return info;
		}

		@Override
		public int hashCode() {
			return Objects.hash(error, info);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Result other = (Result) obj;
			return Objects.equals(error, other.error) && Objects.equals(info, other.info);
		}

		@Override
		public String toString() {
			return "Result [error=" + error + ", info=" + info + "]";
		}
	}
}
