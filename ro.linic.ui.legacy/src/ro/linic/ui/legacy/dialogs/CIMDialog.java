package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.extractPercentage;
import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.safeString;

import java.math.BigDecimal;
import java.util.function.Supplier;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import ro.colibri.entities.comercial.CIM;
import ro.colibri.entities.user.User;
import ro.colibri.util.PresentationUtils;
import ro.linic.ui.legacy.session.UIUtils;

public class CIMDialog extends TitleAreaDialog
{
	private Text baseMonthSalary;
	private Text baseHourSalary;
	private Text tichetMasaValue;
	private Text normaOreSapt;
	private Text saturdayExtraPercent;
	private Text sundayExtraPercent;
	private Text globalDayThreshold;
	private Text globalMonthThreshold;
	private Text globalProfitBrutPercent;
	private Text gestiuneDayThreshold;
	private Text gestiuneMonthThreshold;
	private Text gestiuneProfitBrutPercent;
	private Text teamDayThreshold;
	private Text teamMonthThreshold;
	private Text teamProfitBrutPercent;
	private Text individualDayThreshold;
	private Text individualMonthThreshold;
	private Text individualProfitBrutPercent;
	
	private CIM cim;
	
	private Supplier<Boolean> okPressed;
	
	private Bundle bundle;
	private Logger log;

	public CIMDialog(final Bundle bundle, final Logger log, final Shell parent, final CIM cim)
	{
		super(parent);
		this.cim = cim;
		this.bundle = bundle;
		this.log = log;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite contents = new Composite(parent, SWT.NONE);
		contents.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(contents);
		setTitle("CIM " + safeString(cim, CIM::getUser, User::displayName));
		
		final Label baseMonthSalaryLabel = new Label(contents, SWT.NONE);
		baseMonthSalaryLabel.setText("Salariu de baza/luna (RON)");
		UIUtils.setFont(baseMonthSalaryLabel);
		
		baseMonthSalary = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(baseMonthSalary);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(baseMonthSalary);
		
		final Label baseHourSalaryLabel = new Label(contents, SWT.NONE);
		baseHourSalaryLabel.setText("Salariu de baza/ora (RON)");
		UIUtils.setFont(baseHourSalaryLabel);
		
		baseHourSalary = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(baseHourSalary);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(baseHourSalary);
		
		final Label tichetMasaValueLabel = new Label(contents, SWT.NONE);
		tichetMasaValueLabel.setText("Valoare tichete de masa (RON)");
		UIUtils.setFont(tichetMasaValueLabel);
		
		tichetMasaValue = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(tichetMasaValue);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(tichetMasaValue);
		
		final Label normaOreSaptLabel = new Label(contents, SWT.NONE);
		normaOreSaptLabel.setText("Norma ore/Saptamana");
		UIUtils.setFont(normaOreSaptLabel);
		
		normaOreSapt = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(normaOreSapt);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(normaOreSapt);
		
		final Label saturdayExtraPercentLabel = new Label(contents, SWT.NONE);
		saturdayExtraPercentLabel.setText("Bonus de sambata (%*SalBaza)");
		UIUtils.setFont(saturdayExtraPercentLabel);

		saturdayExtraPercent = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(saturdayExtraPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(saturdayExtraPercent);
		
		final Label sundayExtraPercentLabel = new Label(contents, SWT.NONE);
		sundayExtraPercentLabel.setText("Bonus de duminica (%*SalBaza)");
		UIUtils.setFont(sundayExtraPercentLabel);
		
		sundayExtraPercent = new Text(contents, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(sundayExtraPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sundayExtraPercent);
		
		final Group globalGroup = new Group(contents, SWT.NONE);
		globalGroup.setText("Profit Brut Global");
		globalGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(globalGroup);
		UIUtils.setFont(globalGroup);
		
		final Label globalDayThresholdLabel = new Label(globalGroup, SWT.NONE);
		globalDayThresholdLabel.setText("Prag Bonus (RON/ZI)");
		UIUtils.setFont(globalDayThresholdLabel);
		
		globalDayThreshold = new Text(globalGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(globalDayThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(globalDayThreshold);
		
		final Label globalMonthThresholdLabel = new Label(globalGroup, SWT.NONE);
		globalMonthThresholdLabel.setText("Prag Bonus (RON/LUNA)");
		UIUtils.setFont(globalMonthThresholdLabel);
		
		globalMonthThreshold = new Text(globalGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(globalMonthThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(globalMonthThreshold);
		
		final Label globalProfitBrutPercentLabel = new Label(globalGroup, SWT.NONE);
		globalProfitBrutPercentLabel.setText("Bonus (%)");
		UIUtils.setFont(globalProfitBrutPercentLabel);
		
		globalProfitBrutPercent = new Text(globalGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(globalProfitBrutPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(globalProfitBrutPercent);
		
		final Group gestiuneGroup = new Group(contents, SWT.NONE);
		gestiuneGroup.setText("Profit Brut Gestiune");
		gestiuneGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(gestiuneGroup);
		UIUtils.setFont(gestiuneGroup);
		
		final Label gestiuneDayThresholdLabel = new Label(gestiuneGroup, SWT.NONE);
		gestiuneDayThresholdLabel.setText("Prag Bonus (RON/ZI)");
		UIUtils.setFont(gestiuneDayThresholdLabel);
		
		gestiuneDayThreshold = new Text(gestiuneGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(gestiuneDayThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gestiuneDayThreshold);
		
		final Label gestiuneMonthThresholdLabel = new Label(gestiuneGroup, SWT.NONE);
		gestiuneMonthThresholdLabel.setText("Prag Bonus (RON/LUNA)");
		UIUtils.setFont(gestiuneMonthThresholdLabel);
		
		gestiuneMonthThreshold = new Text(gestiuneGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(gestiuneMonthThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gestiuneMonthThreshold);
		
		final Label gestiuneProfitBrutPercentLabel = new Label(gestiuneGroup, SWT.NONE);
		gestiuneProfitBrutPercentLabel.setText("Bonus (%)");
		UIUtils.setFont(gestiuneProfitBrutPercentLabel);
		
		gestiuneProfitBrutPercent = new Text(gestiuneGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(gestiuneProfitBrutPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gestiuneProfitBrutPercent);
		
		final Group teamGroup = new Group(contents, SWT.NONE);
		teamGroup.setText("Profit Brut Echipa");
		teamGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(teamGroup);
		UIUtils.setFont(teamGroup);
		
		final Label teamDayThresholdLabel = new Label(teamGroup, SWT.NONE);
		teamDayThresholdLabel.setText("Prag Bonus (RON/ZI)");
		UIUtils.setFont(teamDayThresholdLabel);
		
		teamDayThreshold = new Text(teamGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(teamDayThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(teamDayThreshold);
		
		final Label teamMonthThresholdLabel = new Label(teamGroup, SWT.NONE);
		teamMonthThresholdLabel.setText("Prag Bonus (RON/LUNA)");
		UIUtils.setFont(teamMonthThresholdLabel);
		
		teamMonthThreshold = new Text(teamGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(teamMonthThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(teamMonthThreshold);
		
		final Label teamProfitBrutPercentLabel = new Label(teamGroup, SWT.NONE);
		teamProfitBrutPercentLabel.setText("Bonus (%)");
		UIUtils.setFont(teamProfitBrutPercentLabel);
		
		teamProfitBrutPercent = new Text(teamGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(teamProfitBrutPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(teamProfitBrutPercent);
		
		final Group individualGroup = new Group(contents, SWT.NONE);
		individualGroup.setText("Profit Brut Individual");
		individualGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(individualGroup);
		UIUtils.setFont(individualGroup);
		
		final Label individualDayThresholdLabel = new Label(individualGroup, SWT.NONE);
		individualDayThresholdLabel.setText("Prag Bonus (RON/ZI)");
		UIUtils.setFont(individualDayThresholdLabel);
		
		individualDayThreshold = new Text(individualGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(individualDayThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(individualDayThreshold);
		
		final Label individualMonthThresholdLabel = new Label(individualGroup, SWT.NONE);
		individualMonthThresholdLabel.setText("Prag Bonus (RON/LUNA)");
		UIUtils.setFont(individualMonthThresholdLabel);
		
		individualMonthThreshold = new Text(individualGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(individualMonthThreshold);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(individualMonthThreshold);
		
		final Label individualProfitBrutPercentLabel = new Label(individualGroup, SWT.NONE);
		individualProfitBrutPercentLabel.setText("Bonus (%)");
		UIUtils.setFont(individualProfitBrutPercentLabel);
		
		individualProfitBrutPercent = new Text(individualGroup, SWT.SINGLE | SWT.BORDER);
		UIUtils.setFont(individualProfitBrutPercent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(individualProfitBrutPercent);
		
		fillFields();
		addListeners();
		return contents;
	}
	
	private void addListeners()
	{
	}

	@Override
	protected Point getInitialSize()
	{
		final Point initialSize = super.getInitialSize();
		return new Point(initialSize.x, initialSize.y+5);
	}
	
	@Override
	protected int getShellStyle()
	{
		return super.getShellStyle() | SWT.RESIZE;
	}
	
	@Override
	protected void okPressed()
	{
		if (okPressed.get())
			super.okPressed();
	}
	
	private void fillFields()
	{
		baseMonthSalary.setText(safeString(cim, CIM::getBaseMonthSalary, BigDecimal::toString));
		baseHourSalary.setText(safeString(cim, CIM::getBaseHourSalary, BigDecimal::toString));
		tichetMasaValue.setText(safeString(cim, CIM::getTichetMasaValue, BigDecimal::toString));
		normaOreSapt.setText(safeString(cim, CIM::getNormaOreSapt, BigDecimal::toString));
		saturdayExtraPercent.setText(safeString(cim, CIM::getSaturdayExtraPercent, PresentationUtils::displayPercentageRaw));
		sundayExtraPercent.setText(safeString(cim, CIM::getSundayExtraPercent, PresentationUtils::displayPercentageRaw));
		globalDayThreshold.setText(safeString(cim, CIM::getGlobalDayThreshold, BigDecimal::toString));
		globalMonthThreshold.setText(safeString(cim, CIM::getGlobalMonthThreshold, BigDecimal::toString));
		globalProfitBrutPercent.setText(safeString(cim, CIM::getGlobalProfitBrutPercent, PresentationUtils::displayPercentageRaw));
		gestiuneDayThreshold.setText(safeString(cim, CIM::getGestiuneDayThreshold, BigDecimal::toString));
		gestiuneMonthThreshold.setText(safeString(cim, CIM::getGestiuneMonthThreshold, BigDecimal::toString));
		gestiuneProfitBrutPercent.setText(safeString(cim, CIM::getGestiuneProfitBrutPercent, PresentationUtils::displayPercentageRaw));
		teamDayThreshold.setText(safeString(cim, CIM::getTeamDayThreshold, BigDecimal::toString));
		teamMonthThreshold.setText(safeString(cim, CIM::getTeamMonthThreshold, BigDecimal::toString));
		teamProfitBrutPercent.setText(safeString(cim, CIM::getTeamProfitBrutPercent, PresentationUtils::displayPercentageRaw));
		individualDayThreshold.setText(safeString(cim, CIM::getIndividualDayThreshold, BigDecimal::toString));
		individualMonthThreshold.setText(safeString(cim, CIM::getIndividualMonthThreshold, BigDecimal::toString));
		individualProfitBrutPercent.setText(safeString(cim, CIM::getIndividualProfitBrutPercent, PresentationUtils::displayPercentageRaw));
	}
	
	private void fillCim()
	{
		cim.setBaseMonthSalary(parse(baseMonthSalary.getText()));
		cim.setBaseHourSalary(parse(baseHourSalary.getText()));
		cim.setTichetMasaValue(parse(tichetMasaValue.getText()));
		cim.setNormaOreSapt(parse(normaOreSapt.getText()));
		cim.setSaturdayExtraPercent(extractPercentage(saturdayExtraPercent.getText()));
		cim.setSundayExtraPercent(extractPercentage(sundayExtraPercent.getText()));
		cim.setGlobalDayThreshold(parse(globalDayThreshold.getText()));
		cim.setGlobalMonthThreshold(parse(globalMonthThreshold.getText()));
		cim.setGlobalProfitBrutPercent(extractPercentage(globalProfitBrutPercent.getText()));
		cim.setGestiuneDayThreshold(parse(gestiuneDayThreshold.getText()));
		cim.setGestiuneMonthThreshold(parse(gestiuneMonthThreshold.getText()));
		cim.setGestiuneProfitBrutPercent(extractPercentage(gestiuneProfitBrutPercent.getText()));
		cim.setTeamDayThreshold(parse(teamDayThreshold.getText()));
		cim.setTeamMonthThreshold(parse(teamMonthThreshold.getText()));
		cim.setTeamProfitBrutPercent(extractPercentage(teamProfitBrutPercent.getText()));
		cim.setIndividualDayThreshold(parse(individualDayThreshold.getText()));
		cim.setIndividualMonthThreshold(parse(individualMonthThreshold.getText()));
		cim.setIndividualProfitBrutPercent(extractPercentage(individualProfitBrutPercent.getText()));
	}
	
	public void setOkSupplier(final Supplier<Boolean> okPressed)
	{
		this.okPressed = okPressed;
	}
	
	public CIM filledCim()
	{
		fillCim();
		return cim;
	}
}