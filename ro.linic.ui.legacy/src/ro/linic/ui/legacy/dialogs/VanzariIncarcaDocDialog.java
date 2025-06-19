package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toHashSet;
import static ro.colibri.util.LocalDateUtils.displayLocalDate;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.PresentationUtils.displayBigDecimal;
import static ro.colibri.util.StringUtils.globalIsMatch;
import static ro.linic.ui.legacy.session.UIUtils.setFont;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.AccountingDocumentMapping;
import ro.colibri.util.StringUtils.TextFilterMethod;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.mapper.AccDocMapper;
import ro.linic.ui.legacy.parts.components.VanzareInterface;
import ro.linic.ui.legacy.service.CasaMarcat;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.tables.AccDocsNatTable;
import ro.linic.ui.pos.base.model.PaymentType;
import ro.linic.ui.pos.base.services.ECRService;

public class VanzariIncarcaDocDialog extends Dialog
{
	private static final int SHELL_HEIGHT = 160;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 100;
	private static final int TABLE_WIDTH = 520;
	
	private Text infoArea;
	private AccDocsNatTable docsTable;
	private Button incarca;
	private Button printare;
	private Button printareFaraDisc;
	private Button printCasaMarcat;
	private Button inchide;
	
	private VanzareInterface vanzarePart;
	private UISynchronize sync;
	private boolean bonIncarcat = false;
	private ECRService ecrService;
	
	public VanzariIncarcaDocDialog(final Shell parent, final VanzareInterface vanzarePart, final IEclipseContext ctx)
	{
		super(parent);
		this.vanzarePart = vanzarePart;
		this.sync = ctx.get(UISynchronize.class);
		this.ecrService = ctx.get(ECRService.class);
	}
	
	@Override
	protected Point getInitialSize()
	{
		return new Point(Display.getCurrent().getClientArea().width, SHELL_HEIGHT);
	}
	
	@Override
	protected Point getInitialLocation(final Point initialSize)
	{
		return new Point(0, Display.getCurrent().getClientArea().height - SHELL_HEIGHT);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		container.setLayout(new GridLayout(7, false));
		getShell().setText(Messages.VanzariIncarcaDocDialog_Title);
		
		infoArea = new Text(container, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		final GridData infoAreaGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		infoAreaGD.heightHint = BUTTON_HEIGHT;
		infoAreaGD.minimumWidth = 120;
		infoArea.setLayoutData(infoAreaGD);
		
		docsTable = new AccDocsNatTable();
		docsTable.postConstruct(container);
		docsTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		final GridData docsTableGD = new GridData(TABLE_WIDTH, BUTTON_HEIGHT-10);
		docsTable.getTable().setLayoutData(docsTableGD);
		
		incarca = new Button(container, SWT.PUSH | SWT.WRAP);
		incarca.setText(Messages.VanzariIncarcaDocDialog_Load);
		final GridData incarcaGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		incarca.setLayoutData(incarcaGD);
		incarca.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		incarca.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		setFont(incarca);
		
		final Composite printContainer = new Composite(container, SWT.NONE);
		printContainer.setLayout(GridLayoutFactory.swtDefaults().spacing(0, 0).margins(0, 5).create());
		printContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.TOP).applyTo(printContainer);
		
		printare = new Button(printContainer, SWT.PUSH | SWT.WRAP);
		printare.setText(Messages.VanzariIncarcaDocDialog_Print);
		final GridData printareGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT*2/3);
		printare.setLayoutData(printareGD);
		printare.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		printare.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		setFont(printare);
		
		printareFaraDisc = new Button(printContainer, SWT.PUSH | SWT.WRAP);
		printareFaraDisc.setText(Messages.VanzariIncarcaDocDialog_PrintDocNoDisc);
		final GridData printareFaraDiscGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT/3);
		printareFaraDisc.setLayoutData(printareFaraDiscGD);
		printareFaraDisc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		printareFaraDisc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		setFont(printareFaraDisc);
		
		printCasaMarcat = new Button(container, SWT.PUSH | SWT.WRAP);
		printCasaMarcat.setText(Messages.VanzariIncarcaDocDialog_ReprintReceipt);
		final GridData printCasaMarcatGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		printCasaMarcat.setLayoutData(printCasaMarcatGD);
		printCasaMarcat.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		printCasaMarcat.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		setFont(printCasaMarcat);
		
		inchide = new Button(container, SWT.PUSH | SWT.WRAP);
		inchide.setText(Messages.VanzariIncarcaDocDialog_Close);
		final GridData inchideGD = new GridData(BUTTON_WIDTH, BUTTON_HEIGHT);
		inchide.setLayoutData(inchideGD);
		inchide.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		inchide.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		setFont(inchide);
		
		loadData();
		addListeners();
		return container;
	}
	
	private void loadData()
	{
		BusinessDelegate.bonuriZiuaCurenta(new AsyncLoadData<AccountingDocument>()
		{
			@Override public void success(final ImmutableList<AccountingDocument> data)
			{
				docsTable.loadData(data);
				
				final BigDecimal total = data.stream()
						.filter(accDoc -> globalIsMatch(accDoc.getDoc(), AccountingDocument.BON_CONSUM_NAME, TextFilterMethod.NOT_EQUALS))
						.map(AccountingDocument::getTotal)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
				final BigDecimal totalFacturi = data.stream()
						.filter(accDoc -> globalIsMatch(accDoc.getDoc(), AccountingDocument.FACTURA_NAME, TextFilterMethod.EQUALS))
						.map(AccountingDocument::getTotal)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
				final BigDecimal totalAvize = data.stream()
						.filter(accDoc -> globalIsMatch(accDoc.getDoc(), AccountingDocument.AVIZ_NAME, TextFilterMethod.EQUALS))
						.map(AccountingDocument::getTotal)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
				final BigDecimal totalPrinCasa = data.stream()
						.filter(accDoc -> globalIsMatch(accDoc.getDoc(), AccountingDocument.BON_CASA_NAME, TextFilterMethod.EQUALS))
						.map(AccountingDocument::getTotal)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
				final BigDecimal totalProcese = data.stream()
						.filter(accDoc -> globalIsMatch(accDoc.getDoc(), AccountingDocument.PROCES_VERBAL_NAME, TextFilterMethod.EQUALS))
						.map(AccountingDocument::getTotal)
						.reduce(BigDecimal::add)
						.orElse(BigDecimal.ZERO);
				
				infoArea.setText(MessageFormat.format(Messages.VanzariIncarcaDocDialog_Info, NEWLINE, displayLocalDate(LocalDate.now()),
						displayBigDecimal(total), displayBigDecimal(totalPrinCasa), displayBigDecimal(totalFacturi), displayBigDecimal(totalAvize),
						displayBigDecimal(totalProcese)));
			}

			@Override public void error(final String details)
			{
				MessageDialog.openError(getShell(), Messages.VanzariIncarcaDocDialog_ErrorLoading, details);
			}
		}, sync);
	}
	
	private void addListeners()
	{
		incarca.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final List<AccountingDocument> selection = ((RowSelectionModel<AccountingDocument>) docsTable.getSelectionLayer().getSelectionModel())
						.getSelectedRowObjects();
				
				if (!selection.isEmpty())
				{
					vanzarePart.updateBonCasa(BusinessDelegate.reloadDoc(selection.get(0)), true);
					bonIncarcat = true;
				}
			}
		});
		
		printare.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					printDoc();
				}
				catch (IOException | JRException ex)
				{
					vanzarePart.log().error(ex);
					MessageDialog.openError(getShell(), Messages.VanzariIncarcaDocDialog_ErrorPrinting, ex.getMessage());
				}
			}
		});
		
		printareFaraDisc.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				try
				{
					printDocFaraDisc();
				}
				catch (IOException | JRException ex)
				{
					vanzarePart.log().error(ex);
					MessageDialog.openError(getShell(), Messages.VanzariIncarcaDocDialog_ErrorPrinting, ex.getMessage());
				}
			}
		});
		
		printCasaMarcat.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				sendToCasaMarcat();
			}
		});
		
		inchide.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				setReturnCode(OK);
				close();
			}
		});
	}
	
	private void printDoc() throws IOException, JRException
	{
		if (!bonIncarcat)
			return;
		
		if (vanzarePart.getBonCasa() == null)
			return;
		
		final AccountingDocument chitantaToPrint = vanzarePart.getBonCasa().getPaidBy().stream()
				.map(AccountingDocumentMapping::getPays)
				.sorted(Comparator.comparing(AccountingDocument::getDataDoc).reversed())
				.findFirst()
				.orElse(null);
		
		final boolean isFactura = globalIsMatch(vanzarePart.getBonCasa().getDoc(), AccountingDocument.FACTURA_NAME, TextFilterMethod.EQUALS);
		final boolean isAviz = globalIsMatch(vanzarePart.getBonCasa().getDoc(), AccountingDocument.AVIZ_NAME, TextFilterMethod.EQUALS);
		if (isFactura || isAviz)
			JasperReportManager.instance(vanzarePart.getBundle(), vanzarePart.log()).printFactura(vanzarePart.getBundle(), vanzarePart.getBonCasa(), chitantaToPrint);
		else
			JasperReportManager.instance(vanzarePart.getBundle(), vanzarePart.log()).printNonOfficialDoc(vanzarePart.getBundle(), vanzarePart.getBonCasa(), false);
	}
	
	private void printDocFaraDisc() throws IOException, JRException
	{
		if (!bonIncarcat)
			return;
		
		if (vanzarePart.getBonCasa() == null)
			return;
		
		final AccountingDocument chitantaToPrint = vanzarePart.getBonCasa().getPaidBy().stream()
				.map(AccountingDocumentMapping::getPays)
				.sorted(Comparator.comparing(AccountingDocument::getDataDoc).reversed())
				.findFirst()
				.orElse(null);
		
		if (vanzarePart.getBonCasa().isOfficialDoc())
			JasperReportManager.instance(vanzarePart.getBundle(), vanzarePart.log())
			.printFactura(vanzarePart.getBundle(), vanzarePart.getBonCasa(), chitantaToPrint);
		else
		{
			final AccountingDocument reloadedBon = BusinessDelegate.reloadDoc(vanzarePart.getBonCasa());
			
			if (reloadedBon == null)
				return;
			
			reloadedBon.setOperatiuni(reloadedBon.getOperatiuni_Stream()
					.filter(op -> !Product.DISCOUNT_CATEGORY.equalsIgnoreCase(op.getCategorie()))
					.collect(toHashSet()));
			
			final ImmutableList<Operatiune> operatiuni = AccountingDocument.extractOperations(reloadedBon);
			reloadedBon.setOperatiuni(new HashSet<Operatiune>(operatiuni));
			
			JasperReportManager.instance(vanzarePart.getBundle(), vanzarePart.log())
			.printNonOfficialDoc(vanzarePart.getBundle(), reloadedBon, false);
		}
	}
	
	private void sendToCasaMarcat()
	{
		if (!bonIncarcat)
			return;
		
		if (vanzarePart.getBonCasa() == null)
			return;
		
		final boolean isBonCasa = globalIsMatch(vanzarePart.getBonCasa().getDoc(), AccountingDocument.BON_CASA_NAME, TextFilterMethod.EQUALS);
		if (isBonCasa)
		{
			final int buttonIndex = MessageDialog.open(MessageDialog.QUESTION, getShell(), Messages.VanzariIncarcaDocDialog_ReprintReceipt,
					Messages.VanzariIncarcaDocDialog_PaymentType, SWT.NONE, Messages.VanzariIncarcaDocDialog_Cash, Messages.VanzariIncarcaDocDialog_Card);
			
			if(buttonIndex == -1)
				return;
			
			final boolean incasarePrinCard = buttonIndex == 1;
			ecrService.printReceipt(AccDocMapper.toReceipt(List.of(vanzarePart.getBonCasa())),
					incasarePrinCard ? PaymentType.CARD : PaymentType.CASH)
			.thenAcceptAsync(new CasaMarcat.UpdateDocStatus(Set.of(vanzarePart.getBonCasa().getId()), true));
		}
	}

	@Override
	protected Control createButtonBar(final Composite parent)
	{
		buttonBar = super.createButtonBar(parent);
		final GridData gd = new GridData();
		gd.exclude = true;
		buttonBar.setLayoutData(gd);
		return buttonBar;
	}
}
