package ro.linic.ui.pos.widgets;

import static ro.flexbiz.util.commons.PresentationUtils.LIST_SEPARATOR;

import java.math.BigDecimal;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jakarta.inject.Inject;
import ro.flexbiz.util.commons.ListUtils;
import ro.linic.ui.base.services.binding.NotEmptyValidator;
import ro.linic.ui.base.services.binding.NotNullValidator;
import ro.linic.ui.base.services.binding.StringToBigDecimalConverter;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.ProductDataLoader;

public class ProductWidget extends Composite {
	private Combo type;
	private Button autoSku;
	private Text sku;
	private Text barcodes;
	private Text name;
	private Text uom;
	private Text price;
	private Text taxCode;
	private Text departmentCode;
	
	private Product model;
	final private DataBindingContext ctx;
	@Inject private ProductDataLoader productLoader;
	
	public ProductWidget(final Composite parent, final DataBindingContext ctx) {
		this(parent, ctx, new Product());
	}
	
	public ProductWidget(final Composite parent, final DataBindingContext ctx, final Product model) {
		super(parent, SWT.NONE);
		this.ctx = ctx;
		createComposite();
		updateModel(model);
	}
	
	private void createComposite() {
		final GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		setLayout(layout);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.ProductType);
		type = new Combo(this, SWT.DROP_DOWN);
		type.setItems(Product.RO_CATEGORIES.toArray(new String[] {}));
		UIUtils.setFont(type);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(type);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.SKU);
		sku = new Text(this, SWT.BORDER);
		UIUtils.setFont(sku);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sku);
		
		autoSku = new Button(this, SWT.PUSH);
		autoSku.setText("AUTO"); //$NON-NLS-1$
		autoSku.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		UIUtils.setFont(autoSku);
		GridDataFactory.swtDefaults().applyTo(autoSku);
		autoSku.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				sku.setText(productLoader.autoSku());
			}
		});
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.BarcodesLong);
		barcodes = new Text(this, SWT.BORDER);
		barcodes.setMessage("12345678, 87654321");
		UIUtils.setFont(barcodes);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(barcodes);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.Name);
		name = new Text(this, SWT.BORDER);
		UIUtils.setFont(name);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(name);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.UOM);
		uom = new Text(this, SWT.BORDER);
		uom.setMessage(Messages.UOMHint);
		UIUtils.setFont(uom);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(uom);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.Price);
		price = new Text(this, SWT.BORDER);
		price.setMessage("100.0");
		UIUtils.setFont(price);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(price);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.TaxCode);
		taxCode = new Text(this, SWT.BORDER);
		taxCode.setMessage("1");
		UIUtils.setFont(taxCode);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(taxCode);
		
		UIUtils.setFont(new Label(this, SWT.NONE)).setText(Messages.DepartmentCode);
		departmentCode = new Text(this, SWT.BORDER);
		departmentCode.setMessage("1");
		UIUtils.setFont(departmentCode);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(departmentCode);
	}
	
	private void bindValues() {
		ctx.dispose();
		
		final IObservableValue<String> typeWidget = WidgetProperties.text().observe(type);
		final IObservableValue<String> typeModel = BeanProperties.<Product, String>value(Product.TYPE_FIELD).observe(model);
		final Binding typeBind = ctx.bindValue(typeWidget, typeModel, new UpdateValueStrategy<String, String>()
				.setBeforeSetValidator(new NotEmptyValidator(ro.linic.ui.pos.base.Messages.ProductTypeMandatory)), null);
		ControlDecorationSupport.create(typeBind, SWT.TOP | SWT.LEFT);
		
		final IObservableValue<String> skuWidget = WidgetProperties.text(SWT.Modify).observe(sku);
		final IObservableValue<String> skuModel = BeanProperties.<Product, String>value(Product.SKU_FIELD).observe(model);
		ctx.bindValue(skuWidget, skuModel);
		
		final IConverter<String, Set<String>> convertToSet =
                IConverter.<String, Set<String>>create(String.class, Set.class,
                		(o1) -> ListUtils.toStream(new StringTokenizer(o1, LIST_SEPARATOR).asIterator()).map(String.class::cast).collect(Collectors.toSet()));
        final IConverter<Set<String>, String> convertToString =
                IConverter.create(Set.class, String.class, (o1) -> o1.stream().collect(Collectors.joining(LIST_SEPARATOR)));
		
		final IObservableValue<String> barcodesWidget = WidgetProperties.text(SWT.Modify).observe(barcodes);
		final IObservableValue<Set<String>> barcodesModel = BeanProperties.<Product, Set<String>>value(Product.BARCODES_FIELD).observe(model);
		ctx.bindValue(barcodesWidget, barcodesModel, UpdateValueStrategy.create(convertToSet), UpdateValueStrategy.create(convertToString));
		
		final IObservableValue<String> nameWidget = WidgetProperties.text(SWT.Modify).observe(name);
		final IObservableValue<String> nameModel = BeanProperties.<Product, String>value(Product.NAME_FIELD).observe(model);
		final Binding nameBind = ctx.bindValue(nameWidget, nameModel, new UpdateValueStrategy<String, String>()
				.setBeforeSetValidator(new NotEmptyValidator(ro.linic.ui.pos.base.Messages.NameMandatory)), null);
		ControlDecorationSupport.create(nameBind, SWT.TOP | SWT.LEFT);
		
		final IObservableValue<String> uomWidget = WidgetProperties.text(SWT.Modify).observe(uom);
		final IObservableValue<String> uomModel = BeanProperties.<Product, String>value(Product.UOM_FIELD).observe(model);
		final Binding uomBind = ctx.bindValue(uomWidget, uomModel, new UpdateValueStrategy<String, String>()
				.setBeforeSetValidator(new NotEmptyValidator(ro.linic.ui.pos.base.Messages.UOMMandatory)), null);
		ControlDecorationSupport.create(uomBind, SWT.TOP | SWT.LEFT);
		
		final IObservableValue<String> priceWidget = WidgetProperties.text(SWT.Modify).observe(price);
		final IObservableValue<BigDecimal> priceModel = BeanProperties.<Product, BigDecimal>value(Product.PRICE_FIELD).observe(model);
		final Binding priceBind = ctx.bindValue(priceWidget, priceModel, UpdateValueStrategy.create(new StringToBigDecimalConverter())
				.setBeforeSetValidator(new NotNullValidator(ro.linic.ui.pos.base.Messages.PriceMandatory)), null);
		ControlDecorationSupport.create(priceBind, SWT.TOP | SWT.LEFT);
		StringToBigDecimalConverter.applyCalculationOnKeyPress(price);
		
		final IObservableValue<String> taxCodeWidget = WidgetProperties.text(SWT.Modify).observe(taxCode);
		final IObservableValue<String> taxCodeModel = BeanProperties.<Product, String>value(Product.TAX_CODE_FIELD).observe(model);
		ctx.bindValue(taxCodeWidget, taxCodeModel);
		
		final IObservableValue<String> departmentCodeWidget = WidgetProperties.text(SWT.Modify).observe(departmentCode);
		final IObservableValue<String> departmentCodeModel = BeanProperties.<Product, String>value(Product.DEPARTMENT_CODE_FIELD).observe(model);
		ctx.bindValue(departmentCodeWidget, departmentCodeModel);
	}
	
	@Override
	public void dispose() {
		ctx.dispose();
		super.dispose();
	}

	public void updateModel(final Product model) {
		this.model = model;
		bindValues();
	}
	
	public Product getModel() {
		return model;
	}
}
