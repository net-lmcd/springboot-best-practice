package com.kts.out.imageserver.service;

import com.kts.out.imageserver.exception.RequestFullException;
import com.kts.out.imageserver.queue.controllerQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Redis 사용, -> no use
 */
@Slf4j
@Service
public class ConfigServerDynamicService {

    @Autowired
    private controllerQueue atc_map;

    @Value("${server.list.atc}")
    public String atcServerList;

    @PostConstruct
    public void init(){ inits(); }

    private ConcurrentHashMap<String, ? extends Number> inits() {
        atcServerList = atcServerList.trim();
        atcServerList = atcServerList.replaceAll(" ", "");
        atcServerList = atcServerList.replaceAll("\\p{Z}", "");
        StringTokenizer st = new StringTokenizer(atcServerList, ",");
        while(st.hasMoreElements()) {
            String server = st.nextToken();
            atc_map.getAtcServers().put(server,0);
        }
        return atc_map.getAtcServers();
    }

    /**
     *
     * @return
     */
    public String getMinKeyFromATCServers() {
        Iterator<String> iterator = sortedATCServerByValue().keySet().iterator();
        String key ="";
        int value = 0;
        while(iterator.hasNext()) {
            key = iterator.next();
            value = atc_map.getAtcServers().get(key);
            break;
        }
        if(value < 5) return key;
        else throw new RequestFullException();
    }

    /**
     *
     * @return map
     */
    public LinkedHashMap<String, Integer> sortedATCServerByValue() {
        return atc_map.getAtcServers().entrySet().stream()
                                        .sorted(Map.Entry.comparingByValue())
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                (x,y) ->{ throw new AssertionError();},
                                                LinkedHashMap::new));
    }

    /**
     *
     * @param httpMethod update count
     * @param key server address
     */
    public void update(HttpMethod httpMethod, String key) {
        switch (httpMethod) {
            case GET:
                atc_map.getAtcServers().computeIfPresent(key, (k,v) -> (v > 0) ? v = v - 1 : 0);
                break;
            case POST:
                atc_map.getAtcServers().computeIfPresent(key, (k,v) -> v + 1);
                break;
            default:
                atc_map.getAtcServers().put(key, 0);
                break;
        }
    }
}
