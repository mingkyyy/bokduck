package com.project.bokduck.oauth;

import com.project.bokduck.domain.Member;
import com.project.bokduck.domain.MemberType;
import com.project.bokduck.domain.OAuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@Slf4j
/**
 * @Author MunKyoung
 */
public class OAuth2Attributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String username;
    private final OAuthType oAuthType;


    /**
     * 로그인한 방식이 카카오인지 구글인지 네이버인지 구별후 각로직으로 리턴
     * @param registeredId
     * @param userNameAttributeName
     * @param attributes
     * @return
     */
    public static OAuth2Attributes of(String registeredId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registeredId)) {
            return ofNaver("id", attributes);
        }
        if ("kakao".equals(registeredId)){
            return ofKakao("id",attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    /**
     *
     * 네이버로 로그인시 OAuth타입 카카오로 변경후 등록
     * @param id
     * @param attributes
     * @return
     */
    private static OAuth2Attributes ofNaver(String id, Map<String, Object> attributes) {

        log.info("Naver 로 로그인!");
        log.info("userNameAttributeName : {}", id);
        log.info("attributes : {}", attributes);
        log.info("attributes.get(\"response\") : {}", attributes.get("response"));

        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2Attributes.builder()
                .username((String) response.get("email"))
                .oAuthType(OAuthType.NAVER)
                .attributes(response)
                .nameAttributeKey(id)
                .build();

    }

    /**
     * 구글로 로그인시 OAuth타입 카카오로 변경후 등록
     * @param id
     * @param attributes
     * @return
     */

    private static OAuth2Attributes ofGoogle(String id, Map<String, Object> attributes) {
        log.info("Google 로 로그인!");
        log.info("userNameAttributeName : {}", id);
        log.info("attributes : {}", attributes);
        log.info("attributes.get(\"response\") : {}", attributes.get("response"));

        return OAuth2Attributes.builder()
                .username((String) attributes.get("email"))
                .oAuthType(OAuthType.GOOGLE)
                .attributes(attributes)
                .nameAttributeKey(id)
                .build();
    }

    /**
     * 카카오로 로그인시 OAuth타입 카카오로 변경후 등록
     * @param id
     * @param attributes
     * @return
     */
    private static OAuth2Attributes ofKakao(String id, Map<String, Object> attributes) {
        // kakao는 kakao_account에 유저정보가 있다. (email)
        Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");

        return OAuth2Attributes.builder()
                .username((String) kakaoAccount.get("email"))
                .oAuthType(OAuthType.KAKAO)
                .attributes(attributes)
                .nameAttributeKey(id)
                .build();
    }




    public Member toMember(){
        return Member.builder()
                .username(username)
                .nickname("{noop}")
                .memberType(MemberType.ROLE_USER)
                .emailVerified(true)
                .password("{noop}")
                .oAuthType(oAuthType)
                .tel("00000000000")
                .build();
    }
}
