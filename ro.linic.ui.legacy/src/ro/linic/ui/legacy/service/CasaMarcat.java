package ro.linic.ui.legacy.service;

import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.ImmutableSet;

import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.pos.base.services.ECRDriver.Result;

public class CasaMarcat
{
	public static class UpdateDocStatus implements Consumer<Result>
	{
		private Set<Long> docIds;
		private boolean retriedBon;
		
		public UpdateDocStatus(final Set<Long> docIds, final boolean retriedBon)
		{
			this.docIds = docIds;
			this.retriedBon = retriedBon;
		}

		@Override
		public void accept(final Result result) {
			if (!result.isOk())
				Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getCurrent().getActiveShell(), "Eroare Casa Marcat", result.error()));

			if (!result.isOk())
				BusinessDelegate.closeBonCasa_Failed(ImmutableSet.copyOf(docIds));
			else if (retriedBon)
				BusinessDelegate.closeBonCasa_RetrySuccess(ImmutableSet.copyOf(docIds));
		}
	}
}
