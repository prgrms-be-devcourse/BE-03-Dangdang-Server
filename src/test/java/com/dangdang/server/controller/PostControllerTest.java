package com.dangdang.server.controller;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dangdang.server.domain.common.StatusType;
import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.post.application.PostService;
import com.dangdang.server.domain.post.domain.Category;
import com.dangdang.server.domain.post.dto.request.PostSaveRequest;
import com.dangdang.server.domain.post.dto.request.PostUpdateStatusRequest;
import com.dangdang.server.domain.post.dto.response.PostDetailResponse;
import com.dangdang.server.domain.postImage.dto.PostImageRequest;
import com.dangdang.server.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureRestDocs
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class PostControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  PostService postService;

  @Autowired
  MemberRepository memberRepository;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  JwtTokenProvider jwtTokenProvider;

  String accessToken;
  Member member;
  PostSaveRequest postSaveRequest;
  Long postId;

  @BeforeEach
  void setUp() {
    Member newMember = new Member("01098765467", "yb");
    member = memberRepository.save(newMember);
    accessToken = "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    PostImageRequest postImageRequest = new PostImageRequest(
        List.of("http://s3.amazonaws.com/test1.png", "http://s3.amazonaws.com/test2.png"));
    postSaveRequest = new PostSaveRequest("테스트 제목", "테스트 내용", Category.디지털기기, 20000,
        "테스트 희망장소", BigDecimal.valueOf(127.0000), BigDecimal.valueOf(36.0000), false, "천호동",
        postImageRequest);
    PostDetailResponse postDetailResponse = postService.savePost(postSaveRequest, member);
    postId = postDetailResponse.postId();
  }

  @ParameterizedTest
  @ValueSource(strings = {"RESERVED", "SELLING", "COMPLETED"})
  @DisplayName("사용자는 판매중, 예약중, 판매완료 중 1개의 상태로 post의 상태를 변경할 수 있다.")
  void updatePostStatus(String status) throws Exception {

    mockMvc.perform(patch("/posts/status/" + postId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("AccessToken", accessToken)
            .content(objectMapper.writeValueAsString(new PostUpdateStatusRequest(
                StatusType.valueOf(status)))))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andDo(document("post/api/patch/updateStatus",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestHeaders(
                headerWithName("AccessToken").description("Access Token")
            ),
            requestFields(
                fieldWithPath("status").type(JsonFieldType.STRING)
                    .description("거래 상태(판매중,예약중,완료)")),
            responseFields(
                fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("게시글 식별자"),
                fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                    .description("게시글 제목"),
                fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                    .description("게시글 내용"),
                fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                    .description("카테고리"),
                fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                    .description("거래희망 장소 이름").optional(),
                fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 경도").optional(),
                fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 위도").optional(),
                fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER)
                    .description("조회수"),
                fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                    .description("나눔 여부"),
                fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                    .description("거래 기준 동네"),
                fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                    .description("거래 상태"),
                fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                    .description("작성자 식별자"),
                fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                    .description("작성자 프로필 이미지 url").optional(),
                fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                    .description("작성자 닉네임"),
                fieldWithPath("imageUrls").type(JsonFieldType.ARRAY)
                    .description("게시글 이미지 url 리스트").optional()))
        );
  }

  @Test
  @DisplayName("게시글을 작성할 수 있다.")
  void savePostTest() throws Exception {
    mockMvc.perform(post("/posts")
            .header("AccessToken", accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(postSaveRequest)))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andDo(MockMvcResultHandlers.print())
        .andDo(document("post/api/post/save",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestHeaders(
                headerWithName("AccessToken").description("Access Token")
            ),
            requestFields(
                fieldWithPath("title").type(JsonFieldType.STRING).description("게시글 제목"),
                fieldWithPath("content").type(JsonFieldType.STRING).description("게시글 내용"),
                fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
                fieldWithPath("price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("desiredPlaceName").type(JsonFieldType.STRING)
                    .description("거래희망 장소 이름").optional(),
                fieldWithPath("desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 경도").optional(),
                fieldWithPath("desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 위도").optional(),
                fieldWithPath("sharing").type(JsonFieldType.BOOLEAN).description("나눔 여부"),
                fieldWithPath("townName").type(JsonFieldType.STRING).description("거래 기준 동네"),
                fieldWithPath("postImageRequest.urls").type(JsonFieldType.ARRAY)
                    .description("게시글 이미지 url 리스트").optional()),
            responseFields(
                fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("게시글 식별자"),
                fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                    .description("게시글 제목"),
                fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                    .description("게시글 내용"),
                fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                    .description("카테고리"),
                fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                    .description("거래희망 장소 이름").optional(),
                fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 경도").optional(),
                fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 위도").optional(),
                fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER)
                    .description("조회수"),
                fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                    .description("나눔 여부"),
                fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                    .description("거래 기준 동네"),
                fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                    .description("거래 상태"),
                fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                    .description("작성자 식별자"),
                fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                    .description("작성자 프로필 이미지 url").optional(),
                fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                    .description("작성자 닉네임"),
                fieldWithPath("imageUrls").type(JsonFieldType.ARRAY)
                    .description("게시글 이미지 url 리스트").optional()))
        );
  }

  @Test
  @DisplayName("게시글 상세 정보를 확인할 수 있다.")
  void findPostDetailTest() throws Exception {

    mockMvc.perform(RestDocumentationRequestBuilders.get("/posts/{id}", postId)
            .header("AccessToken", accessToken))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(MockMvcResultHandlers.print())
        .andDo(document("post/api/get/findById",
            preprocessResponse(prettyPrint()),
            requestHeaders(
                headerWithName("AccessToken").description("Access Token")
            ),
            responseFields(
                fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("게시글 식별자"),
                fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                    .description("게시글 제목"),
                fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                    .description("게시글 내용"),
                fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                    .description("카테고리"),
                fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("가격"),
                fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                    .description("거래희망 장소 이름").optional(),
                fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 경도").optional(),
                fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                    .description("거래희망 장소 위도").optional(),
                fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER)
                    .description("조회수"),
                fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                    .description("나눔 여부"),
                fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                    .description("거래 기준 동네"),
                fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                    .description("거래 상태"),
                fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                    .description("작성자 식별자"),
                fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                    .description("작성자 프로필 이미지 url").optional(),
                fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                    .description("작성자 닉네임"),
                fieldWithPath("imageUrls").type(JsonFieldType.ARRAY)
                    .description("게시글 이미지 url 리스트").optional()))
        );

  }

}