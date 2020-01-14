/**
 * (c) Copyright [2020] Micro Focus or one of its affiliates.
 */
package com.fortify.sample.bugtracker.dimensions;

/**
 * Constants for using inside the Dimension CM Bug plugin.
 *
 * @author Kevin A. Lee (kevin.lee@microfocus.com)
 * @version 1.0 01/01/2020
 */
final class DimCMPluginConstants {

	//
	// Plugin Configuration
	//
	public static final String SUPPORTED_VERSION_LABEL = "Supported Versions";
	public static final String SUPPORTED_VERSION_DESCRIPTION = "Dimensions CM versions supported by the plugin";
	public static final String SUPPORTED_VERSION_DEFAULT_VALUE = "14.x";

	public static final String DIMCM_SERVER_CONFIG_NAME = "dimCmServer";
	public static final String DIMCM_SERVER_LABEL = "Server Name";
	public static final String DIMCM_SERVER_DESCRIPTION = "Dimensions CM Server Name";
	public static final String DIMCM_SERVER_DEFAULT_VALUE = "localhost";

	public static final String DIMCM_DBNAME_CONFIG_NAME = "dimCmDbName";
	public static final String DIMCM_DBNAME_LABEL = "Database Name";
	public static final String DIMCM_DBNAME_DESCRIPTION = "Dimensions CM Database Name";
	public static final String DIMCM_DBNAME_DEFAULT_VALUE = "cm_typical";

	public static final String DIMCM_DBCONN_CONFIG_NAME = "dimCmDbConn";
	public static final String DIMCM_DBCONN_LABEL = "Database Connection";
	public static final String DIMCM_DBCONN_DESCRIPTION = "Dimensions CM Database Connection";
	public static final String DIMCM_DBCONN_DEFAULT_VALUE = "dimcm";

	public static final String DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME = "dimCmSupportedReqType";
	public static final String DIMCM_SUPPORTED_REQ_TYPE_LABEL = "Request Types";
	public static final String DIMCM_SUPPORTED_REQ_TYPE_DESCRIPTION = "Supported Dimensions CM Request Types";
	public static final String DIMCM_SUPPORTED_REQ_TYPE_DEFAULT_VALUE = "DEFECT,CR,TASK";

	public static final String DIMCM_SEVERITY_FIELD_CONFIG_NAME = "dimCmSeverityField";
	public static final String DIMCM_SEVERITY_LABEL = "Severity Field Name";
	public static final String DIMCM_SEVERITY_DESCRIPTION = "The field name of a severity or priority field to set";
	public static final String DIMCM_SEVERITY_DEFAULT_VALUE = "SEVERITY";

	public static final String DIMCM_OWNER_ROLE_CONFIG_NAME = "dimCmOwnerRole";
	public static final String DIMCM_OWNER_ROLE_LABEL = "Owner Role";
	public static final String DIMCM_OWNER_ROLE_DESCRIPTION = "Dimensions CM Role to assign as Owner";
	public static final String DIMCM_OWNER_ROLE_DEFAULT_VALUE = "DEVELOPER";

	public static final String DIMCM_OWNER_CAPABILITIES_CONFIG_NAME = "dimCmOwnerCapabilities";
	public static final String DIMCM_OWNER_CAPABILITIES_LABEL = "Owner Capabilities";
	public static final String DIMCM_OWNER_CAPABILITIES_DESCRIPTION = "List of supported Dimensions CM Role Capabilities to assign Owner as";
	public static final String DIMCM_OWNER_CAPABILITIES_DEFAULT_VALUE = "LEADER,PRIMARY,SECONDARY";

	public static final String DIMCM_RESOLUTION_FIELD_CONFIG_NAME = "dimCmResolutionField";
	public static final String DIMCM_RESOLUTION_LABEL = "Resolution Field Name";
	public static final String DIMCM_RESOLUTION_DESCRIPTION = "The field name of a resolution field in the Dimensions CM Request type to use";
	public static final String DIMCM_RESOLUTION_DEFAULT_VALUE = "DETAILS_OF_THE_SOLUTION";

	public static final String DIMCM_BUG_URL_CONFIG_NAME = "dimCmBugUrl";
	public static final String DIMCM_BUG_URL_LABEL = "Bug URL Deep Link";
	public static final String DIMCM_BUG_URL_DESCRIPTION = "Templated link to Dimensions Request in Pulse (default) or Web Client.";
	//public static final String DIMCM_BUG_URL_DEFAULT_VALUE = "http://localhost:8080/dimensions?jsp=api&command=opencd&DB_CONN=%DBCONN%&DB_NAME=%DBNAME%&object_id=%BUG_ID%";
	public static final String DIMCM_BUG_URL_DEFAULT_VALUE = "http://localhost:8080/pulse/agile.html#/suites/1/products/1/requests/%BUG_ID%";

	public static final String DIMCM_CLOSED_STATES_CONFIG_NAME = "dimCmClosedStates";
	public static final String DIMCM_CLOSED_STATES_LABEL = "Closed State";
	public static final String DIMCM_CLOSED_STATES_DESCRIPTION = "List of request states for a closed request";
	public static final String DIMCM_CLOSED_STATES_DEFAULT_VALUE = "REJECTED,CLOSED";

	public static final String DIMCM_SSC_COMMENTS_FIELD_CONFIG_NAME = "dimCmSscCommentsField";
	public static final String DIMCM_SSC_COMMENTS_LABEL = "SSC Comments Field Name";
	public static final String DIMCM_SSC_COMMENTS_DESCRIPTION = "The field name of a Dimmensions CM text field to updated with SSC comments";
	public static final String DIMCM_SSC_COMMENTS_DEFAULT_VALUE = "SSC_COMMENTS";

	public static final String DIMCM_USERNAME_CONFIG_NAME = "dimCmUsername";
	public static final String DIMCM_USERNAME_LABEL = "Dimensions CM User Name";
	public static final String DIMCM_USERNAME_DESCRIPTION = "Dimensions CM User Name";
	public static final String DIMCM_USERNAME_DEFAULT_VALUE = "dmsys";

	public static final String DIMCM_PASSWORD_CONFIG_NAME = "dimCMPassword";
	public static final String DIMCM_PASSWORD_LABEL = "Dimensions CM Password";
	public static final String DIMCM_PASSWORD_DESCRIPTION = "Dimensions CM Password";
	public static final String DIMCM_PASSWORD_DEFAULT_VALUE = "dmsys";

	//
	// Bug Parameters
	//
	public static final String PRODUCT_PARAM_NAME = "product";
	public static final String PRODUCT_LABEL = "Product";
	public static final String PRODUCT_DESCRIPTION = "Name of Product against which bug needs to be logged";

	public static final String TITLE_PARAM_NAME = "title";
	public static final String TITLE_LABEL = "Title";
	public static final String TITLE_DESCRIPTION = "Title of the bug to be logged";

	public static final String DESCRIPTION_PARAM_NAME = "description";
	public static final String DESCRIPTION_LABEL = "Description";
	public static final String DESCRIPTION_DESCRIPTION = "Detailed description of the bug to be logged";

	public static final String PROJECT_PARAM_NAME = "project";
	public static final String PROJECT_LABEL = "Project";
	public static final String PROJECT_DESCRIPTION = "Name of Project/Stream against which bug needs to be logged";

	public static final String OWNER_PARAM_NAME = "owner";
	public static final String OWNER_LABEL = "Owner";
	public static final String OWNER_DESCRIPTION = "Name of Owner against which bug needs to be delegated";

	public static final String OWNER_CAPABILITY_PARAM_NAME = "ownerCapability";
	public static final String OWNER_CAPABILITY_LABEL = "Owner Capability";
	public static final String OWNER_CAPABILITY_DESCRIPTION = "The capability of the Owner to be assigned the Bug";

	public static final String REQ_TYPE_PARAM_NAME = "reqType";
	public static final String REQ_TYPE_LABEL = "Request Type";
	public static final String REQ_TYPE_DESCRIPTION = "Type of Request to create for the Bug";

	public static final String PARTS_PARAM_NAME = "parts";
	public static final String PARTS_LABEL = "Design Parts";
	public static final String PARTS_DESCRIPTION = "Name of Design Parts against which bug needs to be logged";

	public static final String SEVERITY_PARAM_NAME = "severity";
	public static final String SEVERITY_LABEL = "Severity";
	public static final String SEVERITY_DESCRIPTION = "Severity of the bug that needs to be logged";

	public static final String ADDITIONAL_FIELDS_PARAM_NAME = "additionalFields";
	public static final String ADDITIONAL_FIELDS_LABEL = "Additional Fields";
	public static final String ADDITIONAL_FIELDS_DESCRIPTION = "New line separated list on attribute name=value pairs to be sent as additional fields";

	public static final String BUG_STATUS_PARAM_NAME = "bugStatus";
	public static final String BUG_ID_PARAM_NAME = "BugId";
	public static final String BUG_COMMENT_PARM_NAME = "bugComment";

	// TODO: allow the user to configure states
	public static final String STATUS_NEW = "RAISED";
	public static final String STATUS_REOPENED = "UNDER WORK";
	public enum CLOSED_STATUS { RESOLVED, CLOSED, VERIFIED };
	public enum NON_REOPENABLE_RESOLUTION { DUPLICATE, REJECTED };


    /**
     * Maximum length for bug summary string.
     */
    public static final int MAX_SUMMARY_LENGTH = 80;

	private DimCMPluginConstants() {
		// No implementation.
	}
}
