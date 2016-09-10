/**
 * Copyright (C) 2016 UniKnow (info.uniknow@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.nietongxue.mockServer.MockServer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Validates functionality of MockServer (swagger)
 */
public class MockServerYamlTest {

    private static MockServer server;

    @BeforeClass
    public static void init() {

        try {
            server = new MockServer();
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @AfterClass
    public static void clean(){
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
    public void testInvokeStubbedOperationWithNoResponseDefined()
        throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request;
        HttpResponse response;

        request = new HttpGet("http://localhost:8080/v1/people?size=10");
        response = client.execute(request);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine()
            .getStatusCode());
        EntityUtils.consumeQuietly(response.getEntity());
    }





}
