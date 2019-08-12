package com.kts.out.imageserver.data.avatar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter @Setter @Builder
public class ResourceSaved {

    @NotEmpty
    @JsonProperty("filepath")
    private String filepath;

    @JsonProperty("list")
    private List<Resource> resources;

    public ResourceSaved(
            @JsonProperty("filepath") String filepath,
            @JsonProperty("list") List<Resource> resources) {
        this.filepath = filepath;
        this.resources = resources;
    }
}
