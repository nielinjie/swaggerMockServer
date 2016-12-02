package xyz.nietongxue;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by nielinjie on 02/12/2016.
 */
public class AssertResponseBuilder {


    private List<Consumer<HttpResponse>> asserts = new ArrayList<>();
    private String body = null;

    public AssertResponseBuilder status(int status) {
        asserts.add((HttpResponse response) -> assertEquals(status, response.getStatusLine()
                .getStatusCode()));
        return this;
    }


    public void apply(HttpResponse response) {
        asserts.forEach((Consumer<HttpResponse> c) ->
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
        asserts.add((HttpResponse response) -> {
            String finalBody = this.getBody(response);
            for (String text : texts) {
                assertTrue("must contain - " + text, finalBody.contains(text));
            }
        });
        return this;
    }

    public AssertResponseBuilder bodyNotContains(String... texts) {
        asserts.add((HttpResponse response) -> {
            String finalBody = this.getBody(response);
            for (String text : texts) {
                assertFalse("must not contain - " + text, finalBody.contains(text));
            }
        });
        return this;
    }

}
