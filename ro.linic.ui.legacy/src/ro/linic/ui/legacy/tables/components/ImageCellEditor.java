package ro.linic.ui.legacy.tables.components;

import java.io.IOException;

import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.edit.gui.AbstractDialogCellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import ro.linic.ui.legacy.session.Icons;

public class ImageCellEditor extends AbstractDialogCellEditor
{
	/**
	 * The selection result of the {@link FileDialog}. Needed to update the data
	 * model after closing the dialog.
	 */
	private byte[] selectedImage;
	/**
	 * Flag to determine whether the dialog was closed or if it is still open.
	 */
	private boolean closed = false;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #open()
	 */
	@Override
	public int open()
	{
		final String selectedFile = getDialogInstance().open();
		if (selectedFile == null)
		{
			this.selectedImage = null;
			commit(MoveDirectionEnum.NONE);
			this.closed = true;
			return Window.OK;
		}
		else
		{
			try
			{
				this.selectedImage = Icons.imgPathToBytes(selectedFile);
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				this.selectedImage = null;
			}
			commit(MoveDirectionEnum.NONE);
			this.closed = true;
			return Window.OK;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #createDialogInstance()
	 */
	@Override
	public FileDialog createDialogInstance()
	{
		this.closed = false;
		return new FileDialog(this.parent.getShell(), SWT.OPEN);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #getDialogInstance()
	 */
	@Override
	public FileDialog getDialogInstance()
	{
		return (FileDialog) this.dialog;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #getEditorValue()
	 */
	@Override
	public Object getEditorValue()
	{
		return this.selectedImage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #setEditorValue(java.lang.Object)
	 */
	@Override
	public void setEditorValue(final Object value)
	{
		//        getDialogInstance().setFileName(value != null ? value.toString() : null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #close()
	 */
	@Override
	public void close()
	{
		// as the FileDialog does not support a programmatically way of closing,
		// this method is forced to do nothing
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.nebula.widgets.nattable.edit.editor.AbstractDialogCellEditor
	 * #isClosed()
	 */
	@Override
	public boolean isClosed()
	{
		return this.closed;
	}
}