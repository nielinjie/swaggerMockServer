package xyz.nietongxue.mockServer;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.nietongxue.TestUtil;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static xyz.nietongxue.TestUtil.*;


/**
 * Created by nielinjie on 9/13/16.
 */
public class RichPeoplePostRequestTest {
    private static final String HTTP_LOCALHOST_8082_V1_PEOPLE = "http://localhost:8082/v1/people";
    private static MockServer server;


    @BeforeClass
    public static void init() {
        try {
            server = new MockServer("./src/test/resources/peopleRichPost.yaml", 8082);
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




    @Test
    public void testOk() {

        assertRequestAndResponse(postWithJSON(people()
                        .put("single", true)
                        .put("birth", "2016-09-07")
                )
                , assertResponse().status(SC_OK));
    }

    private HttpPost postWithJSON(JSONObject put) {
        return TestUtil.postWithJSON(HTTP_LOCALHOST_8082_V1_PEOPLE,put);
    }

    @Test
    public void testNoFind() {
        assertRequestAndResponse(postWithJSON(people()
                .put("single", true)
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }

    @Test
    public void testNotMatch() {
        assertRequestAndResponse(postWithJSON(people()
                .put("single", true)
                .put("birth", "2016")
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }

    @Test
    public void testMatchDateTime() {
        assertRequestAndResponse(postWithJSON(people()
                .put("single", true)
                .put("birth", "2016-09-07")
                .put("birthTime", "2016-09-07T13:00:00Z")
        ), assertResponse().status(SC_OK));
    }

    @Test
    public void testNotMatchDateTime() {
        assertRequestAndResponse(postWithJSON(people()
                .put("single", true)
                .put("birth", "2016-09-07")
                .put("birthTime", "2016-09-07")
        ), assertResponse().status(SC_BAD_REQUEST).bodyContains("not match"));
    }


}
