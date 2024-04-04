package ro.linic.ui.legacy.tables;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.LocalDateUtils.DATE_FORMATTER;
import static ro.colibri.util.NumberUtils.subtract;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.displayPercentage;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.globalIsMatch;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.nebula.widgets.nattable.Messages;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.ExtendedReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.ConversionFailedException;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBigDecimalDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultBooleanDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexKeyHandler;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.editor.CheckBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.edit.editor.ComboBoxCellEditor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsFilterStrategy;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeData;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.config.DefaultFreezeGridBindings;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.AbstractOverrider;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.CheckBoxPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;
import ro.colibri.embeddable.Address;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Document;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.security.Permissions;
import ro.colibri.util.HeterogeneousDataComparator;
import ro.colibri.util.StringUtils;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.anaf.AnafReporter;
import ro.linic.ui.legacy.anaf.ReportedInvoice;
import ro.linic.ui.legacy.anaf.ReportedInvoice.ReportState;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.tables.components.Column;
import ro.linic.ui.legacy.tables.components.FilterRowConfiguration;
import ro.linic.ui.legacy.tables.components.FreezeMenuConfiguration;
import ro.linic.ui.legacy.tables.components.GestiuneImportNameDisplayConverter;
import ro.linic.ui.legacy.tables.components.GestiuneNameDisplayConverter;
import ro.linic.ui.legacy.tables.components.LinicColumnHeaderStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LocalDateCellEditor;
import ro.linic.ui.legacy.tables.components.LocalDateTimeDisplayConverter;
import ro.linic.ui.legacy.tables.components.PresentableDisplayConverter;

public class DocumentNatTable
{
	private static final String ACC_DOC_LABEL = "acc_doc_label";
	private static final String DISCOUNT_DOC_LABEL = "discount_doc_label";
	private static final String CALCULATED_AT_TVA_LABEL = "calculated_at_tva_label";
	private static final String FACTURA_LABEL = "factura_label";
	
	private static final int gestiuneID = 0;
	private static final int gestiuneShortID = 1;
	private static final int partnerID = 2;
	private static final int tipDocID = 3;
	private static final int docID = 4;
	private static final int nrDocID = 5;
	private static final int dataDocID = 6;
	private static final int nameID = 7;
	private static final int tvaPercentID = 8;
	private static final int totalID = 9;
	private static final int tvaID = 10;
	private static final int valRpzID = 11;
	private static final int valRpzTvaID = 12;
	private static final int rpzID = 13;
	private static final int regCasaID = 14;
	private static final int regBancaID = 15;
	private static final int operatorID = 16;
	private static final int idxID = 17;
	private static final int inContabilitateID = 18;
	private static final int editableID = 19;
	private static final int transportDateTimeID = 20;
	private static final int masinaID = 21;
	private static final int shouldTransportID = 22;
	private static final int deliveryAddressID = 23;
	private static final int scadentaID = 24;
	private static final int payAtDriverID = 25;
	private static final int anafID = 26;
	
	private static final Column gestiuneColumn = new Column(gestiuneID, AccountingDocument.GESTIUNE_FIELD, "Gestiunea", 90);
	private static final Column gestiuneShortColumn = new Column(gestiuneShortID, AccountingDocument.GESTIUNE_FIELD, "L", 40);
	private static final Column partnerColumn = new Column(partnerID, AccountingDocument.PARTNER_FIELD, "Partener", 150);
	private static final Column tipDocColumn = new Column(tipDocID, AccountingDocument.TIP_DOC_FIELD, "Tip", 30);
	private static final Column docColumn = new Column(docID, AccountingDocument.DOC_FIELD, "Doc", 100);
	private static final Column nrDocColumn = new Column(nrDocID, AccountingDocument.NR_DOC_FIELD, "Numar", 100);
	private static final Column dataDocColumn = new Column(dataDocID, AccountingDocument.DATA_DOC_FIELD, "Data", 150);
	private static final Column nameColumn = new Column(nameID, AccountingDocument.NAME_FIELD, "Descriere", 200);
	private static final Column tvaPercentColumn = new Column(tvaPercentID, "TVA", "%TVA", 50);
	private static final Column totalColumn = new Column(totalID, AccountingDocument.TOTAL_FIELD, "ValCuTVA", 90);
	private static final Column tvaColumn = new Column(tvaID, AccountingDocument.TVA_FIELD, "ValTVA", 90);
	private static final Column valRpzColumn = new Column(valRpzID, "ValRPZ", "ValRPZ", 70);
	private static final Column valRpzTvaColumn = new Column(valRpzTvaID, "ValRPZ", "ValRPZ_TVA", 90);
	private static final Column rpzColumn = new Column(rpzID, AccountingDocument.RPZ_FIELD, "RPZ", 40);
	private static final Column regCasaColumn = new Column(regCasaID, AccountingDocument.REG_CASA_FIELD, "Casa", 40);
	private static final Column regBancaColumn = new Column(regBancaID, AccountingDocument.CONT_BANCAR_FIELD, "Banca", 50);
	private static final Column operatorColumn = new Column(operatorID, AccountingDocument.OPERATOR_FIELD, "Operator", 100);
	private static final Column idxColumn = new Column(idxID, AccountingDocument.ID_FIELD, "Idx", 70);
	private static final Column inContabilitateColumn = new Column(inContabilitateID, AccountingDocument.IN_CONTABILITATE_FIELD, "Conta", 50);
	private static final Column editableColumn = new Column(editableID, AccountingDocument.EDITABLE_FIELD, "Editabil", 50);
	private static final Column transportDateTimeColumn = new Column(transportDateTimeID, EMPTY_STRING, "Programat", 150);
	private static final Column masinaColumn = new Column(masinaID, AccountingDocument.MASINA_FIELD, "Masina", 90);
	private static final Column shouldTransportColumn = new Column(shouldTransportID, AccountingDocument.SHOULD_TRANSPORT_FIELD, "Transport", 90);
	private static final Column deliveryAddressColumn = new Column(deliveryAddressID, EMPTY_STRING, "Adresa", 300);
	private static final Column scadentaDocColumn = new Column(scadentaID, Document.SCADENTA_FIELD, "Scadenta", 120);
	private static final Column payAtDriverColumn = new Column(payAtDriverID, AccountingDocument.PAY_AT_DRIVER_FIELD, "Achitare la sofer", 90);
	private static final Column anafColumn = new Column(anafID, EMPTY_STRING, "ANAF", 90);
	
	public enum SourceLoc
	{
		URMARIRE_PARTENERI, CONTABILITATE, SCHEDULE_DIALOG;
	}
	
	private ImmutableList<Column> columns;
	private TransformedList<Object, Object> sourceList;
	private FilterList<Object> filteredData;
	private SortedList<Object> filteredSortedData;
	private Map<Long, ReportedInvoice> reportedInvoices = new HashMap<>();
	
	private NatTable table;
	private IRowDataProvider<Serializable> bodyDataProvider;
	private SelectionLayer selectionLayer;
	private DataChangeLayer dataChangeLayer;
	private TreeColumnAccessor columnAccessor;
	
	private Consumer<Object> afterChange;
	private IMouseAction doubleClickAction;
	private ImmutableList<Gestiune> allGestiuni;
	private ImmutableList<ContBancar> allConturiBancare;
	private SourceLoc source;
	private Bundle bundle;
	private Logger log;

	public DocumentNatTable(final SourceLoc source, final Bundle bundle, final Logger log)
	{
		allGestiuni = BusinessDelegate.allGestiuni();
		allConturiBancare = BusinessDelegate.allConturiBancare();
		this.source = source;
		this.bundle = bundle;
		this.log = log;
		
		final Builder<Column> builder = ImmutableList.<Column>builder();
		
		switch (source)
		{
		case URMARIRE_PARTENERI:
		case CONTABILITATE:
			builder.add(gestiuneColumn)
			.add(gestiuneShortColumn)
			.add(partnerColumn)
			.add(tipDocColumn)
			.add(docColumn)
			.add(nrDocColumn)
			.add(dataDocColumn)
			.add(scadentaDocColumn)
			.add(nameColumn)
			.add(tvaPercentColumn)
			.add(totalColumn)
			.add(tvaColumn)
			.add(valRpzColumn)
			.add(valRpzTvaColumn)
			.add(rpzColumn)
			.add(regCasaColumn)
			.add(regBancaColumn)
			.add(inContabilitateColumn)
			.add(editableColumn)
			.add(anafColumn)
			.add(shouldTransportColumn)
			.add(payAtDriverColumn)
			.add(transportDateTimeColumn)
			.add(masinaColumn)
			.add(operatorColumn)
			.add(idxColumn);
			break;
			
		case SCHEDULE_DIALOG:
			builder
			.add(gestiuneShortColumn)
			.add(partnerColumn)
			.add(tipDocColumn)
			.add(docColumn)
			.add(nrDocColumn.withSize(60))
			.add(transportDateTimeColumn)
			.add(masinaColumn)
			.add(totalColumn)
			.add(deliveryAddressColumn);
			break;
		default:
			throw new IllegalArgumentException("Source location "+source+" not implemented!");
		}
		
		columns = builder.build();
	}

	public void postConstruct(final Composite parent)
	{
        final BodyLayerStack bodyLayerStack = new BodyLayerStack(new ArrayList<>(), new OperatiuneTwoLevelTreeFormat());

		// create the column header layer stack
		final IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(columns.stream().map(Column::getName).toArray(String[]::new));
		final DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		final ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, bodyLayerStack, selectionLayer);
		columnHeaderLayer.addConfiguration(new LinicColumnHeaderStyleConfiguration());
		
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final SortHeaderLayer<Object> sortHeaderLayer = new SortHeaderLayer<Object>(columnHeaderLayer,
				new GlazedListsSortModel<Object>(filteredSortedData, columnAccessor, configRegistry, columnHeaderDataLayer));

		final FilterRowHeaderComposite<Object> filterRowHeaderLayer = new FilterRowHeaderComposite<>(
				new DefaultGlazedListsFilterStrategy<>(filteredData, columnAccessor, configRegistry),
				sortHeaderLayer, columnHeaderDataLayer.getDataProvider(), configRegistry);
		
		// create the row header layer stack
		final IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayerStack, selectionLayer);

		// create the corner layer stack
		final ILayer cornerLayer = new CornerLayer(
				new DataLayer(new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider)),
				rowHeaderLayer, filterRowHeaderLayer);

		// create the grid layer composed with the prior created layer stacks
		final GridLayer gridLayer = new GridLayer(bodyLayerStack, filterRowHeaderLayer, rowHeaderLayer, cornerLayer);

		table = new NatTable(parent, gridLayer, false);
		table.setConfigRegistry(configRegistry);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new SingleClickSortConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());
		table.addConfiguration(new DefaultFreezeGridBindings());
		table.addConfiguration(new FreezeMenuConfiguration(table));
		table.addConfiguration(new FilterRowConfiguration());
		table.setData("org.eclipse.e4.ui.css.CssClassName", "modern");
		new NatTableContentTooltip(table);

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<Serializable>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Serializable>()
				{
					@Override public Serializable getRowId(final Serializable rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multiple selection

		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}

	public DocumentNatTable loadData(final ImmutableList<? extends Document> data)
	{
		try
		{
			loadReportedInvoices(data);
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.clear();
			this.sourceList.addAll(data);
			table.refresh();
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	private void loadReportedInvoices(final ImmutableList<? extends Document> data)
	{
		if (!source.equals(SourceLoc.SCHEDULE_DIALOG))
		{
			final ImmutableList<Long> invoiceIds = data.stream()
					.filter(AccountingDocument.class::isInstance)
					.map(AccountingDocument.class::cast)
					.filter(accDoc -> TipDoc.VANZARE.equals(accDoc.getTipDoc()) &&
							AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
					.map(AccountingDocument::getId)
					.collect(toImmutableList());
			
			this.reportedInvoices = AnafReporter.findReportedInvoicesById(invoiceIds).stream()
					.collect(Collectors.toMap(ReportedInvoice::getInvoiceId, Function.identity()));
		}
	}

	public DocumentNatTable remove(final Object doc)
	{
		try
		{
			if (doc instanceof AccountingDocument)
				this.reportedInvoices.remove(((AccountingDocument) doc).getId());
			
			this.sourceList.getReadWriteLock().writeLock().lock();
			this.sourceList.remove(doc);
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public DocumentNatTable replace(final Object old, final Object newData)
	{
		try
		{
			if (old instanceof AccountingDocument && newData instanceof AccountingDocument && 
					!source.equals(SourceLoc.SCHEDULE_DIALOG))
				replaceReportedInvoice((AccountingDocument) old, (AccountingDocument) newData);
				
			this.sourceList.getReadWriteLock().writeLock().lock();
			final int indexOf = sourceList.indexOf(old);
			if (indexOf != -1)
				sourceList.set(indexOf, newData);
			else
				sourceList.add(newData);
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	private void replaceReportedInvoice(final AccountingDocument oldAccDoc, final AccountingDocument newAccDoc)
	{
		this.reportedInvoices.remove(oldAccDoc.getId());
		AnafReporter.findReportedInvoicesById(List.of(newAccDoc.getId()))
		.stream().findFirst()
		.ifPresent(repInv -> reportedInvoices.put(repInv.getInvoiceId(), repInv));
	}

	public DocumentNatTable add(final Object newData)
	{
		try
		{
			if (newData instanceof AccountingDocument && !source.equals(SourceLoc.SCHEDULE_DIALOG))
				AnafReporter.findReportedInvoicesById(List.of(((AccountingDocument) newData).getId()))
				.stream().findFirst()
				.ifPresent(repInv -> reportedInvoices.put(repInv.getInvoiceId(), repInv));
			
			this.sourceList.getReadWriteLock().writeLock().lock();
			sourceList.add(newData);
		}
		finally
		{
			this.sourceList.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public void afterChange(final Consumer<Object> afterChange)
	{
		this.afterChange = afterChange;
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}

	public NatTable getTable()
	{
		return table;
	}
	
	public SelectionLayer getSelectionLayer()
	{
		return selectionLayer;
	}
	
	public DataChangeLayer getDataChangeLayer()
	{
		return dataChangeLayer;
	}
	
	public TransformedList<Object, Object> getSourceList()
	{
		return sourceList;
	}
	
	public List<Object> selection()
	{
		return ((RowSelectionModel<Object>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public Stream<AccountingDocument> selectedAccDocs_Stream()
	{
		return selection().stream()
				.filter(AccountingDocument.class::isInstance)
				.map(AccountingDocument.class::cast);
	}
	
	public ImmutableList<AccountingDocument> selectedAccDocs()
	{
		return selectedAccDocs_Stream().collect(toImmutableList());
	}
	
	private class DocumentColumnAccessor<T extends Document> extends ExtendedReflectiveColumnPropertyAccessor<T>
	{
		public DocumentColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final T rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case tipDocID:
				return safeString(rowObject.getTipDoc(), TipDoc::shortName);
			case tvaPercentID:
				final BigDecimal tvaPercent = AccountingDocument.extractTvaPercentage(subtract(rowObject.getTotal(), rowObject.getTotalTva()),
						rowObject.getTotalTva());
				return tvaPercent.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_EVEN);
			case totalID:
				return rowObject.getTotal();
			case tvaID:
				return rowObject.getTotalTva();
			case valRpzID:
				return rowObject.getTotalRpz();
			case valRpzTvaID:
				return rowObject.getTotalRpzTva();

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
	
	private class AccDocColumnAccessor extends DocumentColumnAccessor<AccountingDocument>
	{
		public AccDocColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final AccountingDocument rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case deliveryAddressID:
				final Optional<Partner> selPartner = Optional.ofNullable(rowObject.getPartner());
				return rowObject.address().orElse(selPartner.map(Partner::getDeliveryAddress)
						.orElse(selPartner.map(Partner::getAddress).map(Address::displayName).orElse(EMPTY_STRING)));
			
			case transportDateTimeID:
				return rowObject.transportDateReadable();
				
			case anafID:
				return reportedInvoices.get(rowObject.getId());
			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
		
		@Override
		public void setDataValue(final AccountingDocument rowObj, final int columnIndex, final Object newValue)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case nameID:
				if (globalIsMatch(safeString(rowObj, AccountingDocument::getName), "#", TextFilterMethod.NOT_BEGINS_WITH) &&
						globalIsMatch(safeString(newValue, Object::toString), "#", TextFilterMethod.NOT_BEGINS_WITH))
					super.setDataValue(rowObj, columnIndex, newValue);
				break;
			default:
				super.setDataValue(rowObj, columnIndex, newValue);
			}
		}
	}
	
	private class DiscountDocColumnAccessor extends DocumentColumnAccessor<DocumentWithDiscount>
	{
		public DiscountDocColumnAccessor(final List<String> propertyNames)
		{
			super(propertyNames);
	    }
		
		@Override
		public Object getDataValue(final DocumentWithDiscount rowObject, final int columnIndex)
		{
			switch (columns.get(columnIndex).getIndex())
			{
			case docID:
				return "FIDELIZARE";

			case valRpzID:
				return displayPercentage(rowObject.getDiscountPercentage());
			case valRpzTvaID:
				return rowObject.calculatedDiscount();
				
			case gestiuneID:
			case gestiuneShortID:
			case nrDocID:
				return EMPTY_STRING;
				
			case rpzID:
			case regCasaID:
			case regBancaID:
			case inContabilitateID:
			case editableID:
			case transportDateTimeID:
			case shouldTransportID:
			case masinaID:
			case deliveryAddressID:
			case payAtDriverID:
			case anafID:
				return EMPTY_STRING;

			default:
				return super.getDataValue(rowObject, columnIndex);
			}
		}
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry)
		{
			configureCommon(configRegistry, ACC_DOC_LABEL);
			configureCommon(configRegistry, DISCOUNT_DOC_LABEL);
			
			final Style cyanStyle = new Style();
			cyanStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
			
			if (SourceLoc.CONTABILITATE.equals(source))
				configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cyanStyle, DisplayMode.NORMAL, CALCULATED_AT_TVA_LABEL
						+ columns.indexOf(tvaColumn));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cyanStyle, DisplayMode.NORMAL,
					FACTURA_LABEL + columns.indexOf(anafColumn));
			
			// Register Cell Painters
			final CheckBoxPainter checkboxPainter = new CheckBoxPainter();
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(rpzColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(regCasaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(inContabilitateColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(editableColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(shouldTransportColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER,
					checkboxPainter, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(payAtDriverColumn));
			
			final CellPainterDecorator iconPainter = new CellPainterDecorator(
					new TextPainter(), CellEdgeEnum.LEFT, new AnafReportStateImagePainter());
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, iconPainter, DisplayMode.NORMAL, 
					FACTURA_LABEL + columns.indexOf(anafColumn));
			
			// Sort comparators
			configRegistry.registerConfigAttribute(SortConfigAttributes.SORT_COMPARATOR,
					HeterogeneousDataComparator.INSTANCE, DisplayMode.NORMAL);
			
			// Display converters
			final DefaultBooleanDisplayConverter booleanConv = new DefaultBooleanDisplayConverter();

			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new GestiuneNameDisplayConverter(), DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(gestiuneColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new GestiuneImportNameDisplayConverter(), DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(gestiuneShortColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(rpzColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(regCasaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(regBancaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(inContabilitateColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(editableColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(shouldTransportColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, booleanConv, DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(payAtDriverColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					ACC_DOC_LABEL + columns.indexOf(masinaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new AnafReportStateDisplayConverter(), DisplayMode.NORMAL, 
					FACTURA_LABEL + columns.indexOf(anafColumn));
			
			// CELL EDITOR CONFIG
			if (SourceLoc.URMARIRE_PARTENERI.equals(source) || SourceLoc.CONTABILITATE.equals(source))
			{
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(gestiuneColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(gestiuneShortColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(rpzColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(regCasaColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(regBancaColumn));
				if (ClientSession.instance().hasStrictPermission(Permissions.CONTABILITATE))
					configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
							IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(inContabilitateColumn));
				if (ClientSession.instance().hasStrictPermission(Permissions.SET_EDITABLE_ACC_DOCS))
					configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
							IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(editableColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(shouldTransportColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(payAtDriverColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(scadentaDocColumn));
				configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE,
						IEditableRule.ALWAYS_EDITABLE, DisplayMode.NORMAL, ACC_DOC_LABEL + columns.indexOf(nameColumn));
			}

			// CELL EDITOR
			final ComboBoxCellEditor regBancaEditor = new ComboBoxCellEditor(allConturiBancare);
			regBancaEditor.setFreeEdit(true);
			
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new ComboBoxCellEditor(allGestiuni), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(gestiuneColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new ComboBoxCellEditor(allGestiuni), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(gestiuneShortColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(rpzColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(regCasaColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					regBancaEditor, DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(regBancaColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(inContabilitateColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(editableColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(shouldTransportColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new CheckBoxCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(payAtDriverColumn));
			configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR,
					new LocalDateCellEditor(), DisplayMode.EDIT, ACC_DOC_LABEL + columns.indexOf(scadentaDocColumn));
			
			// add a special style to highlight the modified cells
			final Style style = new Style();
			style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_RED);
			configRegistry.registerConfigAttribute(
					CellConfigAttributes.CELL_STYLE,
					style,
					DisplayMode.NORMAL,
					DataChangeLayer.DIRTY);
		}
		
		private void configureCommon(final IConfigRegistry configRegistry, final String labelPrefix)
		{
			final Style leftAlignStyle = new Style();
			leftAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(gestiuneColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(partnerColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(docColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(nrDocColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(nameColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, leftAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(deliveryAddressColumn));
			
			final Style rightAlignStyle = new Style();
			rightAlignStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.RIGHT);
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(totalColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(tvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(valRpzColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, rightAlignStyle, DisplayMode.NORMAL, labelPrefix
					+ columns.indexOf(valRpzTvaColumn));
			
			// Display converters
			final DefaultBigDecimalDisplayConverter bigDecimalConv = new DefaultBigDecimalDisplayConverter();

			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(partnerColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new PresentableDisplayConverter(), DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(operatorColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(), DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(dataDocColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(DATE_FORMATTER), DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(scadentaDocColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(totalColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(tvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(valRpzColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, bigDecimalConv, DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(valRpzTvaColumn));
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new LocalDateTimeDisplayConverter(), DisplayMode.NORMAL, 
					labelPrefix + columns.indexOf(transportDateTimeColumn));
		}
		
		@Override
		public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
			if (doubleClickAction != null)
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.rowHeaderLeftClick(0), doubleClickAction);
			
			final CellLabelMouseEventMatcher anafMouseEventMatcher = new AnafMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON,
            		FACTURA_LABEL + columns.indexOf(anafColumn));
            final CellLabelMouseEventMatcher anafMouseHoverMatcher = new AnafMouseEventMatcher(GridRegion.BODY, 0,
            		FACTURA_LABEL + columns.indexOf(anafColumn));
            
            uiBindingRegistry.registerFirstSingleClickBinding(anafMouseEventMatcher, new AnafMouseClickAction());
            // show hand cursor, which is usually used for links
            uiBindingRegistry.registerFirstMouseMoveBinding(anafMouseHoverMatcher, (natTable, event) -> {
            	natTable.setCursor(natTable.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            });
		}
	}
	
	/**
     * Always encapsulate the body layer stack in an AbstractLayerTransform to
     * ensure that the index transformations are performed in later commands.
     *
     * @param 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class BodyLayerStack extends AbstractLayerTransform
    {
        public BodyLayerStack(final List values, final TreeList.Format treeFormat)
        {
            // wrapping of the list to show into GlazedLists
            // see http://publicobject.com/glazedlists/ for further information
            final EventList eventList = GlazedLists.eventList(values);
            sourceList = GlazedLists.threadSafeList(eventList);
            filteredData = new FilterList(sourceList);
            filteredSortedData = new SortedList<>(filteredData, null);

            // wrap the SortedList with the TreeList
            final TreeList treeList = new TreeList(filteredSortedData, treeFormat, TreeList.nodesStartCollapsed());

            final IColumnAccessor<AccountingDocument> accDocAccessor = new AccDocColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
            final IColumnAccessor<DocumentWithDiscount> discountDocAccessor = new DiscountDocColumnAccessor(columns.stream().map(Column::getProperty).collect(toImmutableList()));
            columnAccessor = new TreeColumnAccessor(accDocAccessor, discountDocAccessor);
            
            bodyDataProvider = new ListDataProvider(treeList, columnAccessor);
            final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
            bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
            for (int i = 0; i < columns.size(); i++)
    			bodyDataLayer.setDefaultColumnWidthByPosition(i, columns.get(i).getSize());
    		
            // only apply labels in case it is a real row object and not a tree
            // structure object
            bodyDataLayer.setConfigLabelAccumulator(new AbstractOverrider()
            {
                @Override public void accumulateConfigLabels(final LabelStack configLabels, final int columnPosition, final int rowPosition)
                {
                    final Object rowObject = bodyDataProvider.getRowObject(rowPosition);
                    if (rowObject instanceof AccountingDocument)
                    {
                    	configLabels.addLabel(ACC_DOC_LABEL + columnPosition);
                    	final AccountingDocument accDoc = (AccountingDocument) rowObject;
						if (accDoc.calculeazaLaTva())
                    		configLabels.addLabelOnTop(CALCULATED_AT_TVA_LABEL + columnPosition);
                    	
						if (TipDoc.VANZARE.equals(accDoc.getTipDoc()) &&
								AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
                    		configLabels.addLabelOnTop(FACTURA_LABEL + columnPosition);
                    }
                    else if (rowObject instanceof DocumentWithDiscount)
                    	configLabels.addLabel(DISCOUNT_DOC_LABEL + columnPosition);
                }

                @Override public Collection getProvidedLabels()
                {
                    // make the custom labels available for the CSS engine
                    final Collection result = super.getProvidedLabels();
                    result.add(ACC_DOC_LABEL);
                    result.add(DISCOUNT_DOC_LABEL);
                    return result;
                }
            });

    		final IdIndexKeyHandler<Serializable> keyHandler = new IdIndexKeyHandler<>(bodyDataProvider, new RowIdAccessor());
    		try
			{
				final Field declaredField = IdIndexKeyHandler.class.getDeclaredField("clazz");
				final boolean accessible = declaredField.isAccessible();
				declaredField.setAccessible(true);
				declaredField.set(keyHandler, AccountingDocument.class);
				declaredField.setAccessible(accessible);
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
			{
				log.error(e);
			}
			dataChangeLayer = new DataChangeLayer(bodyDataLayer, keyHandler, false);
            // layer for event handling of GlazedLists and PropertyChanges
            final GlazedListsEventLayer glazedListsEventLayer = new GlazedListsEventLayer(dataChangeLayer, treeList);

            final GlazedListTreeData treeData = new GlazedListTreeData(treeList);
            final ITreeRowModel treeRowModel = new GlazedListTreeRowModel<>(treeData);

            selectionLayer = new SelectionLayer(glazedListsEventLayer);

            final TreeLayer treeLayer = new TreeLayer(selectionLayer, treeRowModel);
            final ViewportLayer viewportLayer = new ViewportLayer(treeLayer);
            final FreezeLayer freezeLayer = new FreezeLayer(selectionLayer);
    		final CompositeFreezeLayer compositeFreezeLayer = new CompositeFreezeLayer(freezeLayer, viewportLayer, selectionLayer);
            
            setUnderlyingLayer(compositeFreezeLayer);
        }
    }
    
    private static class RowIdAccessor implements IRowIdAccessor<Serializable>
	{
		@Override public Serializable getRowId(final Serializable rowObject)
		{
			if (rowObject instanceof Document)
				return ((Document) rowObject).getId();
			else if (rowObject instanceof Operatiune)
				return ((Operatiune) rowObject).getId();
			return rowObject.hashCode();
		}
	}
    
    private class TreeColumnAccessor extends ReflectiveColumnPropertyAccessor<Object>
    {
    	private IColumnAccessor<AccountingDocument> accDocAccessor;
    	private IColumnAccessor<DocumentWithDiscount> discountDocAccessor;

        public TreeColumnAccessor(final IColumnAccessor<AccountingDocument> accDocAccessor,
				final IColumnAccessor<DocumentWithDiscount> discountDocAccessor)
		{
			this.accDocAccessor = accDocAccessor;
			this.discountDocAccessor = discountDocAccessor;
		}

		@Override public Object getDataValue(final Object rowObject, final int columnIndex)
        {
			if (rowObject instanceof AccountingDocument)
				return accDocAccessor.getDataValue((AccountingDocument) rowObject, columnIndex);
			else if (rowObject instanceof DocumentWithDiscount)
                return discountDocAccessor.getDataValue((DocumentWithDiscount) rowObject, columnIndex);
            else
                return rowObject;
        }

        @Override public void setDataValue(final Object rowObject, final int columnIndex, final Object newValue)
        {
            if (rowObject instanceof AccountingDocument)
            	accDocAccessor.setDataValue((AccountingDocument) rowObject, columnIndex, newValue);
            else if (rowObject instanceof DocumentWithDiscount)
            	discountDocAccessor.setDataValue((DocumentWithDiscount) rowObject, columnIndex, newValue);
            
            if (afterChange != null)
				afterChange.accept(rowObject);
        }

        @Override public int getColumnCount()
        {
            return accDocAccessor.getColumnCount();
        }
    }
    
    private class OperatiuneTwoLevelTreeFormat implements TreeList.Format<Object>
    {
        @Override public void getPath(final List<Object> path, final Object element)
        {
        	path.add(element);
        }

        @Override public boolean allowsChildren(final Object element)
        {
            return true;
        }

        @Override public Comparator<Object> getComparator(final int depth)
        {
        	return null;
        }
    }
    
    private static class AnafReportStateDisplayConverter extends DisplayConverter
    {
    	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(AnafReportStateDisplayConverter.class.getSimpleName());
    	
    	public AnafReportStateDisplayConverter()
    	{
    		super();
    	}
    	
    	@Override
        public Object canonicalToDisplayValue(final Object canonicalValue)
    	{
            try
            {
                if (ObjectUtils.isNotNull(canonicalValue) && canonicalValue instanceof ReportedInvoice)
                	return displayAnafReportStatus((ReportedInvoice) canonicalValue);
            }
            catch (final Exception e)
            {
                log.log(Level.WARNING, "Display exception", e);
            }
            return canonicalValue;
        }
    	
    	private String displayAnafReportStatus(final ReportedInvoice invoice)
		{
			switch (invoice.getState()) {
			case SENT:
				return invoice.getUploadIndex();
				
			case WAITING_VALIDATION:
				return "Asteptare validare";
				
			case UPLOAD_ERROR:
			case REJECTED_INVALID:
				return invoice.getErrorMessage();

			default:
				return "STATE UNKNOWN: " + invoice.getState();
			}
		}

        @Override
        public Object displayToCanonicalValue(final Object displayValue)
        {
            try
            {
                return displayValue.toString();
            }
            catch (final Exception e)
            {
                throw new ConversionFailedException(Messages.getString("AnafReportStateDisplayConverter.failure", //$NON-NLS-1$
                        new Object[] { displayValue }), e);
            }
        }
    }
    
    private class AnafReportStateImagePainter extends ImagePainter
	{
    	private final Image validImg;
        private final Image waitingImg;
        private final Image invalidImg;
        
        public AnafReportStateImagePainter()
        {
            super();
            this.validImg = Icons.createImageResource(bundle, Icons.OK_16x16_PATH, ILog.get()).orElse(null);
            this.waitingImg = Icons.createImageResource(bundle, Icons.LOADING_16x16_PATH, ILog.get()).orElse(null);
            this.invalidImg = Icons.createImageResource(bundle, Icons.ERROR_16x16_PATH, ILog.get()).orElse(null);
        }
        
		@Override protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry)
		{
			final Object rowObject = cell.getDataValue();
			
			if (ObjectUtils.isNotNull(rowObject) && rowObject instanceof ReportedInvoice)
			{
				final ReportedInvoice reportedInvoice = (ReportedInvoice) rowObject;
				return convertToImage(reportedInvoice.getState());
			}
			
			return null;
		}
		
		private Image convertToImage(final ReportState state)
		{
			switch (state) {
			case SENT:
				return validImg;
				
			case WAITING_VALIDATION:
				return waitingImg;
				
			case UPLOAD_ERROR:
			case REJECTED_INVALID:
				return invalidImg;

			default:
				return null;
			}
		}
	}
    
    private class AnafMouseEventMatcher extends CellLabelMouseEventMatcher 
    {
		public AnafMouseEventMatcher(final int stateMask, final String regionName, final int button, final String labelToMatch)
		{
			super(stateMask, regionName, button, labelToMatch);
		}

		public AnafMouseEventMatcher(final String regionName, final int button, final String labelToMatch)
		{
			super(regionName, button, labelToMatch);
		}
    	
		@Override
		public boolean matches(final NatTable natTable, final MouseEvent event, final LabelStack regionLabels)
		{
			final boolean superMatches = super.matches(natTable, event, regionLabels);
			if (!superMatches)
				return false;
			
			final NatEventData eventData = NatEventData.createInstanceFromEvent(event);
			final int columnIndex = natTable.getColumnIndexByPosition(eventData.getColumnPosition());
			final int rowIndex = natTable.getRowIndexByPosition(eventData.getRowPosition());

	        final Object cellDataValue = bodyDataProvider.getDataValue(columnIndex, rowIndex);
	        if (ObjectUtils.isNotNull(cellDataValue) && cellDataValue instanceof ReportedInvoice)
	        	return matches((ReportedInvoice) cellDataValue);
	        
			return false;
		}

		private boolean matches(final ReportedInvoice repInv)
		{
			switch (repInv.getState())
			{
			case WAITING_VALIDATION:
				return true;
			case REJECTED_INVALID:
			case SENT:
				return StringUtils.notEmpty(repInv.getDownloadId());
			case UPLOAD_ERROR:
				return false;

			default:
				return false;
			}
		}
    }
    
    private class AnafMouseClickAction implements IMouseAction
    {
		@Override
		public void run(final NatTable natTable, final MouseEvent event)
		{
			final NatEventData eventData = NatEventData.createInstanceFromEvent(event);
			final int columnIndex = natTable.getColumnIndexByPosition(eventData.getColumnPosition());
			final int rowIndex = natTable.getRowIndexByPosition(eventData.getRowPosition());

	        final Object cellDataValue = bodyDataProvider.getDataValue(columnIndex, rowIndex);
	        if (ObjectUtils.isNotNull(cellDataValue) && cellDataValue instanceof ReportedInvoice)
	        	runAction((ReportedInvoice) cellDataValue);
		}

		private void runAction(final ReportedInvoice repInv)
		{
			switch (repInv.getState())
			{
			case WAITING_VALIDATION:
				if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Continuati?",
						"Fortati o verificare a starii de validare a facturilor raportate la ANAF?"))
					AnafReporter.forceCheckReportedInvoices();
				break;
			case REJECTED_INVALID:
			case SENT:
				AnafReporter.downloadResponse(repInv.getDownloadId());
				break;
			case UPLOAD_ERROR:
				break;

			default:
				break;
			}
		}
    }
}
