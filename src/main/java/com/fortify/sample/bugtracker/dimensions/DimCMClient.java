/**
 * (c) Copyright [2020] Micro Focus or one of its affiliates.
 */
package com.fortify.sample.bugtracker.dimensions;

import com.serena.dmclient.api.*;
import com.serena.dmclient.api.Filter.Criterion;
import com.serena.dmclient.collections.Types;
import com.serena.dmclient.objects.*;
import merant.adm.dimensions.objects.core.AdmAttrNames;
import merant.adm.dimensions.objects.core.AdmObject;
import merant.adm.exception.AdmObjectException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DimCMClient {
    private static final Log LOG = LogFactory.getLog(DimCMClient.class);

    private static final String ERROR_STR_DB = "Error: Could not connect to database %s@%s. Please check the database name and connection and verify that the remote listener is running";
    private static final String ERROR_STR_CREDS = "Error: User authentication failed";
    private static final String ERROR_STR_HOST = "Error: An unknown host or IP address was provided";

    private static final String DIMCM_AUTH_ERROR_CODE = "PRG4500325E";
    private static final String DIMCM_DB_ERROR_CODE = "Could not connect to database";

    private static final String DIMCM_DEFAULT_LIFECYCLE = "LC_DM_STAGE";

    private static final SimpleDateFormat DIMCM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS'Z'");

    public enum GetOptions {
        PROJECTS_AND_STREAMS, STREAMS, PROJECTS
    }

    public enum EntityType {
        BASELINE, STREAM, PROJECT;

        public static EntityType parseType(String type) {
            if (type.toUpperCase().equals("BASELINE")) {
                return BASELINE;
            }
            if (type.toUpperCase().equals("STREAM")) {
                return STREAM;
            }
            if (type.toUpperCase().equals("PROJECT")) {
                return PROJECT;
            }
            throw new IllegalArgumentException("There is no such entity type as '" + type + "' !");
        }
    }
    private DimensionsConnection connection;

    public void connect(String username, String password, String dbName, String dbConn, String server) {
        try {
            InetAddress ia = InetAddress.getByName(server);
            if (!ia.isReachable(5000)) {
                throw new RuntimeException("Error: An unknown host or IP address was provided - " + server);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error: An unknown host or IP address was provided - " + server, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            DimensionsConnectionDetails details = new DimensionsConnectionDetails();
            details.setUsername(username);
            details.setPassword(password);
            details.setDbName(dbName);
            details.setDbConn(dbConn);
            details.setServer(server);
            connection = DimensionsConnectionManager.getConnection(details);
        } catch (LoginFailedException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(DIMCM_AUTH_ERROR_CODE)) {
                System.err.println(ERROR_STR_CREDS);
            }
            if (e.getMessage() != null && e.getMessage().startsWith(DIMCM_DB_ERROR_CODE)) {
                System.err.println(String.format(ERROR_STR_DB, dbName, dbConn));
            }
            throw e;
        } catch (DimensionsRuntimeException e) {
            if (isCausedBy(e, UnknownHostException.class)) {
                System.err.println(ERROR_STR_HOST);
            }
            throw e;
        }
    }

    public List<String> getProducts() {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        List<Product> products = factory.getBaseDatabase().getProducts();

        List<String> res = new ArrayList<String>();
        for (Product p : products) {
            res.add(p.getName());
        }
        return res;
    }

    public List<String> getProjectsStreams(String productName) {
        return getProjectsStreams(productName, GetOptions.PROJECTS_AND_STREAMS);
    }

    @SuppressWarnings("unchecked")
    public List<String> getProjectsStreams(String productName, GetOptions opts) {
        productName = prepareDimCMParam(productName);
        DimensionsObjectFactory factory = connection.getObjectFactory();
        productShouldExist(factory, productName);

        Filter filter = new Filter();
        filter.criteria().add(new Filter.Criterion(SystemAttributes.PRODUCT_NAME, productName, Filter.Criterion.EQUALS));

        switch (opts) {
            case STREAMS:
                filter.criteria().add(new Filter.Criterion(SystemAttributes.WSET_IS_STREAM, "Y", Filter.Criterion.EQUALS));
                break;
            case PROJECTS:
                filter.criteria().add(new Filter.Criterion(SystemAttributes.WSET_IS_STREAM, "Y", Filter.Criterion.NOT));
                break;
            default:
                break;
        }

        List<Project> projects = factory.getProjects(filter);

        List<String> res = new ArrayList<String>();
        for (Project proj : projects) {
            res.add(cutProductName(proj.getName()));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public List<Part> getDesignPartAsList(String productName, String partName) {
        if (partName.contains(":")) partName = partName.replace(productName+":","");
        if (partName.contains(".")) partName = partName.replace(".A;1", "");
        DimensionsObjectFactory factory = connection.getObjectFactory();
        Product product = factory.getBaseDatabase().getProduct(productName);
        Filter filter = new Filter();
        List<Filter.Criterion> criteria = filter.criteria();
        if (partName != null) {
            criteria.add(new Filter.Criterion(SystemAttributes.OBJECT_ID, partName,
                    Criterion.EQUALS));
        }
        return product.getParts(filter);
    }

    @SuppressWarnings("unchecked")
    public List<String> getDesignParts(String productName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        Product product = factory.getBaseDatabase().getProduct(productName);
        Filter filter = new Filter();
        List<Part> parts = product.getParts(null);

        List<String> res = new ArrayList<String>();
        for (Part p : parts) {
            res.add(p.getName());
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public List<String> getReqTypes(String productName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        Product product = factory.getBaseDatabase().getProduct(productName);
        Types reqTypes = product.getRequestTypes();

        List<String> res = new ArrayList<String>();
        Iterator it = reqTypes.iterator();
        while (it.hasNext()) {

            res.add((String) it.next());
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public List<String> getFieldValues(String fieldName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        List<AttributeDefinition> attributeDefinitions = factory.getBaseDatabase().getAttributeDefinitions(Request.class, AttributeType.SFSV);

        List<String> res = new ArrayList<String>();
        for (AttributeDefinition attr : attributeDefinitions) {
            if (attr.getName().equals(fieldName.toUpperCase())) {
                ValidSet validSet = attr.getValidSet();
                List<ValidSetRowDetails> values = validSet.getValues();
                for (ValidSetRowDetails v : values) {
                    res.add(v.getColumnValue(0));
                }
            }
        }

        return res;
    }

    @SuppressWarnings("unchecked")
    public int getFieldId(String fieldName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        List<AttributeDefinition> attributeDefinitions = factory.getBaseDatabase().getAttributeDefinitions(Request.class, AttributeType.SFSV);

        List<String> res = new ArrayList<String>();
        for (AttributeDefinition attr : attributeDefinitions) {
            if (attr.getName().equals(fieldName.toUpperCase())) {
                return attr.getNumber();
            }
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoleUsers(String productName, String roleName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        Product product = factory.getBaseDatabase().getProduct(productName);
        List roleAssignments = product.getRoleAssignments();

        List<String> res = new ArrayList<String>();
        for (Object roleAssignment : roleAssignments) {
            RoleAssignmentDetails rad = (RoleAssignmentDetails) roleAssignment;
            if (rad.getRoleName().equals(roleName.toUpperCase())) {
                res.add(rad.getUserName());
            }
        }

        return res;
    }

    public DimensionsResult createRequest(
            String productName,
            String projectName,
            List<Part> part,
            String requestType,
            String summary,
            String description,
            String severity,
            String owner,
            String attributeNames) {
        productName = prepareDimCMParam(productName);
        projectName = prepareDimCMParam(projectName);

        DimensionsObjectFactory factory = connection.getObjectFactory();
        Product product = factory.getBaseDatabase().getProduct(productName);

        RequestDetails requestDetails = new RequestDetails();
        requestDetails.setProductName(productName);
        if (projectName != null) requestDetails.setRelatedProject(projectName);
        if (part.size() > 0) {
            requestDetails.setRelatedParts(part);
        }
        requestDetails.setTypeName(requestType);
        requestDetails.setDescription(summary);
        requestDetails.setDetailedDescription(description);

        if (severity != null) {
            requestDetails.setAttribute(factory.getAttributeNumber("SEVERITY", Request.class), severity);
        }
        if (!attributeNames.isEmpty()) {
            String[] attributePairs = attributeNames.split("\n");
            for (String pair : attributePairs) {
                String[] attributeArray = pair.split("=");
                requestDetails.setAttribute(factory.getAttributeNumber(attributeArray[0], Request.class), attributeArray[1]);
            }
        }

        return factory.createRequest(requestDetails);

    }

    public String getRequestIdFromResult(DimensionsResult result, String productName, String requestType) {
        String pattern = productName + "_" + requestType + "_\\d+";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(result.getMessage());
        if (m.find( )) {
            return m.group();
        } else {
            return null;
        }
    }

    public String getResultMessage(DimensionsResult result) {
        return result.getMessage();
    }

    public List getResultList(DimensionsResult result) {
        return result.getResultList();
    }

    public Request delegateRequest(String requestId, List users, String role, String capability) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        Request request = factory.findRequest(requestId);
        if (request != null) {
            request.delegateTo(users, role, capability, true);
        }
        return request;
    }

    public Request getRequest(String requestId) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        return factory.findRequest(requestId);
    }

    // =========================================================================
    // DimCM API helpers
    // =========================================================================

    private static Project getProjectIfExists(DimensionsObjectFactory factory, String product, String project) {
        return getProjectIfExists(factory, product + ":" + project);
    }

    private static Project getProjectIfExists(DimensionsObjectFactory factory, String projectFullName) {
        try {
            return factory.getProject(projectFullName);
        } catch (DimensionsRuntimeException e) {
            throw new RuntimeException("Error: the specified Project is not found!", e);
        }
    }

    private static Project getProjectIfExists(DimensionsObjectFactory factory, String projectFullName, boolean throwIfNull) {
        try {
            return factory.getProject(projectFullName);
        } catch (DimensionsRuntimeException e) {
            if (throwIfNull) {
                throw new RuntimeException("Error: the specified Project is not found!", e);
            }
            return null;
        }
    }

    private static boolean isProductExists(DimensionsObjectFactory factory, String productName) {
        List<Product> list = factory.getBaseDatabase().getProducts();
        for (Product product : list) {
            if (product.getName().equals(productName)) {
                return true;
            }
        }
        return false;
    }

    private static void productShouldExist(DimensionsObjectFactory factory, String productName) {
        if (!isProductExists(factory, productName)) {
            throw new RuntimeException("Error: the specified product name - " + productName + " - does not exist");
        }
    }

    private static List<String> getLifeCycleStages(DimensionsObjectFactory factory) {
        List<String> res = new ArrayList<String>();
        Lifecycle lc = factory.getBaseDatabase().getLifecycle(DIMCM_DEFAULT_LIFECYCLE);
        for (Object o : lc.getNormalStates()) {
            res.add(o.toString());
        }
        return res;
    }

    // ========================================================================
    // Simple Helper methods
    // ========================================================================

    private static String prepareDimCMParam(String param) {
        if (param == null) {
            return null;
        }
        return param.toUpperCase();
    }

    private static String escapeString(String str) {
        return "\"" + str + "\"";
    }

    private static String cleverEscapeString(String str) {
        if(str.contains(" ")){
            return "\"" + str + "\"";
        }
        return str;
    }

    private static String formatAttributes(String str) {
        return "(" + trimTextAreaLines(str).replace('\n', ',') + ")";
    }

    private static boolean isCausedBy(Throwable e, Class<? extends Throwable> cl) {
        Throwable cause;
        while ((cause = e.getCause()) != null) {
            if (cl.isAssignableFrom(cause.getClass())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused") //Debug utility
    public static void viewAttributes(DimensionsArObject obj, int... attr_id) {
        System.out.println("Attributes for " + obj.getName());
        for (int i : attr_id) {
            System.out.println(i);
            obj.queryAttribute(i);
            System.out.println(obj.getAttribute(i));
        }
    }

    /**
     * Replaces ; with \n.
     * Removes spaces at beginning and ending of each line.
     * Removes duplicating \n characters.
     */
    private static String trimTextAreaLines(String str){
        return str.replaceAll(";", "\n").replaceAll("\\s*(\n|^|$)\\s*", "$1").replaceAll("\\s*\n\\s*\n*\\s*", "\n");
    }

    private static String cutProductName(String str){
        int pos = str.indexOf(":");
        if (pos > 0){
            return str.substring(pos + 1);
        }
        return str;
    }


    // ========================================================================
    // Example invocation
    // ========================================================================
    public static void main(final String[] args) {

        DimCMClient cmClient = new DimCMClient();
        cmClient.connect("dmsys", "", "cm_typical", "dimcm", "localhost");
        System.out.println(cmClient.getProjectsStreams("QLARIUS"));
        System.out.println(cmClient.getDesignParts("QLARIUS"));
        List <Part> parts = cmClient.getDesignPartAsList("QLARIUS", "QLARIUS:WEBSITE.A;1");
        for (Part p : parts) {
            System.out.println(p.getAttribute(SystemAttributes.OBJECT_ID));
        }
        // create a new request
        DimensionsResult result = cmClient.createRequest("QLARIUS", "MAINLINE_JAVA_STR", parts,
                "TASK", "this is the summary", "this is the description",
                "High", "JOSH", "");
        String requestId = cmClient.getRequestIdFromResult(result, "QLARIUS", "TASK");
        List<String> users = new ArrayList<>();
        users.add("JOSH");
        cmClient.delegateRequest(requestId, users, "DEVELOPER", "SECONDARY");
        // get the request
        Request requestObj = (Request) cmClient.getRequest(requestId);
        int solutionFieldId = cmClient.getFieldId("DETAILS_OF_THE_SOLUTION");
        requestObj.queryAttribute(new int[]{SystemAttributes.TITLE,
                SystemAttributes.DESCRIPTION,
                SystemAttributes.OBJECT_SPEC,
                SystemAttributes.STATUS,
                solutionFieldId});
        System.out.println("REQUEST_ID : " + requestObj.getAttribute(SystemAttributes.OBJECT_ID));
        System.out.println("TITLE     : "  + requestObj.getAttribute(SystemAttributes.TITLE));
        System.out.println("STATUS    : "  + requestObj.getLcState());
        System.out.println("SOLUTION  : "  + requestObj.getAttribute(solutionFieldId));
    }
}

