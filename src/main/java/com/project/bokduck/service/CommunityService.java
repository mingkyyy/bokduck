package com.project.bokduck.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.bokduck.domain.*;
import com.project.bokduck.repository.CommunityRepository;
import com.project.bokduck.repository.MemberRepository;
import com.project.bokduck.util.CommunityFormVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityRepository communityRepository;
    private final MemberRepository memberRepository;

    /**
     * 커뮤니티에 있는 모든 게시물을 찾는다.
     * @author 민경
     * @return 커뮤니티의 모든 게시물
     */
    public List<Community> findAll() {
        return communityRepository.findAll();
    }

    /**
     * 커뮤니티에 있는 게시글 중 해당 id 를 찾는다
     * @author 민경
     * @param id 찾을 id
     * @return 해당 아이디의 게시물
     */
    public Community findCommunityId(Long id) {
        return communityRepository.findById(id).orElseThrow();
    }


    /**
     * 좋아요를 눌렀을때 FlagLike 이넘에 따른 분류
     * @author 민경
     * @param member 현재 로그인한 회원
     * @param id 좋아요를 누른 게시글 번호
     * @return FlagLike 이넘
     */
    @Transactional
    public FlagLike addLike(Member member, Long id) {
        Community community = findCommunityId(id);
        if (member == null) {
            return FlagLike.ERROR_AUTH;
        }
        member = memberRepository.findById(member.getId()).orElseThrow();
        try {
            community = findCommunityId(id);
        } catch (NoSuchElementException e) {
            return FlagLike.ERROR_INVALID;
        }
        if (member.getLikes().contains(findCommunityId(id))) {
            member.getLikes().remove(findCommunityId(id));
            return FlagLike.ERROR_DUPLICATE;
        }
        member.addLikeCommunity(community);
        return FlagLike.OK;
    }


    /**
     * 좋아요 숫자 생성
     * @author 민경
     */
    public void createLikeCount() {
        List<Community> communityList = communityRepository.findAll();
        for (Community c : communityList) {
            c.setLikeCount(c.getLikers().size());
        }
        communityRepository.saveAll(communityList);
    }

    /**
     * 커뮤니티 모든 게시글에 대한 페이징 처리
     * @author 민경
     * @param pageable
     * @return 페이징 처리한 모든 게시글
     */
    public Page<Community> findPage(Pageable pageable) {
        return communityRepository.findAll(pageable);
    }

    /**
     * 카테고리에 따른 페이징 처리
     * @author 민경
     * @param tip 카테고리 종류
     * @param pageable
     * @return 페이징, 카테고리에 따른 리스트
     */
    public Page<Community> findCommunityCategoryPage(CommunityCategory tip, Pageable pageable) {
        return communityRepository.findByCommunityCategory(tip, pageable);
    }

    public Community saveCommunity(Member member, CommunityFormVo vo) {
        String strTags = vo.getTags();

        //DB에 저장할 List<Tag>형 변수 설정
        List<Tag> tagList = new ArrayList<>();
        if (!vo.getTags().isEmpty()) {
            JsonArray tagsJsonArray = new Gson().fromJson(vo.getTags(), JsonArray.class);

            for (int i = 0; i < tagsJsonArray.size(); ++i) {
                JsonObject object = tagsJsonArray.get(i).getAsJsonObject();
                String tagValue = object.get("value").getAsString();

                Tag tag = Tag.builder()
                        .tagName(tagValue)
                        .build();

                tagList.add(tag);
            }
        }

        //DB에 저장할 CommunityCategory형 변수 설정
        CommunityCategory category = CommunityCategory.TIP;

        switch (vo.getCommunityCategory()) {
            case 0:
                category = CommunityCategory.TIP;
                break;
            case 1:
                category = CommunityCategory.INTERIOR;
                break;
            case 2:
                category = CommunityCategory.EAT;
                break;
            case 3:
                category = CommunityCategory.BOARD;
                break;
            default:
        }


        //데이터를 DB에 저장
        Community community = Community.builder()
                .postName(vo.getPostName())
                .postContent(vo.getPostContent())
                .regdate(LocalDateTime.now())
                .writer(member)
                .tags(tagList)
                .communityCategory(category)
                .build();

        Community savedCommu = communityRepository.save(community);

        //TAG_TAG_TO_POST 테이블에 데이터 넣기
        for (Tag t : tagList) {
            if (t.getTagToPost() == null) {
                t.setTagToPost(new ArrayList<Post>());
            }
            t.getTagToPost().add(community);
        }

        return savedCommu;
    }

    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }


    /**
     * 좋아요 했을때의 이넘 종류
     * @author 민경
     */
    public enum FlagLike {
        ERROR_AUTH, ERROR_INVALID, ERROR_DUPLICATE, OK
    }
}