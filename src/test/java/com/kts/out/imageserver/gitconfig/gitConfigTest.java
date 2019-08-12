package com.kts.out.imageserver.gitconfig;

import com.kts.out.imageserver.queue.controllerQueue;
import com.kts.out.imageserver.service.ConfigServerDynamicService;
import com.kts.out.imageserver.storage.FileSystemStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class gitConfigTest {

    @Autowired
    private controllerQueue atc_map;

    @Autowired
    FileSystemStorageService fileSystemStorageService;

    @Autowired
    private ConfigServerDynamicService configServerDynamicService;

    @Test
    public void testGitConfigInit() {
        String atcServerList = this.configServerDynamicService.atcServerList;
        assertThat(atcServerList).isNotEmpty();
        System.out.println(atcServerList);
    }
/*
    @Test
    public void initATCServer() {
        ConcurrentHashMap<String, Integer> linkedHashMap = this.configServerDynamicService.initATCServers();
        assertThat(linkedHashMap).isNotEmpty();
    }

    @Test
    public void isgitConfigClientHasServers() {
        this.configServerDynamicService.initATCServers();
        LinkedHashMap<String, Integer> linkedHashMap = configServerDynamicService.sortedATCServerByValue();
        assertThat(linkedHashMap.size()).isEqualTo(2);
    }

    @Test
    public void updateConcurrentHashMap() {
        this.configServerDynamicService.initATCServers();
        ConcurrentHashMap<String, Integer> temp = this.atc_map.getAtcServers();
        assertThat(temp.size()).isEqualTo(2);
        assertThat(temp.containsKey("183.98.154.45:4000")).isEqualTo(true);
        assertThat(temp.containsKey("183.98.154.45:4001")).isEqualTo(true);

        IntStream.range(0, 6).forEach(index -> {
            String key = this.configServerDynamicService.getMinKeyFromATCServers();
            this.configServerDynamicService.update(HttpMethod.POST, key);
        });

        assertThat(this.atc_map.getAtcServers().get("183.98.154.45:4000")).isEqualTo(3);
        assertThat(this.atc_map.getAtcServers().get("183.98.154.45:4001")).isEqualTo(3);
    }
    */
}
