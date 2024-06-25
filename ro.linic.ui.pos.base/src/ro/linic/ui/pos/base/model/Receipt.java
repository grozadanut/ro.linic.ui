package ro.linic.ui.pos.base.model;

import static ro.linic.util.commons.NumberUtils.add;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ro.linic.ui.base.services.model.JavaBean;

public class Receipt extends JavaBean {
	public static final String ID_FIELD = "id";
	public static final String LINES_FIELD = "lines";
	public static final String ALLOWANCE_CHARGE_FIELD = "allowanceCharge";
	public static final String CREATION_TIME_FIELD = "creationTime";
	
	private Long id;
	private AllowanceCharge allowanceCharge;
	private Instant creationTime = Instant.now();

	transient private List<ReceiptLine> lines = new ArrayList<>();
	
	/**
	 * @param id unique id of this receipt
	 * @param lines that compose this Receipt
	 * @param allowanceCharge that applies to the receipt as a whole, after subtotal
	 */
	public Receipt(final Long id, final List<ReceiptLine> lines, final AllowanceCharge allowanceCharge) {
		super();
		this.id = id;
		this.lines = lines;
		this.allowanceCharge = allowanceCharge;
	}
	
	public Receipt() {
	}

	/**
	 * @return total including taxes and allowance or charge amount
	 */
	public BigDecimal total() {
		return lines.stream()
				.map(ReceiptLine::getTotal)
				.reduce(BigDecimal::add)
				.map(subtotal -> add(subtotal, Optional.ofNullable(allowanceCharge)
						.map(AllowanceCharge::amountWithSign)
						.orElse(null)))
				.orElse(BigDecimal.ZERO);
	}
	
	public BigDecimal taxTotal() {
		return lines.stream()
				.map(ReceiptLine::getTaxTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		firePropertyChange("id", this.id, this.id = id);
	}

	public List<ReceiptLine> getLines() {
		return lines;
	}

	public void setLines(final List<ReceiptLine> lines) {
		firePropertyChange("lines", this.lines, this.lines = lines);
	}

	public AllowanceCharge getAllowanceCharge() {
		return allowanceCharge;
	}

	public void setAllowanceCharge(final AllowanceCharge allowanceCharge) {
		firePropertyChange("allowanceCharge", this.allowanceCharge, this.allowanceCharge = allowanceCharge);
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(final Instant creationTime) {
		firePropertyChange("creationTime", this.creationTime, this.creationTime = creationTime);
	}

	@Override
	public String toString() {
		return "Receipt [id=" + id + ", lines=" + lines + ", allowanceCharge=" + allowanceCharge + ", creationTime="
				+ creationTime + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(allowanceCharge, creationTime, id, lines);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Receipt other = (Receipt) obj;
		return Objects.equals(allowanceCharge, other.allowanceCharge)
				&& Objects.equals(creationTime, other.creationTime) && Objects.equals(id, other.id)
				&& Objects.equals(lines, other.lines);
	}
}
