package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectRowsCommand;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.osgi.framework.Bundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.nattable.Column;
import ro.linic.ui.base.services.nattable.FullFeaturedNatTable;
import ro.linic.ui.base.services.nattable.TableBuilder;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.security.services.AuthenticationSession;

public class ReconciliationPart {
	private static final ILog log = ILog.of(ReconciliationPart.class);
	
	public static final String PART_ID = "linic_gest_client.part.reconciliation"; //$NON-NLS-1$
	public static final String DATA_HOLDER = "Reconciliation"; //$NON-NLS-1$
	
	private static final String TABLE_LEFT_STATE_PREFIX = "reconciliation.left_nt"; //$NON-NLS-1$
	private static final String TABLE_RIGHT_STATE_PREFIX = "reconciliation.right_nt"; //$NON-NLS-1$
	private static final String HORIZONTAL_SASH_STATE_PREFIX = "statistics.horizontal_sash"; //$NON-NLS-1$

	private static final Column statusColumn = new Column(0, "result", "Status", 90);
	private static final Column matchColumn = new Column(1, "result", "Match", 350);
	
	private static final List<Column> COLUMNS = List.of(statusColumn, matchColumn);
	
	private static void selectRow(final SelectionLayer selectionLayer, final List<GenericValue> rowObjects, final EventList<GenericValue> data) {
		selectionLayer.clear(false);
		final Set<Integer> rowPositions = new HashSet<>();
		for (final GenericValue rowObject : rowObjects) {
			final int rowIndex = data.indexOf(rowObject);
			final int rowPosition = selectionLayer.getRowPositionByIndex(rowIndex);
			rowPositions.add(Integer.valueOf(rowPosition));
		}
		int intValue = -1;
		if (!rowPositions.isEmpty()) {
			final Integer max = Collections.max(rowPositions);
			intValue = max.intValue();
		}
		selectionLayer.doCommand(
				new SelectRowsCommand(
						selectionLayer,
						0,
						ObjectUtils.asIntArray(rowPositions),
						false,
						true,
						intValue));
	}
	
	private FullFeaturedNatTable<GenericValue> tableLeft;
	private FullFeaturedNatTable<GenericValue> tableRight;
	private SashForm horizontalSash;
	
	private Button actionToRight;

	@Inject private MPart part;
	@Inject private EPartService partService;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private UISynchronize sync;
	@Inject private AuthenticationSession authSession;
	@Inject private DataServices dataServices;

	private ImmutableList<Gestiune> allGestiuni;

	@PostConstruct
	public void createComposite(final Composite parent) {
		this.allGestiuni = BusinessDelegate.allGestiuni().stream()
				.sorted(Comparator.comparing(Gestiune::getName))
				.collect(toImmutableList());
		
		final Composite tableContainer = new Composite(parent, SWT.NONE);
		tableContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableContainer);
		
		horizontalSash = new SashForm(tableContainer, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(horizontalSash);

		final EventList<GenericValue> data = dataServices.holder(DATA_HOLDER).getData();
		tableLeft = TableBuilder.with(GenericValue.class, COLUMNS, data)
				.addConfiguration(new LeftStyleConfiguration())
				.addLabels(Set.of("GREEN_LABEL", "YELLOW_LABEL", "RED_LABEL"), gv -> {
					switch (gv.getChild("result").getChild("match").getString("status") + "_" + gv.getChild("result").getString("status")) {
					case "CONFIRMED_RECONCILED":
						return List.of("GREEN_LABEL");
					case "CONFIRMED_MISMATCH":
						return List.of("RED_LABEL");
					case "CONFIRMED_LEFT_ONLY":
					case "PROBABLE_LEFT_ONLY":
						return List.of("YELLOW_LABEL");

					default:
						return List.of();
					}
				})
				.build(horizontalSash);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableLeft.natTable());
		loadState(TABLE_LEFT_STATE_PREFIX, tableLeft.natTable(), part);
		
		final Composite rightContainer = new Composite(horizontalSash, SWT.NONE);
		rightContainer.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(rightContainer);
		
		actionToRight = new Button(rightContainer, SWT.PUSH);
		actionToRight.setText(">");
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, true).applyTo(actionToRight);
		actionToRight.setEnabled(false);
		
		tableRight = TableBuilder.with(GenericValue.class, COLUMNS, data)
				.addConfiguration(new RightStyleConfiguration())
				.addLabels(Set.of("GREEN_LABEL", "YELLOW_LABEL", "RED_LABEL"), gv -> {
					switch (gv.getChild("result").getChild("match").getString("status") + "_" + gv.getChild("result").getString("status")) {
					case "CONFIRMED_RECONCILED":
						return List.of("GREEN_LABEL");
					case "CONFIRMED_MISMATCH":
						return List.of("RED_LABEL");
					case "CONFIRMED_RIGHT_ONLY":
					case "PROBABLE_RIGHT_ONLY":
						return List.of("YELLOW_LABEL");

					default:
						return List.of();
					}
				})
				.build(rightContainer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableRight.natTable());
		loadState(TABLE_RIGHT_STATE_PREFIX, tableRight.natTable(), part);
		new RightContentTooltip(tableRight.natTable());
		
		final int[] verticalWeights = new int[2];
		verticalWeights[0] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".0", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		verticalWeights[1] = Integer.parseInt(part.getPersistedState().getOrDefault(HORIZONTAL_SASH_STATE_PREFIX+".1", "200")); //$NON-NLS-1$ //$NON-NLS-2$
		horizontalSash.setWeights(verticalWeights);
		
		addListeners();
	}
	
	private void addListeners() {
		final AtomicBoolean internal = new AtomicBoolean(false);
		final EventList<GenericValue> data = dataServices.holder(DATA_HOLDER).getData();
		new RowSelectionProvider<>(tableLeft.selectionLayer(), tableLeft.bodyDataProvider(), false)
		.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if (!internal.get()) {
					internal.set(true);
					selectRow(tableRight.selectionLayer(), tableLeft.selection(), data);
					internal.set(false);
				}
				actionToRight.setEnabled(!event.getSelection().isEmpty() && 
						tableLeft.selection().stream()
						.filter(gv -> "createAccDoc".equals(gv.getString("endpoint")))
						.findFirst().isPresent());
			}
		});
		new RowSelectionProvider<>(tableRight.selectionLayer(), tableRight.bodyDataProvider(), false)
		.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if (!internal.get()) {
					internal.set(true);
					selectRow(tableLeft.selectionLayer(), tableRight.selection(), data);
					internal.set(false);
				}
			}
		});
		
		actionToRight.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final MPart createdPart = partService.showPart(UrmarireParteneriPart.PART_ID, PartState.ACTIVATE);
				if (createdPart == null)
					return;
				final UrmarireParteneriPart part = (UrmarireParteneriPart) createdPart.getObject();
				if (part == null)
					return;
				
				final GenericValue gv = tableLeft.selection().stream()
						.filter(g -> "createAccDoc".equals(g.getString("endpoint")))
						.findFirst()
						.get();
				
				final Integer gestId = gv.getChild("params").getInt("gestiuneId");

				part.selectGest(gestId);
				part.selectPartner(gv.getChild("params").getString("partnerName"));
				part.selectTipDoc(TipDoc.valueOf(gv.getChild("params").getString("tipDoc")));
				part.insertFrom(gv.getChild("params").getLocalDate("dataDoc"));
				part.insertTo(gv.getChild("params").getLocalDate("dataDoc"));
				part.openAddAccDocDialog(gv.getChild("params"));
			}
		});
	}

	@PersistState
	public void persistVisualState() {
		saveState(TABLE_LEFT_STATE_PREFIX, tableLeft.natTable(), part);
		saveState(TABLE_RIGHT_STATE_PREFIX, tableRight.natTable(), part);

		final int[] verticalWeights = horizontalSash.getWeights();
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX + ".0", String.valueOf(verticalWeights[0])); //$NON-NLS-1$
		part.getPersistedState().put(HORIZONTAL_SASH_STATE_PREFIX + ".1", String.valueOf(verticalWeights[1])); //$NON-NLS-1$
	}
	
	private class LeftStyleConfiguration extends AbstractRegistryConfiguration {
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry) {
			final Style greenStyle = new Style();
			greenStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			final Style yellowStyle = new Style();
			yellowStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			final Style redStyle = new Style();
			redStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenStyle, DisplayMode.NORMAL, "GREEN_LABEL");
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowStyle, DisplayMode.NORMAL, "YELLOW_LABEL");
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, redStyle, DisplayMode.NORMAL, "RED_LABEL");
			
			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new DefaultDisplayConverter() {
				@Override
				public Object canonicalToDisplayValue(final Object rowObj) {
					if (rowObj instanceof Map) {
						final Map result = (Map) rowObj;
						return ((Map) result.get("match")).get("status") + ": " + result.get("status");
					}
					
					return super.canonicalToDisplayValue(rowObj);
				}
			}, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + COLUMNS.indexOf(statusColumn));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new DefaultDisplayConverter() {
						@Override
						public Object canonicalToDisplayValue(final Object rowObj) {
							if (rowObj instanceof Map) {
								final Map result = (Map) rowObj;
								final List<Map> right = (List<Map>) ((Map) result.get("match")).get("left");
					    		return right.stream().map(nr -> nr.get("fields").toString()).collect(Collectors.joining(PresentationUtils.NEWLINE));
					    	}
					    	
					        return super.canonicalToDisplayValue(rowObj);
						}
					}, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + COLUMNS.indexOf(matchColumn));
		}
	}
	
	private class RightStyleConfiguration extends AbstractRegistryConfiguration {
		@Override
		public void configureRegistry(final IConfigRegistry configRegistry) {
			final Style greenStyle = new Style();
			greenStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			final Style yellowStyle = new Style();
			yellowStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			final Style redStyle = new Style();
			redStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, greenStyle, DisplayMode.NORMAL, "GREEN_LABEL");
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, yellowStyle, DisplayMode.NORMAL, "YELLOW_LABEL");
			configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, redStyle, DisplayMode.NORMAL, "RED_LABEL");
			
			// Display converters
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new DefaultDisplayConverter() {
				@Override
				public Object canonicalToDisplayValue(final Object rowObj) {
					if (rowObj instanceof Map) {
						final Map result = (Map) rowObj;
						return ((Map) result.get("match")).get("status") + ": " + result.get("status");
					}
					
					return super.canonicalToDisplayValue(rowObj);
				}
			}, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + COLUMNS.indexOf(statusColumn));
			
			configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER,
					new DefaultDisplayConverter() {
						@Override
						public Object canonicalToDisplayValue(final Object rowObj) {
							if (rowObj instanceof Map) {
								final Map result = (Map) rowObj;
								final List<Map> right = (List<Map>) ((Map) result.get("match")).get("right");
					    		return right.stream().map(nr -> nr.get("fields").toString()).collect(Collectors.joining(PresentationUtils.NEWLINE));
					    	}
					    	
					        return super.canonicalToDisplayValue(rowObj);
						}
					}, DisplayMode.NORMAL, ColumnLabelAccumulator.COLUMN_LABEL_PREFIX + COLUMNS.indexOf(matchColumn));
		}
	}
	
	private class RightContentTooltip extends NatTableContentTooltip {
		public RightContentTooltip(final NatTable natTable, final String... tooltipRegions) {
			super(natTable, tooltipRegions);
		}

		@Override
		protected String getText(final Event event) {
			final int col = this.natTable.getColumnPositionByX(event.x);
			final int row = this.natTable.getRowPositionByY(event.y);
			final EventList<GenericValue> data = dataServices.holder(DATA_HOLDER).getData();

			if (data.isEmpty())
				return super.getText(event);

//	        int colIdx = LayerUtil.convertColumnPosition(this.natTable, col, this.bodyDataLayer);
//	        int rowIdx = LayerUtil.convertRowPosition(this.natTable, row, this.bodyDataLayer);
	        
			if (col == COLUMNS.indexOf(statusColumn) + 1 && row > 0) // +1 because of the row header column
				return prettyPrintJson(HttpUtils.toJSON(data.get(this.natTable.getRowIndexByPosition(row))));

			return super.getText(event);
		}
		
		public String prettyPrintJson(final String uglyJsonString) {
			try {
				final ObjectMapper objectMapper = new ObjectMapper();
				final Object jsonObject = objectMapper.readValue(uglyJsonString, Object.class);
				return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
			} catch (final JsonProcessingException e) {
				log.error(e.getMessage(), e);
				return "";
			}
		}
	}
}
