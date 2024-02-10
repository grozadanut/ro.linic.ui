package ro.linic.ui.legacy.wizards;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.NumberUtils.greaterThan;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.NumberUtils.smallerThan;
import static ro.colibri.util.NumberUtils.smallerThanOrEqual;
import static ro.colibri.util.NumberUtils.truncate;
import static ro.colibri.util.PresentationUtils.safeString;
import static ro.linic.ui.legacy.session.UIUtils.extractLocalDate;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.security.Permissions;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.CustomerDebtNatTable;

public class CustomerDebtSelectPage extends WizardPage
{
	private Text filter;
	private CustomerDebtNatTable table;
	private Text incasat;
	private DateTime dataDoc;
	private Button transformaInFactura;
	private Button addFidelityPoints;
	private Combo contBancar;
	private Button casaActiva;
	private Text paidDocNr;
	private Text discDisponibil;
	private Text discChelt;
	
	private Button printVanzari;
	private Button printVanzariComasat;
	
//	private TempDocsNatTable tempDocsTable;
//	private Text tempFilter;
//	private Button deleteTempDocs;
	
	private UISynchronize sync;
	private Bundle bundle;
	private Logger log;
	private ImmutableList<ContBancar> allConturiBancare;
	
	public CustomerDebtSelectPage(final UISynchronize sync, final Bundle bundle, final Logger log)
	{
        super("Incaseaza");
        setTitle("Selecteaza documente");
        setMessage("Selecteaza documentele care trebuie incasate sau partenerul la care se adauga incasarea");
        this.sync = sync;
        this.bundle = bundle;
        this.log = log;
        allConturiBancare = BusinessDelegate.allConturiBancare();
    }
	
	@Override
	public void createControl(final Composite parent)
	{
//		final SashForm horizontalSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH | SWT.BORDER);
//		GridDataFactory.fillDefaults().applyTo(horizontalSash);

		createLeftSide(parent);
//		createRightSide(horizontalSash);

//		horizontalSash.setWeights(new int[] {225, 175});
//		setControl(horizontalSash);
		addListeners();
		loadData();
		setPageComplete(false);
	}

	private void createLeftSide(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		final Label filterLabel = new Label(container, SWT.NONE);
		filterLabel.setText("Partener");
		UIUtils.setFont(filterLabel);
		
		filter = new Text(container, SWT.SINGLE | SWT.BORDER);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(filter);
		
		table = new CustomerDebtNatTable();
		table.postConstruct(container);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(table.getTable());
		
		final Label incasatLabel = new Label(container, SWT.NONE);
		incasatLabel.setText("Incasat");
		UIUtils.setFont(incasatLabel);
		
		incasat = new Text(container, SWT.SINGLE | SWT.BORDER);
		incasat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(incasat);
		refreshIncasatEnablement();
		
		final Label dataDocLabel = new Label(container, SWT.NONE);
		dataDocLabel.setText("Data doc");
		UIUtils.setFont(dataDocLabel);
		
		dataDoc = new DateTime(container, SWT.DATE | SWT.DROP_DOWN | SWT.CALENDAR_WEEKNUMBERS);
		UIUtils.setFont(dataDoc);
		
		final Label discDispLabel = new Label(container, SWT.NONE);
		discDispLabel.setText("Disc disponibil");
		UIUtils.setFont(discDispLabel);
		
		discDisponibil = new Text(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		discDisponibil.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(discDisponibil);
		
		final Label discCheltLabel = new Label(container, SWT.NONE);
		discCheltLabel.setText("Foloseste Discount");
		UIUtils.setFont(discCheltLabel);
		
		discChelt = new Text(container, SWT.SINGLE | SWT.BORDER);
		discChelt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		UIUtils.setFont(discChelt);
		refreshDiscCheltEnablement();
		
		new Label(container, SWT.NONE); // layout purposes
		transformaInFactura = new Button(container, SWT.CHECK);
		transformaInFactura.setText("Transforma in Factura");
		UIUtils.setFont(transformaInFactura);
		refreshTransformaFacturaEnablement();
		
		new Label(container, SWT.NONE); // layout purposes
		addFidelityPoints = new Button(container, SWT.CHECK);
		addFidelityPoints.setText("Adauga Puncte de Fidelitate");
		UIUtils.setFont(addFidelityPoints);
		refreshAddDiscountEnablement();
		
		new Label(container, SWT.NONE); // layout purposes
		casaActiva = new Button(container, SWT.CHECK);
		casaActiva.setText("casa marcat activa?");
		casaActiva.setSelection(true);
		casaActiva.setEnabled(ClientSession.instance().hasPermission(Permissions.CLOSE_WITHOUT_CASA));
		UIUtils.setFont(casaActiva);
		
		final Label contBancarLabel = new Label(container, SWT.NONE);
		contBancarLabel.setText("Cont bancar");
		UIUtils.setFont(contBancarLabel);
		
		contBancar = new Combo(container, SWT.DROP_DOWN);
		contBancar.setItems(allConturiBancare.stream().map(ContBancar::displayName).toArray(String[]::new));
		UIUtils.setFont(contBancar);
		GridDataFactory.swtDefaults().hint(InchideBonWizard.EDITABLE_TEXT_WIDTH, SWT.DEFAULT).applyTo(contBancar);
		
		final Label paidDocNrLabel = new Label(container, SWT.NONE);
		paidDocNrLabel.setText("Nr chitanta bancara");
		paidDocNrLabel.setToolTipText("Daca introduceti un Ordin de Plata, prefixati numarul cu textul 'OP'");
		UIUtils.setFont(paidDocNrLabel);
		
		paidDocNr = new Text(container, SWT.SINGLE | SWT.BORDER);
		paidDocNr.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		paidDocNr.setToolTipText("Daca introduceti un Ordin de Plata, prefixati numarul cu textul 'OP'");
		UIUtils.setFont(paidDocNr);
		refreshPaidDocNrEnablement();
		
		final Composite bottomButtonsCont = new Composite(container, SWT.NONE);
		bottomButtonsCont.setLayout(new GridLayout(2, false));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(bottomButtonsCont);
		
		printVanzari = new Button(bottomButtonsCont, SWT.PUSH);
		printVanzari.setText("Printeaza documentele selectate");
		UIUtils.setFont(printVanzari);
		
		printVanzariComasat = new Button(bottomButtonsCont, SWT.PUSH);
		printVanzariComasat.setText("Printeaza documentele selectate comasate");
		UIUtils.setFont(printVanzariComasat);
		
		setControl(container);
	}
	
//	private void createRightSide(final Composite parent)
//	{
//		final Composite container = new Composite(parent, SWT.NONE);
//		container.setLayout(new GridLayout(2, false));
//
//		final Label filterLabel = new Label(container, SWT.NONE);
//		filterLabel.setText("Operator");
//		UIUtils.setFont(filterLabel);
//
//		tempFilter = new Text(container, SWT.SINGLE | SWT.BORDER);
//		tempFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		UIUtils.setFont(tempFilter);
//
//		tempDocsTable = new TempDocsNatTable();
//		tempDocsTable.postConstruct(container);
//		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(tempDocsTable.getTable());
//
//		final Composite bottomButtonsCont = new Composite(container, SWT.NONE);
//		bottomButtonsCont.setLayout(new GridLayout(2, false));
//		GridDataFactory.swtDefaults().span(2, 1).applyTo(bottomButtonsCont);
//
//		deleteTempDocs = new Button(bottomButtonsCont, SWT.PUSH);
//		deleteTempDocs.setText("Am primit banii");
//		deleteTempDocs.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
//		deleteTempDocs.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
//		UIUtils.setFont(deleteTempDocs);
//	}
	
	private void refreshIncasatEnablement()
	{
		//incasat.setEditable(table.selection().size() == 1);
	}
	
	private void refreshPaidDocNrEnablement()
	{
		paidDocNr.setEditable(contBancar() != null);
	}
	
	private void refreshTransformaFacturaEnablement()
	{
		transformaInFactura.setEnabled(canTransformInFactura());
	}
	
	private void refreshAddDiscountEnablement()
	{
		final boolean canAddDiscount = canAddDiscount();
		addFidelityPoints.setEnabled(canAddDiscount);
		addFidelityPoints.setSelection(canAddDiscount);
	}
	
	private void refreshDiscCheltEnablement()
	{
		discChelt.setEditable(canUseDiscount());
	}

	private void addListeners()
	{
		filter.addModifyListener(e -> table.filter(filter.getText()));
//		tempFilter.addModifyListener(e -> tempDocsTable.filter(tempFilter.getText()));
		
		table.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener()
		{
            @Override public void selectionChanged(final SelectionChangedEvent event)
            {
                @SuppressWarnings({ "unchecked" })
				final List<Object> selection = event.getStructuredSelection().toList();
                
                final BigDecimal partnerTotal = selection.stream()
                		.filter(RulajPartener.class::isInstance)
                		.map(RulajPartener.class::cast)
                		.map(RulajPartener::getDeIncasat)
                		.reduce(BigDecimal::add)
                		.orElse(BigDecimal.ZERO);
                
                final BigDecimal discDisp = selection.stream()
                		.map(sel -> sel instanceof RulajPartener ? (RulajPartener)sel : ((AccountingDocument)sel).getRulajPartener())
                		.map(RulajPartener::getDiscDisponibil)
                		.findFirst()
                		.orElse(BigDecimal.ZERO);
                
                if (smallerThanOrEqual(partnerTotal, BigDecimal.ZERO))
                {
                	final BigDecimal docsTotal = selection.stream()
                    		.filter(AccountingDocument.class::isInstance)
                    		.map(AccountingDocument.class::cast)
                    		.map(AccountingDocument::totalUnlinked)
                    		.reduce(BigDecimal::add)
                    		.orElse(BigDecimal.ZERO);
                	
                	incasat.setText(safeString(docsTotal, BigDecimal::toString));
                }
                else
                	incasat.setText(partnerTotal.toString());
                
                discDisponibil.setText(truncate(discDisp, 2).toString());
                refreshIncasatEnablement();
                refreshTransformaFacturaEnablement();
                refreshAddDiscountEnablement();
                refreshDiscCheltEnablement();
                validate();
            }
        });
		
//		tempDocsTable.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener()
//		{
//			@Override public void selectionChanged(final SelectionChangedEvent event)
//			{
//				final List<Object> selection = event.getStructuredSelection().toList();
//				final ImmutableList<Object> accDocsOrPartnerToSelect = selection.stream()
//						.filter(TempDocument.class::isInstance)
//						.map(TempDocument.class::cast)
//						.filter(doc -> !doc.incasareCreata())
//						.flatMap(tempDoc -> 
//						{
//							return table.getTreeList().stream()
//									.filter(docOrRulaj -> 
//									{
//										if (isEmpty(tempDoc.getName()))
//											return docOrRulaj instanceof RulajPartener &&
//													Objects.equal(((RulajPartener) docOrRulaj).getId(), tempDoc.getPartner().getId());
//										else
//											return docOrRulaj instanceof AccountingDocument &&
//													((AccountingDocument) docOrRulaj).getId() == parseToLong(tempDoc.getName());
//									});
//						})
//						.collect(toImmutableList());
//
//				table.getSelectionProvider().setSelection(new StructuredSelection(accDocsOrPartnerToSelect));
//				incasat.setText(selection.stream()
//						.filter(TempDocument.class::isInstance)
//						.map(TempDocument.class::cast)
//						.map(TempDocument::getTotal)
//						.reduce(BigDecimal::add)
//						.map(BigDecimal::toString)
//						.orElse("0"));
//			}
//		});
		
		incasat.addModifyListener(new ModifyListener()
		{
			@Override public void modifyText(final ModifyEvent e)
			{
				validate();
			}
		});
		
		contBancar.addModifyListener(e -> refreshPaidDocNrEnablement());
		
		addFidelityPoints.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				refreshDiscCheltEnablement();
			}
		});
		
		discChelt.addModifyListener(e -> 
		{
			validate();
			refreshAddDiscountEnablement();
		});
		
		printVanzari.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, CustomerDebtWizard.notFullyCoveredVanzari(selection())
							.collect(toImmutableList()), true);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});
		
		printVanzariComasat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					JasperReportManager.instance(bundle, log)
					.printDocs(bundle, JasperReportManager.comasarePtPrint(
							CustomerDebtWizard.notFullyCoveredVanzari(selection())
							.collect(toImmutableList()), true), false);
				}
				catch (final Exception ex)
				{
					log.error(ex);
					showException(ex, "Documentele nu au putut fi printate!");
				}
			}
		});
		
//		deleteTempDocs.addSelectionListener(new SelectionAdapter()
//		{
//			@Override public void widgetSelected(final SelectionEvent e)
//			{
//				final ImmutableSet<TempDocument> tempDocs = tempDocsTable.selection().stream()
//						.filter(TempDocument.class::isInstance)
//						.map(TempDocument.class::cast)
//						.collect(toImmutableSet());
//				final BigDecimal total = tempDocs.stream()
//						.map(TempDocument::getTotal)
//						.reduce(BigDecimal::add)
//						.orElse(BigDecimal.ZERO);
//
//				if (!tempDocs.isEmpty() && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), 
//						"Receptie bani", MessageFormat.format("Ati primit {0} RON de la sofer?", total)))
//				{
//					showResult(BusinessDelegate.deleteTempDocs(tempDocs.stream()
//							.map(TempDocument::getId)
//							.collect(toImmutableSet())));
//					loadData();
//				}
//			}
//		});
	}
	
	private void validate()
	{
		final List<Object> selection = table.selection();
		final boolean morePartnersSelected = selection.stream()
        		.filter(RulajPartener.class::isInstance)
        		.count() > 1;
        final boolean moreBCSelected = CustomerDebtWizard.notFullyCoveredVanzari(selection)
        		.filter(AccountingDocument::isUnofficialDoc)
        		.count() > 1;
		
		if (selection.isEmpty())
		{
			setPageComplete(false);
			setErrorMessage("Selectati un Document sau un Client");
			return;
		}
		
		if (morePartnersSelected)
		{
			setPageComplete(false);
			setErrorMessage("Selectati un singur Client");
			return;
		}
		
		if (smallerThan(parse(incasat.getText()), BigDecimal.ZERO))
		{
			setPageComplete(false);
			setErrorMessage("Suma incasata trebuie sa fie mai mare sau egala cu 0");
			return;
		}
		
		// temp fix for #48 Disc used for multiple BC
		if (moreBCSelected && !transformaInFactura() && greaterThan(discChelt(), BigDecimal.ZERO))
		{
			setPageComplete(false);
			setErrorMessage("Daca folositi discount trebuie selectat un singur BON CONSUM");
			return;
		}
		
		setErrorMessage(null);
		setPageComplete(true);
	}
	
	public List<Object> selection()
	{
		return table.selection();
	}
	
	public ImmutableSet<Long> selectedTempDocIds()
	{
		return ImmutableSet.of();
//		return tempDocsTable.selection().stream()
//				.filter(TempDocument.class::isInstance)
//				.map(TempDocument.class::cast)
//				.map(TempDocument::getId)
//				.collect(toImmutableSet());
	}
	
	public BigDecimal incasat()
	{
		return parse(incasat.getText());
	}
	
	public LocalDate dataDoc()
	{
		return extractLocalDate(dataDoc);
	}
	
	public ContBancar contBancar()
	{
		final int index = contBancar.getSelectionIndex();
		if (index == -1)
			return null;

		return allConturiBancare.get(index);
	}
	
	public boolean casaActiva()
	{
		return casaActiva.getSelection();
	}
	
	public boolean transformaInFactura()
	{
		return transformaInFactura.getSelection();
	}
	
	public boolean addDiscount()
	{
		return addFidelityPoints.getSelection();
	}
	
	public BigDecimal discChelt()
	{
		return parse(discChelt.getText());
	}
	
	public boolean canTransformInFactura()
	{
		if (table.selection().stream()
				.filter(RulajPartener.class::isInstance)
				.findAny()
				.isPresent())
			return false;
		
		return table.selection().stream()
				.filter(AccountingDocument.class::isInstance)
				.map(AccountingDocument.class::cast)
				.filter(AccountingDocument::isUnofficialDoc)
				.findAny()
				.isPresent();
	}
	
	public boolean canAddDiscount()
	{
		if (!ClientSession.instance().hasPermission(Permissions.ADD_CLIENT_DOCS))
			return false;
		
		if (smallerThanOrEqual(parse(incasat.getText()), BigDecimal.ZERO))
			return false;
		
		if (parse(discChelt.getText()).compareTo(BigDecimal.ZERO) != 0)
			return false;
		
		if (table.selection().stream()
				.filter(RulajPartener.class::isInstance)
				.map(RulajPartener.class::cast)
				.filter(RulajPartener::hasFidelityCard)
				.findAny()
				.isPresent())
			return true;
		
		return table.selection().stream()
				.filter(AccountingDocument.class::isInstance)
				.map(AccountingDocument.class::cast)
				.map(AccountingDocument::getPartner)
				.filter(Partner::hasFidelityCard)
				.findAny()
				.isPresent();
	}
	
	public boolean canUseDiscount()
	{
		if (!ClientSession.instance().hasPermission(Permissions.ADD_CLIENT_DOCS))
			return false;
		
		if (canAddDiscount() && addDiscount())
			return false;
		
		if (parse(discDisponibil.getText()).compareTo(BigDecimal.ZERO) <= 0)
			return false;
		
		return true;
	}
	
	public String paidDocNr()
	{
		return contBancar() != null ? paidDocNr.getText() : null;
	}

	private void loadData()
	{
		BusinessDelegate.customerDebtDocs(new AsyncLoadResult<InvocationResult>()
		{
			@Override public void success(final InvocationResult result)
			{
				final ImmutableList<AccountingDocument> unpaidDocs = result.extra(InvocationResult.ACCT_DOC_KEY);
				final ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY);
				table.loadData(unpaidDocs, unpaidPartners);
				
				if (unpaidDocs.isEmpty() && unpaidPartners.isEmpty())
					setMessage("Niciun document neincasat gasit!");
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea documentelor", details);
			}
		}, sync);
		
//		BusinessDelegate.incasariOfDrivers(new AsyncLoadData<TempDocument>()
//		{
//			@Override public void success(final ImmutableList<TempDocument> data)
//			{
//				tempDocsTable.loadData(data);
//			}
//
//			@Override public void error(final String details)
//			{
//				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare la incarcarea incasarilor soferilor", details);
//			}
//		}, sync);
	}
}
