package com.kts.out.imageserver.data.avatar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;


@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class RequestAvatar {

    @NotEmpty
    @JsonProperty("userId")
    private String userId;

    @NotEmpty
    @JsonProperty("image_name")
    private MultipartFile image;
}
