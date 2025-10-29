package ro.linic.ui.legacy.parts;

import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MAX;
import static ro.colibri.util.LocalDateUtils.POSTGRES_MIN;
import static ro.colibri.util.PresentationUtils.NEWLINE;
import static ro.colibri.util.ServerConstants.GENERAL_TOPIC_REMOTE_JNDI;
import static ro.colibri.util.ServerConstants.JMS_USERS_KEY;
import static ro.colibri.util.StringUtils.isEmpty;
import static ro.linic.ui.legacy.session.UIUtils.loadState;
import static ro.linic.ui.legacy.session.UIUtils.saveState;
import static ro.linic.ui.legacy.session.UIUtils.showException;
import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.user.User;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.RulajPartener;
import ro.linic.ui.legacy.JMSGeneralTopicHandler;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.dialogs.PrintBarcodeDialog;
import ro.linic.ui.legacy.dialogs.ReplaceUserDialog;
import ro.linic.ui.legacy.inport.ExcelImportTransformer;
import ro.linic.ui.legacy.inport.ImportParseException;
import ro.linic.ui.legacy.service.components.BarcodePrintable;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.MessagingService;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.legacy.tables.PersistedPropNatTable;

public class SysAdminPart
{
	public static final String PART_ID = "linic_gest_client.part.sys_admin";
	
	private static final String PERSISTED_PROP_TABLE_STATE_PREFIX = "sysadmin.persisted_prop_nt";
	
	private Text jobs;
	private Button importPartners;
	private Button importClients;
	private Button importAccDocs;
	private Button importOperations;
	private Button importProducts;
	private Button importDiscountDocs;
	private Button verifyRulaje;
	private Button printBarcodes;
	private Button updateFurnizori;
	private Button requestLogs;
	private Button refresh;
	private PersistedPropNatTable persistedPropTable;
	
	private List users;
	private Button replaceUser;
	private Button fixStocuri;
	
	private ImmutableList<User> allUsers;
	
	@Inject private MPart part;
	@Inject private Logger log;
	@Inject private UISynchronize sync;
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private IEclipseContext ctx;
	
	@PostConstruct
	public void createComposite(final Composite parent)
	{
		allUsers = BusinessDelegate.dbUsers();
		
		final ScrolledComposite scrollable = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		final Composite container = new Composite(scrollable, SWT.NONE);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		scrollable.setContent(container);
		
		jobs = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		jobs.setText(BusinessDelegate.importJobs(log));
		UIUtils.setFont(jobs);
		GridDataFactory.swtDefaults().hint(800, 200).applyTo(jobs);
		
		importPartners = new Button(container, SWT.PUSH);
		importPartners.setText("Importa Parteneri");
		UIUtils.setFont(importPartners);
		
		importClients = new Button(container, SWT.PUSH);
		importClients.setText("Importa Mesteri");
		UIUtils.setFont(importClients);
		
		importAccDocs = new Button(container, SWT.PUSH);
		importAccDocs.setText("Importa Acc Docs");
		UIUtils.setFont(importAccDocs);
		
		importProducts = new Button(container, SWT.PUSH);
		importProducts.setText("Importa Produse");
		UIUtils.setFont(importProducts);
		
		importOperations = new Button(container, SWT.PUSH);
		importOperations.setText("Importa Operatiuni");
		UIUtils.setFont(importOperations);
		
		importDiscountDocs = new Button(container, SWT.PUSH);
		importDiscountDocs.setText("Importa Discount Docs");
		UIUtils.setFont(importDiscountDocs);
		
		verifyRulaje = new Button(container, SWT.PUSH);
		verifyRulaje.setText("Verifica Rulaje");
		UIUtils.setFont(verifyRulaje);
		
		printBarcodes = new Button(container, SWT.PUSH);
		printBarcodes.setText("Printeaza etichete din fisier");
		printBarcodes.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		printBarcodes.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		printBarcodes.setToolTipText("BARCODE, NAME, UOM, PV");
		UIUtils.setFont(printBarcodes);
		
		updateFurnizori = new Button(container, SWT.PUSH);
		updateFurnizori.setText("Update product furnizori to latest");
		updateFurnizori.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		updateFurnizori.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(updateFurnizori);
		
		requestLogs = new Button(container, SWT.PUSH);
		requestLogs.setText("Get user Logs");
		requestLogs.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		requestLogs.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setFont(requestLogs);
		
		users = new List(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		users.setItems(allUsers.stream().map(User::displayName).toArray(String[]::new));
		UIUtils.setFont(users);
		GridDataFactory.swtDefaults().hint(800, 200).applyTo(users);
		
		fixStocuri = new Button(container, SWT.PUSH);
		fixStocuri.setText("Sterge stocuri(persistGestiune bug)");
		fixStocuri.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		fixStocuri.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(fixStocuri);
		
		replaceUser = new Button(container, SWT.PUSH);
		replaceUser.setText("Inlocuieste Utilizator");
		replaceUser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		replaceUser.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		UIUtils.setBoldFont(replaceUser);
		
		persistedPropTable = new PersistedPropNatTable();
		persistedPropTable.afterChange(op -> part.setDirty(true));
		persistedPropTable.postConstruct(container);
		persistedPropTable.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200).applyTo(persistedPropTable.getTable());
		loadState(PERSISTED_PROP_TABLE_STATE_PREFIX, persistedPropTable.getTable(), part);
		persistedPropTable.loadData(BusinessDelegate.allPersistedProps());
		
		createBottom(container);
		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		addListeners();
	}
	
	private void createBottom(final Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		
		refresh = new Button(container, SWT.PUSH);
		refresh.setText("Refresh");
		UIUtils.setBoldFont(refresh);
	}
	
	@PersistState
	public void persistVisualState()
	{
		saveState(PERSISTED_PROP_TABLE_STATE_PREFIX, persistedPropTable.getTable(), part);
	}
	
	@Persist
	public void onSave()
	{
		if (part.isDirty())
		{
			BusinessDelegate.mergePersistedProps(persistedPropTable.getDataChangeLayer().getDataChanges().stream()
					.map(dataChange -> (IdIndexIdentifier<PersistedProp>)dataChange.getKey())
					.map(key -> key.rowObject)
					.distinct()
					.collect(toImmutableSet()));
			part.setDirty(false);
			refresh();
		}
	}
	
	private void addListeners()
	{
		importPartners.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importPartners(filePath);
			}
		});
		
		importClients.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importClients(filePath);
			}
		});
		
		importAccDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importAccDocs(filePath);
			}
		});
		
		importOperations.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importOperations(filePath);
			}
		});
		
		importProducts.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importProducts(filePath);
			}
		});
		
		importDiscountDocs.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					importDiscountDocs(filePath);
			}
		});
		
		verifyRulaje.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					verifyRulaje(filePath);
			}
		});
		
		printBarcodes.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xlsx", "*.xls"});
				final String filePath = dialog.open();
				if (!isEmpty(filePath) && Files.exists(Paths.get(filePath)))
					printBarcodes(filePath);
			}
		});
		
		updateFurnizori.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (!MessageDialog.openConfirm(updateFurnizori.getShell(), "Actualizati furnizorii?", 
						"Aceasta operatiune va actualiza produsele, inlocuind campul Furnizori cu furnizorul de la ultima factura, "
						+ "pentru produsele care au 2 sau mai multi furnizori. Continuati?"))
					return;
				
				final InvocationResult result = BusinessDelegate.updateProductFurnizoriToLatest();
				showResult(result);
				if (result.statusOk())
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Succes!",
							MessageFormat.format("Au fost actualizate {0} produse.", result.extraLong(InvocationResult.UPDATE_COUNT_KEY)));
			}
		});
		
		requestLogs.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(final SelectionEvent e) {
				if (selectedUser().isPresent())
					MessagingService.instance().sendMsg(GENERAL_TOPIC_REMOTE_JNDI, JMSGeneralTopicHandler.JMSMSGTYPE_LOG_REQUEST, 
							ImmutableMap.of(JMS_USERS_KEY, selectedUser().get().getId()+"",
									JMSGeneralTopicHandler.REPLY_TO, ClientSession.instance().getLoggedUser().getId()+""), "");
			}
		});
		
		fixStocuri.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (MessageDialog.openConfirm(fixStocuri.getShell(), "Sterge stocuri gresite", 
						"Atentie! Aceasta operatiune va sterge conexiunile de la produse la stocurile din alta companie, "
						+ "create de catre o eroare de programare in ManagerBean.persistGestiune. "
						+ "Asigurati-va ca a-ti facut backup la baza de date prima data. Continuati?"))
				{
					final InvocationResult result = BusinessDelegate.fixStocuriAltaCompanie();
					if (result.statusCanceled())
						showResult(result);
					else
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "OK", 
								"Deleted connections number: "+result.extraLong(InvocationResult.UPDATE_COUNT_KEY));
				}
			}
		});
		
		replaceUser.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				if (selectedUser().isPresent())
				{
					new ReplaceUserDialog(replaceUser.getShell(), selectedUser().get(), allUsers).open();
					refresh();
				}
			}
		});
		
		refresh.addSelectionListener(new SelectionAdapter()
		{
			@Override public void widgetSelected(final SelectionEvent e)
			{
				refresh();
			}
		});
	}
	
	private void refresh()
	{
		askSave();
		part.setDirty(false);
		jobs.setText(BusinessDelegate.importJobs(log));
		allUsers = BusinessDelegate.dbUsers();
		users.setItems(allUsers.stream().map(User::displayName).toArray(String[]::new));
		persistedPropTable.loadData(BusinessDelegate.allPersistedProps());
	}
	
	private void askSave()
	{
		if (part.isDirty() && MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Salveaza", "Salvati modificarile facute?"))
			onSave();
	}
	
	private void importPartners(final String filePath)
	{
		try
		{
			final ImmutableSet<Partner> partners = ExcelImportTransformer.toPartners(Files.newInputStream(Paths.get(filePath)));
			BusinessDelegate.importPartners(partners);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void importClients(final String filePath)
	{
		try
		{
			final ImmutableSet<Partner> mesteri = ExcelImportTransformer.toMesteri(Files.newInputStream(Paths.get(filePath)));
			BusinessDelegate.importMesteri(mesteri);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void importAccDocs(final String filePath)
	{
		try
		{
			final ImmutableList<AccountingDocument> accDocs = ExcelImportTransformer.toAccDocs(Files.newInputStream(Paths.get(filePath)));
			BusinessDelegate.importAccDocs(accDocs);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void importProducts(final String filePath)
	{
		try
		{
			final ImmutableSet<Product> products = ExcelImportTransformer.toProducts(Files.newInputStream(Paths.get(filePath)),
					BusinessDelegate.allGestiuni());
			BusinessDelegate.importProducts(products);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void importOperations(final String filePath)
	{
		try
		{
			final ImmutableSet<Operatiune> ops = ExcelImportTransformer.toOperations(Files.newInputStream(Paths.get(filePath)));
			BusinessDelegate.importOperations(ops);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void importDiscountDocs(final String filePath)
	{
		try
		{
			final ImmutableSet<DocumentWithDiscount> discDocs = ExcelImportTransformer.toClientDocs(Files.newInputStream(Paths.get(filePath)));
			BusinessDelegate.importDiscountDocs(discDocs);
			refresh();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void verifyRulaje(final String filePath)
	{
		try
		{
			final Map<String, java.util.List<RulajPartener>> rulajeOld = ExcelImportTransformer.toRulaje(Files.newInputStream(Paths.get(filePath))).stream()
					.collect(Collectors.groupingBy(RulajPartener::getName));
			BusinessDelegate.rulajeParteneri(new AsyncLoadData<RulajPartener>()
			{
				@Override public void success(final ImmutableList<RulajPartener> data)
				{
					final Map<String, java.util.List<RulajPartener>> rulajeNoi = data.stream()
							.collect(Collectors.groupingBy(RulajPartener::getName));
					
					final java.util.List<String> errors = new ArrayList<String>();
					
					rulajeOld.entrySet().forEach(entry ->
					{
						if (rulajeNoi.get(entry.getKey()) == null)
						{
							errors.add(entry.getKey()+" not found");
							return;
						}
						
						final Optional<RulajPartener> vechi = RulajPartener.addSums(entry.getValue());
						final Optional<RulajPartener> nou = RulajPartener.addSums(rulajeNoi.get(entry.getKey()));
						
						if (!RulajPartener.equalsWithoutDisc(nou.orElse(null), vechi.orElse(null)))
							errors.add(entry.getKey()+NEWLINE+"VECHI: "+vechi+NEWLINE+"NOU: "+nou);
					});
					
					if (errors.isEmpty())
						jobs.setText("Success - Nicio diferenta");
					else
						jobs.setText(errors.stream().collect(Collectors.joining(NEWLINE)));
				}
				
				@Override
				public void error(final String details)
				{
					jobs.setText("Eroare "+details);
				}
			}, sync, null, null, POSTGRES_MIN.toLocalDate(), POSTGRES_MAX.toLocalDate(), log);
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private void printBarcodes(final String filePath)
	{
		try
		{
			final ImmutableSet<Product> products = ExcelImportTransformer.toShortProducts(Files.newInputStream(Paths.get(filePath)));
			final ImmutableList<BarcodePrintable> printables = BarcodePrintable.fromProducts(ctx, products);
			
			if (!printables.isEmpty())
				new PrintBarcodeDialog(printBarcodes.getShell(), printables, log, bundle).open();
		}
		catch (EncryptedDocumentException | InvalidFormatException | ImportParseException | IOException e)
		{
			log.error(e);
			showException(e);
		}
	}
	
	private Optional<User> selectedUser()
	{
		final int index = users.getSelectionIndex();
		if (index == -1)
			return Optional.empty();
		
		return Optional.of(allUsers.get(index));
	}
}
