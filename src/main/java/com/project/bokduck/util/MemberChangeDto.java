package com.project.bokduck.util;

import com.project.bokduck.domain.Member;
import com.project.bokduck.domain.UserAddress;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class MemberChangeDto {
    private String nickname;
    private String postcode;
    private String baseAddress;
    private String detailAddress;

    @Builder
    public MemberChangeDto(String nickname, String postcode, String baseAddress, String detailAddress){
        this.nickname = nickname;
        this.postcode = postcode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }



}
