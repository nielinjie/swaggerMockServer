package xyz.nietongxue.mockServer;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.fail;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static xyz.nietongxue.TestUtil.assertResponse;
import static xyz.nietongxue.TestUtil.checkWithAssert;


/**
 * Created by nielinjie on 9/13/16.
 */
public class PostPeopleRequestTest {
    private static final String HTTP_LOCALHOST_8081_V1_PEOPLE = "http://localhost:8081/v1/people";
    private static MockServer server;

    @BeforeClass
    public static void init() {
        try {
            server = new MockServer("./src/test/resources/people.yaml",8081);
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void clean() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    JSONObject people() {
        return new JSONObject().put("firstname", "string")
                .put("lastname", "string");
    }

    public HttpPost postWithJSON(JSONObject json){
        HttpPost request = new HttpPost(HTTP_LOCALHOST_8081_V1_PEOPLE);
        request.setHeader("Content-Type", "application/json");
        try {
            request.setEntity(new StringEntity(json.toString()));
        } catch (UnsupportedEncodingException e) {
            fail();
        }
        return request;
    }
    @Test
    public void testOK() {
        checkWithAssert(postWithJSON(people()
                .put("single",true)
        ), assertResponse().status(SC_OK).bodyContains("firstname", "lastname", "single"));
    }

    @Test
    public void testNotMatch() {
        checkWithAssert(postWithJSON(people()
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }
    @Test
    public void testNotMatch2() {
        checkWithAssert(postWithJSON(people()
                .put("single",12)
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }
    @Test
    public void testNotMatch3() {
        checkWithAssert(postWithJSON(people()
                .put("single","ok")
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }
    @Test
    public void testNotMatch4() {
        checkWithAssert(postWithJSON(people()
                .put("single",true)
                .put("firstname","stringTooLong")
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }
}
