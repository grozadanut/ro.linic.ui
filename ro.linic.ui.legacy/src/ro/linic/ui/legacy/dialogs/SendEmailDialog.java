package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.PresentationUtils.BR_SEPARATOR;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.sun.istack.Nullable;

import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.util.InvocationResult;
import ro.colibri.util.ServerConstants;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;

public class SendEmailDialog extends TitleAreaDialog
{
	private Text email;
	private Text subject;
	private Text message;
	
	final private String initialEmail;
	final private String initialSubject;
	final private String initialMessage;
	final private byte[] fileAttachement;
	final private String attachementText;
	
	private Logger log;
	
	public static int open(final Shell parent, final Logger log, final String initialEmail, final String initialSubject,
			final String initialMessage, @Nullable final byte[] fileAttachement, final String attachementText)
	{
		final boolean hasMailConfigured = Boolean.valueOf(BusinessDelegate.persistedProp(PersistedProp.HAS_MAIL_SMTP_KEY)
				.getValueOr(PersistedProp.HAS_MAIL_SMTP_DEFAULT));
		if (!hasMailConfigured)
			return 0;
		
		return new SendEmailDialog(parent, log, initialEmail, initialSubject, initialMessage, fileAttachement, attachementText)
				.open();
	}
	
	private SendEmailDialog(final Shell parent, final Logger log, final String initialEmail, final String initialSubject,
			final String initialMessage, @Nullable final byte[] fileAttachement, final String attachementText)
	{
		super(parent);
		this.log = log;
		this.initialEmail = initialEmail;
		this.initialSubject = initialSubject;
		this.initialMessage = initialMessage;
		this.fileAttachement = fileAttachement;
		this.attachementText = attachementText;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(contents);
		setTitle(Messages.SendEmailDialog_Title);
		setMessage(Messages.SendEmailDialog_Message);
		
		final Label emailLabel = new Label(contents, SWT.NONE);
		emailLabel.setText(Messages.SendEmailDialog_Email);
		UIUtils.setFont(emailLabel);
		
		email = new Text(contents, SWT.SINGLE | SWT.BORDER);
		email.setText(safeString(initialEmail));
		email.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(email);
		
		final Label subjectLabel = new Label(contents, SWT.NONE);
		subjectLabel.setText(Messages.SendEmailDialog_Subject);
		UIUtils.setFont(subjectLabel);
		
		subject = new Text(contents, SWT.SINGLE | SWT.BORDER);
		subject.setText(safeString(initialSubject));
		subject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(subject);
		
		final Label textLabel = new Label(contents, SWT.NONE);
		textLabel.setText(Messages.SendEmailDialog_Text);
		UIUtils.setFont(textLabel);
		
		message = new Text(contents, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		message.setText(safeString(initialMessage).replaceAll(BR_SEPARATOR, NEWLINE));
		message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		UIUtils.setFont(message);
		
		final Label attachementLabel = new Label(contents, SWT.NONE);
		attachementLabel.setText(Messages.SendEmailDialog_Attachment);
		UIUtils.setFont(attachementLabel);
		
		final Text attachement = new Text(contents, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
		attachement.setText(safeString(attachementText));
		attachement.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		UIUtils.setFont(attachement);
		
		return contents;
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(1000, 750);
	}
	
	@Override
	protected void okPressed()
	{
		if (isEmpty(email.getText()))
		{
			setErrorMessage(Messages.SendEmailDialog_EmailMissing);
			return;
		}
		if (isEmpty(subject.getText()))
		{
			setErrorMessage(Messages.SendEmailDialog_SubjectMissing);
			return;
		}
		
		final Future<InvocationResult> result = BusinessDelegate.sendMail(ServerConstants.FIRMA_EMAIL, email.getText(),
				subject.getText(), message.getText().replaceAll(NEWLINE, BR_SEPARATOR), fileAttachement, ServerConstants.DEVELOPER_EMAIL);
		super.okPressed();
		try
		{
			showResult(result.get());
		}
		catch (InterruptedException | ExecutionException e)
		{
			log.error(e);
		}
	}
}
