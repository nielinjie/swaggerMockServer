/*
 *  Copyright 2016 SmartBear Software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.swagger.inflector.processors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.inflector.examples.models.Example;
import io.swagger.inflector.examples.models.ObjectExample;
import io.swagger.inflector.examples.models.StringExample;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

public class JsonExampleDeserializer extends JsonDeserializer<Example> {

    @Override
    public Example deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        Example output = null;
        JsonNode node = jp.getCodec().readTree(jp);
        if (node instanceof ObjectNode) {
            ObjectExample obj = new ObjectExample();
            ObjectNode on = (ObjectNode) node;

            for (Iterator<Entry<String, JsonNode>> x = on.fields(); x.hasNext(); ) {
                Entry<String, JsonNode> i = x.next();
                String key = i.getKey();
                JsonNode value = i.getValue();

                obj.put(key, new StringExample(value.asText()));
                output = obj;
            }
        } else if (node instanceof TextNode) {
            output = new StringExample(((TextNode) node).asText());
        }
        return output;
    }
}
