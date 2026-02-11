package ro.linic.ui.legacy.parts;

import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.linic.ui.legacy.session.UIUtils.createTopBar;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.command.DiscardDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.datachange.command.SaveDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.style.TextDecorationEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.NumberUtils;
import ro.colibri.wrappers.ProductProfitability;
import ro.colibri.wrappers.RaionProfitability;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.GenericDataHolder;
import ro.linic.ui.base.services.di.DiscardChanges;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.base.services.nattable.UpdateCommand;
import ro.linic.ui.base.services.nattable.components.GenericValueDisplayConverter;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.http.pojo.Result;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.AllProductsNatTable;
import ro.linic.ui.security.services.AuthenticationSession;

public class SupplierOrderPart
{
	public static final String PART_ID = "linic_gest_client.part.comenzi_furnizori"; //$NON-NLS-1$
	
	private static final String TABLE_STATE_PREFIX = "supplier_order.products_nt"; //$NON-NLS-1$
	public static final String DATA_HOLDER = "SupplierOrderPart.ProductsToOrder"; //$NON-NLS-1$
	
	private static final int STOC_ID_BASE = 1000;
	
	private static final Column barcodeColumn = new Column(0, Product.BARCODE_FIELD, "Cod", 70);
	private static final Column nameColumn = new Column(1, Product.NAME_FIELD, "Denumire", 350);
	private static final Column uomColumn = new Column(2, Product.UOM_FIELD, "UM", 50);
	private static final Column paretoColumn = new Column(3, "pareto", "Pareto", 50);
	private static final Column ULPColumn = new Column(4,Product.LAST_BUYING_PRICE_FIELD, "ULPfTVA", 70);
	private static final Column priceColumn = new Column(5, Product.PRICE_FIELD, "PU", 90);
	private static final Column furnizoriColumn = new Column(6, Product.FURNIZORI_FIELD, "Furnizori", 220);
	private static final Column inventoryValueColumn = new Column(7, "stocAchCuTVA", "StocAchCuTVA", 90, true);
	private static final Column dioColumn = new Column(8, "daysOfStock", "(DIO)Zile epuizare stoc", 70, "Stoc la AchCuTVA / Medie vanzari pe zi la PAcuTVA", true);
	private static final Column inventoryDioColumn = new Column(9, "inventoryDio", "7*8", 90, "StocAchCuTVA * DIO", true);
	private static final Column minStockColumn = new Column(10, "minimumStock", "Stoc minim", 70);
	private Column recommendedOrderColumn;
	private Column supplierOrdersColumn;
	private static final Column addRequirementColumn = new Column(4000, "newRequirement", "Adauga", 100);
	private static final Column addRequirementButtonColumn = new Column(4001, "add", "", 50);
	
	private static final ImmutableMap<Column, Gestiune> stocColumns;
	
	static {
		final com.google.common.collect.ImmutableMap.Builder<Column, Gestiune> stocBuilder = ImmutableMap.builder();
		
		int i = 0;
		for (final Gestiune gest : BusinessDelegate.allGestiuni()) {
			stocBuilder.put(new Column(STOC_ID_BASE+i, "STC_"+gest.getImportName(), "STC "+gest.getImportName(), 90), gest);
			i++;
		}
		
		stocColumns = stocBuilder.build();
	}
	
	private EventList<GenericValue> allSuppliers = GlazedLists.eventListOf();
	
	private ImmutableList<Column> columns;
	private Combo searchMode;
	private Text searchFilter;
	private Text furnizorFilter;
	private FullFeaturedNatTable<GenericValue> allProductsTable;
	private Button refreshButton;
	
	private int quickSearchFilterMode = TextMatcherEditor.CONTAINS;
	private SearchEngineTextMatcherEditor<GenericValue> quickSearchFilter;
	private TextMatcherEditor<GenericValue> furnizoriFilter;
	
	@Inject private MPart part;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	@Inject private AuthenticationSession authSession;
	@Inject private DataServices dataServices;
	
	private ImmutableList<Column> buildColumns() {
		final Gestiune gest = ClientSession.instance().getGestiune();
		recommendedOrderColumn = new Column(2000, "QTO_"+gest.getImportName(), "Comanda recomandata "+gest.getImportName(), 100);
		supplierOrdersColumn = new Column(3000, "requiredQuantityTotal", "Comenzi "+gest.getImportName(), 100);
		
		final Builder<Column> builder = ImmutableList.<Column>builder();
		builder.add(barcodeColumn)
		.add(nameColumn)
		.add(uomColumn)
		.add(paretoColumn)
		.add(ULPColumn)
		.add(priceColumn)
		.add(furnizoriColumn)
		.add(inventoryValueColumn)
		.add(dioColumn)
		.add(inventoryDioColumn)
		.add(minStockColumn);
		for (int i = 0; i < stocColumns.size(); i++) {
			builder.add(stocColumns.keySet().asList().get(i));
		}
		builder.add(recommendedOrderColumn)
		.add(supplierOrdersColumn)
		.add(addRequirementColumn)
		.add(addRequirementButtonColumn);
		return builder.build();
	}
	
	@PostConstruct
	public void createComposite(final Composite parent, final ESelectionService selectionService)
	{
		RestCaller.get("/rest/s1/moqui-linic-legacy/suppliers")
		.internal(authSession.authentication())
		.async(t -> UIUtils.showException(t, sync))
		.thenAccept(allSuppliers::addAll);
		
		columns = buildColumns();
		parent.setLayout(new GridLayout());
		createTopBar(parent);
		
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		searchMode = new Combo(container, SWT.DROP_DOWN);
		searchMode.setItems(AllProductsNatTable.ALL_SEARCH_MODES.toArray(new String[] {}));
		searchMode.select(0);
		UIUtils.setFont(searchMode);
		GridDataFactory.swtDefaults().applyTo(searchMode);
		
		searchFilter = new Text(container, SWT.BORDER);
		searchFilter.setMessage(Messages.BarcodeName);
		UIUtils.setFont(searchFilter);
		GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT).applyTo(searchFilter);
		
		furnizorFilter = new Text(container, SWT.BORDER);
		furnizorFilter.setMessage(Messages.SupplierOrderPart_Supplier);
		UIUtils.setFont(furnizorFilter);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).applyTo(furnizorFilter);
		
		allProductsTable = TableBuilder.with(GenericValue.class, columns, dataServices.holder(DATA_HOLDER).getData())
				.addConfiguration(new ProductStyleConfiguration())
				.connectDirtyProperty(part)
				.provideSelection(selectionService)
				.saveToDbHandler(this::saveChangesToDb)
				.addClickListener(addRequirementButtonColumn, this::createRequirement)
				.build(parent);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(allProductsTable.natTable());
		loadState(TABLE_STATE_PREFIX, allProductsTable.natTable(), part);
		createTableFilters();
		
		createBottomBar(parent);
		
		addListeners();
		loadData(false);
	}
	
	private void createTableFilters() {
		final EventList<MatcherEditor<GenericValue>> stringMatcherEditors = new BasicEventList<MatcherEditor<GenericValue>>();
		
		quickSearchFilter = new SearchEngineTextMatcherEditor<GenericValue>(new TextFilterator<GenericValue>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final GenericValue element)
			{
				baseList.add(element.getString(Product.BARCODE_FIELD));
				baseList.add(element.getString(Product.NAME_FIELD));
			}
		});
		quickSearchFilter.setMode(quickSearchFilterMode);
		
		furnizoriFilter = new TextMatcherEditor<GenericValue>(new TextFilterator<GenericValue>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final GenericValue element)
			{
				baseList.add(element.getString(Product.FURNIZORI_FIELD));
			}
		});
		furnizoriFilter.setMode(TextMatcherEditor.CONTAINS);
		
		stringMatcherEditors.add(quickSearchFilter);
		stringMatcherEditors.add(furnizoriFilter);
		
		final CompositeMatcherEditor<GenericValue> matcherEditor = new CompositeMatcherEditor<>(stringMatcherEditors);
        allProductsTable.filterList().setMatcherEditor(matcherEditor);
	}

	private void createBottomBar(final Composite parent)
	{
		final Composite footerContainer = new Composite(parent, SWT.NONE);
		footerContainer.setLayout(new GridLayout(3, false));
		footerContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		refreshButton = new Button(footerContainer, SWT.PUSH);
		refreshButton.setText(Messages.Refresh);
		UIUtils.setBannerFont(refreshButton);
		GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).grab(true, false).applyTo(refreshButton);
	}
	
	@PersistState
	public void persistVisualState() {
		saveState(TABLE_STATE_PREFIX, allProductsTable.natTable(), part);
	}
	
	@Persist
	public void onSave(final MPart part) {
		allProductsTable.natTable().doCommand(new SaveDataChangesCommand());
	}
	
	@DiscardChanges
	public void onDiscard(final MPart part) {
		allProductsTable.natTable().doCommand(new DiscardDataChangesCommand());
	}
	
	private boolean saveChangesToDb(final List<UpdateCommand> updateCommands) {
		return saveSuppliers(updateCommands.stream().filter(uc -> uc.updatedProperty().equalsIgnoreCase("furnizori"))) &&
				saveMinStock(updateCommands.stream().filter(uc -> uc.updatedProperty().equalsIgnoreCase("minimumStock")));
	}
	
	private boolean saveSuppliers(final Stream<UpdateCommand> updateCommands) {
		final String organizationPartyId = ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName();
		final List<GenericValue> valuesToUpdate = updateCommands.map(uc -> GenericValue.of("", "", Map.of("organizationPartyId", organizationPartyId,
				"productId", ((GenericValue) uc.model()).getString(Product.ID_FIELD),
				"supplierId", ((GenericValue) uc.newValue()).getString("partyId"))))
		.toList();
		
		return RestCaller.put("/rest/s1/moqui-linic-legacy/products/suppliers")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(valuesToUpdate)))
				.get(Result.class, t -> UIUtils.showException(t, sync))
				.isPresent();
	}
	
	private boolean saveMinStock(final Stream<UpdateCommand> updateCommands) {
		final String facilityId = ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName();
		final List<GenericValue> valuesToUpdate = updateCommands.map(uc -> GenericValue.of("", "", Map.of(
				"productId", ((GenericValue) uc.model()).getString(Product.ID_FIELD),
				"facilityId", facilityId,
				"minimumStock", (BigDecimal) uc.newValue())))
		.toList();
		
		return RestCaller.put("/rest/s1/moqui-linic-legacy/products/facility")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(valuesToUpdate)))
				.get(Result.class, t -> UIUtils.showException(t, sync))
				.isPresent();
	}
	
	private void createRequirement(final GenericValue row, final Object cellValue) {
		// fix for DataChangeLayer.temporaryDataStorage: if the data changes should be handled temporary in this layer and update the model on save
		allProductsTable.natTable().doCommand(new SaveDataChangesCommand());
		
		final BigDecimal quantity = row.getBigDecimal("newRequirement");
		if (NumberUtils.smallerThanOrEqual(quantity, BigDecimal.ZERO))
			return;
		
		final String facilityId = ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName();
		final Map<String, Object> body = Map.of("facilityId", facilityId,
				"requirementTypeEnumId", "RqTpInventory",
				"statusId", "RqmtStCreated",
				"productId", row.getString(Product.ID_FIELD),
				"quantity", quantity);
		
		RestCaller.post("/rest/s1/moqui-linic-legacy/requirements")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(body)))
				.get(GenericValue.class, t -> UIUtils.showException(t, sync))
				.ifPresent(reqId -> {
					row.put("newRequirement", "");
					row.put("requiredQuantityTotal", NumberUtils.add(row.getBigDecimal("requiredQuantityTotal"), quantity));
					// update ApproveOrderPart row
					dataServices.holder(ApproveOrderPart.DATA_HOLDER).getData().stream()
					.filter(gv -> gv.getString(Product.ID_FIELD).equals(row.getString(Product.ID_FIELD)))
					.findFirst()
					.ifPresentOrElse(r -> r.put("requiredQuantityTotal", NumberUtils.add(r.getBigDecimal("requiredQuantityTotal"), quantity)),
							() -> dataServices.holder(ApproveOrderPart.DATA_HOLDER).add(GenericValue.of("", Product.ID_FIELD, 
									Product.ID_FIELD, row.getString(Product.ID_FIELD),
									Product.BARCODE_FIELD, row.getString(Product.BARCODE_FIELD),
									Product.NAME_FIELD, row.getString(Product.NAME_FIELD),
									Product.UOM_FIELD, row.getString(Product.UOM_FIELD),
									Product.LAST_BUYING_PRICE_FIELD, row.getBigDecimal(Product.LAST_BUYING_PRICE_FIELD),
									Product.PRICE_FIELD, row.getString(Product.PRICE_FIELD),
									Product.FURNIZORI_FIELD, row.getString(Product.FURNIZORI_FIELD),
									"requiredQuantityTotal", quantity)));
				});
	}

	private void addListeners()
	{
		searchMode.addModifyListener(this::filterModeChange);
		searchFilter.addModifyListener(e -> filter(searchFilter.getText()));
		furnizorFilter.addModifyListener(e -> filterFurnizori(furnizorFilter.getText()));
		
		refreshButton.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				loadData(true);
			}
		});
	}
	
	private void filterModeChange(final ModifyEvent e)
	{
		int filterMode = TextMatcherEditor.CONTAINS;
		
		if (AllProductsNatTable.STARTS_WITH_MODE.equalsIgnoreCase(searchMode.getText()))
			filterMode = TextMatcherEditor.STARTS_WITH;
		
		filterMode(filterMode);
		filter(searchFilter.getText());
	}
	
	private void filterMode(final int mode)
	{
		quickSearchFilterMode = mode;
		quickSearchFilter.setMode(quickSearchFilterMode);
		quickSearchFilter.setFilterText(new String[] {});
	}
	
	private void filter(final String searchText)
	{
        if (quickSearchFilterMode == TextMatcherEditor.CONTAINS)
        	quickSearchFilter.refilter(searchText);
        else
        	quickSearchFilter.setFilterText(new String[] {searchText});
	}
	
	private void filterFurnizori(final String furnizori)
	{
        furnizoriFilter.setFilterText(furnizori.split(SPACE));
	}
	
	private void loadData(final boolean showConfirmation)
	{
		final GenericDataHolder productsHolder = dataServices.holder(DATA_HOLDER);
		productsHolder.clear();
		
		RestCaller.get("/rest/s1/moqui-linic-legacy/products/suppliers")
				.internal(authSession.authentication())
				.addUrlParam("organizationPartyId", ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName())
				.async(t -> UIUtils.showException(t, sync))
				.thenApply(ps -> ps.stream().filter(gv -> gv.getString("preferredOrderEnumId") == null ||
															Objects.equals(gv.getString("preferredOrderEnumId"), "SpoMain")).toList())
				.thenAccept(productSuppliers -> productsHolder.update(productSuppliers, "productId", Product.ID_FIELD,
						Map.of("productId", Product.ID_FIELD, "supplierName", Product.FURNIZORI_FIELD, "pareto", "pareto", "minimumStock", "minimumStock",
								"requiredQuantityTotal", "requiredQuantityTotal")));
		
		BusinessDelegate.allProductsForOrdering(new AsyncLoadData<Product>()
		{
			@Override public void success(final ImmutableList<Product> data)
			{
				productsHolder.update(data.stream()
						.map(p -> {
							final GenericValue gv = GenericValue.of(Product.class.getName(), Product.ID_FIELD,
									Map.of(Product.ID_FIELD, p.getId(),
									Product.BARCODE_FIELD, p.getBarcode(), Product.NAME_FIELD, p.getName(),
									Product.UOM_FIELD, p.getUom(), Product.LAST_BUYING_PRICE_FIELD, p.getLastBuyingPriceNoTva(),
									Product.PRICE_FIELD, p.getPricePerUom(), "add", "Add"));
							for (final Gestiune gest : stocColumns.values()) {
								gv.put("STC_"+gest.getImportName(), p.stoc(gest));
								gv.put("QTO_"+gest.getImportName(), p.recommendedOrder(gest));
							}
							return gv;
						})
						.toList(),
						Product.ID_FIELD);
				
				updateRecommendedOrder();
				productsHolder.getData().sort(Comparator.comparing(gv -> ((GenericValue) gv).get("minimumStock") != null)
						.thenComparing(gv -> ((GenericValue) gv).getBigDecimal("QTO_"+ClientSession.instance().getGestiune().getImportName())).reversed());
				// need this fix because of the speed fix in GenericDataHolderImpl:
				// source.silenceListeners(true); // speed fix
				allProductsTable.natTable().refresh();

				reloadDaysOfInventoryOutstanding();
				if (showConfirmation)
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.Success,
							Messages.SupplierOrderPart_SuccessMessage);
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SupplierOrderPart_ErrorLoading,
						details);
			}
		}, sync, bundle, log);
	}
	
	private void updateRecommendedOrder() {
		final String gestName = ClientSession.instance().getGestiune().getImportName();
		
		dataServices.holder(DATA_HOLDER).getData().forEach(gv -> {
			if (gv.get("minimumStock") != null) {
				gv.silenceListeners(true);
				final BigDecimal recommendedOrder = NumberUtils.subtract(gv.getBigDecimal("minimumStock"), gv.getBigDecimal("STC_"+gestName));
				gv.put("QTO_"+gestName, recommendedOrder);
				
				final BigDecimal newRequirement = NumberUtils.subtract(recommendedOrder, gv.getBigDecimal("requiredQuantityTotal"));
				if (NumberUtils.greaterThan(newRequirement, BigDecimal.ZERO))
					gv.put("newRequirement", newRequirement);
				gv.silenceListeners(false);
			}
		});
	}

	private void reloadDaysOfInventoryOutstanding() {
		final BigDecimal tvaPercentPlusOne = new BigDecimal(BusinessDelegate.persistedProp(PersistedProp.TVA_PERCENT_KEY)
				.getValueOr(PersistedProp.TVA_PERCENT_DEFAULT))
				.add(BigDecimal.ONE);
		final Gestiune gestiune = ClientSession.instance().getGestiune();
		
		BusinessDelegate.profitability(new AsyncLoadResult<ImmutableMap<RaionProfitability, List<ProductProfitability>>>()
		{
			@Override public void success(final ImmutableMap<RaionProfitability, List<ProductProfitability>> data)
			{
				final GenericDataHolder productsHolder = dataServices.holder(DATA_HOLDER);
				final Map<String, List<ProductProfitability>> productProfitabilities = data.values().stream()
						.flatMap(List::stream)
						.collect(Collectors.groupingBy(ProductProfitability::getBarcode));
				
				for (final GenericValue product : productsHolder.getData()) {
					product.silenceListeners(true);
					// "Stoc la AchCuTVA / Medie vanzari pe zi la PAcuTVA";
					final List<ProductProfitability> productProfitability = productProfitabilities.getOrDefault(product.get(Product.BARCODE_FIELD), List.of());
					final BigDecimal lastBuyingPriceWithVAT = NumberUtils.multiply(product.getBigDecimal(Product.LAST_BUYING_PRICE_FIELD), tvaPercentPlusOne);
					
					if (NumberUtils.smallerThanOrEqual(lastBuyingPriceWithVAT, BigDecimal.ZERO)) {
						product.put("stocAchCuTVA", BigDecimal.ZERO);
						product.put("daysOfStock", BigDecimal.ZERO);
						product.put("inventoryDio", BigDecimal.ZERO);
						continue;
					}
					
					final BigDecimal stocAchCuTVA = NumberUtils.multiply(product.getBigDecimal("STC_"+gestiune.getImportName()), lastBuyingPriceWithVAT)
							.setScale(4, RoundingMode.HALF_EVEN);
					final BigDecimal dailyCogs = productProfitability.stream()
							.map(pp -> NumberUtils.subtract(pp.getTotalSales(), pp.getTotalProfit()))
							.reduce(BigDecimal::add)
							.map(total -> NumberUtils.divide(total, new BigDecimal(250), 12, RoundingMode.HALF_EVEN)) // around 250 working days per year
							.orElse(BigDecimal.ZERO);
					
					final BigDecimal dio = NumberUtils.smallerThanOrEqual(dailyCogs, BigDecimal.ZERO)
							? new BigDecimal(730)
							: NumberUtils.divide(stocAchCuTVA, dailyCogs, 0, RoundingMode.UP);
					
					product.put("stocAchCuTVA", stocAchCuTVA.setScale(0, RoundingMode.UP));
					product.put("daysOfStock", dio);
					product.put("inventoryDio", NumberUtils.multiply(dio, stocAchCuTVA)
							.setScale(0, RoundingMode.UP)
							.divide(new BigDecimal(1000000), 2, RoundingMode.UP));
					product.silenceListeners(false);
				}
				allProductsTable.natTable().refresh();
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ErrorFiltering, details);
			}
		}, sync, LocalDate.now().minusYears(1), LocalDate.now(), gestiune,
		ImmutableSet.of(AccountingDocument.FACTURA_NAME, AccountingDocument.BON_CONSUM_NAME, AccountingDocument.BON_CASA_NAME));
	}

	private class ProductStyleConfiguration extends AbstractRegistryConfiguration {
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry) {
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			final Style yellowBgStyle = new Style();
			yellowBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			yellowBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(barcodeColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(uomColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			final Style blueBgStyle = new Style();
			blueBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			blueBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			blueBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			final Style magentaBgStyle = new Style();
			magentaBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			magentaBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
			magentaBgStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			final Style greenBgStyle = new Style();
			greenBgStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			greenBgStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(ULPColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, blueBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					greenBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
					magentaBgStyle, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(recommendedOrderColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL,
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(addRequirementColumn));
			
			final Style linkStyle = new Style();
	        linkStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
	        linkStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);
	        final Style linkSelectStyle = new Style();
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	        linkSelectStyle.setAttributeValue(CellStyleAttributes.TEXT_DECORATION, TextDecorationEnum.UNDERLINE);
	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkStyle, DisplayMode.NORMAL,
	        		ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(addRequirementButtonColumn));
	        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, linkSelectStyle, DisplayMode.SELECT,
	        		ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(addRequirementButtonColumn));
			
			// Display converters
			final DefaultBigDecimalDisplayConverter defaultBigDecimalConv = new DefaultBigDecimalDisplayConverter();
			final DefaultBigDecimalDisplayConverter bigDecimalConverter = new DefaultBigDecimalDisplayConverter();
			bigDecimalConverter.setMinimumFractionDigits(2);
			final DefaultBigDecimalDisplayConverter quantityConverter = new DefaultBigDecimalDisplayConverter();
			quantityConverter.setMinimumFractionDigits(4);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new GenericValueDisplayConverter("organizationName"),
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter,
					DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(minStockColumn));
			stocColumns.keySet().forEach(col -> configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(col)));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(recommendedOrderColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(supplierOrdersColumn));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, defaultBigDecimalConv, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(ULPColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(priceColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new DefaultDisplayConverter() {
						@Override
						public Object canonicalToDisplayValue(final Object sourceValue) {
							final String val = super.canonicalToDisplayValue(sourceValue).toString();
							return val.length() > 2 ? val.substring(2) : val;
						}
					}, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(paretoColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConverter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(addRequirementColumn));
			
			// CELL EDITOR CONFIG
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(minStockColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
					IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(addRequirementColumn));
			
			final ComboBoxCellEditor supplierCellEditor = new ComboBoxCellEditor(allSuppliers);
			supplierCellEditor.setFreeEdit(false);
			supplierCellEditor.setShowDropdownFilter(true);
			supplierCellEditor.setFocusOnDropdownFilter(true);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new GenericValueDisplayConverter("organizationName"),
					DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					supplierCellEditor, DisplayMode.EDIT, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + columns.indexOf(furnizoriColumn));
		}
	}
}
