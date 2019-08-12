package com.kts.out.imageserver.controller;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * 통합 테스트 - 컨트롤러
 */
@RunWith(SpringRunner.class)
public class ImageControllerTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void simpleTest() {
        logger.info("simpleTest");
        MultiValueMap<String, Object> map = requestLinkedMultiMap("keys-image", "value-userId");
        Set<String> keySet = map.keySet();
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            logger.info("keys => " + key);
            logger.info("values ==> " + map.get(key));
        }
        logger.info("simpleTest Finished");
    }

    @Test
    public void simpledTest2() {
        logger.info("simpleTest2");
        LinkedList<String> keys = new LinkedList<>();
        keys.add("image");
        keys.add("userId");
        keys.add("image2");
        keys.add("userId2");

        LinkedList<Object> values = new LinkedList<>();
        values.add("keys-image");
        values.add("values-userId");
        values.add("keys-image2");
        values.add("values-userId2");
        MultiValueMap map = addRequestsLinkedMap(keys, values);
        Set<?> keySet = map.keySet();
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            logger.info("keys == > " + key);
            logger.info("values ==> " + map.get(key));
        }
        logger.info("simpleTest2 Finished");

    }

    public <V> MultiValueMap<String, V> requestLinkedMultiMap (V image_resource, V value) {
        LinkedMultiValueMap<String, V> multiValueMap = new LinkedMultiValueMap<>();
        addRequestLinkedMultiMap("image", image_resource, multiValueMap);
        addRequestLinkedMultiMap("userId", value, multiValueMap);
        return multiValueMap;
    }

    private <K, V> MultiValueMap addRequestsLinkedMap (LinkedList<K> keys, LinkedList<V> values) {
        //keys, values size equal check
        LinkedMultiValueMap <K, V> multiValueMap = new LinkedMultiValueMap<>();
        while (!keys.isEmpty() && !values.isEmpty()) {
            addRequestLinkedMultiMap(keys.poll(), values.poll(), multiValueMap);
        }

        return multiValueMap;
    }

    private <K, V> void addRequestLinkedMultiMap (@NotNull K key, @NotNull V value, @NotNull LinkedMultiValueMap <K, V> linkedMultiValueMap) {
        ((MultiValueMap <K, V>) linkedMultiValueMap).add(key, value);
    }

    private <K, V> void addRequestLinkedMultiMapOptional (@NotNull K key, @NotNull V value, Optional<LinkedMultiValueMap <K, V>> linkedMultiValueMapOptional) {
        MultiValueMap <K, V> linkedMultiValueMapReference = linkedMultiValueMapOptional.orElseGet(LinkedMultiValueMap::new);
        linkedMultiValueMapReference.add(key, value);
        linkedMultiValueMapReference = null;
    }
}
