package org.fogbowcloud.app.ps;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.api.dtos.ResourceNode;
import org.fogbowcloud.app.core.constants.ConfProperty;
import org.fogbowcloud.app.ps.Constants.Endpoint;
import org.fogbowcloud.app.ps.models.Node;
import org.fogbowcloud.app.ps.models.Pool;
import org.fogbowcloud.app.utils.HttpWrapper;

public class ProvisioningRequestHelper {

    private static final Logger logger = Logger.getLogger(ProvisioningRequestHelper.class);
    private final String serviceBaseUrl;
    private final Gson jsonUtil;

    public ProvisioningRequestHelper(Properties properties) {
        serviceBaseUrl = properties.getProperty(ConfProperty.PROVISIONING_SERVICE_URL.getProp());
        this.jsonUtil = new Gson();
    }

    public String createPool(String poolName) throws Exception {
        String poolId;
        String url = serviceBaseUrl + Endpoint.POOLS;

        JsonObject poolBody = new JsonObject();
        poolBody.addProperty(Constants.POOL_NAME_KEY, poolName);
        StringEntity body = new StringEntity(poolBody.toString());

        String jsonResponse = HttpWrapper.doRequest(HttpPost.METHOD_NAME, url, new LinkedList<>(), body);
        JsonObject response = this.jsonUtil.fromJson(jsonResponse, JsonObject.class);
        poolId = response.get(Constants.ID_KEY).getAsString();
        logger.info("Created pool [" + poolName + "] on provider service: " + poolId);
        return poolId;
    }

    public Pool getPool(String poolName) throws Exception {
        String url = String.format(serviceBaseUrl + Endpoint.POOL, poolName);
        String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, url, new LinkedList<>());
        Pool pool = this.jsonUtil.fromJson(jsonResponse, Pool.class);
        return pool;
    }

    public String addNode(String poolId, ResourceNode node) throws Exception {
        String url = String.format(serviceBaseUrl + Endpoint.NODES, poolId);

        JsonElement jsonNode = jsonUtil.toJsonTree(node);
        StringEntity body = new StringEntity(jsonNode.toString());

        String jsonResponse = HttpWrapper.doRequest(HttpPost.METHOD_NAME, url, new LinkedList<>(), body);
        JsonObject response = this.jsonUtil.fromJson(jsonResponse, JsonObject.class);
        String nodeId = response.get(Constants.ID_KEY).getAsString();
        logger.info("Created node [" + nodeId + "] pool [" + poolId + "] on provider service");
        return nodeId;
    }

    public Node getNode(String poolId, String nodeId) throws Exception {
        String url = String.format(serviceBaseUrl + Endpoint.NODE, poolId, nodeId);
        String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, url, new LinkedList<>());
        Node node = this.jsonUtil.fromJson(jsonResponse, Node.class);
        return node;
    }

    public boolean containsPool(String poolName) throws Exception {
        boolean answer = false;
        String url = serviceBaseUrl + Endpoint.POOLS;
        Type type = new TypeToken<Map<String, Pool>>() {}.getType();
        String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, url, new LinkedList<>());
        Map<String, Pool> pools = this.jsonUtil.fromJson(jsonResponse, type);
        if (pools.containsKey(poolName)) {
            answer = true;
        }
        return answer;
    }

    public String getPublicKey() throws Exception {
        String url = serviceBaseUrl + Endpoint.PUBLIC_KEY;
        String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, url, new LinkedList<>());
        JsonObject response = this.jsonUtil.fromJson(jsonResponse, JsonObject.class);
        return response.get("public_key").getAsString();
    }
}
