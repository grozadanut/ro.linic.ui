package ro.linic.ui.legacy.widgets;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.LIST_SEPARATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.nattable.edit.EditConstants;
import org.eclipse.nebula.widgets.nattable.style.CellStyleUtil;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.VerticalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.ui.matcher.LetterOrDigitKeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.util.ArrayUtil;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * Customized combobox control that supports editing directly in the text field
 * and selecting items from the dropdown.
 *
 * <p>
 * This control supports the ability for multi select in the dropdown of the
 * combo which is not available for the SWT Combo control. This feature was
 * added with Nebula NatTable 1.0.0
 *
 * <p>
 * The following style bits are supported by this control.
 *
 * @see SWT#BORDER (if a border should be added to the Text control)
 * @see SWT#READ_ONLY (default for Text control, if this is missing, the Text
 *      control can be edited)
 * @see SWT#CHECK (if the items in the combo should be showed with checkboxes)
 * @see SWT#MULTI (if multi selection is allowed)
 */
public class NatComboWidget extends Composite
{
	/**
	 * Default String that is used to separate values in the String representation
	 * showed in the text control if multiselect is supported.
	 */
	public static final String DEFAULT_MULTI_SELECT_VALUE_SEPARATOR = LIST_SEPARATOR;
	/**
	 * Default String that is used to prefix the generated String representation
	 * showed in the text control if multiselect is supported.
	 */
	public static final String DEFAULT_MULTI_SELECT_PREFIX = EMPTY_STRING;
	/**
	 * String that is used to suffix the generated String representation showed in
	 * the text control if multiselect is supported.
	 */
	public static final String DEFAULT_MULTI_SELECT_SUFFIX = EMPTY_STRING;
	/**
	 * The default number of visible items on open the combo.
	 */
	public static final int DEFAULT_NUM_OF_VISIBLE_ITEMS = 15;

	/**
	 * The maximum number of visible items of the combo. Setting this value to -1
	 * will result in always showing all items at once.
	 */
	protected int maxVisibleItems;

	/**
	 * The items that are showed within the combo transformed to a java.util.List.
	 * Needed for indexed operations in the dropdown
	 */
	protected java.util.List<String> itemList;

	/**
	 * Map used to hold the selection state of items in the drop. Needed to maintain
	 * state when filtering
	 *
	 * @since 1.4
	 */
	protected Map<String, Boolean> selectionStateMap;

	/**
	 * The text control allowing filtering of options
	 *
	 * @since 1.4
	 */
	protected Text filterBox;

	/**
	 * The text control of this NatComboWidget, allowing to enter values directly.
	 */
	protected Text text;
	protected Canvas iconCanvas;

	/**
	 * The Shell containing the dropdown of this NatComboWidget
	 */
	protected PopupDialog dropdownShell;

	/**
	 * The Table control used for the combo component of this NatComboWidget
	 */
	protected Table dropdownTable;

	/**
	 * The Table control used for the combo component of this NatComboWidget
	 *
	 * @since 1.4
	 */
	protected TableViewer dropdownTableViewer;

	/**
	 * The image that is shown at the right edge of the text control if the
	 * NatComboWidget is opened.
	 */
	protected Image iconImage;

	/**
	 * The style bits that where set on creation time. Needed in case the dropdown
	 * shell was disposed and needs to be created again.
	 */
	protected final int style;

	/**
	 * Flag that indicated whether this NatComboWidget supports filtering of the
	 * values in the dropdown control
	 *
	 * @since 1.4
	 */
	protected boolean showDropdownFilter;

	/**
	 * Flag that indicates whether this ComboBoxCellEditor supports free editing in
	 * the text control of the NatComboWidget or not. By default free editing is
	 * disabled.
	 */
	protected boolean freeEdit;

	/**
	 * Flag that indicates whether this NatComboWidget supports multiselect or not.
	 * By default multiselect is disabled.
	 */
	protected boolean multiselect;

	/**
	 * Flag that indicates whether checkboxes should be shown for the items in the
	 * dropdown.
	 */
	protected boolean useCheckbox;

	/**
	 * String that is used to separate values in the String representation showed in
	 * the text control if multiselect is supported.
	 */
	protected String multiselectValueSeparator = DEFAULT_MULTI_SELECT_VALUE_SEPARATOR;
	/**
	 * String that is used to prefix the generated String representation showed in
	 * the text control if multiselect is supported. Needed to visualize the
	 * multiselection to the user.
	 */
	protected String multiselectTextPrefix = DEFAULT_MULTI_SELECT_PREFIX;
	/**
	 * String that is used to suffix the generated String representation showed in
	 * the text control if multiselect is supported. Needed to visualize the
	 * multiselection to the user.
	 */
	protected String multiselectTextSuffix = DEFAULT_MULTI_SELECT_SUFFIX;

	/**
	 * Flag that tells whether the NatComboWidget has focus or not. The flag is set
	 * by the FocusListenerWrapper that is set as focus listener on both, the Text
	 * control and the dropdown table control. This flag is necessary as the
	 * NatComboWidget has focus if either of both controls have focus.
	 */
	private boolean hasFocus = false;
	/**
	 * The list of FocusListener that contains the listeners that will be informed
	 * if the NatComboWidget control gains or looses focus. We keep our own list of
	 * listeners because the two controls that are combined in this control share
	 * the same focus.
	 */
	private List<FocusListener> focusListener = new ArrayList<FocusListener>();

	/**
	 * List of KeyListener that should be added to the dropdown table once it is
	 * created. Kept locally because the table creation is deferred to the first
	 * access.
	 */
	private List<KeyListener> keyListener = new ArrayList<KeyListener>();
	/**
	 * List of TraverseListener that should be added to the dropdown table once it
	 * is created. Kept locally because the table creation is deferred to the first
	 * access.
	 */
	private List<TraverseListener> traverseListener = new ArrayList<TraverseListener>();
	/**
	 * List of MouseListener that should be added to the dropdown table once it is
	 * created. Kept locally because the table creation is deferred to the first
	 * access.
	 */
	private List<MouseListener> mouseListener = new ArrayList<MouseListener>();
	/**
	 * List of SelectionListener that should be added to the dropdown table once it
	 * is created. Kept locally because the table creation is deferred to the first
	 * access.
	 */
	private List<SelectionListener> selectionListener = new ArrayList<SelectionListener>();
	/**
	 * List of ShellListener that should be added to the dropdown table once it is
	 * created. Kept locally because the table creation is deferred to the first
	 * access.
	 */
	private List<ShellListener> shellListener = new ArrayList<ShellListener>();

	/**
	 * Creates a new NatComboWidget using the given IStyle for rendering, showing
	 * the given amount of items at once in the dropdown.
	 *
	 * @param parent             A widget that will be the parent of this
	 *                           NatComboWidget
	 * @param cellStyle          Style configuration containing horizontal
	 *                           alignment, font, foreground and background color
	 *                           information.
	 * @param maxVisibleItems    the max number of items the drop down will show
	 *                           before introducing a scroll bar.
	 * @param style              The style for the {@link Text} Control to
	 *                           construct. Uses this style adding internal styles
	 *                           via ConfigRegistry.
	 * @param iconImage          The image to use as overlay to the {@link Text}
	 *                           Control if the dropdown is visible. Using this
	 *                           image will indicate that the control is an open
	 *                           combo to the user.
	 *
	 * @param showDropdownFilter Flag indicating whether the dropdown filter is
	 *                           displayed
	 *
	 * @since 1.4
	 */
	public NatComboWidget(final Composite parent, final int style)
	{
		super(parent, SWT.NONE);

		this.maxVisibleItems = DEFAULT_NUM_OF_VISIBLE_ITEMS;
		this.iconImage = GUIHelper.getImage("down_2");

		this.style = style;

		this.showDropdownFilter = false;
		this.freeEdit = (style & SWT.READ_ONLY) == 0;
		this.multiselect = (style & SWT.MULTI) != 0;
		this.useCheckbox = (style & SWT.CHECK) != 0;

		final GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);

		createTextControl(style);

		// typically the dropdown shell should be hidden when the focus is lost
		// but in case the NatComboWidget is the first control in a shell, the text
		// control will get the focus immediately after the shell lost focus.
		// as handling with focus listeners in such a case fails, we add a move
		// listener that will update the position of the dropdown shell if the
		// parent shell moves
		final Listener moveListener = new Listener()
		{
			@Override
			public void handleEvent(final Event event)
			{
				calculateBounds();
			}
		};
		getShell().addListener(SWT.Move, moveListener);

		addDisposeListener(new DisposeListener()
		{
			@Override
			public void widgetDisposed(final DisposeEvent e)
			{
				if (NatComboWidget.this.dropdownShell != null)
				{
					NatComboWidget.this.dropdownShell.close();
				}
				NatComboWidget.this.text.dispose();
				NatComboWidget.this.getShell().removeListener(SWT.Move, moveListener);
			}
		});
		
//		addFocusListener(new FocusAdapter()
//		{
//			@Override
//			public void focusLost(final FocusEvent e)
//			{
//				hideDropdownControl();
//			}
//		});
	}

	/**
	 * Sets the given items to be the items shown in the dropdown of this
	 * NatComboWidget.
	 *
	 * @param items The array of items to set.
	 */
	public void setItems(final String[] items)
	{
		if (items != null)
		{
			this.itemList = Arrays.asList(items);
			this.selectionStateMap = new LinkedHashMap<String, Boolean>();
			for (final String item : items)
			{
				this.selectionStateMap.put(item, Boolean.FALSE);
			}
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTableViewer.setInput(items);
			}
		}
	}
	
	public java.util.List<String> getItems()
	{
		return itemList;
	}

	/**
	 * Creates the Text control of this NatComboWidget, adding styles, look&amp;feel
	 * and needed listeners for the control only.
	 *
	 * @param style The style for the Text Control to construct. Uses this style
	 *              adding internal styles via ConfigRegistry.
	 */
	protected void createTextControl(final int style)
	{
		this.text = new Text(this, SWT.BORDER);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.text.setLayoutData(gridData);

		this.text.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(final KeyEvent event)
			{
				if (event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_UP)
				{
					showDropdownControl();

					final int selectionIndex = getDropdownTable().get().getSelectionIndex();
					if (selectionIndex < 0)
					{
						select(0);
					} else
					{
						// only visualize the selection in the dropdown, do not
						// perform a selection
						getDropdownTable().get().select(selectionIndex);
					}

					// ensure the arrow key events do not have any further
					// effect
					event.doit = false;
				} else if (!LetterOrDigitKeyEventMatcher.isLetterOrDigit(event.character))
				{
					if (NatComboWidget.this.freeEdit)
					{
						// simply clear the selection in dropdownlist so the
						// free value in text control will be used
						if (getDropdownTable().filter(t -> !t.isDisposed()).isPresent())
						{
							getDropdownTable().get().deselectAll();
							for (final Map.Entry<String, Boolean> entry : NatComboWidget.this.selectionStateMap
									.entrySet())
							{
								entry.setValue(Boolean.FALSE);
							}
						}
					} else
					{
						showDropdownControl();
					}
				}
			}
		});

		this.text.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(final MouseEvent e)
			{
				if (!NatComboWidget.this.freeEdit)
				{
					if (getDropdownTable().isEmpty() || getDropdownTable().get().isDisposed() || !getDropdownTable().get().isVisible())
					{
						showDropdownControl();
					} else
					{
						// if there is no free edit enabled, set the focus back
						// to the dropdownlist so it handles key strokes itself
						getDropdownTable().get().forceFocus();
					}
				}
			}
		});

		this.text.addControlListener(new ControlListener()
		{
			@Override
			public void controlResized(final ControlEvent e)
			{
				calculateBounds();
			}

			@Override
			public void controlMoved(final ControlEvent e)
			{
				calculateBounds();
			}
		});

		this.text.addFocusListener(new FocusListenerWrapper());

		iconCanvas = new Canvas(this, SWT.NONE)
		{

			@Override
			public Point computeSize(final int wHint, final int hHint, final boolean changed)
			{
				final Rectangle iconImageBounds = NatComboWidget.this.iconImage.getBounds();
				return new Point(iconImageBounds.width + 2, iconImageBounds.height + 2);
			}

		};

		gridData = new GridData(GridData.BEGINNING, SWT.FILL, false, true);
		iconCanvas.setLayoutData(gridData);

		iconCanvas.addPaintListener(new PaintListener()
		{

			@Override
			public void paintControl(final PaintEvent event)
			{
				final GC gc = event.gc;

				final Rectangle iconCanvasBounds = iconCanvas.getBounds();
				final Rectangle iconImageBounds = NatComboWidget.this.iconImage.getBounds();
				final int horizontalAlignmentPadding = CellStyleUtil.getHorizontalAlignmentPadding(
						HorizontalAlignmentEnum.CENTER, iconCanvasBounds, iconImageBounds.width);
				final int verticalAlignmentPadding = CellStyleUtil.getVerticalAlignmentPadding(
						VerticalAlignmentEnum.MIDDLE, iconCanvasBounds, iconImageBounds.height);
				gc.drawImage(NatComboWidget.this.iconImage, horizontalAlignmentPadding, verticalAlignmentPadding);

				final Color originalFg = gc.getForeground();
				gc.setForeground(GUIHelper.COLOR_WIDGET_BORDER);
				gc.drawRectangle(0, 0, iconCanvasBounds.width - 1, iconCanvasBounds.height - 1);
				gc.setForeground(originalFg);
			}

		});

		iconCanvas.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(final MouseEvent e)
			{
				if (NatComboWidget.this.dropdownShell != null && NatComboWidget.this.dropdownShell.getShell() != null &&
						!NatComboWidget.this.dropdownShell.getShell().isDisposed())
				{
					if (NatComboWidget.this.dropdownShell.getShell().isVisible())
					{
						NatComboWidget.this.text.forceFocus();
						hideDropdownControl();
					} else
					{
						showDropdownControl();
					}
				} else
				{
					showDropdownControl();
				}
			}
		});
	}

	/**
	 * Create the dropdown control of this NatComboWidget, adding styles,
	 * look&amp;feel and needed listeners for the control only.
	 *
	 * @param style The style for the Table Control to construct. Uses this style
	 *              adding internal styles via ConfigRegistry.
	 */
	protected void createDropdownControl(final int style)
	{
		this.dropdownShell = new PopupDialog(getShell(), SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null)
		{
			@Override protected Control createContents(final Composite parent)
			{
				// (SWT.V_SCROLL | SWT.NO_SCROLL) prevents appearance of unnecessary
				// horizontal scrollbar on mac
				// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=304128
				final int scrollStyle = ((itemList != null && itemList.size() > maxVisibleItems)
						&& maxVisibleItems > 0) ? (SWT.V_SCROLL | SWT.NO_SCROLL) : SWT.NO_SCROLL;
				final int dropdownListStyle = style | scrollStyle | SWT.FULL_SELECTION;

				dropdownTable = new Table(parent, dropdownListStyle);
				dropdownTableViewer = new TableViewer(dropdownTable);
				dropdownTable.setFont(text.getFont());
				
				// add a column to be able to resize the item width in the dropdown
				new TableColumn(dropdownTable, SWT.NONE);

				dropdownTableViewer.setContentProvider(new IStructuredContentProvider()
						{
							@Override
							public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
							{
							}
							
							@Override
							public void dispose()
							{
							}
							
							@Override
							public Object[] getElements(final Object inputElement)
							{
								return (Object[]) inputElement;
							}
						});

				dropdownTableViewer.setLabelProvider(new ILabelProvider()
						{
							@Override
							public void removeListener(final ILabelProviderListener listener)
							{
							}
							
							@Override
							public boolean isLabelProperty(final Object element, final String property)
							{
								return false;
							}
							
							@Override
							public void dispose()
							{
							}
							
							@Override
							public void addListener(final ILabelProviderListener listener)
							{
							}

							@Override
							public String getText(final Object element)
							{
								return element.toString();
							}

							@Override
							public Image getImage(final Object element)
							{
								return null;
							}
						});

				dropdownTable.addKeyListener(new KeyAdapter()
				{
					@Override
					public void keyPressed(final KeyEvent event)
					{
						if (event.keyCode == SWT.ARROW_DOWN)
						{
							final int selected = NatComboWidget.this.dropdownTable.getSelectionIndex();
							if (selected < 0)
							{
								// no selection before, select the first entry
								if (!NatComboWidget.this.useCheckbox)
								{
									select(0);
								} else
								{
									dropdownTable.select(0);
								}
								event.doit = false;
							}
						}
					}
				});
				
				dropdownTable.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(final SelectionEvent e)
					{
						final boolean selected = e.detail != SWT.CHECK;
						final boolean isCtrlPressed = (e.stateMask & SWT.MODIFIER_MASK) == SWT.CTRL;
						final TableItem chosenItem = (TableItem) e.item;

						// Given the ability to filter we need to find the item's
						// table index which may not match the index in the itemList
						final int itemTableIndex = NatComboWidget.this.dropdownTable.indexOf(chosenItem);
						
						// This case handles check actions
						if (!selected)
						{
							if (!chosenItem.getChecked())
							{
								NatComboWidget.this.selectionStateMap.put(chosenItem.getText(), Boolean.FALSE);
							} else
							{
								NatComboWidget.this.selectionStateMap.put(chosenItem.getText(), Boolean.TRUE);
							}
						} else if (!NatComboWidget.this.useCheckbox)
						{
							if (NatComboWidget.this.multiselect && isCtrlPressed)
							{
								final boolean isSelected = NatComboWidget.this.dropdownTable.isSelected(itemTableIndex);
								NatComboWidget.this.selectionStateMap.put(chosenItem.getText(), isSelected);
								if (NatComboWidget.this.useCheckbox)
								{
									chosenItem.setChecked(isSelected);
								}
							} else
							{
								// A single item was selected. Clear all previous state
								for (final String item : NatComboWidget.this.itemList)
								{
									NatComboWidget.this.selectionStateMap.put(item, Boolean.FALSE);
								}
						
								// Set the state for the selected item
								NatComboWidget.this.selectionStateMap.put(chosenItem.getText(), Boolean.TRUE);
							}
						}

						updateTextControl(false);
					}
				});

				dropdownTable.addKeyListener(new KeyAdapter()
				{
					@Override
					public void keyPressed(final KeyEvent event)
					{
						if ((event.keyCode == SWT.CR) || (event.keyCode == SWT.KEYPAD_CR))
						{
							updateTextControl(true);
						} else if (event.keyCode == SWT.F2 && NatComboWidget.this.freeEdit)
						{
							NatComboWidget.this.text.forceFocus();
							hideDropdownControl();
						}
					}
				});
				
				dropdownTable.addFocusListener(new FocusListenerWrapper());
				
				final FormLayout layout = new FormLayout();
				layout.spacing = 0;
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				parent.setLayout(layout);
				
				final FormData dropDownLayoutData = new FormData();
				dropDownLayoutData.left = new FormAttachment(0);
				dropDownLayoutData.right = new FormAttachment(100);
				dropDownLayoutData.bottom = new FormAttachment(100);
				
				if (showDropdownFilter)
				{
					filterBox = new Text(parent, SWT.BORDER);
					filterBox.setFont(text.getFont());
					filterBox.setEnabled(true);
					filterBox.setEditable(true);
					filterBox.addFocusListener(new FocusListenerWrapper());
					filterBox.addKeyListener(new KeyAdapter()
					{
						@Override
						public void keyReleased(final KeyEvent e)
						{
							if (null != NatComboWidget.this.dropdownTableViewer
									&& !NatComboWidget.this.dropdownTable.isDisposed())
							{
								NatComboWidget.this.dropdownTableViewer.refresh();
								calculateBounds();
								setDropdownSelection(getTextAsArray());
							}
						}
					});

					FormData data = new FormData();
					data.top = new FormAttachment(0);
					data.left = new FormAttachment(0);
					data.right = new FormAttachment(100);
					filterBox.setLayoutData(data);

					data = new FormData();
					if (showDropdownFilter)
					{
						dropDownLayoutData.top = new FormAttachment(filterBox, 0, SWT.BOTTOM);
					} else
					{
						dropDownLayoutData.top = new FormAttachment(parent, 0, SWT.TOP);
					}

					dropdownTable.setLayoutData(dropDownLayoutData);
					
					final ViewerFilter viewerFilter = new ViewerFilter()
					{
						@Override
						public boolean select(final Viewer viewer, final Object parentElement, final Object element)
						{
							if (null != element && element instanceof String)
							{
								return ((String) element).toLowerCase()
										.contains(NatComboWidget.this.filterBox.getText().toLowerCase());
							}
							return false;
						}
					};
					dropdownTableViewer.addFilter(viewerFilter);
				} else
				{
					dropDownLayoutData.top = new FormAttachment(0);
					dropdownTable.setLayoutData(dropDownLayoutData);
				}

				if (itemList != null)
				{
					setItems(itemList.toArray(new String[] {}));
				}
				// apply the listeners that were registered before the creation of the
				// dropdown control
				applyDropdownListener();
				setDropdownSelection(getTextAsArray());
				return dropdownTable;
			}

			@Override protected Control createDialogArea(final Composite parent)
			{
				return parent;
			}
			
			@Override
			protected Control getFocusControl()
			{
				return dropdownTable;
			}
		};
		this.dropdownShell.create();
	}

	/**
	 * This method will be called if an item of the dropdown control is selected via
	 * mouse click or pressing enter. It will populate the text control with the
	 * information gathered out of the selection in the dropdown control and hide
	 * the dropdown if necessary.
	 *
	 * @param hideDropdown <code>true</code> if the dropdown should be hidden after
	 *                     updating the text control
	 */
	protected void updateTextControl(final boolean hideDropdown)
	{
		this.text.setText(getTransformedTextForSelection());
		if (hideDropdown)
		{
			hideDropdownControl();
		}
	}

	/**
	 * Shows the dropdown of this NatComboWidget. Will always calculate the size of
	 * the dropdown regarding the current size of the Text control.
	 */
	public void showDropdownControl()
	{
		showDropdownControl(false);
	}

	/**
	 * Shows the dropdown of this NatComboWidget. Will always calculate the size of
	 * the dropdown regarding the current size of the Text control.
	 *
	 * @param focusOnText <code>true</code> if the focus should be set to the text
	 *                    control instead of the dropdown after opening the
	 *                    dropdown.
	 */
	public void showDropdownControl(final boolean focusOnText)
	{
		if (this.dropdownShell == null || this.dropdownShell.getShell() == null || this.dropdownShell.getShell().isDisposed())
		{
			createDropdownControl(this.style);
		}
		calculateBounds();
		this.dropdownShell.open();
		if (focusOnText)
		{
			this.text.forceFocus();
			this.text.setSelection(this.text.getText().length());
		}
	}

	/**
	 * @return The {@link Table} control that is used in the dropdown. Will be
	 *         created if it does not exist yet.
	 * @since 1.5
	 */
	protected Optional<Table> getDropdownTable()
	{
//		if (this.dropdownTable == null)
//		{
//			createDropdownControl(this.style);
//		}
		return Optional.ofNullable(this.dropdownTable);
	}

	/**
	 * Hide the dropdown of this NatComboWidget.
	 */
	public void hideDropdownControl()
	{
		if (this.dropdownShell != null && dropdownShell.getShell() != null && !this.dropdownShell.getShell().isDisposed())
		{
			this.dropdownShell.close();
		}
	}

	/**
	 * Calculates the number of items that should be showed in the dropdown at once.
	 * It is needed to calculate the height of the dropdown. If maxVisibleItems is
	 * configured -1, this method always returns the number of items in the list.
	 * Otherwise if will return the configured maximum number of items to be visible
	 * at once or less if there are less than the configured maximum.
	 *
	 * @return the number of items that should be showed in the dropdown at once.
	 */
	protected int getVisibleItemCount()
	{
		int itemCount = getDropdownTable().map(Table::getItemCount).orElse(-1);
		if (itemCount > 0)
		{
			// if maxVisibleItems == -1 show all items at once
			// otherwise use the minimum for item count or max visible item
			// configuration
			int visibleItemCount = itemCount;
			if (this.maxVisibleItems > 0)
			{
				visibleItemCount = Math.min(itemCount, this.maxVisibleItems);
			}
			itemCount = visibleItemCount;
		}
		return itemCount;
	}

	/**
	 * Calculates the size and location of the Shell that represents the dropdown
	 * control of this NatComboWidget. Size and location will be calculated
	 * dependent the position and size of the corresponding Text control and the
	 * information showed in the dropdown.
	 */
	protected void calculateBounds()
	{
		if (this.dropdownShell != null && dropdownShell.getShell() != null && !this.dropdownShell.getShell().isDisposed())
		{
			final Point size = getSize();
			// calculate the height by multiplying the number of visible items
			// with the item height of items in the list and adding 2*grid line
			// width to work around a calculation error regarding the descent of
			// the font metrics for the last shown item
			// Note: if there are no items to show in the combo, calculate with
			// the item count of 3 so an empty combo will open
			final int listHeight = (getVisibleItemCount() > 0 ? getVisibleItemCount() : 3)
					* this.dropdownTable.getItemHeight() + this.dropdownTable.getGridLineWidth() * 2;

			// since introduced the TableColumn for real full row selection, we
			// call pack() to perform autoresize to ensure the width shows the
			// whole content
			this.dropdownTable.getColumn(0).pack();
			final int listWidth = Math.max(this.dropdownTable.computeSize(SWT.DEFAULT, listHeight, true).x, size.x);

			final Point textPosition = this.text.toDisplay(this.text.getLocation());

			// by default the dropdown shell will be created below the cell in
			// the table
			int dropdownShellStartingY = textPosition.y + this.text.getBounds().height;

			final int textBottomY = textPosition.y + this.text.getBounds().height + listHeight;
			// if the bottom of the drowdown is below the display, render it
			// above the cell
			if (textBottomY > Display.getCurrent().getBounds().height)
			{
				dropdownShellStartingY = textPosition.y - listHeight;
			}

			final Rectangle parentBounds = getParent().getBounds();
			final Point parentStart = getParent().toDisplay(parentBounds.x, parentBounds.y);
			int parentBottomY = parentStart.y + parentBounds.height - parentBounds.y;
			if (getParent().getHorizontalBar() != null && getParent().getHorizontalBar().isVisible())
			{
				parentBottomY -= getParent().getHorizontalBar().getSize().y;
			}
			if (dropdownShellStartingY > parentBottomY)
			{
				dropdownShellStartingY = parentBottomY;
			}

			final int filterTextBoxHeight = this.showDropdownFilter
					? this.filterBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
					: 0;
			final Rectangle shellBounds = new Rectangle(textPosition.x, dropdownShellStartingY,
					listWidth + (this.dropdownTable.getGridLineWidth() * 2), listHeight + filterTextBoxHeight);

			this.dropdownShell.getShell().setBounds(shellBounds);

			calculateColumnWidth();
		}
	}

	/**
	 * Calculates and applies the column width to ensure that the column has the
	 * same width as the table itself, so selection is possible for the whole row.
	 */
	protected void calculateColumnWidth()
	{
		int width = this.dropdownTable.getBounds().width;
		// only reduce if a scrollbar is available and visible
		if (this.dropdownTable.getVerticalBar() != null && this.dropdownTable.getItemCount() > this.maxVisibleItems
				&& this.maxVisibleItems > 0)
		{
			width -= this.dropdownTable.getVerticalBar().getSize().x;
		}
		this.dropdownTable.getColumn(0).setWidth(width);
	}

	/**
	 * Returns the zero-relative index of the item which is currently selected in
	 * the receiver, or -1 if no item is selected.
	 * <p>
	 * Note that this only returns useful results if this NatComboWidget supports
	 * single selection or only one item is selected.
	 *
	 * @return the index of the selected item or -1
	 */
	public int getSelectionIndex()
	{
		if (this.selectionStateMap != null)
		{
			for (final String item : this.selectionStateMap.keySet())
			{
				if (this.selectionStateMap.get(item))
				{
					return this.itemList.indexOf(item);
				}
			}
		} else if (!this.text.isDisposed())
		{
			return this.itemList.indexOf(this.text.getText());
		}
		return -1;
	}

	/**
	 * Returns the zero-relative indices of the items which are currently selected
	 * in the receiver. The order of the indices is unspecified. The array is empty
	 * if no items are selected.
	 * <p>
	 * Note: This is not the actual structure used by the receiver to maintain its
	 * selection, so modifying the array will not affect the receiver.
	 * </p>
	 *
	 * @return the array of indices of the selected items
	 */
	public int[] getSelectionIndices()
	{
		if (this.selectionStateMap != null)
		{
			final List<Integer> selectedIndices = new ArrayList<Integer>();
			for (final String item : this.selectionStateMap.keySet())
			{
				if (this.selectionStateMap.get(item))
				{
					selectedIndices.add(this.itemList.indexOf(item));
				}
			}
			final int[] indices = new int[selectedIndices.size()];
			for (int i = 0; i < selectedIndices.size(); i++)
			{
				indices[i] = selectedIndices.get(i);
			}
			return indices;
		} else
		{
			final String[] selectedItems = getTextAsArray();
			final int[] result = new int[selectedItems.length];
			for (int i = 0; i < selectedItems.length; i++)
			{
				result[i] = this.itemList.indexOf(selectedItems[i]);
			}
			return result;
		}
	}

	/**
	 * Returns the number of selected items contained in the receiver.
	 *
	 * @return the number of selected items
	 */
	public int getSelectionCount()
	{
		if (this.selectionStateMap != null)
		{
			final List<Integer> selectedIndices = new ArrayList<Integer>();
			for (final String item : this.selectionStateMap.keySet())
			{
				if (this.selectionStateMap.get(item))
				{
					selectedIndices.add(this.itemList.indexOf(item));
				}
			}
			return selectedIndices.size();
		} else
		{
			return getTextAsArray().length;
		}
	}

	/**
	 * Returns an array of <code>String</code>s that are currently selected in the
	 * receiver. The order of the items is unspecified. An empty array indicates
	 * that no items are selected.
	 * <p>
	 * Note: This is not the actual structure used by the receiver to maintain its
	 * selection, so modifying the array will not affect the receiver.
	 * </p>
	 *
	 * @return an array representing the selection
	 */
	public String[] getSelection()
	{
//		String[] result = getTransformedSelection();
//		if (result == null || (result.length == 0 && !this.text.isDisposed() && this.text.getText().length() > 0))
//		{
//			result = getTextAsArray();
//		}
//		return result;
		return getTextAsArray();
	}

	/**
	 * Selects the items at the given zero-relative indices in the receiver. The
	 * current selection is cleared before the new items are selected.
	 * <p>
	 * Indices that are out of range and duplicate indices are ignored. If the
	 * receiver is single-select and multiple indices are specified, then all
	 * indices are ignored.
	 * <p>
	 * The text control of this NatComboWidget will also be updated with the new
	 * selected values.
	 *
	 * @param items the items to select
	 */
	public void setSelection(final String[] items)
	{
		String textValue = ""; //$NON-NLS-1$
		if (items != null)
		{
			if (getDropdownTable().map(t -> !t.isDisposed()).isPresent())
			{
				setDropdownSelection(items);
				if (this.freeEdit && getDropdownTable().get().getSelectionCount() == 0)
				{
					textValue = getTransformedText(items);
				} else
				{
					textValue = getTransformedTextForSelection();
				}
			} else
			{
				textValue = getTransformedText(items);
			}
		}
		this.text.setText(textValue);

		if (this.multiselect)
		{
			this.text.setSelection(textValue.length() - this.multiselectTextSuffix.length());
		}
	}

	/**
	 * Selects the item at the given zero-relative index in the receiver's list. If
	 * the item at the index was already selected, it remains selected. Indices that
	 * are out of range are ignored.
	 *
	 * @param index the index of the item to select
	 */
	public void select(final int index)
	{
		if (getDropdownTable().map(t -> !t.isDisposed()).isPresent())
		{
			getDropdownTable().get().select(index);
			for (int i = 0; i < this.itemList.size(); i++)
			{
				this.selectionStateMap.put(this.itemList.get(i), i == index);
			}
			this.text.setText(getTransformedTextForSelection());
		} else if (index >= 0)
		{
			this.text.setText(this.itemList.get(index));
		}
	}

	/**
	 * Selects the items at the given zero-relative indices in the receiver. The
	 * current selection is not cleared before the new items are selected.
	 * <p>
	 * If the item at a given index is not selected, it is selected. If the item at
	 * a given index was already selected, it remains selected. Indices that are out
	 * of range and duplicate indices are ignored. If the receiver is single-select
	 * and multiple indices are specified, then all indices are ignored.
	 *
	 * @param indices the array of indices for the items to select
	 */
	public void select(final int[] indices)
	{
		if (getDropdownTable().map(t -> !t.isDisposed()).isPresent())
		{
			getDropdownTable().get().select(indices);
			final List<Integer> indicesList = ArrayUtil.asIntegerList(indices);
			for (int i = 0; i < this.itemList.size(); i++)
			{
				this.selectionStateMap.put(this.itemList.get(i), indicesList.contains(i));
			}
			this.text.setText(getTransformedTextForSelection());
		} else
		{
			final String[] selectedItems = new String[indices.length];
			for (int i = 0; i < indices.length; i++)
			{
				if (indices[i] >= 0)
				{
					selectedItems[i] = this.itemList.get(indices[i]);
				}
			}
			this.text.setText(getTransformedText(selectedItems));
		}
	}

	/**
	 * @since 1.5
	 */
	protected void applyDropdownListener()
	{
		for (final KeyListener l : this.keyListener)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addKeyListener(l);
			}
		}
		for (final TraverseListener l : this.traverseListener)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addTraverseListener(l);
			}
		}
		for (final MouseListener l : this.mouseListener)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addMouseListener(l);
			}
		}
		for (final SelectionListener l : this.selectionListener)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addSelectionListener(l);
			}
		}
		for (final ShellListener l : this.shellListener)
		{
			if (this.dropdownShell != null && dropdownShell.getShell() != null && !this.dropdownShell.getShell().isDisposed())
			{
				this.dropdownShell.getShell().addShellListener(l);
			}
		}
	}

	@Override
	public void addKeyListener(final KeyListener listener)
	{
		if (listener != null)
		{
			if (this.text != null && !this.text.isDisposed())
			{
				this.text.addKeyListener(listener);
			}
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addKeyListener(listener);
			}
			this.keyListener.add(listener);
		}
	}

	@Override
	public void removeKeyListener(final KeyListener listener)
	{
		if (this.text != null && !this.text.isDisposed())
		{
			this.text.removeKeyListener(listener);
		}
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
		{
			this.dropdownTable.removeKeyListener(listener);
		}
		this.keyListener.remove(listener);
	}

	@Override
	public void addTraverseListener(final TraverseListener listener)
	{
		if (listener != null)
		{
			if (this.text != null && !this.text.isDisposed())
			{
				this.text.addTraverseListener(listener);
			}
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addTraverseListener(listener);
			}
			this.traverseListener.add(listener);
		}
	}

	@Override
	public void removeTraverseListener(final TraverseListener listener)
	{
		if (this.text != null && !this.text.isDisposed())
		{
			this.text.removeTraverseListener(listener);
		}
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
		{
			this.dropdownTable.removeTraverseListener(listener);
		}
		this.traverseListener.remove(listener);
	}

	@Override
	public void addMouseListener(final MouseListener listener)
	{
		// only add the mouse listener to the dropdown, as clicking in the text
		// control should not trigger anything else than it is handled by the
		// text control itself.
		if (listener != null)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addMouseListener(listener);
			}
			this.mouseListener.add(listener);
		}
	}

	@Override
	public void removeMouseListener(final MouseListener listener)
	{
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
		{
			this.dropdownTable.removeMouseListener(listener);
		}
		this.mouseListener.remove(listener);
	}

	@Override
	public void notifyListeners(final int eventType, final Event event)
	{
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
		{
			this.dropdownTable.notifyListeners(eventType, event);
		}
	}

	public void addSelectionListener(final SelectionListener listener)
	{
		if (listener != null)
		{
			if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			{
				this.dropdownTable.addSelectionListener(listener);
			}
			this.selectionListener.add(listener);
		}
	}

	public void removeSelectionListener(final SelectionListener listener)
	{
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
		{
			this.dropdownTable.removeSelectionListener(listener);
		}
		this.selectionListener.remove(listener);
	}

	public void addShellListener(final ShellListener listener)
	{
		if (listener != null)
		{
			if (this.dropdownShell != null && dropdownShell.getShell() != null && !this.dropdownShell.getShell().isDisposed())
			{
				this.dropdownShell.getShell().addShellListener(listener);
			}
			this.shellListener.add(listener);
		}
	}

	public void removeShellListener(final ShellListener listener)
	{
		if (this.dropdownShell != null && dropdownShell.getShell() != null && !this.dropdownShell.getShell().isDisposed())
		{
			this.dropdownShell.getShell().removeShellListener(listener);
		}
		this.shellListener.remove(listener);
	}

	public void addTextControlListener(final ControlListener listener)
	{
		if (listener != null && this.text != null && !this.text.isDisposed())
		{
			this.text.addControlListener(listener);
		}
	}

	public void removeTextControlListener(final ControlListener listener)
	{
		if (this.text != null && !this.text.isDisposed())
		{
			this.text.removeControlListener(listener);
		}
	}

	@Override
	public boolean isFocusControl()
	{
		return this.hasFocus;
	}

	@Override
	public boolean forceFocus()
	{
		return this.text.forceFocus();
	}

	@Override
	public void addFocusListener(final FocusListener listener)
	{
		if (listener != null)
		{
			this.focusListener.add(listener);
		}
	}

	@Override
	public void removeFocusListener(final FocusListener listener)
	{
		// The FocusListenerWrapper is executing the focusLost event
		// in a separate thread with 100ms delay to ensure that the NatComboe
		// lost focus. This is necessary because the NatComboWidget is a combination
		// of a text field and a table as dropdown which do not share the
		// same focus by default.
		this.focusListener.remove(listener);
	}

	/**
	 * Transforms the selection in the Table control dropdown into a String[]. Doing
	 * this is necessary to provide a SWT List like interface regarding selections
	 * for the NatComboWidget.
	 *
	 * @return Array containing all selected TableItem text attributes
	 */
	protected String[] getTransformedSelection()
	{
		final List<String> selectedItems = new ArrayList<String>();
		for (final String item : this.selectionStateMap.keySet())
		{
			final Boolean isSelected = this.selectionStateMap.get(item);
			if (isSelected != null && isSelected)
			{
				selectedItems.add(item);
			}
		}
		return selectedItems.toArray(new String[selectedItems.size()]);
	}

	/**
	 * Transforms the given String array whose contents represents selected items to
	 * a selection that can be handled by the underlying Table control in the
	 * dropdown.
	 *
	 * @param selection The Strings that represent the selected items
	 */
	protected void setDropdownSelection(final String[] selection)
	{
		if (getDropdownTable().map(t -> !t.isDisposed()).isEmpty())
			return;
		
		final java.util.List<String> selectionList = Arrays.asList(selection);
		final java.util.List<TableItem> selectedItems = new ArrayList<TableItem>();
		for (final TableItem item : getDropdownTable().get().getItems())
		{
			if (selectionList.contains(EditConstants.SELECT_ALL_ITEMS_VALUE) || selectionList.contains(item.getText()))
			{
				this.selectionStateMap.put(item.getText(), Boolean.TRUE);
				if (this.useCheckbox)
				{
					item.setChecked(true);
				} else
				{
					selectedItems.add(item);
				}
			} else
			{
				this.selectionStateMap.put(item.getText(), Boolean.FALSE);
			}
		}
		getDropdownTable().get().setSelection(selectedItems.toArray(new TableItem[] {}));
	}

	/**
	 * Will transform the text for the Text control of this NatComboWidget to an
	 * array of Strings. This is necessary for the multiselect feature.
	 *
	 * <p>
	 * Note that by default the multiselect String is specified to show with
	 * enclosing [] brackets and values separated by ", ". If you need to change
	 * this you need to set the corresponding values in this NatComboWidget.
	 *
	 * @return The text for the Text control of this NatComboWidget converted to an
	 *         array of Strings.
	 */
	protected String[] getTextAsArray()
	{
		if (!this.text.isDisposed())
		{
			String transform = this.text.getText();
			if (transform.length() > 0)
			{
				if (this.multiselect)
				{
					// for multiselect the String is defined by default in
					// format [a, b, c]
					// the prefix and suffix for multiselect String
					// representation need to be removed
					// in free edit mode we need to check if the format is used
					int prefixLength = this.multiselectTextPrefix.length();
					int suffixLength = this.multiselectTextSuffix.length();
					if (this.freeEdit)
					{
						if (!transform.startsWith(this.multiselectTextPrefix))
						{
							prefixLength = 0;
						}
						if (!transform.endsWith(this.multiselectTextSuffix))
						{
							suffixLength = 0;
						}
					}
					transform = transform.substring(prefixLength, transform.length() - suffixLength);

					// if the transform value length is still > 0, try to split
					if (transform.length() > 0)
					{
						return transform.split(this.multiselectValueSeparator);
					}
				}
				return new String[] { transform };
			}
		}
		return new String[] {};
	}

	/**
	 * Transforms the selection of the dropdown to a text representation that can be
	 * added to the text control of this combo.
	 *
	 * <p>
	 * Note that by default the multiselect String is specified to show with
	 * enclosing [] brackets and values separated by ", ". If you need to change
	 * this you need to set the corresponding values in this NatComboWidget.
	 *
	 * @return String representation for the selection within the combo.
	 */
	protected String getTransformedTextForSelection()
	{
		String result = ""; //$NON-NLS-1$
		final String[] selection = getTransformedSelection();
		if (selection != null)
		{
			result = getTransformedText(selection);
		}
		return result;
	}

	/**
	 * Transforms the given array of Strings to a text representation that can be
	 * added to the text control of this combo.
	 * <p>
	 * If this NatComboWidget is only configured to support single selection, than
	 * only the first value in the array will be processed. Otherwise the result
	 * will be processed by concatenating the values.
	 * <p>
	 * Note that by default the multiselect String is specified to show with
	 * enclosing [] brackets and values separated by ", ". If you need to change
	 * this you need to set the corresponding values in this NatComboWidget.
	 *
	 * @param values The values to build the text representation from.
	 * @return String representation for the selection within the combo.
	 */
	protected String getTransformedText(final String[] values)
	{
		String result = ""; //$NON-NLS-1$
		if (this.multiselect)
		{
			for (int i = 0; i < values.length; i++)
			{
				final String selection = values[i];
				result += selection;
				if ((i + 1) < values.length)
				{
					result += this.multiselectValueSeparator;
				}
			}
			result = this.multiselectTextPrefix + result + this.multiselectTextSuffix;
		} else if (values.length > 0)
		{
			result = values[0];
		}
		return result;
	}

	/**
	 * @param multiselectValueSeparator String that should be used to separate
	 *                                  values in the String representation showed
	 *                                  in the text control if multiselect is
	 *                                  supported. <code>null</code> to use the
	 *                                  default value separator.
	 * @see NatComboWidget#DEFAULT_MULTI_SELECT_VALUE_SEPARATOR
	 */
	public void setMultiselectValueSeparator(final String multiselectValueSeparator)
	{
		if (multiselectValueSeparator == null)
		{
			this.multiselectValueSeparator = DEFAULT_MULTI_SELECT_VALUE_SEPARATOR;
		} else
		{
			this.multiselectValueSeparator = multiselectValueSeparator;
		}
	}

	/**
	 * Set the prefix and suffix that will parenthesize the text that is created out
	 * of the selected values if this NatComboWidget supports multiselection.
	 *
	 * @param multiselectTextPrefix String that should be used to prefix the
	 *                              generated String representation showed in the
	 *                              text control if multiselect is supported.
	 *                              <code>null</code> to use the default prefix.
	 * @param multiselectTextSuffix String that should be used to suffix the
	 *                              generated String representation showed in the
	 *                              text control if multiselect is supported.
	 *                              <code>null</code> to use the default suffix.
	 * @see NatComboWidget#DEFAULT_MULTI_SELECT_PREFIX
	 * @see NatComboWidget#DEFAULT_MULTI_SELECT_SUFFIX
	 */
	public void setMultiselectTextBracket(final String multiselectTextPrefix, final String multiselectTextSuffix)
	{
		if (multiselectTextPrefix == null)
		{
			this.multiselectTextPrefix = DEFAULT_MULTI_SELECT_PREFIX;
		} else
		{
			this.multiselectTextPrefix = multiselectTextPrefix;
		}

		if (multiselectTextSuffix == null)
		{
			this.multiselectTextSuffix = DEFAULT_MULTI_SELECT_SUFFIX;
		} else
		{
			this.multiselectTextSuffix = multiselectTextSuffix;
		}
	}

	@Override
	public void setFont(final Font font)
	{
		this.text.setFont(font);
		if (this.dropdownTable != null && !this.dropdownTable.isDisposed())
			this.dropdownTable.setFont(this.text.getFont());
	}

	@Override
	public Font getFont()
	{
		return this.text.getFont();
	}
	
	public Text getTextControl()
	{
		return text;
	}
	
	public Canvas getIconCanvas()
	{
		return iconCanvas;
	}

	/**
	 * FocusListener that is used to ensure that the Text control and the dropdown
	 * table control are sharing the same focus. If either of both controls looses
	 * focus, the local focus flag is set to false and a delayed background thread
	 * for focus lost is started. If the other control gains focus, the local focus
	 * flag is set to true which skips the execution of the delayed background
	 * thread. This means the NatComboWidget hasn't lost focus.
	 *
	 * @since 1.4
	 */
	public class FocusListenerWrapper implements FocusListener
	{
		@Override
		public void focusLost(final FocusEvent e)
		{
			NatComboWidget.this.hasFocus = false;
			Display.getCurrent().timerExec(100, new Runnable()
			{
				@Override
				public void run()
				{
					if (!NatComboWidget.this.hasFocus)
					{
						final List<FocusListener> copy = new ArrayList<FocusListener>(
								NatComboWidget.this.focusListener);
						for (final FocusListener f : copy)
						{
							f.focusLost(e);
						}
					}
				}
			});
		}

		@Override
		public void focusGained(final FocusEvent e)
		{
			NatComboWidget.this.hasFocus = true;
			for (final FocusListener f : NatComboWidget.this.focusListener)
			{
				f.focusGained(e);
			}
		}
	}
}
