/**
 * (c) Copyright [2020] Micro Focus or one of its affiliates.
 */
package com.fortify.sample.bugtracker.dimensions;

/**
 * Constants for using inside the Dimension CM Bugplugin.
 *
 * @author Kevin A. Lee (kevin.lee@microfocus.com)
 * @version 1.0 01/01/2020
 */
final class DimCMPluginConstants {

	public static final String DIMCM_SERVER = "dimCmServer";
	public static final String DIMCM_DBNAME = "dimCmDbName";
	public static final String DIMCM_DBCONN = "dimCmDbConn";
	public static final String DIMCM_WEB_URL = "dimCmWebUrl";
	public static final String DIMCM_SUPPORTED_REQ_TYPE = "dimCmSupportedReqType";
	public static final String DIMCM_OWNER_ROLE = "dimCmOwnerRole";
	public static final String DIMCM_OWNER_CAPABILITIES = "dimCmOwnerCapabilities";
	public static final String DIMCM_SEVERITY_FIELD = "dimCmSeverityField";
	public static final String COMMENT = "comment";
	public static final String PRODUCT_PARAM_NAME = "product";
	public static final String SUMMARY_PARAM_NAME = "summary";
	public static final String DESCRIPTION_PARAM_NAME = "description";

	public static final String PROJECT_PARAM_NAME = "project";
	public static final String OWNER_PARAM_NAME = "owner";
	public static final String OWNER_CAPABILITY_PARAM_NAME = "ownerCapability";
	public static final String REQ_TYPE_PARAM_NAME = "reqType";
	public static final String PARTS_PARAM_NAME = "parts";
	public static final String SEVERITY_PARAM_NAME = "severity";
	public static final String ADDITIONAL_FIELDS_PARAM_NAME = "additionalFields";
	public static final String PRIORITY_PARAM_NAME = "priority";
	public static final String BUG_STATUS_PARAM_NAME = "bugStatus";
	public static final String BUG_ID_PARAM_NAME = "BugId";

	// TODO: allow the user to configure states
	public static final String STATUS_NEW = "RAISED";
	public static final String STATUS_REOPENED = "CONFIRMED";
	public enum CLOSED_STATUS { RESOLVED, CLOSED, VERIFIED };
	public enum NON_REOPENABLE_RESOLUTION { DUPLICATE, REJECTED };

	public static final String PRODUCT_LABEL = "Product";
	public static final String PRODUCT_DESCRIPTION = "Name of Product against which bug needs to be logged";
	public static final String REQ_TYPE_LABEL = "Request Type";
	public static final String REQ_TYPE_DESCRIPTION = "Type of Request to create for the Bug";
	public static final String PROJECT_LABEL = "Project";
	public static final String PROJECT_DESCRIPTION = "Name of Project/Stream against which bug needs to be logged";
	public static final String OWNER_LABEL = "Owner";
	public static final String OWNER_DESCRIPTION = "Name of Owner against which bug needs to be delegated";
	public static final String OWNER_CAPABILITY_LABEL = "Owner Capability";
	public static final String OWNER_CAPABILITY_DESCRIPTION = "The capability of the Owner to be assigned the Bug";
	public static final String ADDITIONAL_FIELDS_LABEL = "Additional Fields";
	public static final String ADDITIONAL_FIELDS_DESCRIPTION = "New line separated list on attribute name=value pairs to be sent as additional fields";
	public static final String PARTS_LABEL = "Design Parts";
	public static final String PARTS_DESCRIPTION = "Name of Design Parts against which bug needs to be logged";
	public static final String SEVERITY_LABEL = "Severity";
	public static final String SEVERITY_DESCRIPTION = "Severity of the bug that needs to be logged";

	public static final String VERSION_LABEL = "Version";
	public static final String VERSION_DESCRIPTION = "Version against which bug needs to be logged";

	public static final String DIMCM_REQ_URL = "?jsp=api&command=opencd&DB_CONN=%DBCONN%&DB_NAME=%DBNAME%&object_id=";

    public static final String SUPPORTED_VERSIONS = "14.x";

    /**
     * Maximum length for bug summary string.
     */
    public static final int MAX_SUMMARY_LENGTH = 255;

	private DimCMPluginConstants() {
		// No implementation.
	}
}
