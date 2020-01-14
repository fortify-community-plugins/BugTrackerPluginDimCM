/**
 * (c) Copyright [2020] Micro Focus or one of its affiliates.
 */
package com.fortify.sample.bugtracker.dimensions;

import com.fortify.pub.bugtracker.plugin.AbstractBatchBugTrackerPlugin;
import com.fortify.pub.bugtracker.plugin.BatchBugTrackerPlugin;
import com.fortify.pub.bugtracker.plugin.BugTrackerPluginImplementation;
import com.fortify.pub.bugtracker.support.*;
import com.serena.dmclient.api.DimensionsResult;
import com.serena.dmclient.api.Part;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.fortify.pub.bugtracker.support.BugTrackerPluginConstants.DISPLAY_ONLY_SUPPORTED_VERSION;
import static com.fortify.sample.bugtracker.dimensions.DimCMPluginConstants.*;

/**
 * This plugin is used for submitting bugs into to the Dimensions CM.
 * Dimensions CM Java API is used for communicating with bug tracker via DimCMClient class.
 *
 * Logging is written to %FORTIFY_HOME%\ssc\plugin-framework\logs
 * A "Bug Filing" Template should be created for submitting into Dimensions CM (see Administration->Templates)
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
	private String cmSuppReqTypes;
	private String cmSeverityField;
	private String cmOwnerRoleField;
	private String cmOwnerCapabilities;
	private String cmBugUrl;
	private String cmResolutionField;
	private Map<String, String> config; // Full Dimensions CM plugin configuration


	@Override
	public List<BugParam> getBatchBugParameters(UserAuthenticationStore credentials) {
		return getBugParameters(null, credentials);
	}

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
				final String[] capabilities = cmOwnerCapabilities.split(",");
				ownerCapabilityParam = getOwnerCapabilityParamChoice(new ArrayList<String>(Arrays.asList(capabilities)));
			} else {
            	/* default to some values if not specified */
            	final List<String> capabilities = new ArrayList<>();
            	capabilities.add("PRIMARY");
            	capabilities.add("SECONDARY");
				ownerCapabilityParam = getOwnerCapabilityParamChoice(capabilities);
			}
			return Arrays.asList(summaryParam, descriptionParam, productParam, reqTypeParam, projectParam, partsParam,
					severityParam, ownerParam, ownerCapabilityParam, additionalFieldsParam);

		} catch (BugTrackerException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new BugTrackerException("Error while setting Dimensions CM bug fields configuration: " + ex.getMessage(), ex);
		}
	}

	private BugParam getSummaryParamText(IssueDetail issueDetail) {
		BugParam titleParam = new BugParamText()
			.setIdentifier(TITLE_PARAM_NAME)
			.setDisplayLabel(TITLE_LABEL)
			.setRequired(true)
			.setDescription(TITLE_DESCRIPTION);
		if (issueDetail == null) {
			titleParam.setValue("Fix $ATTRIBUTE_CATEGORY$ in $ATTRIBUTE_FILE$");
		} else {
			titleParam.setValue(issueDetail.getSummary());
		}
		return titleParam;
	}

	private BugParam getDescriptionParamText(final IssueDetail issueDetail) {
		BugParam descriptionParam = new BugParamTextArea()
			.setIdentifier(DESCRIPTION_PARAM_NAME)
			.setDisplayLabel(DESCRIPTION_LABEL)
			.setDescription(DESCRIPTION_DESCRIPTION)
			.setRequired(true);
		if (issueDetail == null) {
			descriptionParam.setValue("Issue Ids: $ATTRIBUTE_INSTANCE_ID$\n$ISSUE_DEEPLINK$");
		} else {
			descriptionParam.setValue(pluginHelper.buildDefaultBugDescription(issueDetail, true));
		}
		return descriptionParam;
	}


	private BugParam getAdditionalFieldsParamText(final IssueDetail issueDetail) {
		return new BugParamTextArea()
			.setIdentifier(ADDITIONAL_FIELDS_PARAM_NAME)
			.setDisplayLabel(ADDITIONAL_FIELDS_LABEL)
			.setDescription(ADDITIONAL_FIELDS_DESCRIPTION);
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
			LOG.info("Filing Bug in Dimensions CM:");
			List <Part> parts = connector.getDesignPartAsList(bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(PARTS_PARAM_NAME));
			if (LOG.isDebugEnabled()) {
				LOG.debug("Product:" + bugParams.get(PRODUCT_PARAM_NAME));
				LOG.debug("Project:" + bugParams.get(PROJECT_PARAM_NAME));
				LOG.debug("Type:" + bugParams.get(REQ_TYPE_PARAM_NAME));
				LOG.debug("Summary:" + bugParams.get(TITLE_PARAM_NAME));
				LOG.debug("Description:" + bugParams.get(DESCRIPTION_PARAM_NAME));
				LOG.debug("Severity:" + bugParams.get(SEVERITY_PARAM_NAME));
				LOG.debug("Owner:" + bugParams.get(OWNER_PARAM_NAME));
				LOG.debug("Design Part: " + bugParams.get(PARTS_PARAM_NAME));
				LOG.debug("Design Part (Object): " + parts.toString());
				LOG.debug("Additional Fields:" + bugParams.get(ADDITIONAL_FIELDS_PARAM_NAME));
			}
			DimensionsResult result = connector.createRequest(bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(PROJECT_PARAM_NAME), parts,
					bugParams.get(REQ_TYPE_PARAM_NAME), bugParams.get(TITLE_PARAM_NAME), bugParams.get(DESCRIPTION_PARAM_NAME),
					bugParams.get(SEVERITY_PARAM_NAME), bugParams.get(OWNER_PARAM_NAME), bugParams.get(ADDITIONAL_FIELDS_PARAM_NAME));
			String bugId = connector.getRequestIdFromResult(result, bugParams.get(PRODUCT_PARAM_NAME), bugParams.get(REQ_TYPE_PARAM_NAME));
			LOG.info("Created Dimensions CM Request with Id: " + bugId);
			if (bugParams.get(OWNER_PARAM_NAME) != null && (bugParams.get(OWNER_PARAM_NAME)).length() > 0) {
				LOG.info("Delegating CM Request");
				if (LOG.isDebugEnabled()) {
					LOG.debug("Delegating Dimensions CM Request:");
					LOG.debug("Request Id: " + bugId);
					LOG.debug("Owner: " + bugParams.get(OWNER_PARAM_NAME));
					LOG.debug("Role: " + cmOwnerRoleField);
					LOG.debug("Capability: " + bugParams.get(OWNER_CAPABILITY_PARAM_NAME).substring(0, 1));
				}
				List<String> users = new ArrayList<>();
				users.add(bugParams.get(OWNER_PARAM_NAME));
				connector.delegateRequest(bugId, users, cmOwnerRoleField, bugParams.get(OWNER_CAPABILITY_PARAM_NAME).substring(0,1));
				LOG.info("Delegated request to: " + bugParams.get(OWNER_PARAM_NAME));
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
		LOG.info("Trying to reopen Bug " + bug.getBugId() + " but not currently supported");
		/*if (!canReOpenBug(bug)) {
			throw new BugTrackerException("Bug " + bug.getBugId() + " cannot be reopened.");
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
		LOG.info("Trying to add comment to Bug " + bug.getBugId() + " but not currently supported");
	}

	@Override
	public List<BugTrackerConfig> getConfiguration() {

        final BugTrackerConfig supportedVersions = new BugTrackerConfig()
                .setIdentifier(DISPLAY_ONLY_SUPPORTED_VERSION)
                .setDisplayLabel(SUPPORTED_VERSION_LABEL)
                .setDescription(SUPPORTED_VERSION_DESCRIPTION)
                .setValue(SUPPORTED_VERSION_DEFAULT_VALUE)
                .setRequired(false);

		BugTrackerConfig cmServerConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SERVER_CONFIG_NAME)
				.setDisplayLabel(DIMCM_SERVER_LABEL)
				.setDescription(DIMCM_SERVER_DESCRIPTION)
				.setValue(DIMCM_SERVER_DEFAULT_VALUE)
				.setRequired(true);

		BugTrackerConfig cmDbNameConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_DBNAME_CONFIG_NAME)
				.setDisplayLabel(DIMCM_DBNAME_LABEL)
				.setDescription(DIMCM_DBNAME_DESCRIPTION)
				.setValue(DIMCM_DBNAME_DEFAULT_VALUE)
				.setRequired(true);

		BugTrackerConfig cmDbConnConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_DBCONN_CONFIG_NAME)
				.setDisplayLabel(DIMCM_DBCONN_LABEL)
				.setDescription(DIMCM_DBCONN_DESCRIPTION)
				.setValue(DIMCM_DBCONN_DEFAULT_VALUE)
				.setRequired(true);

		BugTrackerConfig cmSuppReqTypeConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME)
				.setDisplayLabel(DIMCM_SUPPORTED_REQ_TYPE_LABEL)
				.setDescription(DIMCM_SUPPORTED_REQ_TYPE_DESCRIPTION)
				.setValue(DIMCM_SUPPORTED_REQ_TYPE_DEFAULT_VALUE)
				.setRequired(true);

		BugTrackerConfig cmSeverityFieldNameConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_SEVERITY_FIELD_CONFIG_NAME)
				.setDisplayLabel(DIMCM_SEVERITY_LABEL)
				.setDescription(DIMCM_SEVERITY_DESCRIPTION)
				.setValue(DIMCM_SEVERITY_DEFAULT_VALUE)
				.setRequired(false);

		BugTrackerConfig cmOwnerRoleConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_OWNER_ROLE_CONFIG_NAME)
				.setDisplayLabel(DIMCM_OWNER_ROLE_LABEL)
				.setDescription(DIMCM_OWNER_ROLE_DESCRIPTION)
				.setValue(DIMCM_OWNER_ROLE_DEFAULT_VALUE)
				.setRequired(false);

		BugTrackerConfig cmOwnerCapabilityConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_OWNER_CAPABILITIES_CONFIG_NAME)
				.setDisplayLabel(DIMCM_OWNER_CAPABILITIES_LABEL)
				.setDescription(DIMCM_OWNER_CAPABILITIES_DESCRIPTION)
				.setValue(DIMCM_OWNER_CAPABILITIES_DEFAULT_VALUE)
				.setRequired(false);

		BugTrackerConfig cmResolutionFieldNameConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_RESOLUTION_FIELD_CONFIG_NAME)
				.setDisplayLabel(DIMCM_RESOLUTION_LABEL)
				.setDescription(DIMCM_RESOLUTION_DESCRIPTION)
				.setValue(DIMCM_RESOLUTION_DEFAULT_VALUE)
				.setRequired(false);

		BugTrackerConfig cmBugUrlConfig = new BugTrackerConfig()
				.setIdentifier(DIMCM_BUG_URL_CONFIG_NAME)
				.setDisplayLabel(DIMCM_BUG_URL_LABEL)
				.setDescription(DIMCM_BUG_URL_DESCRIPTION)
				.setValue(DIMCM_BUG_URL_DEFAULT_VALUE)
				.setRequired(true);

		List<BugTrackerConfig> configs = new ArrayList<>(Arrays.asList(supportedVersions, cmServerConfig, cmDbNameConfig,
				cmDbConnConfig, cmSuppReqTypeConfig, cmSeverityFieldNameConfig, cmOwnerRoleConfig,
				cmOwnerCapabilityConfig, cmResolutionFieldNameConfig, cmBugUrlConfig));

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

		if (config.get(DIMCM_SERVER_CONFIG_NAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_SERVER_CONFIG_NAME);
		}

		if (config.get(DIMCM_DBNAME_CONFIG_NAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_DBNAME_CONFIG_NAME);
		}

		if (config.get(DIMCM_DBCONN_CONFIG_NAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_DBCONN_CONFIG_NAME);
		}

		if (config.get(DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME);
		}

		if (config.get(DIMCM_OWNER_ROLE_CONFIG_NAME) == null) {
			throw new IllegalArgumentException("Invalid configuration passed: no " + DIMCM_OWNER_ROLE_CONFIG_NAME);
		}

		cmServer = config.get(DIMCM_SERVER_CONFIG_NAME);
		cmDbName = config.get(DIMCM_DBNAME_CONFIG_NAME);
		cmDbCon = config.get(DIMCM_DBCONN_CONFIG_NAME);
		cmUsername = config.get(DIMCM_USERNAME_CONFIG_NAME);
		cmPassword= config.get(DIMCM_PASSWORD_CONFIG_NAME);
		cmBugUrl = config.get(DIMCM_BUG_URL_CONFIG_NAME);
		cmSuppReqTypes = config.get(DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME);
		cmSeverityField = config.get(DIMCM_SEVERITY_FIELD_CONFIG_NAME);
		cmOwnerRoleField = config.get(DIMCM_OWNER_ROLE_CONFIG_NAME);
		cmResolutionField = config.get(DIMCM_RESOLUTION_FIELD_CONFIG_NAME);
		cmOwnerCapabilities = config.get(DIMCM_OWNER_CAPABILITIES_CONFIG_NAME);
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

	@Override
	public List<BugParam> onParameterChange(IssueDetail issueDetail, String changedParamIdentifier,
											List<BugParam> currentValues, UserAuthenticationStore credentials) {
        return innerOnParameterChange(changedParamIdentifier, currentValues, credentials);
	}

    private List<BugParam> innerOnParameterChange(String modifiedParamId, List<BugParam> bugParams,
												  UserAuthenticationStore credentials) {

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

					String reqTypesString = config.get(DIMCM_SUPPORTED_REQ_TYPE_CONFIG_NAME);
					String[] suppReqTypes =  reqTypesString.split(",");
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
					String severityFieldName = config.get(DIMCM_SEVERITY_FIELD_CONFIG_NAME);
					if (severityFieldName != null && severityFieldName.length() > 0) {
						List<String> severities = cmClient.getFieldValues(config.get(DIMCM_SEVERITY_FIELD_CONFIG_NAME));
						severityParam.setChoiceList(severities);
					}
					String ownerFieldName = config.get(DIMCM_OWNER_ROLE_CONFIG_NAME);
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
		LOG.info("Trying to fetch Bug " + bugId + " but not currently supported");

		/*final DimCMClient connector = connectToDimensions(credentials);
		try {
			LOG.info("Retrieving request " + bugId + " from Dimensions CM");
			Request request = connector.getRequest(bugId);
			//return new Bug(String.valueOf(dtoBug.getID()), dtoBug.getStatus(), dtoBug.getResolution());
		} catch (Exception e) {
			throw new BugTrackerException("The bug status could not be fetched correctly", e);
		}*/
		return null;
	}

	private DimCMClient connectToDimensions(final UserAuthenticationStore credentials) {
		final DimCMClient cmClient = new DimCMClient();
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Connecting to Dimensions CM:");
				LOG.debug("Server: " + cmServer);
				LOG.debug("Database Name: " + cmDbName);
				LOG.debug("Database Connection: " + cmDbCon);
				LOG.debug("User: " + credentials.getUserName());
			}
			cmClient.connect(credentials.getUserName(), credentials.getPassword(), cmDbName, cmDbCon, cmServer);
			LOG.info("Connected to Dimensions CM successfully");
		} catch (Exception ex) {
			LOG.error("Unable to connection to Dimensions CM: " + ex.toString());
			throw new BugTrackerException("Could not login to Dimensions server at " + cmServer, ex);
		}
		return cmClient;
	}

    @Override
	public String getBugDeepLink(String bugId) {
		String bugUrl = cmBugUrl;
		bugUrl = bugUrl.replace("%DBNAME%", cmDbName);
		bugUrl = bugUrl.replace("%DBCONN%", cmDbCon);
		bugUrl = bugUrl.replace("%BUG_ID%", bugId);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Creating deep link for Bug " + bugId);
			LOG.debug("Bug Full URL: " + bugUrl);

		}
		return bugUrl;
	}

}
