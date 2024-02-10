package ro.linic.ui.legacy.dialogs;

import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class ReplaceUserDialog extends TitleAreaDialog
{
	private Combo replacedWith;

	private ImmutableList<User> allUsers;
	private User userToDelete;
	
	public ReplaceUserDialog(final Shell parent, final User userToDelete, final ImmutableList<User> allUsers)
	{
		super(parent);
		this.userToDelete = userToDelete;
		this.allUsers = allUsers;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle("Inlocuieste Utilizator");
		
		final Label replacedWithLabel = new Label(contents, SWT.NONE);
		replacedWithLabel.setText("Inlocuieste cu");
		UIUtils.setFont(replacedWithLabel);
		
		replacedWith = new Combo(contents, SWT.DROP_DOWN);
		replacedWith.setItems(allUsers.stream().map(User::displayName).toArray(String[]::new));
		replacedWith.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(replacedWith);
		
		return contents;
	}
	
	@Override
	protected void okPressed()
	{
		if (!selectedReplacedWithUser().isPresent())
		{
			setErrorMessage("Selectati un utilizator care va inlocui utilizatorul pe care vreti sa il stergeti!");
			return;
		}
		
		final InvocationResult result = BusinessDelegate.replaceUserWith(userToDelete.getId(), selectedReplacedWithUser().get().getId());
		MessageDialog.openInformation(null, "Rezultat", result.toTextDescription());
		super.okPressed();
	}
	
	private Optional<User> selectedReplacedWithUser()
	{
		final int index = replacedWith.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allUsers.get(index));
	}
}
