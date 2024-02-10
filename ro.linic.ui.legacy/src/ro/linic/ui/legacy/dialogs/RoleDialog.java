package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import ro.colibri.entities.user.Permission;
import ro.colibri.entities.user.Role;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class RoleDialog extends TitleAreaDialog
{
	private Text name;
	private List permissions;
	
	private ImmutableList<Permission> allPermissions;
	
	private Role role;
	
	private Supplier<Boolean> okPressed;

	public RoleDialog(final Shell parent, final Role role)
	{
		super(parent);
		this.role = role;
		allPermissions = BusinessDelegate.allUserVisPermissions();
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTitle("Rol");
		
		final Label nameLabel = new Label(contents, SWT.NONE);
		nameLabel.setText("Nume");
		UIUtils.setFont(nameLabel);
		
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(name);
		
		final Label permissionsLabel = new Label(contents, SWT.NONE);
		permissionsLabel.setText("Permisiuni");
		UIUtils.setFont(permissionsLabel);
		
		permissions = new List(contents, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		permissions.setItems(allPermissions.stream().map(Permission::displayName).toArray(String[]::new));
		permissions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(permissions);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 250).applyTo(permissions);
		
		fillFields();
		return contents;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(super.getInitialSize().x, 500);
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(name.getText()))
		{
			setErrorMessage("Numele rolului este obligatoriu!");
			return;
		}
		
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		name.setText(safeString(role, Role::getName));
		permissions.setSelection(role.getPermissions().stream().mapToInt(allPermissions::indexOf).toArray());
	}
	
	private void fillRole()
	{
		role.setName(name.getText());
		role.getPermissions().clear();
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public Role filledRole()
	{
		fillRole();
		return role;
	}
	
	public ImmutableSet<String> selectedPermissionNames()
	{
		final Builder<String> builder = ImmutableSet.<String>builder();
		for (final int permSelIndex : permissions.getSelectionIndices())
			builder.add(allPermissions.get(permSelIndex).getName());
		return builder.build();
	}
}
