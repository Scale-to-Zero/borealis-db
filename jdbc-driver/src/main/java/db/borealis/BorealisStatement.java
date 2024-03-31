package db.borealis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest.Builder;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class BorealisStatement extends AbstractStatement {
    private final LambdaClient lambdaClient;
    private Builder requestBuilder;

    public BorealisStatement(BorealisConnection conn) {
        this.lambdaClient = LambdaClient.builder()
                .region(Region.of(conn.regionName))
                .credentialsProvider(ProfileCredentialsProvider.create(conn.profileName))
                .build();

        this.requestBuilder = InvokeRequest.builder()
                .functionName(conn.functionName);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        Map<String, String> payload = new HashMap<>();
        payload.put("query", sql);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        InvokeRequest invokeRequest = requestBuilder
                .payload(SdkBytes.fromUtf8String(jsonPayload))
                .build();

        try {
            InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
            String response = invokeResponse.payload().asUtf8String();

            JsonObject body = parseLambdaResponse(response);
            JsonArray rows = body.getAsJsonArray("rows");
            JsonArray columns = body.getAsJsonArray("column_names");
            return new BorealisResultSet(columns, rows);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        Map<String, String> payload = new HashMap<>();
        payload.put("query", sql);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);

        InvokeRequest invokeRequest = requestBuilder
                .payload(SdkBytes.fromUtf8String(jsonPayload))
                .build();

        String response = "n/a";
        try {
            InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
            response = invokeResponse.payload().asUtf8String();

            JsonObject body = parseLambdaResponse(response);
            int rowsAffected = body.get("rowsAffected").getAsInt();

            return rowsAffected;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(response, e);
        }
    }

    private JsonObject parseLambdaResponse(String response) throws SQLException {
        JsonObject jo = new Gson().fromJson(response, JsonObject.class);
        int status = jo.get("statusCode").getAsInt();
        if (status == 500 && jo.has("body")) {
            String error = new Gson().fromJson(jo.get("body").getAsString(),
                    JsonObject.class).get("error").getAsString();
            throw new SQLException(error);
        }
        if (status != 200) {
            throw new IllegalStateException("statusCode != 200: " + response);
        }
        JsonObject body = new Gson().fromJson(jo.get("body").getAsString(), JsonObject.class);
        return body;
    }

}