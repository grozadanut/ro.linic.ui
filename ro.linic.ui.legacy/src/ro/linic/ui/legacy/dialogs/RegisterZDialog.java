package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableMap;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableMap;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;

public class RegisterZDialog extends Dialog
{
	private Text bfz;
	private DateTime data;
	private Text discount;
	private Text marfa;
	private ImmutableMap<ContBancar, Text> contBancarToAmount;
	
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	
	public RegisterZDialog(final Shell parent, final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		super(parent);
		this.sync = sync;
		this.bundle = bundle;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText(Messages.RegisterZDialog_Title);
		
		final Label descriptionLabel = new Label(contents, SWT.NONE);
		descriptionLabel.setText(Messages.RegisterZDialog_Description);
		final GridData descriptionGD = new GridData();
		descriptionGD.horizontalSpan = 2;
		descriptionLabel.setLayoutData(descriptionGD);
		UIUtils.setFont(descriptionLabel);
		
		final Label bfzLabel = new Label(contents, SWT.NONE);
		bfzLabel.setText(Messages.RegisterZDialog_Number);
		UIUtils.setFont(bfzLabel);
		
		bfz = new Text(contents, SWT.SINGLE | SWT.BORDER);
		bfz.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(bfz);
		
		final Label dataLabel = new Label(contents, SWT.NONE);
		dataLabel.setText(Messages.RegisterZDialog_Date);
		UIUtils.setFont(dataLabel);
		
		data = new DateTime(contents, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		data.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(data);
		
		final Label discountLabel = new Label(contents, SWT.NONE);
		discountLabel.setText(Messages.RegisterZDialog_Discount);
		UIUtils.setFont(discountLabel);
		
		discount = new Text(contents, SWT.SINGLE | SWT.BORDER);
		discount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(discount);
		
		final Label marfaLabel = new Label(contents, SWT.NONE);
		marfaLabel.setText(Messages.RegisterZDialog_Cash);
		UIUtils.setFont(marfaLabel);
		
		marfa = new Text(contents, SWT.SINGLE | SWT.BORDER);
		marfa.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(marfa);
		
		contBancarToAmount = BusinessDelegate.allConturiBancare().stream()
				.collect(toImmutableMap(Function.identity(), contBancar ->
				{
					final Label cardLabel = new Label(contents, SWT.NONE);
					cardLabel.setText(NLS.bind(Messages.RegisterZDialog_Card, contBancar.displayName()));
					UIUtils.setFont(cardLabel);

					final Text card = new Text(contents, SWT.SINGLE | SWT.BORDER);
					card.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
					UIUtils.setFont(card);
					return card;
				}));

		addListeners();
		return contents;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void addListeners()
	{
		marfa.addModifyListener(e -> getButton(IDialogConstants.OK_ID).setEnabled(!isEmpty(marfa.getText())));
	}
	
	@Override
	protected void okPressed()
	{
		final LocalDate dataBfz = getData();
		final InvocationResult result = BusinessDelegate.inregistreazaZ(getBfz(), dataBfz, getDiscount(),
				getMarfa(), contBancarIdToAmount());
		showResult(result);
		if (result.statusOk())
		{
			openRegCasa(result);
			super.okPressed();
		}
	}
	
	private void openRegCasa(final InvocationResult result)
	{
		final Map<LocalDate, java.util.List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
		final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
		try
		{
			JasperReportManager.instance(bundle, log).printRegCasa_Zile(bundle, ClientSession.instance().getLoggedUser().getSelectedGestiune().getName(),
					accDocsByDate, solduriInitiale);
		}
		catch (IOException | JRException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	public String getBfz()
	{
		return bfz.getText();
	}
	
	public LocalDate getData()
	{
		return extractLocalDate(data);
	}
	
	public BigDecimal getDiscount()
	{
		return parse(discount.getText());
	}
	
	public BigDecimal getMarfa()
 	{
		return parse(marfa.getText());
 	}
	
	public ImmutableMap<Integer, BigDecimal> contBancarIdToAmount()
	{
		return contBancarToAmount.entrySet().stream()
				.collect(toImmutableMap(entry -> entry.getKey().getId(), entry -> parse(entry.getValue().getText())));
	}
}
