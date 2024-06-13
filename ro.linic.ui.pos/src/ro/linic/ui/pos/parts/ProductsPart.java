package ro.linic.ui.pos.parts;

import java.util.List;

import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.command.DiscardDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.datachange.command.SaveDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import jakarta.annotation.PostConstruct;
import ro.linic.ui.base.services.di.DiscardChanges;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.base.services.nattable.components.StringSetDisplayConverter;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.pos.Messages;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.ProductDataHolder;
import ro.linic.ui.pos.base.services.ProductDataUpdater;

public class ProductsPart {
	public static final String PART_ID = "ro.linic.ui.pos.part.products"; //$NON-NLS-1$
	
	private static final String TABLE_STATE_PREFIX = "products.all_products_nt"; //$NON-NLS-1$
	
	private static final Column idColumn = new Column(0, Product.ID_FIELD, "ID", 70);
	private static final Column typeColumn = new Column(1, Product.TYPE_FIELD, Messages.ProductType, 150);
	private static final Column skuColumn = new Column(2, Product.SKU_FIELD, Messages.SKU, 90);
	private static final Column barcodesColumn = new Column(3, Product.BARCODES_FIELD, Messages.Barcodes, 100);
	private static final Column nameColumn = new Column(4, Product.NAME_FIELD, Messages.Name, 330);
	private static final Column uomColumn = new Column(5, Product.UOM_FIELD, Messages.UOM, 70);
	private static final Column priceColumn = new Column(6, Product.PRICE_FIELD, Messages.Price, 90);
	private static final Column taxCodeColumn = new Column(7, Product.TAX_CODE_FIELD, Messages.TaxCode, 90);
	private static final Column departmentCodeColumn = new Column(8, Product.DEPARTMENT_CODE_FIELD, Messages.DepartmentCode, 90);
	
	private static final List<Column> allProductsColumns = List.of(idColumn, typeColumn, skuColumn, barcodesColumn, nameColumn,
			uomColumn, priceColumn, taxCodeColumn, departmentCodeColumn);
	
	private FullFeaturedNatTable<Product> table;
	
	@PostConstruct
	public void createComposite(final Composite parent, final MPart part, final ESelectionService selectionService,
			final ProductDataHolder dataHolder, final ProductDataUpdater updater) {
		final GridLayout parentLayout = new GridLayout();
		parentLayout.horizontalSpacing = 0;
		parentLayout.verticalSpacing = 0;
		parent.setLayout(parentLayout);
		
		table = TableBuilder.with(Product.class, allProductsColumns, dataHolder.getData())
				.addConfiguration(new ProductsTableConfiguration())
				.provideSelection(selectionService)
				.connectDirtyProperty(part)
				.saveToDbHandler(data -> true) // TODO implement this
				.build(parent);
		table.natTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table.natTable());
		UIUtils.loadState(TABLE_STATE_PREFIX, table.natTable(), part);
	}
	
	@PersistState
	public void persistVisualState(final MPart part) {
		UIUtils.saveState(TABLE_STATE_PREFIX, table.natTable(), part);
	}
	
	@Persist
	public void onSave(final MPart part) {
		table.natTable().doCommand(new SaveDataChangesCommand());
//			final ImmutableList<InvocationResult> results = table.getDataChangeLayer().getDataChanges().stream()
//					.map(dataChange -> (IdIndexIdentifier<Product>)dataChange.getKey())
//					.map(key -> key.rowObject)
//					.distinct()
//					.map(p -> 
//					{
//						final byte[] img = Icons.imageFromCache(p.getImageUUID());
//						if (img != null)
//							BusinessDelegate.mergeImageLobs(ImmutableList.of(LobImage.from(null, img)));
//						return p;
//					})
//					.map(BusinessDelegate::mergeProduct)
//					.collect(toImmutableList());
//
//			showResult(InvocationResult.flatMap(results));
//			
//			results.stream()
//			.filter(InvocationResult::statusOk)
//			.map(result -> (Product) result.extra(InvocationResult.PRODUCT_KEY))
//			.map(mergedProduct -> table.replace(mergedProduct, mergedProduct))
//			.findAny()
//			.ifPresent(t -> t.getTable().refresh());
//			
//			part.setDirty(false);
	}

	@DiscardChanges
	public void onDiscard(final MPart part) {
		table.natTable().doCommand(new DiscardDataChangesCommand());
	}
	
	private class ProductsTableConfiguration extends AbstractRegistryConfiguration {
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry) {
			// Display converters
			final IDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new StringSetDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(barcodesColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(priceColumn));
			
			// CELL EDITOR CONFIG
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(typeColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(skuColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(barcodesColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(uomColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(priceColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(taxCodeColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + allProductsColumns.indexOf(departmentCodeColumn));
			
		}
	}
}
