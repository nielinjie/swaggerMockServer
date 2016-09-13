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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.awt.SystemColor.text;
import static java.util.Arrays.stream;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by nielinjie on 9/13/16.
 */
public class TestUtil {



    public static AssertResponseBuilder assertResponse() {
        return new AssertResponseBuilder();
    }



    public static void checkWithAssert(HttpUriRequest request, AssertResponseBuilder responseBuilder) {
        checkWithAssert(request, null, responseBuilder);
    }

    public static void checkWithAssert(HttpUriRequest request, Consumer<HttpRequest> buildRequest, AssertResponseBuilder responseBuilder) {
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

    public static void checkWithAssert(String url, AssertResponseBuilder responseBuilder) {
        checkWithAssert(url, null, responseBuilder);
    }


    public static void checkWithAssert(String url, Consumer<HttpRequest> buildRequest, AssertResponseBuilder responseBuilder) {
        checkWithAssert(new HttpGet(url), buildRequest, responseBuilder);
    }


    public static class AssertResponseBuilder {


        private List<Consumer<HttpResponse>> assertors = new ArrayList<>();
        private String body = null;

        public AssertResponseBuilder status(int status) {
            assertors.add((HttpResponse response) -> assertEquals(status, response.getStatusLine()
                    .getStatusCode()));
            return this;
        }


        public void apply(HttpResponse response) {
            assertors.forEach((Consumer<HttpResponse> c) ->
                    c.accept(response));
        }

        public String getBody(HttpResponse response) {
            if (this.body == null) {
                try {
                    this.body = EntityUtils.toString(response.getEntity());
                } catch (IOException e) {
                    fail();
                }
            }
            return this.body;
        }

        public AssertResponseBuilder bodyContains(String... texts) {
            assertors.add((HttpResponse response) -> {
                String finalBody = this.getBody(response);
                for(String text:texts){
                    assertTrue("must contain - "+text,finalBody.contains(text));
                }
            });
            return this;
        }

        public AssertResponseBuilder bodyNotContains(String... texts) {
            assertors.add((HttpResponse response) -> {
                String finalBody = this.getBody(response);
                for(String text:texts){
                    assertFalse("must not contain - "+text,finalBody.contains(text));
                }
            });
            return this;
        }

    }


}
