package com.kts.out.imageserver.queue;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter @Setter
public class controllerQueue {

    /**
     * multi-thread safe 함. Concurrent 임 ..
     */
    private ConcurrentHashMap<String, Integer> atcServers;

    @PostConstruct
    public void init() {
        atcServers = new ConcurrentHashMap<>();
    }
}
