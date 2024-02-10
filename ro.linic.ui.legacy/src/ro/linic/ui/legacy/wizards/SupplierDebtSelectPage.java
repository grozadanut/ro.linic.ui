package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.smallerThanOrEqual;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.SupplierDebtNatTable;

public class SupplierDebtSelectPage extends WizardPage
{
	private Text filter;
	private SupplierDebtNatTable table;
	private Combo doc;
	private Text nrDoc;
	private DateTime dataDoc;
	private Text platit;
	private Button regCasa;
	private Combo contBancar;
	
	private Button printDocs;
	private Button printDocsComasat;
	
	private Bundle bundle;
	private Logger log;
	private UISynchronize sync;
	private ImmutableList<ContBancar> allConturiBancare;
	
	public SupplierDebtSelectPage(final UISynchronize sync, final Bundle bundle, final Logger log)
	{
        super("Plateste");
        setTitle("Selecteaza documente");
        setMessage("Selecteaza documentele care trebuie platite sau partenerul la care se adauga plata");
        this.sync = sync;
        this.bundle = bundle;
        this.log = log;
        allConturiBancare = BusinessDelegate.allConturiBancare();
    }
	
	@Override
	public void createControl(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		final Label filterLabel = new Label(container, SWT.NONE);
		filterLabel.setText("Partener");
		UIUtils.setFont(filterLabel);
		
		filter = new Text(container, SWT.SINGLE | SWT.BORDER);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(filter);
		
		table = new SupplierDebtNatTable();
		table.postConstruct(container);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(table.getTable());
		
		final Label docLabel = new Label(container, SWT.NONE);
		docLabel.setText("Doc");
		UIUtils.setFont(docLabel);
		
		doc = new Combo(container, SWT.DROP_DOWN);
		doc.setItems(AccountingDocument.PLATA_DOC_TYPES.toArray(new String[] {}));
		UIUtils.setFont(doc);
		
		final Label nrDocLabel = new Label(container, SWT.NONE);
		nrDocLabel.setText("Nr Doc");
		UIUtils.setFont(nrDocLabel);
		
		nrDoc = new Text(container, SWT.SINGLE | SWT.BORDER);
		nrDoc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(nrDoc);
		
		final Label dataDocLabel = new Label(container, SWT.NONE);
		dataDocLabel.setText("Data Doc");
		UIUtils.setFont(dataDocLabel);
		
		dataDoc = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(dataDoc);
		
		final Label platitLabel = new Label(container, SWT.NONE);
		platitLabel.setText("Platit");
		UIUtils.setFont(platitLabel);
		
		platit = new Text(container, SWT.SINGLE | SWT.BORDER);
		platit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(platit);
		
		new Label(container, SWT.NONE); // layout purposes
		regCasa = new Button(container, SWT.CHECK);
		regCasa.setText("REG-CASA");
		UIUtils.setFont(regCasa);
		
		final Label contBancarLabel = new Label(container, SWT.NONE);
		contBancarLabel.setText("Cont bancar");
		UIUtils.setFont(contBancarLabel);

		contBancar = new Combo(container, SWT.DROP_DOWN);
		contBancar.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		UIUtils.setFont(contBancar);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(contBancar);
		
		final Composite bottomButtonsCont = new Composite(container, SWT.NONE);
		bottomButtonsCont.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(bottomButtonsCont);
		
		printDocs = new Button(bottomButtonsCont, SWT.PUSH);
		printDocs.setText("Printeaza documentele selectate");
		UIUtils.setFont(printDocs);
		
		printDocsComasat = new Button(bottomButtonsCont, SWT.PUSH);
		printDocsComasat.setText("Printeaza documentele selectate comasate");
		UIUtils.setFont(printDocsComasat);
		
		container.setTabList(new Control[] {filter, doc, nrDoc, dataDoc, platit, regCasa, contBancar});
		
		setControl(container);
		addListeners();
		loadData();
		setPageComplete(false);
	}
	
	private void addListeners()
	{
		filter.addModifyListener(e -> table.filter(filter.getText()));
		
		table.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener()
		{
            @Override public void selectionChanged(final SelectionChangedEvent event)
            {
                @SuppressWarnings({ "unchecked" })
				final List<Object> selection = event.getStructuredSelection().toList();
                
                final BigDecimal partnerTotal = selection.stream()
                		.filter(RulajPartener.class::isInstance)
                		.map(RulajPartener.class::cast)
                		.map(RulajPartener::getDePlata)
                		.reduce(BigDecimal::add)
                		.orElse(BigDecimal.ZERO);

                if (smallerThanOrEqual(partnerTotal, BigDecimal.ZERO))
                {
                	final BigDecimal docsTotal = selection.stream()
                    		.filter(AccountingDocument.class::isInstance)
                    		.map(AccountingDocument.class::cast)
                    		.map(AccountingDocument::totalUnlinked)
                    		.reduce(BigDecimal::add)
                    		.orElse(BigDecimal.ZERO);
                	
                	platit.setText(docsTotal.toString());
                }
                else
                	platit.setText(partnerTotal.toString());
                
                validate();
            }
        });
		
		final ModifyListener validateListener = e -> validate();
		doc.addModifyListener(validateListener);
		nrDoc.addModifyListener(validateListener);
		platit.addModifyListener(validateListener);
		
		final KeyAdapter traverseListener = new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT, e);
					e.doit = false;
				}
			}
		};
		
		filter.addKeyListener(traverseListener);
		doc.addKeyListener(traverseListener);
		nrDoc.addKeyListener(traverseListener);
		dataDoc.addKeyListener(traverseListener);
		platit.addKeyListener(traverseListener);
		regCasa.addKeyListener(traverseListener);
		contBancar.addKeyListener(traverseListener);
		
		printDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, SupplierDebtWizard.notFullyCoveredCumparari(selection())
							.collect(toImmutableList()), true);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});

		printDocsComasat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, JasperReportManager.comasarePtPrint(
							SupplierDebtWizard.notFullyCoveredCumparari(selection())
							.collect(toImmutableList()), true), false);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});
	}
	
	private void validate()
	{
		if (table.selection().isEmpty())
		{
			setPageComplete(false);
			setErrorMessage("Selectati un Document sau un Furnizor");
			return;
		}
		
		if (table.selection().stream()
				.filter(RulajPartener.class::isInstance)
        		.map(RulajPartener.class::cast)
				.map(RulajPartener::getId)
				.distinct()
				.count() > 1)
		{
			setPageComplete(false);
			setErrorMessage("Selectati un singur Furnizor");
			return;
		}
		
		if (smallerThanOrEqual(parse(platit.getText()), BigDecimal.ZERO))
		{
			setPageComplete(false);
			setErrorMessage("Suma platita trebuie sa fie mai mare ca 0");
			return;
		}
		
		if (isEmpty(doc.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Completati tipul documentului");
			return;
		}
		
		if (isEmpty(nrDoc.getText()))
		{
			setPageComplete(false);
			setErrorMessage("Completati numarul documentului");
			return;
		}
		
		setErrorMessage(null);
		setPageComplete(true);
	}
	
	private void loadData()
	{
		BusinessDelegate.supplierDebtDocs(new AsyncLoadResult<InvocationResult>()
		{
			@Override public void success(final InvocationResult result)
			{
				final ImmutableList<AccountingDocument> unpaidDocs = result.extra(InvocationResult.ACCT_DOC_KEY);
				final ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY);
				table.loadData(unpaidDocs, unpaidPartners);
				
				if (unpaidDocs.isEmpty() && unpaidPartners.isEmpty())
					setMessage("Niciun document neplatit gasit!");
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea documentelor", details);
			}
		}, sync);
	}
	
	public List<Object> selection()
	{
		return table.selection();
	}
	
	public BigDecimal platit()
	{
		return parse(platit.getText());
	}
	
	public boolean regCasa()
	{
		return regCasa.getSelection();
	}
	
	public ContBancar contBancar()
	{
		final int index = contBancar.getSelectionIndex();
		if (index == -1)
			return null;
		
		return allConturiBancare.get(index);
	}
	
	public String doc()
	{
		return doc.getText();
	}
	
	public String nrDoc()
	{
		return nrDoc.getText();
	}
	
	public LocalDateTime dataDoc()
	{
		return extractLocalDate(dataDoc).atTime(LocalTime.now());
	}
}
