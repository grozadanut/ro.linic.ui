package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.ListUtils.addToImmutableList;
import static ro.colibri.util.ListUtils.removeFromImmutableList;
import static ro.colibri.util.ListUtils.toArrayList;
import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableMap;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.safeString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.util.InvocationResult;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.Icons;
import ro.linic.ui.legacy.session.UIUtils;

public class ConexiuniDialog extends TitleAreaDialog
{
	private AccountingDocument accDoc;
	private ImmutableList<AccountingDocument> partnerPaysDocs;
	private ImmutableList<AccountingDocument> partnerPaidDocs;
	
	private ImmutableList<ConnectionWidget> paysWidgets = ImmutableList.of();
	private ImmutableList<ConnectionWidget> paidWidgets = ImmutableList.of();
	
	@Inject @OSGiBundle private Bundle bundle;
	@Inject private Logger log;
	
	public ConexiuniDialog(final Shell parent, final AccountingDocument selectedAccDoc, final ImmutableList<AccountingDocument> partnerAccDocs,
			final Bundle bundle, final Logger log)
	{
		super(parent);
		this.accDoc = BusinessDelegate.reloadDoc(selectedAccDoc, true);
		this.bundle = bundle;
		this.log = log;
		
		final Predicate<AccountingDocument> paysPredicate = qDoc -> 
		{
			if (TipDoc.CUMPARARE.equals(accDoc.getTipDoc()))
				return TipDoc.PLATA.equals(qDoc.getTipDoc());
			else if (TipDoc.VANZARE.equals(accDoc.getTipDoc()))
				return TipDoc.INCASARE.equals(qDoc.getTipDoc());
			return false;
		};
		
		final Predicate<AccountingDocument> paidPredicate = qDoc ->
		{
			if (TipDoc.PLATA.equals(accDoc.getTipDoc()))
				return TipDoc.CUMPARARE.equals(qDoc.getTipDoc());
			else if (TipDoc.INCASARE.equals(accDoc.getTipDoc()))
				return TipDoc.VANZARE.equals(qDoc.getTipDoc());
			return false;
		};
		
		this.partnerPaysDocs = partnerAccDocs.stream()
				.filter(paysPredicate)
				.collect(toImmutableList());
		this.partnerPaidDocs = partnerAccDocs.stream()
				.filter(paidPredicate)
				.collect(toImmutableList());
	}

	@Override
	protected Control createContents(final Composite parent)
	{
		final Control contents = super.createContents(parent);
		setTitle(Messages.ConexiuniDialog_Title);
		setMessage(Messages.ConexiuniDialog_Message);
		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite area = (Composite) super.createDialogArea(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		// Create the ScrolledComposite to scroll horizontally and vertically
	    final ScrolledComposite sc = new ScrolledComposite(area, SWT.H_SCROLL | SWT.V_SCROLL);
	    GridDataFactory.fillDefaults().grab(true, true).applyTo(sc);
	    // Create a child composite to hold the controls
	    final Composite container = new Composite(sc, SWT.NONE);
		container.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		sc.setContent(container);
		
		UIUtils.setFont(new Label(container, SWT.NONE)).setText(Messages.ConexiuniDialog_Partner + accDoc.getPartner().displayName());
		UIUtils.setFont(new Label(container, SWT.NONE)).setText(Messages.ConexiuniDialog_SelectedDoc + accDoc.displayName());

		if (TipDoc.CUMPARARE.equals(accDoc.getTipDoc()) || TipDoc.VANZARE.equals(accDoc.getTipDoc()))
		{
			UIUtils.setFont(new Label(container, SWT.NONE)).setText(Messages.ConexiuniDialog_PaidBy);
			accDoc.getPaidBy().forEach(accDocMapping ->
			{
				final int index = partnerPaysDocs.indexOf(accDocMapping.getPays());
				final ArrayList<String> paysDocsSelections = paysDocsSelections().collect(toArrayList());
				if (index == -1)
					paysDocsSelections.add(accDocMapping.getPays().displayName());
				final ConnectionWidget widget = new ConnectionWidget(container, false, paysDocsSelections, accDocMapping.getPays().getId());
				widget.update(index == -1 ? paysDocsSelections.size()-1 : index, accDocMapping.getTotal());
				paysWidgets = addToImmutableList(paysWidgets, widget);
			});
			
			final ConnectionWidget widget = new ConnectionWidget(container, false, paysDocsSelections().collect(toImmutableList()), null);
			paysWidgets = addToImmutableList(paysWidgets, widget);
		}
		else if (TipDoc.PLATA.equals(accDoc.getTipDoc()) || TipDoc.INCASARE.equals(accDoc.getTipDoc()))
		{
			UIUtils.setFont(new Label(container, SWT.NONE)).setText(Messages.ConexiuniDialog_Pays);
			accDoc.getPaidDocs().forEach(accDocMapping ->
			{
				final int index = partnerPaidDocs.indexOf(accDocMapping.getPaid());
				final ArrayList<String> paidDocsSelections = paidDocsSelections().collect(toArrayList());
				if (index == -1)
					paidDocsSelections.add(accDocMapping.getPaid().displayName());
				final ConnectionWidget widget = new ConnectionWidget(container, true, paidDocsSelections, accDocMapping.getPaid().getId());
				widget.update(index == -1 ? paidDocsSelections.size()-1 : index, accDocMapping.getTotal());
				paidWidgets = addToImmutableList(paidWidgets, widget);
			});
			
			final ConnectionWidget widget = new ConnectionWidget(container, true, paidDocsSelections().collect(toImmutableList()), null);
			paidWidgets = addToImmutableList(paidWidgets, widget);
		}
		
		container.setSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return area;
	}
	
	@Override
	protected void okPressed()
	{
		final ImmutableMap<Long, BigDecimal> paysAmounts = paysWidgets.stream()
				.filter(ConnectionWidget::isValid)
				.collect(toImmutableMap(ConnectionWidget::connectedDocId, ConnectionWidget::total));
		final ImmutableMap<Long, BigDecimal> paidAmounts = paidWidgets.stream()
				.filter(ConnectionWidget::isValid)
				.collect(toImmutableMap(ConnectionWidget::connectedDocId, ConnectionWidget::total));
		
		final InvocationResult result = BusinessDelegate.updateAccDocConnections(accDoc.getId(), paysAmounts, paidAmounts);
		
		if (result.statusOk())
			super.okPressed();
		else
			setErrorMessage(result.toTextDescriptionWithCode());
	}
	
	private Stream<String> paysDocsSelections()
	{
		return partnerPaysDocs.stream()
				.map(AccountingDocument::displayName);
	}
	
	private Stream<String> paidDocsSelections()
	{
		return partnerPaidDocs.stream()
				.map(AccountingDocument::displayName);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, Messages.Save, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	private class ConnectionWidget extends Composite
	{
		final private boolean isPaidDoc;
		
		private Combo connectedDoc;
		private Text connectedAmount;
		private Button delete;
		
		private Long docId;
		
		public ConnectionWidget(final Composite parent, final boolean isPaidDoc, final List<String> docSelections, final Long docId)
		{
			super(parent, SWT.NONE);
			setLayout(new GridLayout(3, false));
			this.isPaidDoc = isPaidDoc;
			this.docId = docId;
			
			connectedDoc = new Combo(this, SWT.DROP_DOWN);
			connectedDoc.setItems(docSelections.stream().toArray(String[]::new));
			UIUtils.setFont(connectedDoc);
			GridDataFactory.swtDefaults().hint(400, SWT.DEFAULT).applyTo(connectedDoc);
			
			connectedAmount = new Text(this, SWT.BORDER);
			UIUtils.setFont(connectedAmount);
			GridDataFactory.swtDefaults().hint(100, SWT.DEFAULT).applyTo(connectedAmount);
			
			delete = new Button(this, SWT.PUSH);
			final Optional<Image> trashImg = Icons.createImageResource(bundle, Icons.TRASH_16X16_PATH, ILog.get());
			if (trashImg.isPresent())
				delete.setImage(trashImg.get());
			else
				delete.setText(Messages.Delete);
			
			addListeners();
		}
		
		private void addListeners()
		{
			connectedDoc.addSelectionListener(new SelectionAdapter()
			{
				@Override public void widgetSelected(final SelectionEvent e)
				{
					connectedAmount.setText("0"); //$NON-NLS-1$
				}
			});
			
			delete.addSelectionListener(new SelectionAdapter()
			{
				@Override public void widgetSelected(final SelectionEvent e)
				{
					if (isPaidDoc)
						paidWidgets = removeFromImmutableList(paidWidgets, ConnectionWidget.this);
					else
						paysWidgets = removeFromImmutableList(paysWidgets, ConnectionWidget.this);
					dispose();
				}
			});
		}
		
		public ConnectionWidget update(final int index, final BigDecimal total)
		{
			connectedDoc.select(index);
			connectedAmount.setText(safeString(total, BigDecimal::toString));
			return this;
		}
		
		public boolean isValid()
		{
			return connectedDocId() != null && total().compareTo(BigDecimal.ZERO) != 0;
		}
		
		public Long connectedDocId()
		{
			final int selIndex = connectedDoc.getSelectionIndex();
			
			if (selIndex == connectedDoc.getItemCount()-1 && docId != null)
				return docId;
			
			if (isPaidDoc)
			{
				if (selIndex < 0 || selIndex >= partnerPaidDocs.size())
					return null;
				return partnerPaidDocs.get(selIndex).getId();
			}
			else
			{
				if (selIndex < 0 || selIndex >= partnerPaysDocs.size())
					return null;
				return partnerPaysDocs.get(selIndex).getId();
			}
		}
		
		public BigDecimal total()
		{
			return parse(connectedAmount.getText());
		}
	}
}
