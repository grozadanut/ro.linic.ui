package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.mapper.AccDocMapper;
import ro.linic.ui.legacy.service.CasaMarcat;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.service.SQLiteJDBC;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.wizards.InchideBonWizard.TipInchidere;
import ro.linic.ui.pos.base.services.ECRDriver.PaymentType;
import ro.linic.ui.pos.base.services.ECRService;

public class InchideBonFirstPage extends WizardPage
{
	private AccountingDocument bonCasa;
	private boolean casaActive;
	private TipInchidere tipInchidere;
	
	private Text cuiBonText;
	private Text dePlataText;
	private Text numerarText;
	private Text restPlataText;
	
	private Button inchidePrinCasa;
	private Button inchideFactura;
	private Button inchideCard;
	private Button abandon;
	
	private Bundle bundle;
	private Logger log;
	private ECRService ecrService;

	public InchideBonFirstPage(final AccountingDocument bonCasa, final boolean casaActive,
			final Bundle bundle, final Logger log, final TipInchidere tipInchidere, final IEclipseContext ctx)
	{
        super("InchideBonFirstPage");
        this.bonCasa = bonCasa;
        this.casaActive = casaActive;
        this.bundle = bundle;
        this.log = log;
        this.tipInchidere = tipInchidere;
        this.ecrService = ctx.get(ECRService.class);
    }

	@Override
	public void createControl(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(5, false);
		container.setLayout(layout);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		final Composite textContainer = new Composite(container, SWT.NONE);
		final GridLayout textCLayout = new GridLayout(2, false);
		textContainer.setLayout(textCLayout);
		textContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		final Label cuiBonLabel = new Label(textContainer, SWT.NONE);
		cuiBonLabel.setText("CUI PE BON");
		cuiBonLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		cuiBonLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		cuiBonLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		cuiBonText = new Text(textContainer, SWT.SINGLE | SWT.BORDER);
		final GridData cuiBonGD = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		cuiBonGD.minimumWidth = InchideBonWizard.READ_ONLY_TEXT_WIDTH;
		cuiBonText.setLayoutData(cuiBonGD);
//		cuiBonText.setOrientation(SWT.RIGHT_TO_LEFT);
		UIUtils.setBoldFont(cuiBonText);
		
		final Label dePlataLabel = new Label(textContainer, SWT.NONE);
		dePlataLabel.setText("DE PLATA");
		dePlataLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		dePlataLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		dePlataLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		dePlataText = new Text(textContainer, SWT.READ_ONLY | SWT.BORDER);
		final GridData dePlataGD = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		dePlataGD.minimumWidth = InchideBonWizard.READ_ONLY_TEXT_WIDTH;
		dePlataText.setLayoutData(dePlataGD);
//		dePlataText.setOrientation(SWT.RIGHT_TO_LEFT);
		UIUtils.setBoldFont(dePlataText);
		dePlataText.setText(displayBigDecimal(bonCasa.getTotal()));
		
		final Label numerarLabel = new Label(textContainer, SWT.NONE);
		numerarLabel.setText("NUMERAR");
		numerarLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		numerarLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		numerarLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		numerarText = new Text(textContainer, SWT.SINGLE | SWT.BORDER);
		final GridData numerarGD = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		numerarGD.minimumWidth = InchideBonWizard.EDITABLE_TEXT_WIDTH;
		numerarText.setLayoutData(numerarGD);
//		numerarText.setOrientation(SWT.RIGHT_TO_LEFT);
		numerarText.setFocus();
		UIUtils.setBoldFont(numerarText);
		numerarText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		numerarText.setText(displayBigDecimal(bonCasa.getTotal()));
		
		final Label restPlataLabel = new Label(textContainer, SWT.NONE);
		restPlataLabel.setText("REST DE PLATA");
		restPlataLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		restPlataLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		restPlataLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		
		restPlataText = new Text(textContainer, SWT.READ_ONLY | SWT.BORDER);
		final GridData restPlataGD = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		restPlataGD.minimumWidth = InchideBonWizard.READ_ONLY_TEXT_WIDTH;
		restPlataText.setLayoutData(restPlataGD);
//		restPlataText.setOrientation(SWT.RIGHT_TO_LEFT);
		UIUtils.setBoldFont(restPlataText);
		restPlataText.setText("0");
		
		inchidePrinCasa = new Button(container, SWT.PUSH | SWT.WRAP);
		inchidePrinCasa.setText("Inchidere prin casa - F3");
		final GridData inchidePrinCasaGD = new GridData(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT);
		inchidePrinCasaGD.horizontalIndent = 20;
		inchidePrinCasa.setLayoutData(inchidePrinCasaGD);
		inchidePrinCasa.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchidePrinCasa.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchidePrinCasa);
		
		inchideFactura = new Button(container, SWT.PUSH | SWT.WRAP);
		inchideFactura.setText("Inchide operatia prin Factura, Aviz sau BonConsum");
		final GridData inchideFacturaGD = new GridData(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT);
		inchideFacturaGD.horizontalIndent = 30;
		inchideFactura.setLayoutData(inchideFacturaGD);
		inchideFactura.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		inchideFactura.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideFactura);
		
		inchideCard = new Button(container, SWT.PUSH | SWT.WRAP);
		inchideCard.setText("Inchidere prin CARD/POS");
		inchideCard.setLayoutData(new GridData(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT));
		inchideCard.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		inchideCard.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(inchideCard);
		
		abandon = new Button(container, SWT.PUSH);
		abandon.setText("Abandon");
		final GridData abandonGD = new GridData(InchideBonWizard.BUTTON_WIDTH, InchideBonWizard.BUTTON_HEIGHT);
		abandonGD.horizontalAlignment = SWT.RIGHT;
		abandonGD.grabExcessHorizontalSpace = true;
		abandon.setLayoutData(abandonGD);
		abandon.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		abandon.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBannerFont(abandon);

		// required to avoid an error in the system
		setControl(container);
		setPageComplete(true);
		addListeners();
		updateTipInchidere();
	}
	
	private void updateTipInchidere()
	{
		switch (tipInchidere)
		{
		case PRIN_CASA:
			inchidePrinCasa.setEnabled(true);
			inchideFactura.setEnabled(false);
			inchideCard.setEnabled(false);
			break;
			
		case PRIN_CARD:
			inchidePrinCasa.setEnabled(false);
			inchideFactura.setEnabled(false);
			inchideCard.setEnabled(true);
			break;
			
		case FACTURA_BC:
			inchidePrinCasa.setEnabled(false);
			inchideFactura.setEnabled(true);
			inchideCard.setEnabled(false);
			break;

		case ANY:
		default:
			break;
		}
	}

	private void addListeners()
	{
		inchidePrinCasa.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				inchideBon(false);
			}
		});
		numerarText.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.F3 && inchidePrinCasa.isEnabled())
					inchideBon(false);
			}
		});
		numerarText.addModifyListener(e -> 
		{
			final BigDecimal rest = parse(numerarText.getText()).subtract(bonCasa.getTotal());
			restPlataText.setText(displayBigDecimal(rest, 2, RoundingMode.HALF_EVEN));
		});
		
		inchideCard.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				inchideBon(true);
			}
		});
		
		inchideFactura.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				((InchideBonWizardDialog) getWizard().getContainer()).nextPressed();
			}
		});
		
		abandon.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				((InchideBonWizardDialog) getWizard().getContainer()).cancelPressed();
			}
		});
	}
	
	private void inchideBon(final boolean incasarePrinCard)
	{
		// close bon in db
		InvocationResult result;
		if (ClientSession.instance().isOfflineMode())
			result = SQLiteJDBC.instance(bundle, log).closeBonCasa(bonCasa, casaActive);
		else
			result = BusinessDelegate.closeBonCasa(bonCasa.getId(), casaActive);
		
		try
		{
			if (result.statusCanceled())
			{
				setPageComplete(false);
				setErrorMessage(result.toTextDescription());
				return;
			}
			else
			{
				setPageComplete(true);
				setErrorMessage(null);
				// print bon
				if (!ClientSession.instance().isOfflineMode())
				{
					final AccountingDocument reloadedBon = result.extra(InvocationResult.ACCT_DOC_KEY);
					final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(reloadedBon);
					reloadedBon.setOperatiuni(new HashSet<Operatiune>(operatiuni));
					if (reloadedBon.isShouldTransport())
						JasperReportManager.instance(bundle, log).printNonOfficialDoc(bundle, reloadedBon, false);
				}
			}
		}
		catch (final Exception e)
		{
			log.error(e);
			showException(e, "Eroare la printarea bonului.");
		}
		
		try
		{
			// print bon to casa
			if (casaActive)
				ecrService.printReceipt(AccDocMapper.toReceipt(List.of(bonCasa)),
						incasarePrinCard ? PaymentType.CARD : PaymentType.CASH, Optional.ofNullable(cuiBonText.getText()))
				.thenAcceptAsync(new CasaMarcat.UpdateDocStatus(Set.of(bonCasa.getId()), false));
		}
		catch (final Exception e)
		{
			log.error(e);
			showException(e, "Eroare la scoaterea bonului la casa de marcat.");
			
			if (!ClientSession.instance().isOfflineMode())
				BusinessDelegate.closeBonCasa_Failed(ImmutableSet.of(bonCasa.getId()));
		}
		
		// close dialog
		((InchideBonWizardDialog) getWizard().getContainer()).finishPressed();
	}
}
