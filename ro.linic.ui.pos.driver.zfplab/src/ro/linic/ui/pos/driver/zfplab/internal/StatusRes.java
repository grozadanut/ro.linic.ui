package ro.linic.ui.pos.driver.zfplab.internal;
import java.util.Date;
public class StatusRes {
   /**
    *FM Read only
    */
    public boolean FM_Read_only;
    public boolean getFM_Read_only() {
       return FM_Read_only;
    }
    protected void setFM_Read_only(boolean value) {
       FM_Read_only = value;
    }

   /**
    *Power down in opened fiscal receipt
    */
    public boolean Power_down_in_opened_fiscal_receipt;
    public boolean getPower_down_in_opened_fiscal_receipt() {
       return Power_down_in_opened_fiscal_receipt;
    }
    protected void setPower_down_in_opened_fiscal_receipt(boolean value) {
       Power_down_in_opened_fiscal_receipt = value;
    }

   /**
    *Printer not ready or overheated
    */
    public boolean Printer_not_ready_or_overheated;
    public boolean getPrinter_not_ready_or_overheated() {
       return Printer_not_ready_or_overheated;
    }
    protected void setPrinter_not_ready_or_overheated(boolean value) {
       Printer_not_ready_or_overheated = value;
    }

   /**
    *Incorrect time
    */
    public boolean Incorrect_time;
    public boolean getIncorrect_time() {
       return Incorrect_time;
    }
    protected void setIncorrect_time(boolean value) {
       Incorrect_time = value;
    }

   /**
    *Incorrect date
    */
    public boolean Incorrect_date;
    public boolean getIncorrect_date() {
       return Incorrect_date;
    }
    protected void setIncorrect_date(boolean value) {
       Incorrect_date = value;
    }

   /**
    *RAM reset
    */
    public boolean RAM_reset;
    public boolean getRAM_reset() {
       return RAM_reset;
    }
    protected void setRAM_reset(boolean value) {
       RAM_reset = value;
    }

   /**
    *Date and time hardware error
    */
    public boolean Date_and_time_hardware_error;
    public boolean getDate_and_time_hardware_error() {
       return Date_and_time_hardware_error;
    }
    protected void setDate_and_time_hardware_error(boolean value) {
       Date_and_time_hardware_error = value;
    }

   /**
    *Printer not ready or no paper
    */
    public boolean Printer_not_ready_or_no_paper;
    public boolean getPrinter_not_ready_or_no_paper() {
       return Printer_not_ready_or_no_paper;
    }
    protected void setPrinter_not_ready_or_no_paper(boolean value) {
       Printer_not_ready_or_no_paper = value;
    }

   /**
    *Reports registers overflow
    */
    public boolean Reports_registers_overflow;
    public boolean getReports_registers_overflow() {
       return Reports_registers_overflow;
    }
    protected void setReports_registers_overflow(boolean value) {
       Reports_registers_overflow = value;
    }

   /**
    *Blocking after 24 hours
    */
    public boolean Blocking_after_24_hours;
    public boolean getBlocking_after_24_hours() {
       return Blocking_after_24_hours;
    }
    protected void setBlocking_after_24_hours(boolean value) {
       Blocking_after_24_hours = value;
    }

   /**
    *Non-zero daily report
    */
    public boolean Non_zero_daily_report;
    public boolean getNon_zero_daily_report() {
       return Non_zero_daily_report;
    }
    protected void setNon_zero_daily_report(boolean value) {
       Non_zero_daily_report = value;
    }

   /**
    *Non-zero article report
    */
    public boolean Non_zero_article_report;
    public boolean getNon_zero_article_report() {
       return Non_zero_article_report;
    }
    protected void setNon_zero_article_report(boolean value) {
       Non_zero_article_report = value;
    }

   /**
    *Non-zero operator report
    */
    public boolean Non_zero_operator_report;
    public boolean getNon_zero_operator_report() {
       return Non_zero_operator_report;
    }
    protected void setNon_zero_operator_report(boolean value) {
       Non_zero_operator_report = value;
    }

   /**
    *Non-printed copy
    */
    public boolean Non_printed_copy;
    public boolean getNon_printed_copy() {
       return Non_printed_copy;
    }
    protected void setNon_printed_copy(boolean value) {
       Non_printed_copy = value;
    }

   /**
    *Opened Non-fiscal Receipt
    */
    public boolean Opened_Non_fiscal_Receipt;
    public boolean getOpened_Non_fiscal_Receipt() {
       return Opened_Non_fiscal_Receipt;
    }
    protected void setOpened_Non_fiscal_Receipt(boolean value) {
       Opened_Non_fiscal_Receipt = value;
    }

   /**
    *Opened Fiscal Receipt
    */
    public boolean Opened_Fiscal_Receipt;
    public boolean getOpened_Fiscal_Receipt() {
       return Opened_Fiscal_Receipt;
    }
    protected void setOpened_Fiscal_Receipt(boolean value) {
       Opened_Fiscal_Receipt = value;
    }

   /**
    *Standard Cash Receipt
    */
    public boolean Standard_Cash_Receipt;
    public boolean getStandard_Cash_Receipt() {
       return Standard_Cash_Receipt;
    }
    protected void setStandard_Cash_Receipt(boolean value) {
       Standard_Cash_Receipt = value;
    }

   /**
    *VAT included in the receipt
    */
    public boolean VAT_included_in_the_receipt;
    public boolean getVAT_included_in_the_receipt() {
       return VAT_included_in_the_receipt;
    }
    protected void setVAT_included_in_the_receipt(boolean value) {
       VAT_included_in_the_receipt = value;
    }

   /**
    *EJ near full
    */
    public boolean EJ_near_full;
    public boolean getEJ_near_full() {
       return EJ_near_full;
    }
    protected void setEJ_near_full(boolean value) {
       EJ_near_full = value;
    }

   /**
    *EJ full
    */
    public boolean EJ_full;
    public boolean getEJ_full() {
       return EJ_full;
    }
    protected void setEJ_full(boolean value) {
       EJ_full = value;
    }

   /**
    *No FM module
    */
    public boolean No_FM_module;
    public boolean getNo_FM_module() {
       return No_FM_module;
    }
    protected void setNo_FM_module(boolean value) {
       No_FM_module = value;
    }

   /**
    *FM error
    */
    public boolean FM_error;
    public boolean getFM_error() {
       return FM_error;
    }
    protected void setFM_error(boolean value) {
       FM_error = value;
    }

   /**
    *FM full
    */
    public boolean FM_full;
    public boolean getFM_full() {
       return FM_full;
    }
    protected void setFM_full(boolean value) {
       FM_full = value;
    }

   /**
    *FM near full
    */
    public boolean FM_near_full;
    public boolean getFM_near_full() {
       return FM_near_full;
    }
    protected void setFM_near_full(boolean value) {
       FM_near_full = value;
    }

   /**
    *Decimal point (1=fract, 0=whole)
    */
    public boolean Decimal_point;
    public boolean getDecimal_point() {
       return Decimal_point;
    }
    protected void setDecimal_point(boolean value) {
       Decimal_point = value;
    }

   /**
    *FM fiscalized
    */
    public boolean FM_fiscalized;
    public boolean getFM_fiscalized() {
       return FM_fiscalized;
    }
    protected void setFM_fiscalized(boolean value) {
       FM_fiscalized = value;
    }

   /**
    *FM produced
    */
    public boolean FM_produced;
    public boolean getFM_produced() {
       return FM_produced;
    }
    protected void setFM_produced(boolean value) {
       FM_produced = value;
    }

   /**
    *Printer: automatic cutting
    */
    public boolean Printer_automatic_cutting;
    public boolean getPrinter_automatic_cutting() {
       return Printer_automatic_cutting;
    }
    protected void setPrinter_automatic_cutting(boolean value) {
       Printer_automatic_cutting = value;
    }

   /**
    *External Display Management
    */
    public boolean External_Display_Management;
    public boolean getExternal_Display_Management() {
       return External_Display_Management;
    }
    protected void setExternal_Display_Management(boolean value) {
       External_Display_Management = value;
    }

   /**
    *Missing external display
    */
    public boolean Missing_external_display;
    public boolean getMissing_external_display() {
       return Missing_external_display;
    }
    protected void setMissing_external_display(boolean value) {
       Missing_external_display = value;
    }

   /**
    *Drawer: automatic opening
    */
    public boolean Drawer_automatic_opening;
    public boolean getDrawer_automatic_opening() {
       return Drawer_automatic_opening;
    }
    protected void setDrawer_automatic_opening(boolean value) {
       Drawer_automatic_opening = value;
    }

   /**
    *Customer logo included in the receipt
    */
    public boolean Customer_logo_included_in_the_receipt;
    public boolean getCustomer_logo_included_in_the_receipt() {
       return Customer_logo_included_in_the_receipt;
    }
    protected void setCustomer_logo_included_in_the_receipt(boolean value) {
       Customer_logo_included_in_the_receipt = value;
    }

   /**
    *Service jumper
    */
    public boolean Service_jumper;
    public boolean getService_jumper() {
       return Service_jumper;
    }
    protected void setService_jumper(boolean value) {
       Service_jumper = value;
    }

   /**
    *No Sec.IC
    */
    public boolean No_Sec_IC;
    public boolean getNo_Sec_IC() {
       return No_Sec_IC;
    }
    protected void setNo_Sec_IC(boolean value) {
       No_Sec_IC = value;
    }

   /**
    *No certificates
    */
    public boolean No_certificates;
    public boolean getNo_certificates() {
       return No_certificates;
    }
    protected void setNo_certificates(boolean value) {
       No_certificates = value;
    }

   /**
    *No SD card response
    */
    public boolean No_SD_card_response;
    public boolean getNo_SD_card_response() {
       return No_SD_card_response;
    }
    protected void setNo_SD_card_response(boolean value) {
       No_SD_card_response = value;
    }

   /**
    *Wrong SD card
    */
    public boolean Wrong_SD_card;
    public boolean getWrong_SD_card() {
       return Wrong_SD_card;
    }
    protected void setWrong_SD_card(boolean value) {
       Wrong_SD_card = value;
    }

   /**
    *Near Paper end
    */
    public boolean Near_Paper_end;
    public boolean getNear_Paper_end() {
       return Near_Paper_end;
    }
    protected void setNear_Paper_end(boolean value) {
       Near_Paper_end = value;
    }

   /**
    *SIM is not activated
    */
    public boolean SIM_is_not_activated;
    public boolean getSIM_is_not_activated() {
       return SIM_is_not_activated;
    }
    protected void setSIM_is_not_activated(boolean value) {
       SIM_is_not_activated = value;
    }
}
