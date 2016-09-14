package xyz.nietongxue.mockServer; /**
 * Copyright (C) 2016 UniKnow (info.uniknow@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static xyz.nietongxue.TestUtil.assertResponse;
import static xyz.nietongxue.TestUtil.checkWithAssert;

/**
 * Validates functionality of MockServer (swagger)
 */
public class GetPeopleRequestTest {

    private static final String HTTP_LOCALHOST_8080_V1_PEOPLE = "http://localhost:8080/v1/people";
    private static MockServer server;

    @BeforeClass
    public static void init() {

        try {
            server = new MockServer("./src/test/resources/people.yaml");
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

    /**
     * Verifies default response for stubbed operation is 501
     */
    @Test
    public void testServer()
            throws IOException {

        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=10&name=lala", request -> {
                    request.addHeader("username", "any");
                    request.addHeader("password", "any");
                },
                assertResponse().status(SC_OK));
    }


    @Test
    public void testMax()
            throws IOException {

        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=101&name=lala", request -> {
                    request.addHeader("username", "any");
                    request.addHeader("password", "any");
                },
                assertResponse().status(SC_BAD_REQUEST).bodyContains("size"));
    }

    @Test
    public void testNoName()
            throws IOException {
        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=10", request -> {
                    request.addHeader("username", "any");
                    request.addHeader("password", "any");
                },
                assertResponse().status(SC_BAD_REQUEST).bodyContains("missing", "name"));
    }



    @Test
    public void testNoHead()
            throws IOException {
        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=10", assertResponse().status(SC_BAD_REQUEST).bodyContains(
                "missing", "username", "password"));
    }

    @Test
    public void testBadRequest2()
            throws IOException {
        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=10", request ->
                        request.addHeader("username", "any"),
                assertResponse().status(SC_BAD_REQUEST).bodyContains(
                        "missing", "password").bodyNotContains("username"));
    }

    @Test
    public void testBadTypeOfQueryStringParameter()
            throws IOException {
        checkWithAssert(HTTP_LOCALHOST_8080_V1_PEOPLE + "?size=ab", request -> {
            request.addHeader("username", "any");
            request.addHeader("password", "any");
        }, assertResponse().status(SC_BAD_REQUEST).bodyContains("couldn't convert").bodyNotContains("size"));
    }
}
