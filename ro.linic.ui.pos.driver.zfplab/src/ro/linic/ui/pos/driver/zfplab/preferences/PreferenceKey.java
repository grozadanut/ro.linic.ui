package ro.linic.ui.pos.driver.zfplab.preferences;

public interface PreferenceKey {
	public static final String SERVER_ADDRESS = "zfp_server_address";
	public static final String SERVER_ADDRESS_DEF = "http://localhost:4444/";
	
	public static final String ECR_IP = "zfp_ecr_ip";
	public static final String ECR_PORT = "zfp_ecr_port";
	public static final int ECR_PORT_DEF = 8000;
	public static final String ECR_PASSWORD = "zfp_ecr_password";
	public static final String ECR_PASSWORD_DEF = "123456";
}
