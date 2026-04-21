package ro.linic.ui.pos.driver.zfplab.preferences;

public interface PreferenceKey {
	public static final String SERVER_ADDRESS = "zfp_server_address";
	public static final String SERVER_ADDRESS_DEF = "http://localhost:4444/";
	
	public static final String ECR_IP = "zfp_ecr_ip";
	public static final String ECR_PORT = "zfp_ecr_port";
	public static final int ECR_PORT_DEF = 8000;
	public static final String ECR_PASSWORD = "zfp_ecr_password";
	public static final String ECR_PASSWORD_DEF = "123456";
	public static final String OPERATOR = "zfp_operator_num";
	public static final double OPERATOR_DEF = 1;
	public static final String OPERATOR_PASSWORD = "zfp_operator_password";
	public static final String OPERATOR_PASSWORD_DEF = "0";
	public static final String ECR_TAX_CODE = "zfp_tax_code";
	public static final String ECR_TAX_CODE_DEF = "A";
	public static final String ECR_DEPT = "zfp_department";
	public static final String ECR_DEPT_DEF = "1";
	public static final String REPORT_Z_AND_D = "zfp_department";
	public static final boolean REPORT_Z_AND_D_DEF = true;
}
