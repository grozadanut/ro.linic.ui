package ro.linic.ui.legacy.wizards;

import static ro.linic.ui.legacy.session.UIUtils.showResult;

import java.awt.Desktop;
import java.io.File;

import org.eclipse.e4.core.services.log.Logger;

import ro.colibri.base.ReplacingAddressExtractor;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.exporters.XmlUbl21FileExporter;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;

public class EFacturaFileWizard extends EFacturaBaseWizard
{
	private String filepath;
	
	public EFacturaFileWizard(final Logger log, final AccountingDocument docIncarcat, final String filepath)
	{
		super(log, docIncarcat);
		this.filepath = filepath;
	}

	@Override
	public boolean performFinish()
	{
		final InvocationResult firmaDetails = BusinessDelegate.firmaDetails();
		final String custId = BusinessDelegate.persistedProp(PersistedProp.E_FACTURA_CUST_ID_KEY)
				.getValueOr(PersistedProp.E_FACTURA_CUST_ID_DEFAULT);
		
		final XmlUbl21FileExporter exporter = new XmlUbl21FileExporter(custId, firmaDetails.extraString(PersistedProp.SERIA_FACTURA_KEY),
				firmaDetails.extraString(PersistedProp.FIRMA_NAME_KEY), firmaDetails.extraString(PersistedProp.FIRMA_CUI_KEY),
				firmaDetails.extraString(PersistedProp.FIRMA_CAP_SOCIAL_KEY), one().address(), one().city(), one().codJudet(),
				one().phone(), one().email(), one().iban(), new ReplacingAddressExtractor<>(two().getAddress()), filepath);

		final InvocationResult result = exporter.serialize(docIncarcat);
		showResult(result);
		if (result.statusOk())
		{
			try
			{
				final File dirToOpen = new File(filepath).getParentFile();
				if (Desktop.isDesktopSupported() && dirToOpen != null)
					Desktop.getDesktop().open(dirToOpen);
			}
			catch (final Exception e)
			{
				log.error(e);
			}
		}
		
		return true;
	}
}
