package ro.linic.ui.legacy.widgets;

import static ro.colibri.util.PresentationUtils.EMPTY_STRING;

import java.io.IOException;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import ro.linic.ui.legacy.session.Icons;

public class ImagePicker
{
	private Button button;
	private byte[] selectedImage;
	
	private Consumer<String> fileChangedListener;
	
	public ImagePicker(final Composite parent, final int style)
	{
		button = new Button(parent, style);
		setImage(null);
		addListeners();
	}
	
	private void addListeners()
	{
		button.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final String selectedFile = new FileDialog(button.getShell(), SWT.OPEN).open();
				
				try
				{
					setImage(selectedFile == null ? null :
						Icons.imgPathToBytes(selectedFile));
				}
				catch (final IOException ex)
				{
					ex.printStackTrace();
				}

				if (fileChangedListener != null)
					fileChangedListener.accept(selectedFile);
			}
		});
	}

	public ImagePicker setImage(final byte[] image)
	{
		this.selectedImage = image;
		try
		{
			button.setImage(image == null ? null : Icons.imageFromBytes(image));
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		button.setText(image == null ? "Fara imagine" : EMPTY_STRING);
		return this;
	}
	
	public byte[] getSelectedImage()
	{
		return selectedImage;
	}
	
	public ImagePicker setLayoutData(final Object layoutData)
	{
		button.setLayoutData(layoutData);
		return this;
	}
	
	public void setFileChangedListener(final Consumer<String> fileChangedListener)
	{
		this.fileChangedListener = fileChangedListener;
	}
	
	public Button widget()
	{
		return button;
	}
}