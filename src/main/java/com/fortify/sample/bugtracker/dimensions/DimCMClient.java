package com.fortify.sample.bugtracker.dimensions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.serena.dmclient.api.*;
import com.serena.dmclient.collections.Products;
import com.serena.dmclient.collections.Types;
import com.serena.dmclient.objects.*;
import merant.adm.dimensions.cmds.AdmCmd;
import merant.adm.dimensions.cmds.helper.IDeploymentViewConstants;
import merant.adm.dimensions.cmds.helper.RangeInfo;
import merant.adm.dimensions.cmds.interfaces.CmdArguments;
import merant.adm.dimensions.cmds.interfaces.Relatable;
import merant.adm.dimensions.objects.BaseDatabase;
import merant.adm.dimensions.objects.DeploymentHistoryRecord;
import merant.adm.dimensions.objects.DeploymentViewContext;
import merant.adm.dimensions.objects.collections.FilterCriterion;
import merant.adm.dimensions.objects.core.AdmAttrNames;
import merant.adm.dimensions.objects.core.AdmObject;
import merant.adm.dimensions.objects.userattrs.FilterImpl;
import merant.adm.exception.AdmException;
import merant.adm.exception.AdmObjectException;
import merant.adm.framework.Cmd;

import com.serena.dmclient.api.Filter.Criterion;
import com.serena.dmclient.collections.BuildStages;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DimCMClient {
    private static final Log LOG = LogFactory.getLog(DimCMClient.class);

    private static final String ERROR_STR_DB = "Error: Could not connect to database %s@%s. Please check the database name and connection and verify that the remote listener is running";
    private static final String ERROR_STR_CREDS = "Error: User authentication failed";
    private static final String ERROR_STR_HOST = "Error: An unknown host or IP address was provided";

    private static final String DIMCM_AUTH_ERROR_CODE = "PRG4500325E";
    private static final String DIMCM_DB_ERROR_CODE = "Could not connect to database";

    private static final String DIMCM_DEFAULT_LIFECYCLE = "LC_DM_STAGE";

    private static final SimpleDateFormat DIMCM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS'Z'");

    private static final Comparator<AdmObject> AMD_OBJ_BY_DATE_COMPARATOR = new Comparator<AdmObject>() {
        @Override
        public int compare(AdmObject o1, AdmObject o2) {
            try {
                String dateStr1 = o1.getAttrValue(AdmAttrNames.HISTORY_EVENT_DATE).toString();
                String dateStr2 = o2.getAttrValue(AdmAttrNames.HISTORY_EVENT_DATE).toString();
                Date d1 = DIMCM_DATE_FORMAT.parse(dateStr1);
                Date d2 = DIMCM_DATE_FORMAT.parse(dateStr2);
                return d2.compareTo(d1); // INVERSED ORDER! - we need latest date first
            } catch (AdmObjectException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    };

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
        System.out.println("Connection established");
    }

    public List<String> getProducts() {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
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

    public List<Part> getDesignPartAsList(String productName, String partName) {
        if (partName.contains(":")) partName = partName.replace(productName+":","");
        if (partName.contains(".")) partName = partName.replace(".A;1", "");
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
        Product product = factory.getBaseDatabase().getProduct(productName);
        Filter filter = new Filter();
        List<Filter.Criterion> criteria = filter.criteria();
        if (partName != null) {
            criteria.add(new Filter.Criterion(SystemAttributes.OBJECT_ID, partName,
                    Criterion.EQUALS));
        }
        return product.getParts(filter);
    }

    public List<String> getDesignParts(String productName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
        Product product = factory.getBaseDatabase().getProduct(productName);
        Filter filter = new Filter();
        List<Part> parts = product.getParts(null);

        List<String> res = new ArrayList<String>();
        for (Part p : parts) {
            res.add(p.getName());
        }
        return res;
    }

    public List<String> getReqTypes(String productName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
        Product product = factory.getBaseDatabase().getProduct(productName);
        Types reqTypes = product.getRequestTypes();

        List<String> res = new ArrayList<String>();
        Iterator it = reqTypes.iterator();
        while (it.hasNext()) {

            res.add((String) it.next());
        }
        return res;
    }

    public List<String> getFieldValues(String fieldName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
        List<AttributeDefinition> attributeDefinitions = factory.getBaseDatabase().getAttributeDefinitions(Request.class, AttributeType.SFSV);

        List<String> res = new ArrayList<String>();
        Iterator it = attributeDefinitions.iterator();
        while (it.hasNext()) {
            AttributeDefinition attr = (AttributeDefinition) it.next();
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

    public List<String> getRoleUsers(String productName, String roleName) {
        DimensionsObjectFactory factory = connection.getObjectFactory();
        @SuppressWarnings("unchecked")
        Product product = factory.getBaseDatabase().getProduct(productName);
        List roleAssignments = product.getRoleAssignments();

        List<String> res = new ArrayList<String>();
        Iterator it = roleAssignments.iterator();
        while (it.hasNext()) {
            RoleAssignmentDetails rad = (RoleAssignmentDetails) it.next();
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

        DimensionsResult result = factory.createRequest(requestDetails);
        return result;

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
            Project p = factory.getProject(projectFullName);
            return p;
        } catch (DimensionsRuntimeException e) {
            throw new RuntimeException("Error: the specified Project is not found!", e);
        }
    }

    private static Project getProjectIfExists(DimensionsObjectFactory factory, String projectFullName, boolean throwIfNull) {
        try {
            Project p = factory.getProject(projectFullName);
            return p;
        } catch (DimensionsRuntimeException e) {
            if (throwIfNull) {
                throw new RuntimeException("Error: the specified Project is not found!", e);
            }
            return null;
        }
    }

    private static boolean isProductExists(DimensionsObjectFactory factory, String productName) {
        @SuppressWarnings("unchecked")
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
        for (Iterator<?> i = lc.getNormalStates().iterator(); i.hasNext(); ){
            res.add(i.next().toString());
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
    private static void viewAttributes(DimensionsArObject obj, int... attr_id) {
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
        cmClient.connect("dmsys", "dmsys", "cm_typical", "dimcm", "wins2016srg");
        String bugUrl = "?jsp=api&command=opencd&DB_CONN=%DBCONN%&DB_NAME=%DBNAME%&object_id=";
        bugUrl = bugUrl.replace("%DBCONN%", "abcedf");
        System.out.println(bugUrl);
        //System.out.println(cmClient.getProjectsStreams("QLARIUS"));
        //System.out.println(cmClient.getDesignParts("QLARIUS"));
        //List <Part> parts = cmClient.getDesignPartAsList("QLARIUS", "QLARIUS:WEBSITE.A;1");
        //for (Part p : parts) {
        //    System.out.println(p.getAttribute(SystemAttributes.OBJECT_ID));
        //}
        //System.out.println(cmClient.getFieldValues("SEVERITY"));
        //System.out.println(cmClient.getRoleUsers("JWA", "DEVELOPER"));

        //DimensionsResult result = cmClient.createRequest("QLARIUS", "MAINLINE_JAVA_STR", parts, "TASK",
        //        "this is the summary", "this is the description", "High", "JOSH", "");
        //System.out.println(cmClient.getResultMessage(result));
        //System.out.println(">"+cmClient.getRequestIdFromResult(result, "QLARIUS", "TASK")+"<");
        //List<String> users = new ArrayList<>();
        //users.add("JOSH");
        //cmClient.delegateRequest("QLARIUS_TASK_23", users, "DEVELOPER", "S");
    }
}

