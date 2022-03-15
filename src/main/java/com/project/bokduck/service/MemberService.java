package com.project.bokduck.service;


import com.project.bokduck.domain.Member;
import com.project.bokduck.domain.MemberType;
import com.project.bokduck.domain.OAuthType;
import com.project.bokduck.domain.UserAddress;
import com.project.bokduck.repository.MemberRepository;
import com.project.bokduck.util.MemberChangeDto;
import com.project.bokduck.util.MemberUser;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.json.simple.JSONObject;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.project.bokduck.validation.JoinFormVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PassEmailService passEmailService;

    /**
     * @author 미리
     * 임의의 사용자 계정 만들기 (관리자, 일반유저)
     */
    @PostConstruct
    public void createTestMember() {
        memberRepository.save(Member.builder()
                .username("alsrus1227@naver.com")
                .password(passwordEncoder.encode("1q2w3e4r!"))
                .name("홍길동")
                .tel("01011111111")
                .nickname("관리자")
                .joinedAt(LocalDateTime.now())
                .memberType(MemberType.ROLE_MANAGE)
                .build()
        );

        memberRepository.save(Member.builder()
                .username("test@test.com")
                .name("고길동")
                .password(passwordEncoder.encode("1q2w3e4r!"))
                .tel("01012341234")
                .nickname("test")
                .joinedAt(LocalDateTime.now())
                .build()
        );

        List<Member> memberList = memberRepository.findAll();
    }

    /**
     * @param username 사용자 이메일 주소
     * @return 이메일 주소로 사용자 정보를 찾은 후 없으면 미등록계정 에러 발생
     * @throws UsernameNotFoundException
     * @author 미리
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Member> optional = memberRepository.findByUsername(username);
        Member member = optional.orElseThrow(
                () -> new UsernameNotFoundException("미등록 계정")
        );

        return new MemberUser(member);
    }

    /**
     * 회원가입 프로세스 처리
     * 1. JoinFormVo 객체를 Member DB에 저장
     * 2. 이메일 보내기
     * 3. 로그인 처리해주기
     *
     * @param vo 회원가입 폼을 위한 VO
     * @author 이선주
     */
    public void processNewMember(JoinFormVo vo) {
        Member member = saveNewMember(vo);
        login(member);
    }

    /**
     * JoinFormVo 객체를 Member DB에 저장
     *
     * @param vo 회원가입 폼을 위한 VO
     * @return 회원정보가 저장된 유저
     * @author 이선주
     */
    private Member saveNewMember(JoinFormVo vo) {
        Member member = Member.builder()
                .name(vo.getName())
                .username(vo.getUsername())
                .password(passwordEncoder.encode(vo.getPassword()))
                .tel(vo.getTel())
                .nickname(vo.getNickname())
                .joinedAt(LocalDateTime.now())
                .oAuthType(OAuthType.NONE)
                .memberType(MemberType.ROLE_USER)
                .userAddress(UserAddress.builder()
                        .postcode(vo.getPostcode())
                        .baseAddress(vo.getBaseAddress())
                        .detailAddress(vo.getDetailAddress()).build())
                .build();
        return memberRepository.save(member);
    }

    public void certifiedPhoneNumber(String phoneNumber, String cerNum) {
        String api_key = "NCSNIGG9KCSOYLFV";
        String api_secret = "DHZMV53VX708ZI81TNFA6AA6C29SGS0O";
        Message coolsms = new Message(api_key, api_secret);

        // 4 params(to, from, type, text) are mandatory. must be filled
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("to", phoneNumber);    // 수신전화번호
        params.put("from", " 01053196261");    // 발신전화번호. 테스트시에는 발신,수신 둘다 본인 번호로 하면 됨
        params.put("type", "SMS");
        params.put("text", "bokduck 휴대폰인증 메시지 : 인증번호는" + "[" + cerNum + "]" + "입니다.");
        params.put("app_version", "test app 1.2"); // application name and version

        try {
            JSONObject obj = (JSONObject) coolsms.send(params);
            System.out.println(obj.toString());

        } catch (CoolsmsException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCode());
        }

    }


    /**
     * 강제 로그인
     *
     * @param member
     * @author 이선주
     */
    public void login(Member member) {
        MemberUser user = new MemberUser(member);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        user,
                        user.getMember().getPassword(),
                        user.getAuthorities()
                );

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);
    }


    /**
     * 닉네임 중복 여부
     *
     * @param newnickname 새로운 닉네임
     * @return 중복 유무에 따른 true, false 반환
     * @author 민경
     */
    public boolean checkNickname(String newnickname) {
        return memberRepository.existsBynickname(newnickname);
    }


    public Member findByName(String name) {
        return memberRepository.findByName(name).orElseThrow();
    }

    public String passwordSubmit(String username) {
        Optional<Member> member = memberRepository.findByUsername(username);
        String result = "";
        if (member.isEmpty()) {
            result = "none";
        } else {
            passEmailService.sendPassEmail(member.orElseThrow());
            result = "ok";
        }
        return result;

    }

    public String changeNickname(Member member, String nickname) {
        String nicknameResult = "fail";
        if (member.getNickname().equals(nickname)) {
            nicknameResult = "noChange";
        } else {
            if (memberRepository.existsBynickname(nickname)) {
                nicknameResult = "overlap";
            } else {
                member.updateNickname(nickname);
                memberRepository.save(member);
                nicknameResult = "ok";
            }
        }
        return nicknameResult;
    }

    public String changeAddress(MemberChangeDto memberChangeDto, Member member) {
        String addressResult = "no";
        UserAddress userAddress = UserAddress.builder()
                .baseAddress(memberChangeDto.getBaseAddress())
                .detailAddress(memberChangeDto.getDetailAddress())
                .postcode(memberChangeDto.getPostcode()).build();

        if (member.getUserAddress() != null) {
            if (member.getUserAddress().equals(userAddress)) {
                addressResult = "noChange";
            } else {
                member.updateUserAddress(userAddress);
                memberRepository.save(member);
                addressResult = "ok";
            }
        } else { //  null 일때
            if (userAddress.getBaseAddress().isEmpty() && userAddress.getDetailAddress().isEmpty() &&
                    userAddress.getPostcode().isEmpty()) {
                addressResult = "noChange";
            } else {
                member.updateUserAddress(userAddress);
                memberRepository.save(member);
                addressResult = "ok";
            }
        }
        return addressResult;
    }


}