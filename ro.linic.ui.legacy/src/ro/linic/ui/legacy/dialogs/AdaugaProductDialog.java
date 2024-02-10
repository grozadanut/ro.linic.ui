package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.linic.ui.legacy.session.UIUtils.applyAdaosPopup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.Raion;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class AdaugaProductDialog extends TitleAreaDialog
{
	private Combo category;
	private Combo raion;
	private Text barcode;
	private Text name;
	private Text uom;
	private Text ulpFTVA;
	private Text pret;
	private Text stocMin;
	
	private Label adaosPercent;
	private Label adaosLei;
	
	private Consumer<Product> productConsumer;
	private BigDecimal tvaExtractDivisor;
	private ImmutableList<Raion> raioane;

	public AdaugaProductDialog(final Shell parent, final Consumer<Product> productConsumer)
	{
		super(parent);
		this.productConsumer = productConsumer;
		final BigDecimal tvaPercent = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT));
		this.tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent);
		raioane = BusinessDelegate.raioane_Sync();
	}

	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle("Adauga Produs");
		setMessage("Creeati un produs nou");
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		new Label(container, SWT.NONE).setText("Categorie");
		category = new Combo(container, SWT.DROP_DOWN);
		category.setItems(Product.ALL_CATEGORIES.toArray(new String[] {}));
		category.select(Product.ALL_CATEGORIES.asList().indexOf(Product.MARFA_CATEGORY));
		UIUtils.setFont(category);
		GridDataFactory.swtDefaults().applyTo(category);
		
		new Label(container, SWT.NONE).setText("Raion");
		raion = new Combo(container, SWT.DROP_DOWN);
		raion.setItems(raioane.stream().map(Raion::displayName).toArray(String[]::new));
		if (raioane.size() == 1)
			raion.select(0);
		UIUtils.setFont(raion);
		GridDataFactory.swtDefaults().applyTo(raion);
		
		new Label(container, SWT.NONE).setText("Cod");
		barcode = new Text(container, SWT.BORDER);
		UIUtils.setFont(barcode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(barcode);
		
		new Label(container, SWT.NONE).setText("Denumire");
		name = new Text(container, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(name);
		
		new Label(container, SWT.NONE).setText("UM");
		uom = new Text(container, SWT.BORDER);
		UIUtils.setFont(uom);
		GridDataFactory.swtDefaults().applyTo(uom);
		
		new Label(container, SWT.NONE).setText("UlPret fara TVA");
		ulpFTVA = new Text(container, SWT.BORDER);
		UIUtils.setFont(ulpFTVA);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(ulpFTVA);
		
		new Label(container, SWT.NONE).setText("Pret");
		pret = new Text(container, SWT.BORDER);
		UIUtils.setFont(pret);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(pret);
		
		new Label(container, SWT.NONE).setText("Stoc Min");
		stocMin = new Text(container, SWT.BORDER);
		UIUtils.setFont(stocMin);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(stocMin);
		
		adaosPercent = new Label(container, SWT.NONE);
		adaosPercent.setAlignment(SWT.RIGHT);
		adaosPercent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		adaosPercent.setToolTipText("((PVcuTVA � PUAcuTVA) / PUAcuTVA) * 100");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adaosPercent);
		
		adaosLei = new Label(container, SWT.NONE);
		adaosLei.setAlignment(SWT.RIGHT);
		adaosLei.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		adaosLei.setToolTipText("valVanzFaraTva-valAchFaraTva");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(adaosLei);
		
		applyAdaosPopup(pret, () -> parse(ulpFTVA.getText()).multiply(tvaExtractDivisor),
				result -> pret.setText(result.toString()));
		
		ulpFTVA.addModifyListener(e -> updateAdaos());
		pret.addModifyListener(e -> updateAdaos());

		return area;
	}
	
	@Override
	protected void okPressed()
	{
		final Product p = new Product();
		p.setCategorie(category.getText());
		p.setRaion(selectedRaion().orElse(null));
		p.setBarcode(barcode.getText());
		p.setName(name.getText());
		p.setUom(uom.getText());
		p.setLastBuyingPriceNoTva(parse(ulpFTVA.getText()));
		p.setPricePerUom(parse(pret.getText()));
		p.setMinStoc(parse(stocMin.getText()));
		
		final InvocationResult result = BusinessDelegate.persistProduct(p);
		if (result.statusOk())
		{
			productConsumer.accept(result.extra(InvocationResult.PRODUCT_KEY));
			super.okPressed();
		}
		else
			setErrorMessage(result.toTextDescriptionWithCode());
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(600, 550);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	private void updateAdaos()
	{
		final BigDecimal getLastBuyingPriceWithTva = parse(ulpFTVA.getText())
				.multiply(tvaExtractDivisor);
		final BigDecimal adaosPerc = getLastBuyingPriceWithTva.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
			parse(pret.getText())
			.subtract(getLastBuyingPriceWithTva)
			.divide(getLastBuyingPriceWithTva, 4, RoundingMode.HALF_EVEN)
			.multiply(new BigDecimal("100"));
		
		final BigDecimal valVanzFaraTVA = parse(pret.getText()).divide(tvaExtractDivisor, 2, RoundingMode.HALF_EVEN);
		final BigDecimal adaosComLei = valVanzFaraTVA.subtract(parse(ulpFTVA.getText()));
		
		adaosPercent.setText(MessageFormat.format("Ad.com={0}%", displayBigDecimal(adaosPerc)));
		adaosLei.setText(MessageFormat.format("Ad.com(lei)={0}", displayBigDecimal(adaosComLei)));
	}

	public void setCategory(final String category)
	{
		this.category.setText(category);
	}
	
	public void setRaion(final Raion raion)
	{
		if (raion != null)
			this.raion.select(raioane.indexOf(raion));
	}

	public void setBarcode(final String barcode)
	{
		this.barcode.setText(barcode);
	}

	public void setName(final String name)
	{
		this.name.setText(name);
	}

	public void setUom(final String uom)
	{
		this.uom.setText(uom);
	}

	public void setUlpFTVA(final BigDecimal ulpFTVA)
	{
		this.ulpFTVA.setText(ulpFTVA.toString());
	}

	public void setPret(final BigDecimal pret)
	{
		this.pret.setText(pret.toString());
	}

	public void setStocMin(final BigDecimal stocMin)
	{
		this.stocMin.setText(stocMin.toString());
	}
	
	public Optional<Raion> selectedRaion()
	{
		final int index = raion.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(raioane.get(index));
	}
}
