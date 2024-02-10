package ro.linic.ui.legacy.service.components;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.RO_LOCALE;

import java.time.format.TextStyle;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import ro.colibri.wrappers.LastYearStats;
import ro.colibri.wrappers.SaleValue;
import ro.colibri.wrappers.SalesPerHours;
import ro.colibri.wrappers.SalesPerOperators;

public class JasperChartSerie
{
	private String name;
	private String category;
	private Double value;
	
	public static ImmutableList<JasperChartSerie> from(final LastYearStats stats)
	{
		return stats.getStatsTimeframe().stream()
				.flatMap(month ->
				{
					return Stream.of(new JasperChartSerie("Achizitii", month.getDisplayName(TextStyle.FULL, RO_LOCALE), stats.getAcquisitions().get(month)),
							new JasperChartSerie("Vanzari", month.getDisplayName(TextStyle.FULL, RO_LOCALE), stats.getSales().get(month)),
							new JasperChartSerie("Profit", month.getDisplayName(TextStyle.FULL, RO_LOCALE), stats.getProfit().get(month)));
				}).collect(toImmutableList());
	}
	
	public static ImmutableList<JasperChartSerie> fromVanzari(final SalesPerHours sales)
	{
		return IntStream.range(0, 24).mapToObj(i->i).flatMap(i ->
		{
			return sales.getSales().keySet().stream().sorted().map(dayOfWeek ->
			{
				return new JasperChartSerie(dayOfWeek.getDisplayName(TextStyle.FULL, RO_LOCALE), String.valueOf(i),
						sales.getSales().get(dayOfWeek).getOrDefault(i, SaleValue.EMPTY).getSalesLei());
			});
		}).collect(toImmutableList());
	}
	
	public static ImmutableList<JasperChartSerie> fromOpsPerMinute(final SalesPerHours sales)
	{
		return IntStream.range(0, 24).mapToObj(i->i).flatMap(i ->
		{
			return sales.getSales().keySet().stream().sorted().map(dayOfWeek ->
			{
				return new JasperChartSerie(dayOfWeek.getDisplayName(TextStyle.FULL, RO_LOCALE), String.valueOf(i),
						sales.getSales().get(dayOfWeek).getOrDefault(i, SaleValue.EMPTY).getOpsPerMinute());
			});
		}).collect(toImmutableList());
	}
	
	public static ImmutableList<JasperChartSerie> fromVanzari(final SalesPerOperators sales)
	{
		return IntStream.range(0, 24).mapToObj(i->i).flatMap(i ->
		{
			return sales.getSales().keySet().stream().sorted().map(operator ->
			{
				return new JasperChartSerie(operator, String.valueOf(i),
						sales.getSales().get(operator).getOrDefault(i, SaleValue.EMPTY).getSalesLei());
			});
		}).collect(toImmutableList());
	}
	
	public static ImmutableList<JasperChartSerie> fromOpsPerMinute(final SalesPerOperators sales)
	{
		return IntStream.range(0, 24).mapToObj(i->i).flatMap(i ->
		{
			return sales.getSales().keySet().stream().sorted().map(operator ->
			{
				return new JasperChartSerie(operator, String.valueOf(i),
						sales.getSales().get(operator).getOrDefault(i, SaleValue.EMPTY).getOpsPerMinute());
			});
		}).collect(toImmutableList());
	}

	public JasperChartSerie(final String name, final String category, final Double value)
	{
		super();
		this.name = name;
		this.category = category;
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public void setName(final String name)
	{
		this.name = name;
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(final String category)
	{
		this.category = category;
	}

	public Double getValue()
	{
		return value;
	}

	public void setValue(final Double value)
	{
		this.value = value;
	}
}
