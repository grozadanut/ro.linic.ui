/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package ro.linic.ui.legacy.session;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages
 */
public class Messages extends NLS
{
	private static final String BUNDLE_NAME = Messages.class.getPackageName()+".messages"; //$NON-NLS-0$
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	public static String VAT_Percentage;
	public static String VAT;
	public static String FileSaved;
	public static String AnafReporter_UnauthorizedMessage;
	public static String AnafReporter_ReloadBills;
	public static String AnafReporter_AllSaved;
	public static String AdaugaDocDialog_AddDoc;
	public static String AdaugaDocDialog_Add;
	public static String AdaugaDocDialog_DocType;
	public static String AdaugaDocDialog_DocNumber;
	public static String AdaugaDocDialog_Date;
	public static String AdaugaDocDialog_Due;
	public static String AdaugaDocDialog_NameLabel;
	public static String AdaugaDocDialog_RPZ;
	public static String RegisterZPage_Cafe;
	public static String RegisterZPage_Message;
	public static String RegisterZPage_Standard;
	public static String UrmarireParteneriExtraFilters_AccountingOnly;
	public static String UrmarireParteneriExtraFilters_BankAcct;
	public static String UrmarireParteneriExtraFilters_BankOnly;
	public static String UrmarireParteneriExtraFilters_BetweenLabel;
	public static String UrmarireParteneriExtraFilters_Indifferent;
	public static String UrmarireParteneriExtraFilters_Max;
	public static String UrmarireParteneriExtraFilters_NoAccounting;
	public static String UrmarireParteneriExtraFilters_NoBank;
	public static String UrmarireParteneriExtraFilters_NoTransport;
	public static String UrmarireParteneriExtraFilters_ThisMonth;
	public static String UrmarireParteneriExtraFilters_ThisYear;
	public static String UrmarireParteneriExtraFilters_Today;
	public static String UrmarireParteneriExtraFilters_TransportOnly;
	public static String UrmarireParteneriExtraFilters_UsedOnly;
	public static String UrmarireParteneriExtraFilters_User;
	public static String SyncingSales;
	public static String LegacyReceiptLineUpdater_UpdateQuantitySyncErr;
}
