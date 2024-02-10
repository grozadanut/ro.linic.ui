package ro.linic.ui.legacy.widgets;

import static ro.linic.ui.legacy.session.UIUtils.localToDisplayLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.linic.ui.legacy.session.UIUtils;

public class ExportButton extends Composite
{
	private Button defaultExport;
	private Button arrow;
	
	private int arrowLocation;
	private String arrowImg;
	
	/**
	 * The Shell containing the dropdown of this NatComboWidget
	 */
	private PopupDialog dropdownShell;
	
	/**
	 * Export choices. List should always have at least one item.
	 * The first item is the default export.
	 */
	final private ImmutableList<String> choices;
	private Consumer<Integer> exportCallback;
	
	/**
	 * @param parent
	 * @param arrowLocation one of the styles: SWT.RIGHT, SWT.DOWN, SWT.LEFT, SWT.UP. Default is SWT.RIGHT
	 * @param choices should always have at least one item. The first item is the default export.
	 */
	public ExportButton(final Composite parent, final int arrowLocation, final ImmutableList<String> choices)
	{
		this(parent, arrowLocation, choices, "down_0");
	}
	
	/**
	 * @param parent
	 * @param arrowLocation one of the styles: SWT.RIGHT, SWT.DOWN, SWT.LEFT, SWT.UP. Default is SWT.RIGHT
	 * @param choices should always have at least one item. The first item is the default export.
	 * @param arrowImg name retrieved by org.eclipse.nebula.widgets.nattable.util.GUIHelper.getImage for the arrow button
	 */
	public ExportButton(final Composite parent, final int arrowLocation, final ImmutableList<String> choices,
			final String arrowImg)
	{
		super(parent, SWT.NONE);
		this.arrowLocation = arrowLocation;
		this.arrowImg = arrowImg;
		
		if (choices == null || choices.isEmpty())
			this.choices = ImmutableList.of("Export");
		else
			this.choices = choices;
		
		createComposite();
		addListeners();
	}
	
	private void createComposite()
	{
		final GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = (arrowLocation == SWT.DOWN || arrowLocation == SWT.UP) ? 1 : 2;
		setLayout(layout);
		
		switch (arrowLocation)
		{
		case SWT.UP:
			arrow = new Button(this, SWT.PUSH);
			defaultExport = new Button(this, SWT.PUSH);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(arrow);
			break;
		case SWT.LEFT:
			arrow = new Button(this, SWT.PUSH);
			defaultExport = new Button(this, SWT.PUSH);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(arrow);
			break;

		case SWT.RIGHT:
			defaultExport = new Button(this, SWT.PUSH);
			arrow = new Button(this, SWT.PUSH);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(arrow);
			break;
		case SWT.DOWN:
		default:
			defaultExport = new Button(this, SWT.PUSH);
			arrow = new Button(this, SWT.PUSH);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(arrow);
			break;
		}
		
		arrow.setImage(GUIHelper.getImage(arrowImg));
		defaultExport.setText(choices.get(0));
		UIUtils.setFont(defaultExport);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(defaultExport);
		
		if (choices.size() == 1)
		{
			final GridData gd = new GridData();
			gd.exclude = true;
			arrow.setLayoutData(gd);
		}
	}
	
	private void addListeners()
	{
		arrow.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				dropdownShell = new ExportOptionsPopup(arrow.getShell(), localToDisplayLocation(ExportButton.this, 0, ExportButton.this.getSize().y));
				dropdownShell.open();
			}
		});
		
		defaultExport.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (exportCallback != null)
					exportCallback.accept(0);
				
				if (dropdownShell != null)
					dropdownShell.close();
			}
		});
	}
	
	@Override
	public void setFont(final Font font)
	{
		super.setFont(font);
		defaultExport.setFont(font);
	}
	
	@Override
	public void setBackground(final Color color)
	{
		super.setBackground(color);
		defaultExport.setBackground(color);
		arrow.setBackground(color);
	}
	
	@Override
	public void setForeground(final Color color)
	{
		super.setForeground(color);
		defaultExport.setForeground(color);
		arrow.setForeground(color);
	}
	
	public void addExportCallback(final Consumer<Integer> exportCallback)
	{
		this.exportCallback = exportCallback;
	}
	
	private class ExportOptionsPopup extends PopupDialog
	{
		private Point initialLocation;
		private List<Control> exclusions = new ArrayList<>();
		
		public ExportOptionsPopup(final Shell shell, final Point initialLocation)
		{
			super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
			this.initialLocation = initialLocation;
		}
		
		@Override
		protected Control createDialogArea(final Composite parent)
		{
			final Composite container = (Composite) super.createDialogArea(parent);
			container.setLayout(new GridLayout());
			
			for (int i = 1; i < choices.size(); i++)
			{
				final String choice = choices.get(i);
				
				final Button export = new Button(container, SWT.PUSH);
				export.setText(choice);
				export.setFont(defaultExport.getFont());
				export.setBackground(defaultExport.getBackground());
				export.setForeground(defaultExport.getForeground());
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(export);
				exclusions.add(export);
				
				export.addSelectionListener(new SelectionAdapter()
				{
					@Override public void widgetSelected(final SelectionEvent e)
					{
						if (exportCallback != null)
							exportCallback.accept(choices.indexOf(export.getText()));
						close();
					}
				});
			}
			
			return container;
		}
		
		@Override
		protected Point getDefaultLocation(final Point initialSize)
		{
			if (this.initialLocation != null)
				return initialLocation;
			
			return super.getDefaultLocation(initialSize);
		}
		
		@Override
		protected List<Control> getBackgroundColorExclusions()
		{
			final List<Control> list = super.getBackgroundColorExclusions();
			list.addAll(exclusions);
			return list;
		}
		
		@Override
		protected List<Control> getForegroundColorExclusions()
		{
			final List<Control> list = super.getForegroundColorExclusions();
			list.addAll(exclusions);
			return list;
		}
		
		@Override
		protected Color getBackground()
		{
			return ExportButton.this.getBackground();
		}
	}
}
