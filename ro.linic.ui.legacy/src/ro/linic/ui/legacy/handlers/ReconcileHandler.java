package ro.linic.ui.legacy.handlers;

import static ro.colibri.util.StringUtils.isEmpty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import ro.colibri.base.IPresentable;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.security.Permissions;
import ro.colibri.util.HeterogeneousDataComparator;
import ro.colibri.util.LocalDateUtils;
import ro.linic.ui.base.dialogs.SelectEntityDialog;
import ro.linic.ui.base.services.DataServices;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.dialogs.Messages;
import ro.linic.ui.legacy.parts.ReconciliationPart;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.UIUtils;
import ro.linic.ui.security.services.AuthenticationSession;

public class ReconcileHandler {
	@Execute
	public void execute(final EPartService partService, final AuthenticationSession authSession,
			final UISynchronize sync, final DataServices dataServices) throws IOException {
		final SelectEntityDialog<ContBancar> contBancarDialog = new SelectEntityDialog<>(Display.getCurrent().getActiveShell(),
				Messages.ContBancarDialog_Title, Messages.ContBancar_SelectDescription,
				Messages.ContBancarDialog_Title, BusinessDelegate.allConturiBancare(), IPresentable::displayName,
				IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL);
		final int dialogResult = contBancarDialog.open();

		if (dialogResult != 0)
			return;

		final int contBancarId = contBancarDialog.selectedEntity().map(ContBancar::getId).get();
		
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
		final String selectedFileUri = dialog.open();

		if (isEmpty(selectedFileUri))
			return;

		final Comparator<GenericValue> dateComparator = Comparator.<GenericValue, LocalDate>comparing(gv -> {
			final List<Map> left = (List<Map>) gv.getChild("result").getChild("match").get("left");
			final List<Map> right = (List<Map>) gv.getChild("result").getChild("match").get("right");
			return left.stream().findFirst()
					.map(nr -> nr.get("fields"))
					.map(f -> LocalDateUtils.parse((String) ((Map) f).get("dataDoc")))
					.orElse(right.stream().findFirst()
							.map(nr -> nr.get("fields"))
							.map(f -> LocalDateUtils.parse((String) ((Map) f).get("dataDoc")))
							.orElse(null));
		}, HeterogeneousDataComparator.INSTANCE);

		final Map<String, Object> body = Map.of("mt940", Files.readString(Path.of(selectedFileUri)),
				"gestiuneId", ClientSession.instance().getGestiune().getId(),
				"contBancarId", contBancarId);

		RestCaller.post("/rest/s1/moqui-linic-legacy/reconcile").internal(authSession.authentication())
				.body(BodyProvider.of(HttpUtils.toJSON(body)))
				.async(t -> UIUtils.showException(t, sync))
				.thenAccept(result -> dataServices.holder(ReconciliationPart.DATA_HOLDER).setData(result.stream().sorted(dateComparator).toList()))
				.thenRun(() -> sync.asyncExec(() -> partService.showPart(ReconciliationPart.PART_ID, PartState.ACTIVATE)));
	}

	@CanExecute
	boolean canExecute(final EPartService partService) {
		return ClientSession.instance().hasStrictPermission(Permissions.SYSADMIN_ROLE);
	}
}
