package ro.linic.ui.pos.base.preferences;

import ro.linic.ui.pos.base.services.ECRDriver;

public interface PreferenceKey {
	public static final String ECR_MODEL = "ecr_model";
	public static final String ECR_MODEL_DEF = ECRDriver.ECR_MODEL_DATECS;
	
	public static final String DUDE_ECR_FOLDER = "dude_ecr_folder";
	public static final String DUDE_ECR_FOLDER_DEF = "C:/datecs/";
	public static final String DUDE_ECR_PORT = "dude_ecr_port";
	public static final String DUDE_ECR_PORT_DEF = "3999";
	public static final String DUDE_ECR_IP = "dude_ecr_ip";
	public static final String DUDE_ECR_OPERATOR = "dude_ecr_operator";
	public static final String DUDE_ECR_OPERATOR_DEF = "1";
	public static final String DUDE_ECR_PASSWORD = "dude_ecr_password";
	public static final String DUDE_ECR_PASSWORD_DEF = "0001";
	public static final String DUDE_ECR_NR_AMEF = "dude_ecr_nr_amef";
	public static final String DUDE_ECR_NR_AMEF_DEF = "1";
	public static final String DUDE_ECR_TAX_CODE = "dude_ecr_tax_code";
	public static final String DUDE_ECR_TAX_CODE_DEF = "1";
	public static final String DUDE_ECR_DEPT = "dude_ecr_departament";
	public static final String DUDE_ECR_DEPT_DEF = "1";
	/**
	 * Value is boolean. Whether we should also print D report when we close the day with Z.
	 */
	public static final String DUDE_REPORT_Z_AND_D = "dude_report_z_and_d";
	public static final boolean DUDE_REPORT_Z_AND_D_DEF = false;
	
	public static final String FISCAL_NET_COMMAND_FOLDER = "fiscal_net_command_folder";
	public static final String FISCAL_NET_COMMAND_FOLDER_DEF = "C:\\FiscalNet\\Bonuri";
	public static final String FISCAL_NET_RESPONSE_FOLDER = "fiscal_net_response_folder";
	public static final String FISCAL_NET_RESPONSE_FOLDER_DEF = "C:\\FiscalNet\\Raspuns";
	public static final String FISCAL_NET_TAX_CODE = "fiscal_net_tax_code";
	public static final String FISCAL_NET_TAX_CODE_DEF = "1";
	public static final String FISCAL_NET_DEPT = "fiscal_net_departament";
	public static final String FISCAL_NET_DEPT_DEF = "1";
}
