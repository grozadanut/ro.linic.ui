package ro.linic.ui.legacy.service.components;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.multiply;
import static ro.colibri.util.NumberUtils.smallerThan;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.colibri.util.StringUtils.isEmpty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.eclipse.e4.core.services.log.Logger;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.base.IPresentableEnum;
import ro.colibri.embeddable.Verificat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.CasaDepartment;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.Reducere;
import ro.colibri.entities.comercial.mappings.ProductReducereMapping;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class BarcodePrintable implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final String BARCODE_FIELD = "barcode";
	public static final String NAME_FIELD = "name";
	public static final String UOM_FIELD = "uom";
	public static final String PRICE_FIELD = "pricePerUom";
	public static final String CANTITATE_FIELD = "cantitate";
	public static final String LABEL_TYPE_FIELD = "labelType";
	
	public enum LabelType implements IPresentableEnum
	{
		NORMAL_LABEL("Normal"), BIG_LABEL("Fara cod bare"), NORMAL_A4("A4"), MINI_A4("A4 mini"), BROTHER("Brother");
		
		private final String name;
		
		private LabelType(final String name)
		{
			this.name = name;
		}

		@Override
		public String namestamp()
		{
			return toString();
		}

		@Override
		public String displayName()
		{
			return name;
		}
	}
	
	private final BigDecimal tvaExtractDivisor;
	
	private String barcode;
	private String name;
	private String uom;
	private BigDecimal pricePerUom;
	private int cantitate;
	private LabelType labelType;
	private boolean customLabel = false;
	
	private BigDecimal lastBuyingPriceNoTva;
	private BigDecimal promoCantitate1;
	private BigDecimal promoPrice1;
	private BigDecimal promoCantitate2;
	private BigDecimal promoPrice2;
	private ImmutableList<Reducere> allReduceri;
	
	public static ImmutableList<BarcodePrintable> fromProducts(final Collection<Product> products)
	{
		final BigDecimal tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		
		return products.stream()
				.filter(p -> !isEmpty(p.getName()) && p.getPricePerUom() != null)
				.map(p -> 
				{
					final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
					final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
					return new BarcodePrintable(p, tvaExtractDivisor);
				})
				.collect(toImmutableList());
	}
	
	public static ImmutableList<BarcodePrintable> fromOperations(final ImmutableList<Operatiune> ops, final Bundle bundle, final Logger log)
	{
		final BigDecimal tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		final ImmutableList<Product> products = BusinessDelegate.convertToProducts(ops, bundle, log);
		
		return ops.stream()
				.filter(op -> !isEmpty(op.getName()) && op.getPretVanzareUnitarCuTVA() != null && op.getCantitate() != null)
				.map(op -> 
				{
					final Optional<Product> foundProd = products.stream()
							.filter(p -> globalIsMatch(p.getBarcode(), op.getBarcode(), TextFilterMethod.EQUALS))
							.findAny();
					final BigDecimal tvaPercent = foundProd
							.map(Product::getDepartment)
							.map(CasaDepartment::getTvaPercentage)
							.orElse(tvaPercentDb);
					final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
					return new BarcodePrintable(op, foundProd, tvaExtractDivisor);
				})
				.collect(toImmutableList());
	}
	
	public static ImmutableList<BarcodePrintable> fromOperations_toAromaLabel(final Collection<Operatiune> ops, final String prefix)
	{
		return ops.stream()
				.filter(op -> !isEmpty(op.getName()) && op.getCantitate() != null)
				.map(op -> new BarcodePrintable(op.getName(), prefix, op.getCantitate().intValue()))
				.collect(toImmutableList());
	}
	
	public BarcodePrintable(final Product p, final BigDecimal tvaExtractDivisor)
	{
		this.tvaExtractDivisor = tvaExtractDivisor;
		this.barcode = p.getBarcode();
		this.name = p.getName();
		this.uom = p.getUom();
		this.pricePerUom = p.getPricePerUom();
		this.cantitate = 1;
		this.lastBuyingPriceNoTva = p.getLastBuyingPriceNoTva();
		this.allReduceri = p.getReduceri().stream()
				.map(ProductReducereMapping::getReducere)
				.collect(toImmutableList());
	}
	
	public BarcodePrintable(final String barcode, final String name, final int cantitate)
	{
		this.tvaExtractDivisor = null;
		this.barcode = barcode;
		this.name = name;
		this.cantitate = cantitate;
		this.customLabel = true;
		this.allReduceri = ImmutableList.of();
	}
	
	public BarcodePrintable(final Operatiune op, final Optional<Product> p, final BigDecimal tvaExtractDivisor)
	{
		this.tvaExtractDivisor = tvaExtractDivisor;
		this.barcode = op.getBarcode();
		this.name = op.getName();
		this.uom = op.getUom();
		this.pricePerUom = op.getPretVanzareUnitarCuTVA();
		this.cantitate = Optional.ofNullable(op)
				.filter(opp -> opp.getAccDoc() != null)
				.filter(opp -> opp.getAccDoc().getDoc().equalsIgnoreCase(AccountingDocument.PROCES_SCHIMBARE_PRET_NAME))
				.map(Operatiune::getVerificat)
				.map(Verificat::getVerificatCantitate)
				.map(BigDecimal::intValue)
				.orElseGet(() -> op.getCantitate().intValue());
		this.lastBuyingPriceNoTva = p.map(Product::getLastBuyingPriceNoTva).orElse(null);
		this.allReduceri = p.isPresent() ? 
				p.get().getReduceri().stream()
				.map(ProductReducereMapping::getReducere)
				.collect(toImmutableList()) : ImmutableList.of();
	}

	public BarcodePrintable()
	{
		this.tvaExtractDivisor = null;
		this.allReduceri = ImmutableList.of();
	}

	public String getBarcode()
	{
		return barcode;
	}

	public void setBarcode(final String barcode)
	{
		this.barcode = barcode;
	}

	public String getName()
	{
		return name;
	}

	public void setName(final String name)
	{
		this.name = name;
	}

	public String getUom()
	{
		return uom;
	}

	public void setUom(final String uom)
	{
		this.uom = uom;
	}

	public BigDecimal getPricePerUom()
	{
		return pricePerUom;
	}

	public void setPricePerUom(final BigDecimal pricePerUom)
	{
		this.pricePerUom = pricePerUom;
	}

	public int getCantitate()
	{
		return cantitate;
	}

	public void setCantitate(final int cantitate)
	{
		this.cantitate = cantitate;
	}

	public LabelType getLabelType()
	{
		return labelType;
	}
	
	public void setLabelType(final LabelType labelType)
	{
		this.labelType = labelType;
	}
	
	public BigDecimal getPromoCantitate1()
	{
		return promoCantitate1;
	}

	public void setPromoCantitate1(final BigDecimal promoCantitate1)
	{
		this.promoCantitate1 = promoCantitate1;
	}

	public BigDecimal getPromoPrice1()
	{
		return promoPrice1;
	}

	public void setPromoPrice1(final BigDecimal promoPrice1)
	{
		this.promoPrice1 = promoPrice1;
	}

	public BigDecimal getPromoCantitate2()
	{
		return promoCantitate2;
	}

	public void setPromoCantitate2(final BigDecimal promoCantitate2)
	{
		this.promoCantitate2 = promoCantitate2;
	}

	public BigDecimal getPromoPrice2()
	{
		return promoPrice2;
	}

	public void setPromoPrice2(final BigDecimal promoPrice2)
	{
		this.promoPrice2 = promoPrice2;
	}

	public boolean isNormalLabel()
	{
		return labelType != null && labelType.equals(LabelType.NORMAL_LABEL);
	}
	
	public boolean isBigLabel()
	{
		return labelType != null && labelType.equals(LabelType.BIG_LABEL);
	}
	
	public boolean isNormalA4Label(final Optional<Gestiune> gestiune)
	{
		return labelType != null && labelType.equals(LabelType.NORMAL_A4) &&
				!allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.findAny()
				.isPresent();
	}
	
	public boolean isPromoA4Label(final Optional<Gestiune> gestiune)
	{
		return labelType != null && (labelType.equals(LabelType.NORMAL_A4) || labelType.equals(LabelType.MINI_A4)) &&
				allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> smallerThan(red.getUomThreshold(), BigDecimal.ZERO))
				.findAny()
				.isPresent();
	}
	
	public boolean isCantPromoA4Label(final Optional<Gestiune> gestiune)
	{
		return labelType != null && labelType.equals(LabelType.NORMAL_A4) &&
				allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> greaterThan(red.getUomThreshold(), BigDecimal.ZERO))
				.findAny()
				.isPresent();
	}
	
	public boolean isNormalA4MiniLabel(final Optional<Gestiune> gestiune)
	{
		return labelType != null && labelType.equals(LabelType.MINI_A4) &&
				!allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.findAny()
				.isPresent();
	}
	
	public boolean isCantPromoA4MiniLabel(final Optional<Gestiune> gestiune)
	{
		return labelType != null && labelType.equals(LabelType.MINI_A4) &&
				allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> greaterThan(red.getUomThreshold(), BigDecimal.ZERO))
				.findAny()
				.isPresent();
	}
	
	public boolean isPromoLabel(final Optional<Gestiune> gestiune)
	{
		return allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> smallerThan(red.getUomThreshold(), BigDecimal.ZERO))
				.findAny()
				.isPresent();
	}
	
	public boolean isSingleCantPromoLabel(final Optional<Gestiune> gestiune)
	{
		return allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> greaterThan(red.getUomThreshold(), BigDecimal.ZERO))
				.count() == 1;
	}
	
	public boolean isDoubleCantPromoLabel(final Optional<Gestiune> gestiune)
	{
		return allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> greaterThan(red.getUomThreshold(), BigDecimal.ZERO))
				.count() == 2;
	}
	
	public BarcodePrintable fillPromoFields(final Optional<Gestiune> gestiune)
	{
		final Optional<Reducere> noThresholdPromo = allReduceri.stream()
				.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
				.filter(red -> smallerThan(red.getUomThreshold(), BigDecimal.ZERO))
				.findAny();
		
		if (noThresholdPromo.isPresent())
			promoPrice1 = pricePerUom.subtract(noThresholdPromo.get()
					.calculateDiscount(multiply(lastBuyingPriceNoTva, tvaExtractDivisor), pricePerUom, BigDecimal.ONE));
		else
		{
			final ImmutableList<Reducere> reduceriCantitative = allReduceri.stream()
					.filter(red -> red.getGestiune() == null || !gestiune.isPresent() || gestiune.get().equals(red.getGestiune()))
					.filter(red -> greaterThan(red.getUomThreshold(), BigDecimal.ZERO))
					.sorted(Comparator.comparing(Reducere::getUomThreshold))
					.limit(2)
					.collect(toImmutableList());
			
			if (reduceriCantitative.size() > 0) // promo cant 1
			{
				this.promoCantitate1 = reduceriCantitative.get(0).getUomThreshold();
				this.promoPrice1 = pricePerUom.subtract(reduceriCantitative.get(0)
						.calculateDiscount(multiply(lastBuyingPriceNoTva, tvaExtractDivisor), pricePerUom, this.promoCantitate1));
			}
			if (reduceriCantitative.size() > 1) // promo cant 2
			{
				this.promoCantitate2 = reduceriCantitative.get(1).getUomThreshold();
				this.promoPrice2 = pricePerUom.subtract(reduceriCantitative.get(1)
						.calculateDiscount(multiply(lastBuyingPriceNoTva, tvaExtractDivisor), pricePerUom, this.promoCantitate2));
			}
		}
		return this;
	}
	
	public boolean isNotA4()
	{
		return labelType != null && !labelType.equals(LabelType.NORMAL_A4);
	}
	
	public boolean isCustomLabel()
	{
		return customLabel;
	}
	
	public void setCustomLabel(final boolean customLabel)
	{
		this.customLabel = customLabel;
	}
	
	public boolean hasSomethingToPrintNotA4()
	{
		return getCantitate() > 0 && (isNotA4() || isCustomLabel());
	}
	
	@Override
	public String toString()
	{
		return "BarcodePrintable [barcode=" + barcode + ", name=" + name + ", pricePerUom=" + pricePerUom
				+ ", cantitate=" + cantitate + "]";
	}
}
