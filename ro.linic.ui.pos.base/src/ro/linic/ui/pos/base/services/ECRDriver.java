package ro.linic.ui.pos.base.services;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.model.Receipt;

public interface ECRDriver {
	public static final String ECR_MODEL_DATECS = "Datecs";
	public static final String ECR_MODEL_PARTNER = "Partner";
	
	boolean isECRSupported(String ecrModel);
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
	
	public static class Result {
		private final String error;
		
		public static Result ok() {
			return new Result(null);
		}
		
		public static Result error(final String error) {
			return new Result(error);
		}

		private Result(final String error) {
			this.error = error;
		}
		
		public boolean isOk() {
			return error == null;
		}
		
		public String error() {
			return error;
		}

		@Override
		public int hashCode() {
			return Objects.hash(error);
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
			return Objects.equals(error, other.error);
		}

		@Override
		public String toString() {
			return "Result [error=" + error + "]";
		}
	}
}
