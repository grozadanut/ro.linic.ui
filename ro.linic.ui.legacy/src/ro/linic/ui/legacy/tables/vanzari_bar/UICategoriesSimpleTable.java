package ro.linic.ui.legacy.tables.vanzari_bar;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
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
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.tables.components.ImageDisplayConverter;
import ro.linic.ui.legacy.tables.components.LinicNatTableStyleConfiguration;
import ro.linic.ui.legacy.tables.components.LinicSelectionStyleConfiguration;
import ro.linic.ui.legacy.tables.components.UuidImagePainter;

public class UICategoriesSimpleTable
{
	private EventList<ProductUiCategory> sourceData;
	private NatTable table;
	
	private SelectionLayer selectionLayer;
	private RowSelectionProvider<ProductUiCategory> selectionProvider;
	
	public UICategoriesSimpleTable()
	{
	}

	public void postConstruct(final Composite parent)
	{
		final IColumnAccessor<ProductUiCategory> columnAccessor = new ColumnAccessor();
		
		sourceData = GlazedLists.eventListOf();
        final TransformedList<ProductUiCategory, ProductUiCategory> rowObjectsGlazedList = GlazedLists.threadSafeList(sourceData);
        final SortedList<ProductUiCategory> sortedData = new SortedList<>(rowObjectsGlazedList, null);

		// create the body layer stack
		final IRowDataProvider<ProductUiCategory> bodyDataProvider = new ListDataProvider<>(sortedData, columnAccessor);
		final DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(new ColumnLabelAccumulator(bodyDataProvider));
		bodyDataLayer.setDefaultColumnWidthByPosition(0, 200);
		final GlazedListsEventLayer<ProductUiCategory> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, sortedData);
		selectionLayer = new SelectionLayer(glazedListsEventLayer);
		final ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);
		viewportLayer.setRegionName(GridRegion.BODY);
		
		selectionProvider = new RowSelectionProvider<>(selectionLayer, bodyDataProvider);
		
		table = new NatTable(parent, viewportLayer, false);
		table.addConfiguration(new LinicNatTableStyleConfiguration());
		table.addConfiguration(new LinicSelectionStyleConfiguration());
		table.addConfiguration(new CustomStyleConfiguration());

		// Custom selection configuration
		selectionLayer.setSelectionModel(
				new RowSelectionModel<>(selectionLayer, bodyDataProvider, new IRowIdAccessor<ProductUiCategory>()
				{
					@Override public Serializable getRowId(final ProductUiCategory rowObject)
					{
						return rowObject.hashCode();
					}
				}, true)); //multi selection
		
		selectionLayer.addConfiguration(new RowOnlySelectionConfiguration());
		table.addConfiguration(new RowOnlySelectionBindings());
		table.configure();
	}
	
	public UICategoriesSimpleTable loadData(final ImmutableList<ProductUiCategory> data)
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
	
	public void addSelectionListener(final ISelectionChangedListener listener)
	{
		selectionProvider.addSelectionChangedListener(listener);
	}
	
	public EventList<ProductUiCategory> getSourceData()
	{
		return sourceData;
	}
	
	public NatTable getTable()
	{
		return table;
	}
	
	public List<ProductUiCategory> selection()
	{
		return ((RowSelectionModel<ProductUiCategory>) selectionLayer.getSelectionModel()).getSelectedRowObjects();
	}
	
	public void unselectAll()
	{
		selectionLayer.clear();
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
	}
	
	private static class ColumnAccessor implements IColumnAccessor<ProductUiCategory>
	{
		@Override public Object getDataValue(final ProductUiCategory rowObject, final int columnIndex)
		{
			return rowObject;
		}

		@Override public void setDataValue(final ProductUiCategory rowObject, final int columnIndex, final Object newValue)
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
		}
		
		@Override
		protected String convertDataType(final ILayerCell cell, final IConfigRegistry configRegistry)
		{
			final Object rowObject = cell.getDataValue();
			if (rowObject instanceof ProductUiCategory)
				return ((ProductUiCategory) rowObject).getName();
			return super.convertDataType(cell, configRegistry);
		}
	}
	
	private static class CustomImagePainter extends ImagePainter
	{
		@Override protected Image getImage(final ILayerCell cell, final IConfigRegistry configRegistry)
		{
			final Object rowObject = cell.getDataValue();
			
			if (rowObject instanceof ProductUiCategory && ((ProductUiCategory) rowObject).getImage() != null)
				try
				{
					return Icons.imageFromBytes(((ProductUiCategory) rowObject).getImage(), UuidImagePainter.DEFAULT_HEIGHT);
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			
			return null;
		}
	}
}
