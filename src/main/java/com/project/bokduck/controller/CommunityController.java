package com.project.bokduck.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.bokduck.domain.*;
import com.project.bokduck.repository.CommentCommunityRepository;
import com.project.bokduck.repository.CommunityRepository;
import com.project.bokduck.repository.MemberRepository;
import com.project.bokduck.repository.TagRepository;
import com.project.bokduck.service.CommunityService;
import com.project.bokduck.specification.CommunitySpecs;
import com.project.bokduck.util.CommunityFormVo;
import com.project.bokduck.util.CurrentMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/community")
@Slf4j
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;
    private final MemberRepository memberRepository;
    private final CommunityRepository communityRepository;
    private final CommentCommunityRepository commentCommunityRepository;
    private final TagRepository tagRepository;


    /**
     * 글쓰기를 완료하고 '게시' 버튼을 눌렀을 때 DB에 사용자가 입력한 정보를 저장한다
     *
     * @param member 글쓴이
     * @param vo     커뮤니티 글 입력 폼을 위한 VO
     * @param model  리턴할 페이지로 애트리뷰트 전달
     * @return 글 상세보기 페이지로 이동
     * @author 이선주
     */
    @PostMapping("/write")
    @Transactional
    public String communityWriteSubmit(@CurrentMember Member member, CommunityFormVo vo, Model model) {
        Community saveCommu = communityService.saveCommunity(member, vo);
        return getCommunityRead(model, saveCommu.getId(), member);
    }

    /**
     * '삭제' 버튼을 눌렀을 때 커뮤니티 게시글을 DB에서 삭제해준다
     *
     * @param id 삭제할 커뮤니티 글의 id
     * @return jsonObject
     * @author 이선주
     */
    @GetMapping("/delete")
    @ResponseBody
    public Long communityDelete(Long id) {
        communityService.deleteCommunity(id);
        return id;
    }

    /**
     * 커뮤니티 '글쓰기' 버튼을 눌렀을 때 글쓰기 폼 페이지를 불러온다
     *
     * @param model 리턴할 페이지로 애트리뷰트 전달
     * @return post/community/write.html
     * @author 이선주
     */
    @GetMapping("/write")
    public String communityWriteForm(Model model) {
        model.addAttribute("vo", new CommunityFormVo());
        return "post/community/write";
    }


    /**
     * 사용자가 폼 작성 후 '수정'을 누르면 DB에 수정된 정보를 저장한다
     *
     * @param id     수정할 글의 id
     * @param member 글을 수정한 유저(현재 로그인해있는 유저)
     * @param vo     수정한 정보를 담을 커뮤니티 글 입력 폼 VO
     * @param model  리턴할 페이지로 애트리뷰트 전달
     * @return 수정된 글 상세보기 페이지
     * @author 이선주
     */
    @PostMapping("/modify/{id}")
    @Transactional
    public String communityModifySubmit(@PathVariable long id, @CurrentMember Member member, CommunityFormVo vo, Model model) {
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

        Community community = communityRepository.findById(id).orElseThrow();
        community.setPostName(vo.getPostName());
        community.setPostContent(vo.getPostContent());
        community.setCommunityCategory(category);

        List<Tag> previousTagList = community.getTags();
        community.setTags(tagList);

        //현재 게시물을 수정 시 지운 태그들의 tagToPost에서 제거
        for (Tag t : previousTagList) {
            List<Post> postList = t.getTagToPost();
            postList.remove(communityRepository.findById(id).orElseThrow());
            t.setTagToPost(postList);
        }

        //수정 시 추가한 태그들의 tagToPost에 현재 게시물 추가
        for (Tag t : tagList) {
            if (t.getTagToPost() == null) {
                t.setTagToPost(new ArrayList<Post>());
            }
            t.getTagToPost().add(community);
        }

        return getCommunityRead(model, id, member);
    }





    //*******************************************************************

    /**
     * 커뮤니티 게시글 수정 페이지 요청이 들어왔을 때 수정 폼 페이지를 불러온다
     *
     * @param model 리턴할 페이지로 애트리뷰트 전달
     * @param id    수정할 글의 id
     * @return 글 수정 페이지
     * @author 이선주
     */
    @GetMapping("/modify")
    @Transactional
    public String getCommunityModify(Model model, long id) {

        Community community = communityRepository.findById(id).orElseThrow();

        CommunityFormVo vo = new CommunityFormVo();
        vo.setPostName(community.getPostName());
        vo.setPostContent(community.getPostContent());

        String strTags = "[";
        for (int i = 0; i < community.getTags().size(); ++i) {
            strTags += "{\"value\":\"" + community.getTags().get(i).getTagName() + "\"}";

            if (i == community.getTags().size() - 1) break;

            strTags += ",";
        }
        strTags += "]";

        vo.setTags(strTags);

        //카테고리 작업
        int intCa = 0;

        if (community.getCommunityCategory() == CommunityCategory.TIP) {
            intCa = 0;
        }
        if (community.getCommunityCategory() == CommunityCategory.INTERIOR) {
            intCa = 1;
        }
        if (community.getCommunityCategory() == CommunityCategory.EAT) {
            intCa = 2;
        }
        if (community.getCommunityCategory() == CommunityCategory.BOARD) {
            intCa = 3;
        }

        vo.setCommunityCategory(intCa);

        model.addAttribute("vo", vo);
        model.addAttribute("communityId", id);

        return "post/community/modify";
    }

    /**
     * 댓글을 쓴 후 댓글쓰기 버튼을 누르면 댓글 정보를 DB에 저장한다
     *
     * @param comment 쓴 댓글 내용을 담는 객체
     * @param id      댓글을 쓴 글의 id
     * @param model   리턴할 페이지로 애트리뷰트 전달
     * @param member  댓글을 쓴 유저(현재 로그인해있는 유저)
     * @return 댓글 정보가 업데이트된 상세보기 페이지
     * @author 이선주
     */
    @PostMapping("/read/comment/{id}")
    public String submitComment(CommentCommunity comment, @PathVariable long id, Model model, @CurrentMember Member member) {

        //DB에 댓글정보 저장
        CommentCommunity commentCommunity = CommentCommunity.builder()
                .nickname(member.getNickname())
                .regdate(LocalDateTime.now())
                .text(comment.getText())
                .parentId(-1l)
                .community(communityRepository.findById(id).orElseThrow())
                .build();

        commentCommunityRepository.save(commentCommunity);

        return getCommunityRead(model, id, member);
    }





    /**
     * 커뮤니티 리스트 화면에서 카테고리, 정렬, 페이징, 검색 한 기준에 따라서 변경된 값으로 list 출력
     *
     * @param pageable   한 페이지당 글 게수
     * @param model
     * @param community  카테고리 종류
     * @param check      최신순, 좋아요 순 정렬 기준
     * @param member     현재 로그인한 멤버
     * @param searchText 검색할 키워드
     * @return post/community/list.html
     * @author 민경
     */
    @GetMapping("/list/category")
    public String communityList(@PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                Model model,
                                @RequestParam(required = false, defaultValue = "all") String community,
                                @RequestParam(required = false, defaultValue = "all") String check,
                                @CurrentMember Member member, String searchText) {

        String state = "all";
        String arrayLike = null;
        communityService.createLikeCount();
        Page<Community> communityList = null;
        if (check.equals("like")) {
            check = "like";
        } else {
            check = "good";
        }

        switch (community) {
            case "all":
                communityList = communityRepository.findAll(pageable);
                state = "all";
                break;
            case "tip":
                communityList = communityService.findCommunityCategoryPage(CommunityCategory.TIP, pageable);
                state = "tip";
                break;
            case "interior":
                communityList = communityService.findCommunityCategoryPage(CommunityCategory.INTERIOR, pageable);
                state = "interior";
                break;
            case "eat":
                communityList = communityService.findCommunityCategoryPage(CommunityCategory.EAT, pageable);
                state = "eat";
                break;
            case "board":
                communityList = communityService.findCommunityCategoryPage(CommunityCategory.BOARD, pageable);
                state = "board";
                break;
        }

        if (member != null) {
            member = memberRepository.getById(member.getId());
            model.addAttribute("member", member);
        }

        if (searchText != null) {
            if (!searchText.isEmpty()) {
                state = "search";
                String[] search = {"postName", "postContent"};
                Specification<Community> searchSpec = null;

                for (String s : search) {
                    Map<String, Object> searchMap = new HashMap<>();
                    searchMap.put(s, searchText);
                    searchSpec =
                            searchSpec == null ? CommunitySpecs.searchText(searchMap)
                                    : searchSpec.or(CommunitySpecs.searchText(searchMap));

                }


                Specification<Tag> tagSpec = CommunitySpecs.searchTagDetails(searchText);
                List<Tag> tagList = tagRepository.findAll(tagSpec);
                searchSpec = searchSpec.or(CommunitySpecs.searchTag(tagList));
                communityList = communityRepository.findAll(searchSpec, pageable);
            }
        }

        int startPage = Math.max(1, communityList.getPageable().getPageNumber() - 5);
        int endPage = Math.min(communityList.getTotalPages(), communityList.getPageable().getPageNumber() + 5);
        model.addAttribute("pageable", pageable);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("state", state);
        model.addAttribute("searchText", searchText);
        model.addAttribute("arrayLike", arrayLike);
        model.addAttribute("communityList", communityList);
        model.addAttribute("member", member);
        model.addAttribute("check", check);

        return "post/community/list";
    }


    /**
     * 커뮤니티 리스트에서서 '좋아요' 버튼 눌렀을때 실패시 메세지를 출력하고 성공시 하트의 사진이 변경, db 변경
     * author 민경
     *
     * @param member 좋아요 누른 멤버
     * @return 실패 -> 메세지 출력, 성공 -> 하트의 사진 변경 + db에 좋아요 한 게시물이 생성
     */
    @GetMapping("/list/like") //카테고리 리스트 좋아요
    @ResponseBody
    public String like(Long id, @CurrentMember Member member) {
        String resultCode = "";
        String message = "";

        int likeCheck = communityService.findCommunityId(id).getLikers().size();

        switch (communityService.addLike(member, id)) {
            case ERROR_AUTH:
                resultCode = "error.auth";
                message = "로그인이 필요한 서비스 입니다.";
                break;
            case ERROR_INVALID:
                resultCode = "error.invalid";
                message = "없는 게시글 입니다.";
                break;
            case ERROR_DUPLICATE:
                resultCode = "error.duplicate";
                message = "좋아요를 삭제하였습니다.";
                likeCheck -= 1;
                break;
            case OK:
                resultCode = "ok";
                message = "좋아요를 추가하였습니다.";
                likeCheck += 1;
                break;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("resultCode", resultCode);
        jsonObject.addProperty("message", message);
        jsonObject.addProperty("likeCheck", likeCheck);

        return jsonObject.toString();
    }


    /**
     * 커뮤니티 상세보기 페이지 요청이 들어왔을 때 페이지를 불러온다
     *
     * @param model  리턴할 페이지로 애트리뷰트 전달
     * @param id     상세보기할 글의 id
     * @param member 현재 로그인해있는 유저
     * @return 커뮤니티 상세보기 페이지로 이동
     * @author 이선주
     */
    @GetMapping("/read")
    @Transactional
    public String getCommunityRead(Model model, long id, @CurrentMember Member member) {

        Community community = communityRepository.findById(id).orElseThrow();

        if (community.getVisitedMember() == null) {
            community.setVisitedMember(new ArrayList<>());
        }
        if (community.getLikers() == null) {
            community.setLikers(new ArrayList<>());
        }
        if (community.getCommentCommunity() == null) {
            community.setCommentCommunity(new ArrayList<>());
        }

        //조회수 올리기
        if (!community.getVisitedMember().contains(memberRepository.getById(member.getId()))) { //아직 조회 안했으면
            community.setHit(community.getHit() + 1);
            List members = community.getVisitedMember();
            members.add(member);
            community.setVisitedMember(members);
        }

        model.addAttribute("member",member);
        model.addAttribute("community", community);

        CommentCommunity comment = new CommentCommunity();
        model.addAttribute("comment", comment);

        CommentCommunity subComment = new CommentCommunity();
        model.addAttribute("subComment", subComment);

        return "post/community/read";
    }


    /**
     * 답글을 쓴 후 답글쓰기 버튼을 누르면 답글 정보를 DB에 저장한다
     *
     * @param subComment 쓴 답글 내용을 담는 객체
     * @param id         댓글을 쓴 글의 id
     * @param model      리턴할 페이지로 애트리뷰트 전달
     * @param member     답글을 쓴 유저(현재 로그인해있는 유저)
     * @return 답글 정보가 업데이트된 상세보기 페이지
     * @author 이선주
     */
    @PostMapping("/read/subcomment/{id}")
    public String submitSubComment(CommentCommunity subComment, @PathVariable long id, Model model, @CurrentMember Member member) {

        //DB에 답글정보 저장
        CommentCommunity subCommentCommunity = CommentCommunity.builder()
                .nickname(member.getNickname())
                .regdate(LocalDateTime.now())
                .text(subComment.getText())
                .parentId(subComment.getParentId())
                .community(communityRepository.findById(id).orElseThrow())
                .build();

        commentCommunityRepository.save(subCommentCommunity);

        return getCommunityRead(model, id, member);
    }



    /**
     * 좋아요를 누르면 DB의 정보를 업데이트한다
     *
     * @param id     좋아요를 누를 글의 id
     * @param member 좋아요를 누를 유저(현재 로그인해있는 유저)
     * @return jsonObject
     * @author 이선주
     */
    @GetMapping("/read/like")
    @ResponseBody
    public String communitReadLike(Long id, @CurrentMember Member member) {
        String resultCode = "";
        String message = "";

        // 좋아요 개수
        int likeCheck = communityRepository.findById(id).orElseThrow().getLikers().size();

        switch (communityService.addLike(member, id)) {
            case ERROR_AUTH:
                resultCode = "error.auth";
                message = "로그인이 필요한 서비스입니다.";
                break;
            case ERROR_INVALID:
                resultCode = "error.invalid";
                message = "삭제된 게시물 입니다.";
                break;
            case ERROR_DUPLICATE:
                resultCode = "duplicate";
                message = "좋아요 취소 완료!";
                likeCheck -= 1;
                break;
            case OK:
                resultCode = "ok";
                message = "좋아요 완료!";
                likeCheck += 1;
                break;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("resultCode", resultCode);
        jsonObject.addProperty("message", message);
        jsonObject.addProperty("likeCheck", likeCheck);

        return jsonObject.toString();
    }



    /**
     * 커뮤니티 탭을 클릭하면 커뮤니티 리스트 화면페이지를 불러온다.
     *
     * @param pageable 한 페이지당 보여줄 개수
     * @param member   현재 로그인한 회원
     * @param model
     * @param check    정렬 기준
     * @return 커뮤니티 리스트 화면
     * @author 민경
     */
    @GetMapping("/list")
    public String community(@PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                            @CurrentMember Member member, Model model,
                            @RequestParam(required = false, defaultValue = "all") String check) {
        Page<Community> communityList = communityService.findPage(pageable);

        if (member != null) {
            member = memberRepository.getById(member.getId());
            model.addAttribute("member", member);
        }
        int startPage = Math.max(1, communityList.getPageable().getPageNumber() - 5);
        int endPage = Math.min(communityList.getTotalPages(), communityList.getPageable().getPageNumber() + 5);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("check", check);
        model.addAttribute("communityList", communityList);
        model.addAttribute("state", "all");

        return "post/community/list";
    }

}
