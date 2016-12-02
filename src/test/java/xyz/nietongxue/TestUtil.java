package xyz.nietongxue;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

import static junit.framework.TestCase.fail;

/**
 * Created by nielinjie on 9/13/16.
 */
public class TestUtil {



    public static AssertResponseBuilder assertResponse() {
        return new AssertResponseBuilder();
    }



    public static HttpPost postWithJSON(String url,JSONObject json) {
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        try {
            request.setEntity(new StringEntity(json.toString()));
        } catch (UnsupportedEncodingException e) {
            fail();
        }
        return request;
    }

    public static void assertRequestAndResponse(HttpUriRequest request, AssertResponseBuilder responseBuilder) {
        assertRequestAndResponse(request, null, responseBuilder);
    }

    public static void assertRequestAndResponse(HttpUriRequest request, Consumer<HttpRequest> buildRequest, AssertResponseBuilder responseBuilder) {
        if (null != buildRequest) {
            buildRequest.accept(request);
        }
        try {
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(request);
            responseBuilder.apply(response);
            EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            fail("io exception");
        }
    }

    public static void assertRequestAndResponse(String url, AssertResponseBuilder responseBuilder) {
        assertRequestAndResponse(url, null, responseBuilder);
    }


    public static void assertRequestAndResponse(String url, Consumer<HttpRequest> buildRequest, AssertResponseBuilder responseBuilder) {
        assertRequestAndResponse(new HttpGet(url), buildRequest, responseBuilder);
    }


}
