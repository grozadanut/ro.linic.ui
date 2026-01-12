package ro.linic.ui.legacy.jasper.datasource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.util.NumberUtils;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.preferences.PreferenceKey;

public class AccDocReceptieDatasource extends AccDocDatasource {
	public static List<BigDecimal> VAT_RATES_RO = List.of(new BigDecimal("0.21"), new BigDecimal("0.11"),
			new BigDecimal("0.19"), new BigDecimal("0.09"), new BigDecimal("0.05"), new BigDecimal("0"));

	public AccDocReceptieDatasource(final AccountingDocument doc) {
		super(doc);
	}

	@Override
	public Object getFieldValue(final JRField field) throws JRException {
		final String fieldName = field.getName();
		final Operatiune op = ops.get(index);

		switch (fieldName) {
		case "categorie":
			final Bundle bundle = FrameworkUtil.getBundle(PreferenceKey.class);
			final IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(bundle.getSymbolicName());
			final boolean groupByVAT = prefs.getBoolean(PreferenceKey.RECEPTIE_GROUPBY_VAT_KEY, false);
			return op.getCategorie() + (groupByVAT ? PresentationUtils.SPACE + calculateVAT(op) : "");
		}

		return super.getFieldValue(field);
	}

	private String calculateVAT(final Operatiune op) {
		return PresentationUtils.displayPercentage(getAchizitieTvaPercentCalculated(op));
	}

	public BigDecimal getAchizitieTvaPercentCalculated(final Operatiune op) {
		if (NumberUtils.equal(op.getValoareAchizitieFaraTVA(), BigDecimal.ZERO))
			return BigDecimal.ZERO;
		return findClosest(VAT_RATES_RO, NumberUtils
				.divide(op.getValoareAchizitieTVA(), op.getValoareAchizitieFaraTVA(), 2, RoundingMode.HALF_EVEN).abs());
	}

	/**
	 * Returns the value in the list that is closest to specific input value.
	 */
	public static BigDecimal findClosest(final List<BigDecimal> list, final BigDecimal value) {
		return list.stream()
				.min(Comparator.comparing(a -> value.subtract(a).abs()))
				.orElse(BigDecimal.ZERO);
	}
}
