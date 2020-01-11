/**
 * (c) Copyright [2020] Micro Focus or one of its affiliates.
 */
package com.fortify.sample.bugtracker.dimensions;

import com.fortify.pub.bugtracker.plugin.AbstractBatchBugTrackerPlugin;
import com.fortify.pub.bugtracker.plugin.BatchBugTrackerPlugin;
import com.fortify.pub.bugtracker.plugin.BugTrackerPluginImplementation;
import com.fortify.pub.bugtracker.support.Bug;
import com.fortify.pub.bugtracker.support.BugParam;
import com.fortify.pub.bugtracker.support.BugParamChoice;
import com.fortify.pub.bugtracker.support.BugParamText;
import com.fortify.pub.bugtracker.support.BugParamTextArea;
import com.fortify.pub.bugtracker.support.BugSubmission;
import com.fortify.pub.bugtracker.support.BugTrackerConfig;
import com.fortify.pub.bugtracker.support.BugTrackerException;
import com.fortify.pub.bugtracker.support.IssueDetail;
import com.fortify.pub.bugtracker.support.MultiIssueBugSubmission;
import com.fortify.pub.bugtracker.support.UserAuthenticationStore;

import com.j2bugzilla.rpc.GetBug;
import com.serena.dmclient.api.DimensionsObjectFactory;
import com.serena.dmclient.api.DimensionsResult;
import com.serena.dmclient.api.Part;
import com.serena.dmclient.api.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.fortify.pub.bugtracker.support.BugTrackerPluginConstants.DISPLAY_ONLY_SUPPORTED_VERSION;
import static com.fortify.sample.bugtracker.dimensions.DimCMPluginConstants.*;

/**
 * This plugin is used for submitting bugs into to the Dimensions CM.
 * Dimensions CM Java API is used for communicating with bug tracker via DimCMClient class.
 *
 * Logging is written to %FORTIFY_HOME%\ssc\plugin-framework\logs
 *
 * @author Kevin A. Lee (kevin.lee@microfocus.com)
 * @version 1.0 01/01/2020
 */
@BugTrackerPluginImplementation
public class DimCMBugTrackerPlugin extends AbstractBatchBugTrackerPlugin implements BatchBugTrackerPlugin {

	private static final Log LOG = LogFactory.getLog(DimCMBugTrackerPlugin.class);

	private String cmServer;
	private String cmDbName;
	private String cmDbCon;
	private String cmUsername;
	private String cmPassword;
	private String cmWebUrl;
	private String cmSuppReqTypes;
	private String cmSeverityField;
	private String cmOwnerRoleField;
	private String cmOwnerCapabilities;
	private String cmResolutionField;
	private Map<String, String> config; // Full Dimensions CM plugin configuration

	/**
	 * Retrieve list of the parameters that should be filled by user for bug submission.
	 *
	 * @param credentials bug tracker credentials supplied by the user
	 * @return list of the parameters that should be filled bu user for bug submission.
	 */
	@Override
	public List<BugParam> getBatchBugParameters(UserAuthenticationStore credentials) {
		return getBugParameters(null, credentials);
	}

	/**
	 * Retrieve list of the parameters that should be filled by user for bug submission.
	 *
	 * @param issueDetail
	 *            an object encompassing the details of the issue for which the
	 *            user is attempting to log a bug. Defaults to various bug
	 *            parameters like description, summary can be extracted from
	 *            this object.
	 * @param credentials bug tracker credentials supplied by the user
	 *
	 * @return list of the parameters that should be filled bu user for bug submission.
	 */
	@Override
	public List<BugParam> getBugParameters(IssueDetail issueDetail, UserAuthenticationStore credentials) {

		try {
			final DimCMClient cmClient = new DimCMClient();
			cmClient.connect(credentials.getUserName(), credentials.getPassword(), cmDbName, cmDbCon, cmServer);

            final BugParam summaryParam = getSummaryParamText(issueDetail);
            final BugParam descriptionParam = getDescriptionParamText(issueDetail);
            final BugParam productParam;
            final BugParam reqTypeParam;
            final BugParam projectParam;
            final BugParam partsParam;
            final BugParam severityParam;
            final BugParam ownerParam;
            final BugParam ownerCapabilityParam;
			final BugParam additionalFieldsParam = getAdditionalFieldsParamText(issueDetail);

            final List<String> products = cmClient.getProducts();
            productParam = getProductParamChoice(products);
            reqTypeParam = getReqTypeParamChoice(new ArrayList<>());
            projectParam =  getProjectParamChoice(new ArrayList<>());
            partsParam = getPartsParamChoice(new ArrayList<>());
            severityParam = getSeverityParamChoice(new ArrayList<>());
            ownerParam = getOwnerParamChoice(new ArrayList<>());
            if (cmOwnerCapabilities != null && cmOwnerCapabilities.length() > 0) {
				final String capabilities[] = cmOwnerCapabilities.split(",");
				ownerCapabilityParam = getOwnerCapabilityParamChoice(new ArrayList<String>(Arrays.asList(capabilities)));
			} else {
            	final List<String> capabilities = new ArrayList<>();
            	capabilities.add("PRIMARY");
            	capabilities.add("SECONDARY");
				ownerCapabilityParam = getOwnerCapabilityParamChoice(capabilities);
			}
			return Arrays.asList(summaryParam, descriptionParam, productParam, reqTypeParam, projectParam, partsParam,
					severityParam, ownerParam, ownerCapabilityParam, additionalFieldsParam);

		} catch (BugTrackerException e) {
			throw e;
		} catch (Exception e) {
			throw new BugTrackerException("Error while setting Dimensions CM bug fields configuration: " + e.getMessage(), e);
		}
	}

	private BugParam getSummaryParamText(IssueDetail issueDetail) {
		//LOG.info("Issue Detail:");
		//LOG.info(issueDetail.getSummary());
		final BugParam summaryParam = new BugParamText()
				.setIdentifier(SUMMARY_PARAM_NAME)
				.setDisplayLabel("Bug Summary")
				.setRequired(true)
				.setDescription("Title of the bug to be logged");
		/*if (issueDetail.getSummary().isEmpty()) {
			summaryParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
		} else {
			summaryParam.setValue(issueDetail.getSummary());
		}*/
		return summaryParam;
	}

	private BugParam getDescriptionParamText(final IssueDetail issueDetail) {
		//LOG.info("Issue Detail:");
		//LOG.info(issueDetail.getDescription());
		final BugParam descriptionParam = new BugParamTextArea()
				.setIdentifier(DESCRIPTION_PARAM_NAME)
				.setDisplayLabel("Bug Description")
				.setDescription("Full description of the bug to be logged")
				.setRequired(true);
		/*
		if (issueDetail.getDescription().isEmpty()) {
			descriptionParam.setValue("Issue Ids: $ATTRIBUTE_INSTANCE_ID$\n$ISSUE_DEEPLINK$");
		} else {
			descriptionParam.setValue(pluginHelper.buildDefaultBugDescription(issueDetail, true));
		}*/
		return descriptionParam;
	}


	private BugParam getAdditionalFieldsParamText(final IssueDetail issueDetail) {
		final BugParam additionalFieldsParam = new BugParamTextArea()
				.setIdentifier(ADDITIONAL_FIELDS_PARAM_NAME)
				.setDisplayLabel(ADDITIONAL_FIELDS_LABEL)
				.setDescription(ADDITIONAL_FIELDS_DESCRIPTION)
				.setRequired(false);
		return additionalFieldsParam;
	}

	private BugParam getProductParamChoice(List<String> products) {
		return new BugParamChoice()
				.setChoiceList(products)
				.setHasDependentParams(true)
				.setIdentifier(PRODUCT_PARAM_NAME)
				.setDisplayLabel(PRODUCT_LABEL)
				.setRequired(true)
				.setDescription(PRODUCT_DESCRIPTION);
	}

	private BugParam getReqTypeParamChoice(List<String> reqTypes) {
		return new BugParamChoice()
				.setChoiceList(reqTypes)
				.setHasDependentParams(false)
				.setIdentifier(REQ_TYPE_PARAM_NAME)
				.setDisplayLabel(REQ_TYPE_LABEL)
				.setRequired(true)
				.setDescription(REQ_TYPE_DESCRIPTION);
	}

	private BugParam getProjectParamChoice(List<String> parts) {
		return new BugParamChoice()
				.setChoiceList(parts)
				.setHasDependentParams(false)
				.setIdentifier(PROJECT_PARAM_NAME)
				.setDisplayLabel(PROJECT_LABEL)
				.setRequired(false)
				.setDescription(PROJECT_DESCRIPTION);
	}

	private BugParam getOwnerParamChoice(List<String> owners) {
		return new BugParamChoice()
				.setChoiceList(owners)
				.setHasDependentParams(false)
				.setIdentifier(OWNER_PARAM_NAME)
				.setDisplayLabel(OWNER_LABEL)
				.setRequired(false)
				.setDescription(OWNER_DESCRIPTION);
	}

	private BugParam getOwnerCapabilityParamChoice(List<String> capabilities) {
		return new BugParamChoice()
				.setChoiceList(capabilities)
				.setHasDependentParams(false)
				.setIdentifier(OWNER_CAPABILITY_PARAM_NAME)
				.setDisplayLabel(OWNER_CAPABILITY_LABEL)
				.setRequired(false)
				.setDescription(OWNER_CAPABILITY_DESCRIPTION);
	}

	private BugParam getPartsParamChoice(List<String> parts) {
		return new BugParamChoice()
				.setChoiceList(parts)
				.setHasDependentParams(false)
				.setIdentifier(PARTS_PARAM_NAME)
				.setDisplayLabel(PARTS_LABEL)
				.setRequired(false)
				.setDescription(PARTS_DESCRIPTION);
	}

	private BugParam getSeverityParamChoice(List<String> priorities) {
		return new BugParamChoice()
					.setChoiceList(priorities)
					.setHasDependentParams(false)
					.setIdentifier(SEVERITY_PARAM_NAME)
					.setDisplayLabel(SEVERITY_LABEL)
					.setRequired(false)
					.setDescription(SEVERITY_DESCRIPTION);
	}

	@Override
	public List<BugParam> onBatchBugParameterChange(String changedParamIdentifier, List<BugParam> currentValues, UserAuthenticationStore credentials) {
        return innerOnParameterChange(changedParamIdentifier, currentValues, credentials);
	}

	@Override
	public Bug fileMultiIssueBug(MultiIssueBugSubmission bug, UserAuthenticationStore credentials)
			throws BugTrackerException {
		final DimCMClient connector = connectToDimensions(credentials);
		return fileBugInternal(connector, bug.getParams());
	}

	@Override
	public Bug fileBug(BugSubmission bug, UserAuthenticationStore credentials)
			throws BugTrackerException {
		final DimCMClient connector = connectToDimensions(credentials);
		return fileBugInternal(connector, bug.getParams());
	}

	private Bug fileBugInternal(final DimCMClient connector, final Map<String, String> bugParams) {
		try {
			LOG.info("File Bug in Dimensions CM:");
			List <Part> parts = connector.getDesignPartAsList(bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(PARTS_PARAM_NAME));
			LOG.info("Product:" + bugParams.get(PRODUCT_PARAM_NAME));
			LOG.info("Project:" + bugParams.get(PROJECT_PARAM_NAME));
			LOG.info("Type:" + bugParams.get(REQ_TYPE_PARAM_NAME));
			LOG.info("Summary:" + bugParams.get(SUMMARY_PARAM_NAME));
			LOG.info("Description:" + bugParams.get(DESCRIPTION_PARAM_NAME));
			LOG.info("Severity:" + bugParams.get(SEVERITY_PARAM_NAME));
			LOG.info("Owner:" + bugParams.get(OWNER_PARAM_NAME));
			LOG.info("Design Part: " + bugParams.get(PARTS_PARAM_NAME));
			LOG.info("Design Part (Object): " + parts.toString());
			LOG.info("Additional Fields:" + bugParams.get(ADDITIONAL_FIELDS_PARAM_NAME));
			DimensionsResult result = connector.createRequest(bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(PROJECT_PARAM_NAME), parts,
					bugParams.get(REQ_TYPE_PARAM_NAME), bugParams.get(SUMMARY_PARAM_NAME), bugParams.get(DESCRIPTION_PARAM_NAME),
					bugParams.get(SEVERITY_PARAM_NAME), bugParams.get(OWNER_PARAM_NAME), bugParams.get(ADDITIONAL_FIELDS_PARAM_NAME));
			String bugId = connector.getRequestIdFromResult(result, bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(REQ_TYPE_PARAM_NAME));
			LOG.info("Created Dimensions CM Request with Id: " + bugId);
			if (bugParams.get(OWNER_PARAM_NAME) != null && (bugParams.get(OWNER_PARAM_NAME)).length() > 0) {
				LOG.info("Delegating Dimensions CM Request:");
				LOG.info("Request Id: " + bugId);
				LOG.info("Owner: " + bugParams.get(OWNER_PARAM_NAME));
				LOG.info("Role: " + cmOwnerRoleField);
				LOG.info("Capability: " + bugParams.get(OWNER_CAPABILITY_PARAM_NAME).substring(0,1));
				List<String> users = new ArrayList<>();
				users.add(bugParams.get(OWNER_PARAM_NAME));
				connector.delegateRequest(bugId, users, cmOwnerRoleField, bugParams.get(OWNER_CAPABILITY_PARAM_NAME).substring(0,1));
			}
			return new Bug(bugId, STATUS_NEW);
		} catch (Exception e) {
			LOG.error(e.toString());
			throw new BugTrackerException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isBugOpen(Bug bug, UserAuthenticationStore credentials) {
		return !isBugClosed(bug, credentials);
	}

	@Override
	public boolean isBugClosed(Bug bug, UserAuthenticationStore credentials) {
		return isBugClosed(bug);
	}

	@Override
	public boolean isBugClosedAndCanReOpen(Bug bug, UserAuthenticationStore credentials) {
		return isBugClosed(bug) && canReOpenBug(bug);
	}

	private boolean isBugClosed(Bug bug) {
		String status = bug.getBugStatus();
		boolean retval = false;
		for (CLOSED_STATUS cs : CLOSED_STATUS.values()){
			if (cs.name().equals(status)) {
				retval = true;
				break;
			}
		}
		return retval;
	}

	@Override
	public void reOpenBug(Bug bug, String comment, UserAuthenticationStore credentials) {
		if (!canReOpenBug(bug)) {
			throw new BugTrackerException("Bug " + bug.getBugId() + " cannot be reopened.");
		}
		/*
		final BugzillaConnector connector = connectToDimensions(credentials);
		try {
			final GetBug getBug = new GetBug(Integer.parseInt(bug.getBugId()));
			executeMethod(connector, getBug);

			com.j2bugzilla.base.Bug dtoBug = getBug.getBug();
			sanitizeEmptyDtoBugFields2(dtoBug);
			dtoBug.setStatus(STATUS_REOPENED);
			dtoBug.clearResolution();

			final UpdateBug reportBug = new UpdateBug(dtoBug);
			executeMethod(connector, reportBug);
			final CommentBug commentBug = new CommentBug(Integer.parseInt(bug.getBugId()), comment);
			executeMethod(connector, commentBug);
		} catch (BugzillaException e) {
			throw new BugTrackerException(e.getMessage(), e);
		}*/
	}

	private boolean canReOpenBug(Bug bug) {
		String resolution = bug.getBugResolution();
		boolean retval = true;
		for (NON_REOPENABLE_RESOLUTION rr : NON_REOPENABLE_RESOLUTION.values()){
			if (rr.name().equals(resolution)) {
				retval = false;
				break;
			}
		}
		return retval;
	}

	@Override
	public void addCommentToBug(Bug bug, String comment, UserAuthenticationStore credentials) {

		/*final BugzillaConnector connector = connectToDimensions(credentials);
		final CommentBug commentBug = new CommentBug(Integer.parseInt(bug.getBugId()), comment);
		try {
			executeMethod(connector, commentBug);
		} catch (BugzillaException e) {
			throw new BugTrackerException(e.getMessage(), e);
		}*/
	}

	@Override
	public List<BugTrackerConfig> getConfiguration() {

        final BugTrackerConfig supportedVersions = new BugTrackerConfig()
                .setIdentifier(DISPLAY_ONLY_SUPPORTED_VERSION)
                .setDisplayLabel("Supported Versions")
                .setDescription("Dimensions CM versions supported by the plugin")
                .setValue(SUPPORTED_VERSIONS)
                .setRequired(false);

		BugTrackerConfig cmServerConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SERVER)
				.setDisplayLabel("Server Name")
				.setDescription("Dimensions CM Server Name")
				.setValue("localhost")
				.setRequired(true);

		BugTrackerConfig cmDbNameConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_DBNAME)
				.setDisplayLabel("Database Name")
				.setDescription("Dimensions CM Database Name")
				.setValue("cm_typical")
				.setRequired(true);

		BugTrackerConfig cmDbConnConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_DBCONN)
				.setDisplayLabel("Database Connection")
				.setDescription("Dimensions CM Database Connection")
				.setValue("dimcm")
				.setRequired(true);

		BugTrackerConfig cmWebUrlConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_WEB_URL)
				.setDisplayLabel("Dimensions CM Web URL")
				.setDescription("Dimensions CM Web Server Base URL")
				.setValue("http://localhost:8080/dimensions")
				.setRequired(true);

		BugTrackerConfig cmSuppReqTypeConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SUPPORTED_REQ_TYPE)
				.setDisplayLabel("Request Types")
				.setDescription("Supported Dimensions CM Request Types")
				.setValue("DEFECT,CR,TASK")
				.setRequired(true);

		BugTrackerConfig cmSeverityFieldNameConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SEVERITY_FIELD)
				.setDisplayLabel("Severity Field Name")
				.setDescription("The field name of a severity or priority field to set")
				.setValue("SEVERITY")
				.setRequired(false);

		BugTrackerConfig cmOwnerRoleConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_OWNER_ROLE)
				.setDisplayLabel("Owner Role")
				.setDescription("Dimensions CM Role to assign as Owner")
				.setValue("DEVELOPER")
				.setRequired(false);

		BugTrackerConfig cmOwnerCapabilityConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_OWNER_CAPABILITIES)
				.setDisplayLabel("Owner Capabilities")
				.setDescription("List of supported Dimensions CM Role Capabilities to assign Owner as ")
				.setValue("LEADER,PRIMARY,SECONDARY")
				.setRequired(false);

		List<BugTrackerConfig> configs = new ArrayList<>(Arrays.asList(supportedVersions, cmServerConfig, cmDbNameConfig,
				cmDbConnConfig, cmWebUrlConfig, cmSuppReqTypeConfig, cmSeverityFieldNameConfig, cmOwnerRoleConfig,
				cmOwnerCapabilityConfig));

		//configs.addAll(buildSscProxyConfiguration());
		pluginHelper.populateWithDefaultsIfAvailable(configs);
		return configs;
	}

	private List<BugTrackerConfig> buildSscProxyConfiguration() {
		/* Not supported */
		return null;
	}

	@Override
	public void setConfiguration(Map<String, String> config) {

		this.config = config;

		if (config.get(DIMCM_SERVER) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_SERVER);
		}

		if (config.get(DIMCM_DBNAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_DBNAME);
		}

		if (config.get(DIMCM_DBCONN) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_DBCONN);
		}

		if (config.get(DIMCM_SUPPORTED_REQ_TYPE) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_SUPPORTED_REQ_TYPE);
		}

		if (config.get(DIMCM_OWNER_ROLE) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_OWNER_ROLE);
		}

		cmServer = config.get(DIMCM_SERVER);
		cmDbName = config.get(DIMCM_DBNAME);
		cmDbCon = config.get(DIMCM_DBCONN);
		cmWebUrl = config.get(DIMCM_WEB_URL);
		cmSuppReqTypes = config.get(DIMCM_SUPPORTED_REQ_TYPE);
		cmSeverityField = config.get(DIMCM_SEVERITY_FIELD);
		cmOwnerRoleField = config.get(DIMCM_OWNER_ROLE);
		cmOwnerCapabilities = config.get(DIMCM_OWNER_CAPABILITIES);

	}

	@Override
	public void testConfiguration(com.fortify.pub.bugtracker.support.UserAuthenticationStore credentials) {
		validateCredentials(credentials);
	}

	@Override
	public void validateCredentials(UserAuthenticationStore credentials) {
		connectToDimensions(credentials);
	}

	@Override
	public String getShortDisplayName() {
		return "Dimensions CM";
	}

	@Override
	public String getLongDisplayName() {
		return "Dimensions CM at " + cmServer + ":" + cmDbName + "@" + cmDbCon;
	}

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	/**
	 * Method is executed if value of some of the parameters marked as HasDependentParams has been changed.
	 * @param issueDetail
	 *            an object encompassing the details of the issue for which the
	 *            user is attempting to log a bug
	 * @param changedParamIdentifier
	 *            the identifier of BugParam whose value changed on the UI
	 * @param currentValues
	 *            all of the BugParams and their current values
	 * @param credentials
	 *            bug tracker credentials supplied by the user
	 *
	 * @return list of BugParam objects. Current implementation just returns currentValues list.
	 */
	@Override
	public List<BugParam> onParameterChange(IssueDetail issueDetail, String changedParamIdentifier,
											List<BugParam> currentValues, UserAuthenticationStore credentials) {
        return innerOnParameterChange(changedParamIdentifier, currentValues, credentials);
	}

    private List<BugParam> innerOnParameterChange(
    		String modifiedParamId
			, List<BugParam> bugParams
			, UserAuthenticationStore credentials) {

        if (PRODUCT_PARAM_NAME.equals(modifiedParamId) || PROJECT_PARAM_NAME.equals(modifiedParamId)) {

			final DimCMClient cmClient = new DimCMClient();
			cmClient.connect(credentials.getUserName(), credentials.getPassword(), cmDbName, cmDbCon, cmServer);

			try {
				final BugParam productParam = pluginHelper.findParam(PRODUCT_PARAM_NAME, bugParams);
				final String curProduct = productParam.getValue();

				if (PRODUCT_PARAM_NAME.equals(modifiedParamId)) {
					final BugParamChoice reqTypeParam = (BugParamChoice)pluginHelper.findParam(REQ_TYPE_PARAM_NAME, bugParams);
					final BugParamChoice projectParam = (BugParamChoice)pluginHelper.findParam(PROJECT_PARAM_NAME, bugParams);
					final BugParamChoice partsParam = (BugParamChoice)pluginHelper.findParam(PARTS_PARAM_NAME, bugParams);
					final BugParamChoice severityParam = (BugParamChoice)pluginHelper.findParam(SEVERITY_PARAM_NAME, bugParams);
					final BugParamChoice ownerParam = (BugParamChoice)pluginHelper.findParam(OWNER_PARAM_NAME, bugParams);

					String reqTypesString = config.get(DIMCM_SUPPORTED_REQ_TYPE);
					String suppReqTypes[] =  reqTypesString.split(",");
					List<String> prodReqTypes = cmClient.getReqTypes(curProduct);
					List<String> filteredReqTypes = new ArrayList<>();
					for (String rt : suppReqTypes) {
						if (prodReqTypes.contains(rt)) {
							filteredReqTypes.add(rt);
						} else {
							LOG.info("Ignoring Request type " + rt + " as the product does not support it");
						}
					}
					reqTypeParam.setChoiceList(filteredReqTypes);
					projectParam.setChoiceList(cmClient.getProjectsStreams(curProduct));
					partsParam.setChoiceList(cmClient.getDesignParts(curProduct));
					String severityFieldName = config.get(DIMCM_SEVERITY_FIELD);
					if (severityFieldName != null && severityFieldName.length() > 0) {
						List<String> severities = cmClient.getFieldValues(config.get(DIMCM_SEVERITY_FIELD));
						severityParam.setChoiceList(severities);
					}
					String ownerFieldName = config.get(DIMCM_OWNER_ROLE);
					if (ownerFieldName != null && ownerFieldName.length() > 0) {
						List<String> usersInRole = cmClient.getRoleUsers(curProduct, ownerFieldName);
						ownerParam.setChoiceList(usersInRole);
					}

				}
			} catch (BugTrackerException e) {
				throw e;
			} catch (Exception e) {
				throw new BugTrackerException("Error while changing Dimensions CM bug fields configuration: " + e.getMessage(), e);
			}
        }
        return bugParams;
    }

	@Override
	public Bug fetchBugDetails(String bugId, UserAuthenticationStore credentials) {
		final DimCMClient connector = connectToDimensions(credentials);
		final GetBug getBug = new GetBug(Integer.parseInt(bugId));
		try {
			LOG.info("Retrieving request " + bugId + " from Dimensions CM");
			Request request = connector.getRequest(bugId);
			//return new Bug(String.valueOf(dtoBug.getID()), dtoBug.getStatus(), dtoBug.getResolution());
		} catch (Exception e) {
			throw new BugTrackerException("The bug status could not be fetched correctly", e);
		}
		return null;
	}

	private DimCMClient connectToDimensions(final UserAuthenticationStore credentials) {
		final DimCMClient cmClient = new DimCMClient();
		try {
			LOG.info("Connecting to Dimensions CM:");
			LOG.info("Server: " + cmServer);
			LOG.info("Database Name: " + cmDbName);
			LOG.info("Database Connection: " + cmDbCon);
			LOG.info("User: " + credentials.getUserName());
			cmClient.connect(credentials.getUserName(), credentials.getPassword(), cmDbName, cmDbCon, cmServer);
			LOG.info("Connected successfully");
		} catch (Exception ex) {
			LOG.error("Unable to connection to Dimensions CM: " + ex.toString());
			throw new BugTrackerException("Could not login to Dimensions server at " + cmServer, ex);
		}
		return cmClient;
	}

    @Override
	public String getBugDeepLink(String bugId) {
		LOG.info("Creating deep link for " + bugId);
		LOG.info(cmDbName + "@" + cmDbCon);
		String bugUrl = cmWebUrl;
		if (!bugUrl.endsWith("/")) bugUrl.concat("/");
		bugUrl = bugUrl + DIMCM_REQ_URL + bugId;
		bugUrl = bugUrl.replace("%DBNAME%", cmDbName);
		bugUrl = bugUrl.replace("%DBCONN%", cmDbCon);
		bugUrl = bugUrl.replace("%PULSESUITE%", "");
		bugUrl = bugUrl.replace("%PULSEPRODUCT%", "");
		LOG.info("Bug Full URL: " + bugUrl);
		return bugUrl;
	}

}
