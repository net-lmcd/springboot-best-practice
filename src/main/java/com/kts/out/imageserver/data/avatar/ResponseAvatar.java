package com.kts.out.imageserver.data.avatar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.out.imageserver.data.Response;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * response 변경 후 사용 X -> 자주 변경됨 .. -> Map 으로 처리
 */
@Getter @ToString
@Builder @NoArgsConstructor
public class ResponseAvatar extends Response {

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("gender")
    private Integer gender;

    @JsonProperty("glasses_shape")
    private Integer glassShape;

    @JsonProperty("glasses_type")
    private Integer glassType;

    @JsonProperty("men_hair_length")
    private Integer menHairLength;

    @JsonProperty("men_hair_part")
    private Integer menHairPart;

    @JsonProperty("men_hair_wave")
    private Integer menHairWave;

    @JsonProperty("women_hair_front")
    private Integer womenHairFront;

    @JsonProperty("women_hair_length")
    private Integer womenHairLength;

    @JsonProperty("women_hair_wave")
    private Integer womenHairWave;

    public ResponseAvatar(
            @JsonProperty("filename")  String filename,
            @JsonProperty("gender")    Integer gender,
            @JsonProperty("glasses_shape")  Integer glassShape,
            @JsonProperty("glasses_type") Integer glassType,
            @JsonProperty("men_hair_length") Integer menHairLength,
            @JsonProperty("men_hair_part") Integer menHairPart,
            @JsonProperty("men_hair_wave") Integer menHairWave,
            @JsonProperty("women_hair_front")Integer womenHairFront,
            @JsonProperty("women_hair_length") Integer womenHairLength,
            @JsonProperty("women_hair_wave") Integer womenHairWave)
    {
        this.filename = filename;
        this.gender = gender;
        this.glassShape = glassShape;
        this.glassType = glassType;
        this.menHairLength = menHairLength;
        this.menHairPart = menHairPart;
        this.menHairWave = menHairWave;
        this.womenHairFront = womenHairFront;
        this.womenHairLength = womenHairLength;
        this.womenHairWave = womenHairWave;
    }
}
