package ro.linic.ui.legacy.tables.vanzari_bar;

import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.SPACE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.StringUtils.globalIsMatch;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ImagePainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.selection.command.MoveSelectionCommand;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectCellCommand;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.tables.components.ImageDisplayConverter;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.UuidImagePainter;

public class UIProductsSimpleTable
{
	private EventList<Product> sourceData;
	private NatTable table;
	
	private TextMatcherEditor<Product> quickSearchFilter;
	private SelectionLayer selectionLayer;
	private RowSelectionProvider<Product> selectionProvider;
	private ViewportLayer viewportLayer;
	private IMouseAction doubleClickAction;
	
	private Bundle bundle;
	private Logger log;
	
	public UIProductsSimpleTable(final Bundle bundle, final Logger log)
	{
		this.bundle = bundle;
		this.log = log;
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnAccessor<Product> columnAccessor = new ColumnAccessor();
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<Product, Product> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);
        final FilterList<Product> filteredData = new FilterList<>(rowObjectsGlazedList);
        final SortedList<Product> sortedData = new SortedList<>(filteredData, null);

		// create the body layer stack
		final IRowDataProvider<Product> bodyDataProvider = new ListDataProvider<>(sortedData, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		bodyDataLayer.setDefaultColumnWidthByPosition(0, 250);
		final GlazedListsEventLayer<Product> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedData);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		viewportLayer = new ViewportLayer(selectionLayer);
		viewportLayer.setRegionName(GridRegion.BODY);
		
		selectionProvider = new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
		
		table = new NatTable(parent, viewportLayer, false);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<Product>()
				{
					@Override public Serializable getRowId(final Product rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
		
		// define a TextMatcherEditor and set it to the FilterList
		quickSearchFilter = new TextMatcherEditor<Product>(new TextFilterator<Product>()
		{
			@Override public void getFilterStrings(final List<String> baseList, final Product element)
			{
				baseList.add(element.getBarcode());
				baseList.add(element.getName());
			}
		});
		quickSearchFilter.setMode(TextMatcherEditor.CONTAINS);
		
		filteredData.setMatcherEditor(quickSearchFilter);
	}
	
	public UIProductsSimpleTable loadData(final ImmutableList<Product> data)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			this.sourceData.clear();
			this.sourceData.addAll(data);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public UIProductsSimpleTable remove(final Product product)
	{
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.remove(product);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		return this;
	}
	
	public UIProductsSimpleTable replace(final Product oldProduct, final Product newProduct)
	{
		final boolean restoreSelection = selection().stream()
				.anyMatch(p -> p.equals(oldProduct));
		try
		{
			this.sourceData.getReadWriteLock().writeLock().lock();
			sourceData.set(sourceData.indexOf(oldProduct), newProduct);
			table.refresh();
		}
		finally
		{
			this.sourceData.getReadWriteLock().writeLock().unlock();
		}
		if (restoreSelection)
			selectionProvider.setSelection(new StructuredSelection(new Product[] {newProduct}));
		return this;
	}
	
	public void addSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.addSelectionChangedListener(listener);
	}
	
	public void moveSelection(final MoveDirectionEnum direction)
	{
		final List<Product> sel = selection();
		selectionProvider.setSelection(new StructuredSelection(sel));
		
		if (sel.isEmpty())
			viewportLayer.doCommand(new SelectCellCommand(viewportLayer, 0, 0, false, false));
		else
			viewportLayer.doCommand(new MoveSelectionCommand(direction, false, false));
	}
	
	public UIProductsSimpleTable filter(final String searchText)
	{
	     quickSearchFilter.setFilterText(searchText.split(SPACE));
	     return this;
	}
	
	public EventList<Product> getSourceData()
	{
		return sourceData;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<Product> selection()
	{
		return ((RowSelectionModel<Product>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public void doubleClickAction(final IMouseAction doubleClickAction)
	{
		this.doubleClickAction = doubleClickAction;
	}
	
	private class CustomStyleConfiguration extends AbstractRegistryConfiguration
	{
		@Override public void configureRegistry(final IConfigRegistry configRegistry)
		{
			// image
			final CellPainterDecorator painter = new CellPainterDecorator(
					new CustomTextPainter(),
					CellEdgeEnum.TOP, 
					new CustomImagePainter());
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, painter, DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 0);
			
			// DISPLAY CONVERTERS
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new ImageDisplayConverter(), DisplayMode.NORMAL, 
					ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + 0);
		}
		
		@Override public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
		{
			if (doubleClickAction != null)
				uiBindingRegistry.registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(0), doubleClickAction);
		}
	}
	
	private static class ColumnAccessor implements IColumnAccessor<Product>
	{
		@Override public Object getDataValue(final Product rowObject, final int columnIndex)
		{
			return rowObject;
		}

		@Override public void setDataValue(final Product rowObject, final int columnIndex, final Object newValue)
		{
		}

		@Override public int getColumnCount()
		{
			return 1;
		}
	}
	
	private static class CustomTextPainter extends TextPainter
	{
		public CustomTextPainter()
		{
			setCalculateByTextHeight(true);
			setWrapText(true);
		}
		
		@Override
		protected String convertDataType(final ILayerCell cell, final IConfigRegistry configRegistry)
		{
			final Object rowObject = cell.getDataValue();
			if (rowObject instanceof Product)
			{
				final Product p = ((Product) rowObject);
				return MessageFormat.format("{0}{2}{1} {3}", 
						p.getName(),
						displayBigDecimal(p.getPricePerUom()),
						NEWLINE,
						globalIsMatch(p.getCategorie(), Product.DISCOUNT_CATEGORY, TextFilterMethod.EQUALS) ? p.getUom() : "RON");
			}
			return super.convertDataType(cell, configRegistry);
		}
	}
	
	private class CustomImagePainter extends ImagePainter
	{
		@Override protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry)
		{
			final Object rowObject = cell.getDataValue();
			
			if (rowObject instanceof Product)
			{
				final Product p = (Product) rowObject;
				try
				{
					return Icons.imageFromBytes(BusinessDelegate.imageFromUuid(bundle, log, p.getImageUUID(), false),
							UuidImagePainter.DEFAULT_HEIGHT);
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
			
			return null;
		}
	}
}
