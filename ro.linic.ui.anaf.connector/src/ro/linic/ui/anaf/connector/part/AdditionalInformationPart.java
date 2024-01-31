package ro.linic.ui.anaf.connector.part;

import javax.annotation.PostConstruct;

import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class AdditionalInformationPart {
	public static final String PART_DESCRIPTOR_ID = "ro.linic.ui.anaf.connector.partdescriptor.additionalinformation";

    @PostConstruct
    public void postConstruct(final Composite parent) {
        TextFactory.newText(SWT.BORDER |SWT.MULTI).create(parent);
    }
}