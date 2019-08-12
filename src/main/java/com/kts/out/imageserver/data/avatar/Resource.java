package com.kts.out.imageserver.data.avatar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Getter @Setter @Builder
public class Resource {

    @NotEmpty
    @JsonProperty("uuid")
    private String uuid;

    @NotEmpty
    @JsonProperty("filename")
    private String filename;

    @NotEmpty
    @JsonProperty("responseAvatar")
    private Map<String, Object> responseAvatar; //ResponseAvatar responseAvatar;

    public Resource(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("image") String filename,
            @JsonProperty("responseAvatar") Map <String, Object> responseAvatar) {
        this.uuid = uuid;
        this.filename = filename;
        this.responseAvatar = responseAvatar;
    }
}
