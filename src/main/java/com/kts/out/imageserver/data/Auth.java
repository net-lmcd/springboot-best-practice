package com.kts.out.imageserver.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Auth {

    @JsonProperty("id")
    String id;
    @JsonProperty("password")
    String password;

    public Auth(@JsonProperty("id") String id,
                @JsonProperty("password") String password) {
        this.id = id;
        this.password = password;
    }
}
