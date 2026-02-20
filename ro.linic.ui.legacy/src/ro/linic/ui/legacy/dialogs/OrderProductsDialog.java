package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.linic.ui.legacy.session.UIUtils.showException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import net.sf.jasperreports.engine.JRException;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.base.dialogs.InfoDialog;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.parts.ApproveRequirementsPart;
import ro.linic.ui.legacy.service.JasperReportManager;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.security.services.AuthenticationSession;

public class OrderProductsDialog extends TitleAreaDialog {
	// MODEL
	private EventList<GenericValue> allSuppliers = GlazedLists.eventListOf();
	private EventList<GenericValue> allChannels = GlazedLists.eventListOf();
	private List<GenericValue> requirements;
	private ImmutableList<Gestiune> allGestiuni;

	private Combo supplier;
	private Combo channel;

	private AuthenticationSession authSession;
	private UISynchronize sync;
	private DataServices dataServices;
	private Bundle bundle;
	private Logger log;

	public OrderProductsDialog(final Shell parent, final AuthenticationSession authSession, final UISynchronize sync, final DataServices dataServices,
			final Bundle bundle, final Logger log, final List<GenericValue> requirements) {
		super(parent);
		this.authSession = authSession;
		this.sync = sync;
		this.dataServices = dataServices;
		this.bundle = bundle;
		this.log = log;
		this.requirements = requirements;
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		setTitle(Messages.OrderProductsDialog_Title);
		allGestiuni = BusinessDelegate.allGestiuni();
		return contents;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);

		final Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label supplierLabel = new Label(container, SWT.NONE);
		supplierLabel.setText(Messages.OrderProductsDialog_Supplier);
		GridDataFactory.swtDefaults().applyTo(supplierLabel);

		supplier = new Combo(container, SWT.DROP_DOWN);
		UIUtils.setFont(supplier);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(supplier);
		
		final Label channelLabel = new Label(container, SWT.NONE);
		channelLabel.setText(Messages.OrderProductsDialog_Channel);
		GridDataFactory.swtDefaults().applyTo(channelLabel);
		
		channel = new Combo(container, SWT.DROP_DOWN);
		UIUtils.setFont(channel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(channel);

		fillFields();
		addListeners();
		return area;
	}

	private void fillFields() {
		reloadPartners();
		final Optional<String> mostCommonSupplierName = requirements.stream().map(gv -> gv.getString(Product.FURNIZORI_FIELD))
				.flatMap(names -> Arrays.stream(names.split(", ")))
				.map(name -> name.replaceAll("\\(.*\\)", ""))
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
				.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Entry::getKey);

		if (mostCommonSupplierName.isPresent())
			allSuppliers.stream()
			.filter(p -> p.getString("organizationName").equalsIgnoreCase(mostCommonSupplierName.get()))
			.findFirst()
			.ifPresent(s -> supplier.select(allSuppliers.indexOf(s)));
		reloadChannels();
	}
	
	private void reloadPartners() {
		allSuppliers.addAll(RestCaller.get("/rest/s1/moqui-linic-legacy/suppliers")
				.internal(authSession.authentication())
				.sync(t -> UIUtils.showException(t, sync))
				.stream()
				.sorted(Comparator.comparing(gv -> gv.getString("organizationName")))
				.toList());

		supplier.setItems(allSuppliers.stream().map(gv -> gv.getString("organizationName")).toArray(String[]::new));
	}
	
	private void reloadChannels() {
		allChannels.clear();
		if (supplier().isEmpty()) {
			channel.setItems();
			return;
		}
		
		allChannels.addAll(RestCaller.get("/rest/s1/moqui-linic-legacy/orderChannels")
				.internal(authSession.authentication())
				.addUrlParam("supplierId", supplier().get().getString("partyId"))
				.sync(t -> UIUtils.showException(t, sync)));
		channel.setItems(allChannels.stream().map(gv -> gv.getString("channelName")).toArray(String[]::new));
		
		if (allChannels.size() == 1)
			channel.select(0);
		else
			allChannels.stream()
			.filter(gv -> gv.getString("channelId").equalsIgnoreCase("transfer"))
			.findFirst()
			.ifPresent(transfer -> channel.select(allChannels.indexOf(transfer)));
	}
	
	private void addListeners() {
		supplier.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> reloadChannels()));
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 350);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.PROCEED_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		if (supplier().isEmpty()) {
			setErrorMessage(Messages.OrderProductsDialog_SupplierMissingError);
			return;
		}
		if (channel().isEmpty()) {
			setErrorMessage(Messages.OrderProductsDialog_ChannelMissingError);
			return;
		}
		
		if (channel().get().getString("channelId").equalsIgnoreCase("print")) 
			printRequirements();
		else if (channel().get().getString("channelId").equalsIgnoreCase("transfer"))
			createTransfer();
		else if (channel().get().getString("channelId").equalsIgnoreCase("whatsapp"))
			sendOrder();

		super.okPressed();
	}

	private void printRequirements() {
		if (InfoDialog.open(getParentShell(), Messages.OrderProductsDialog_Title, 
				requirements.stream()
				.map(gv -> MessageFormat.format("{0} \t {1} {2}", 
						gv.getString(Product.NAME_FIELD),
						gv.getBigDecimal("quantityTotal"),
						gv.getString(Product.UOM_FIELD)))
				.collect(Collectors.joining(PresentationUtils.NEWLINE))) == Window.OK) {
			for (final GenericValue req : requirements) {
				final Map<String, Object> body = Map.of("facilityId", ClientSession.instance().getLoggedUser().getSelectedGestiune().getImportName(),
						"requirementTypeEnumId", "RqTpInventory",
						"productId", req.getString(Product.ID_FIELD),
						"description", supplier().get().getString("organizationName"),
						"statusId", "RqmtStOrdered");
				
				RestCaller.put("/rest/s1/moqui-linic-legacy/requirements/status")
				.internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(body)))
				.sync(GenericValue.class, t -> UIUtils.showException(t, sync))
				.ifPresent(r -> dataServices.holder(ApproveRequirementsPart.DATA_HOLDER).getData().remove(req));
			}
		}
	}
	
	private void createTransfer() {
		// select OtherGest
		Integer otherGestiuneId = null;
		final ImmutableList<Gestiune> gestiuni = allGestiuni.stream()
				.filter(g -> !g.equals(ClientSession.instance().getGestiune()))
				.collect(toImmutableList());
		if (gestiuni.size() == 1)
			otherGestiuneId = gestiuni.get(0).getId();
		else
		{
			final SelectEntityDialog<Gestiune> gestiuneDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
					ro.linic.ui.legacy.parts.Messages.Transfer, ro.linic.ui.legacy.parts.Messages.ManagerPart_SelectInventory,
					ro.linic.ui.legacy.parts.Messages.ManagerPart_Inventory, gestiuni, ro.linic.ui.legacy.parts.Messages.OK,
					ro.linic.ui.legacy.parts.Messages.Cancel);
			final int dialogResult = gestiuneDialog.open();

			if (dialogResult != 0)
				throw new UnsupportedOperationException("No Gestiune selected");

			otherGestiuneId = gestiuneDialog.selectedEntity().map(Gestiune::getId).get();
		}

		final Map<String, Object> body = Map.of("facilityId", ClientSession.instance().getGestiune().getImportName(),
				"channel", channel().get(),
				"requirements", requirements,
				"otherGestiuneId", otherGestiuneId);
		
		RestCaller.post("/rest/s1/moqui-linic-legacy/order")
		.internal(authSession.authentication())
		.body(BodyProvider.of(HttpUtils.toJSON(body)))
		.sync(GenericValue.class, t -> UIUtils.showException(t, sync))
		.ifPresent(r -> {
			dataServices.holder(ApproveRequirementsPart.DATA_HOLDER).remove(requirements);
			try {
				final long transferAccDocId = r.getLong("transferId");
				JasperReportManager.instance(bundle, log).printReceptie(bundle, BusinessDelegate.reloadDoc(transferAccDocId));
			} catch (IOException | JRException ex) {
				log.error(ex);
				showException(ex);
			}
		});
	}
	
	private void sendOrder() {
		final Map<String, Object> body = Map.of("facilityId", ClientSession.instance().getGestiune().getImportName(),
				"channel", channel().get(),
				"supplier", supplier().get(),
				"requirements", requirements);
		
		RestCaller.post("/rest/s1/moqui-linic-legacy/order")
		.internal(authSession.authentication())
		.body(BodyProvider.of(HttpUtils.toJSON(body)))
		.sync(GenericValue.class, t -> UIUtils.showException(t, sync))
		.ifPresent(r -> dataServices.holder(ApproveRequirementsPart.DATA_HOLDER).remove(requirements));
	}

	private Optional<GenericValue> supplier() {
		final int index = supplier.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.of(allSuppliers.get(index));
	}
	
	private Optional<GenericValue> channel() {
		final int index = channel.getSelectionIndex();
		if (index == -1)
			return Optional.empty();

		return Optional.of(allChannels.get(index));
	}
}
