package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.calculateAdaosPercent;
import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.greaterThanOrEqual;
import static ro.colibri.util.NumberUtils.smallerThan;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.PresentationUtils.displayPercentage;
import static ro.colibri.util.PresentationUtils.displayPercentageRaw;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.GrupaInteres;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class PrintOfertaDiscountDialog extends Dialog
{
	private ImmutableList<Product> products;
	private ImmutableList<GrupaInteres> allGrupeInteres;
	private BigDecimal tvaPercentDb;
	private GrupaInteres initialSelection;
	
	private Text threshold;
	private Combo grupaInteres;
	private Text discount;
	private Text cappedAdaos;
	private Button withImage;
	
	private Bundle bundle;
	private Logger log;
	
	public PrintOfertaDiscountDialog(final Shell parent, final Bundle bundle, final Logger log, final ImmutableList<Product> products)
	{
		super(parent);
		this.bundle = bundle;
		this.log = log;
		this.products = products;
		allGrupeInteres = BusinessDelegate.grupeInteres_Sync();
		this.tvaPercentDb = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.PrintOfertaDiscountDialog_Title);
		
		final Label descriptionLabel = new Label(contents, SWT.NONE);
		descriptionLabel.setText(Messages.PrintOfertaDiscountDialog_Description);
		final GridData descriptionGD = new GridData();
		descriptionGD.horizontalSpan = 2;
		descriptionLabel.setLayoutData(descriptionGD);
		UIUtils.setFont(descriptionLabel);
		
		final Label thresholdLabel = new Label(contents, SWT.NONE);
		thresholdLabel.setText(Messages.PrintOfertaDiscountDialog_Threshold);
		UIUtils.setFont(thresholdLabel);
		
		threshold = new Text(contents, SWT.SINGLE | SWT.BORDER);
		threshold.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(threshold);
		threshold.setText("15"); //$NON-NLS-1$
		
		final Label grupaInteresLabel = new Label(contents, SWT.NONE);
		grupaInteresLabel.setText(Messages.PrintOfertaDiscountDialog_InterestGroup);
		UIUtils.setFont(grupaInteresLabel);
		
		grupaInteres = new Combo(contents, SWT.DROP_DOWN);
		grupaInteres.setItems(allGrupeInteres.stream()
				.map(GrupaInteres::displayName)
				.toArray(String[]::new));
		grupaInteres.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(grupaInteres);
		
		final Label discountLabel = new Label(contents, SWT.NONE);
		discountLabel.setText(Messages.PrintOfertaDiscountDialog_Discount);
		UIUtils.setFont(discountLabel);
		
		discount = new Text(contents, SWT.SINGLE | SWT.BORDER);
		discount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(discount);
		
		final Label adaosLabel = new Label(contents, SWT.NONE);
		adaosLabel.setText(Messages.PrintOfertaDiscountDialog_MinMargin);
		UIUtils.setFont(adaosLabel);
		
		cappedAdaos = new Text(contents, SWT.SINGLE | SWT.BORDER);
		cappedAdaos.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(cappedAdaos);
		
		new Label(contents, SWT.NONE);
		withImage = new Button(contents, SWT.CHECK);
		withImage.setText(Messages.PrintOfertaDiscountDialog_WithImage);
		withImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(withImage);
		
		if (initialSelection != null)
		{
			grupaInteres.select(allGrupeInteres.indexOf(initialSelection));
			updateFields(initialSelection);
		}
		addListeners();
		return contents;
	}
	
	private void addListeners()
	{
		grupaInteres.addModifyListener(e -> selectedGrupaInteres()
				.ifPresent(grupa -> updateFields(grupa)));
		discount.addModifyListener(e -> getButton(IDialogConstants.OK_ID).setEnabled(greaterThanOrEqual(getDiscount(), BigDecimal.ZERO)));
		cappedAdaos.addModifyListener(e -> getButton(IDialogConstants.OK_ID).setEnabled(greaterThanOrEqual(getCappedAdaos(), BigDecimal.ZERO)));
	}
	
	private void updateFields(final GrupaInteres grupa)
	{
		discount.setText(displayPercentageRaw(grupa.getDiscountPercentage()));
		cappedAdaos.setText(displayPercentageRaw(grupa.getCappedAdaosPerc()));
	}

	@Override
	protected void okPressed()
	{
		try
		{
			generateAdaosWarnings();
			JasperReportManager.instance(bundle, log).printOfertaCuDiscount(bundle, products, getDiscount(), getCappedAdaos(), withImage.getSelection());
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
		super.okPressed();
	}
	
	private void generateAdaosWarnings()
	{
		final BigDecimal threshold = getThreshold();
		
		final String warnings = products.stream()
				.filter(p -> p.getLastBuyingPriceNoTva() != null)
				.filter(p ->
				{
					final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
					final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
					
					final BigDecimal lastBuyingPriceWithTva = p.getLastBuyingPriceNoTva().multiply(tvaExtractDivisor);
					return smallerThan(calculateAdaosPercent(lastBuyingPriceWithTva, p.getPricePerUomAfterDiscount(getDiscount(), getCappedAdaos(), tvaExtractDivisor)), threshold);
				})
				.map(p ->
				{
					final BigDecimal tvaPercent = p.deptTvaPercentage().orElse(tvaPercentDb);
					final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
					
					final BigDecimal lastBuyingPriceWithTva = p.getLastBuyingPriceNoTva().multiply(tvaExtractDivisor);
					final BigDecimal pvDupaDisc = p.getPricePerUomAfterDiscount(getDiscount(), getCappedAdaos(), tvaExtractDivisor);
					final BigDecimal adaosPercent = calculateAdaosPercent(lastBuyingPriceWithTva, pvDupaDisc);
					return MessageFormat.format(Messages.PrintOfertaDiscountDialog_PriceWarning,
							NEWLINE, p.getName(), displayPercentage(adaosPercent),
							displayBigDecimal(lastBuyingPriceWithTva),
							displayBigDecimal(pvDupaDisc),
							displayBigDecimal(pvDupaDisc.subtract(lastBuyingPriceWithTva)));
				})
				.collect(Collectors.joining(NEWLINE));
		
		if (!isEmpty(warnings))
			InfoDialog.open(getShell(), NLS.bind(Messages.PrintOfertaDiscountDialog_MarginBelow, displayPercentage(threshold)),
					MessageFormat.format(Messages.PrintOfertaDiscountDialog_MarginWarning,
							NEWLINE, displayPercentage(threshold), warnings));
	}
	
	public PrintOfertaDiscountDialog initialSelection(final GrupaInteres initialSelection)
	{
		this.initialSelection = initialSelection;
		return this;
	}
	
	public BigDecimal getThreshold()
	{
		return extractPercentage(threshold.getText());
	}

	public BigDecimal getDiscount()
	{
		return extractPercentage(discount.getText());
	}
	
	public BigDecimal getCappedAdaos()
	{
		return extractPercentage(cappedAdaos.getText());
	}
	
	private Optional<GrupaInteres> selectedGrupaInteres()
	{
		final int index = grupaInteres.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.ofNullable(allGrupeInteres.get(index));
	}
}
