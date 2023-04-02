package com.example.challengeservice.controller;

import com.example.challengeservice.common.response.ResponseService;
import com.example.challengeservice.common.result.ListResult;
import com.example.challengeservice.common.result.Result;
import com.example.challengeservice.common.result.SingleResult;
import com.example.challengeservice.dto.request.ChallengeRecordRequestDto;
import com.example.challengeservice.dto.request.ChallengeRoomRequestDto;
import com.example.challengeservice.dto.request.ReportRecordRequestDto;
import com.example.challengeservice.dto.response.ChallengeCreateResponseDto;
import com.example.challengeservice.dto.response.ChallengeRoomResponseDto;
import com.example.challengeservice.dto.response.SimpleChallengeResponseDto;
import com.example.challengeservice.dto.response.*;
import com.example.challengeservice.dto.response.SolvedListResponseDto;
import com.example.challengeservice.infra.amazons3.service.AmazonS3Service;
import com.example.challengeservice.service.ChallengeService;
import com.example.challengeservice.service.ChallengeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/challenges")
@Slf4j
@RequiredArgsConstructor
public class ChallengeController {
    private final ResponseService responseService;
    private final ChallengeService challengeService;

    private final AmazonS3Service s3Service;

    /**
     * author  : 홍금비
     * explain :챌린지 생성
     * **/
    @PostMapping()
    public ResponseEntity<ChallengeCreateResponseDto> createChallenge(@Valid @ModelAttribute ChallengeRoomRequestDto challengeRoomRequestDto) throws IOException {
        log.info(challengeRoomRequestDto.getTitle());
        log.info(challengeRoomRequestDto.getBackGroundFile().isEmpty()+" ");
        log.info(challengeRoomRequestDto.getAlgorithmCount()+" ");
        log.info(challengeRoomRequestDto.getStartDate());
        log.info(challengeRoomRequestDto.getEndDate());
        log.info(challengeRoomRequestDto.getIntroduce());

        Long id=challengeService.createChallenge(challengeRoomRequestDto);
        String message="[Success] 챌린지 방이 생성되었습니다.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ChallengeCreateResponseDto.from(id, message));
    }



    /**
     * author : 홍금비
     * explain: 메인 페이지에서 참여가능한 첼린지 목록 조회
     * */
    @GetMapping("")
    public  ResponseEntity<List<SimpleChallengeResponseDto>> getListSimpleChallenge (@RequestParam ("category") String category, @RequestParam (value = "offset", required = false) Long offset , @RequestParam (value = "search" ,required = false) String search , @RequestParam ("size") int size){

        List<SimpleChallengeResponseDto> list = challengeService.getListSimpleChallenge(category,search,size,offset);
        return  ResponseEntity.status(HttpStatus.OK).body(list);
    }


    /** 신대득
     * 챌린지 조회 **/
    @GetMapping("/{challengeId}")
    public ResponseEntity<ChallengeRoomResponseDto> readChallenge(@PathVariable("challengeId") String challengeId){
        log.info("챌린지 조회 실행");
        return ResponseEntity.status(HttpStatus.OK).body(challengeService.readChallenge(Long.parseLong(challengeId)));
    }

    /** 신대득
     * 챌린지 리스트들의 정보 조회
     */
    @PostMapping("/listInfo")
    public SingleResult<Map<Long, ChallengeInfoResponseDto>> challengeInfoList(@RequestBody Map<String, List<Long>> map){
        List<Long> challengeIdList= map.get("challengeIdList");
        return responseService.getSingleResult(challengeService.challengeInfoList(challengeIdList));
    }


    /** 챌린지 상세 조회 ** (입장 페이지)*/


    /** 신대득
     * 챌린지 참가하기 **/
    /**
     * pay-service로부터 호출되는 API입니다.
     * 챌린지에 유저가 결제가 완료된 뒤, 응답으로 받습니다.
     * @author djunnni
     * @param challengeId
     * @param request
     * @return
     */
    @PostMapping("/{challengeId}/users")
    public SingleResult<Long> joinChallenge(@PathVariable("challengeId") Long challengeId, HttpServletRequest request){
        String userId = request.getHeader("userId");
        log.info("pay-service로 부터 받은 데이터 => challengeId: {}, userId: {}", challengeId, userId);
        return responseService.getSingleResult(1L);
    }
    @PostMapping("/{challengeId}/users/{userId}")
    public SingleResult<String> joinChallenge(@PathVariable("challengeId") Long challengeId, @PathVariable("userId") Long userId){
        return responseService.getSingleResult(challengeService.joinChallenge(challengeId, userId));
    }


    /** 신대득
     * 현재 user가 참가중인 챌린지 개수 반환
     */
    @GetMapping("/challengeInfo/users/{userId}")
    public SingleResult<UserChallengeInfoResponseDto> userChallengeInfo(@PathVariable Long userId){
        return responseService.getSingleResult(challengeService.myChallengeList(userId));

    }

    /** 신대득
     * 유저 백준 아이디를 통해 해당 유저의 푼 문제 리스트 찾기 (크롤링)
     * 나온 결과를 계산해서 user에 넣어줘야한다.
     * Todo : userId로 baekjoonId 가져오는걸로 바꾸기
     */
    @GetMapping("/baekjoon/{baekjoonId}")
    public SingleResult<SolvedListResponseDto> solvedProblemList(@PathVariable("baekjoonId") String baekjoonId){
        return responseService.getSingleResult(challengeService.solvedProblemList(baekjoonId));
    }

    /**
     * 신대득
     * 유저 깃허브 아이디를 통해 해당 유저의 커밋 찾기 (크롤링)
     * 나온 결과를 계산해서 user에 넣어줘야한다.
     * Todo : 반환 타입 등 다시 만들 예정
     * Todo : userId로 commit 가져오는걸로 바꾸기
     */
    @GetMapping("/github/{githubId}")
    public SingleResult<CommitCountResponseDto> getGithubList(@PathVariable("githubId") String githubId){
        return responseService.getSingleResult(challengeService.getGithubCommit(githubId));
    }




    /**
     * 신대득
     * 유저가 푼 문제 리스트 갱신
     */
    @GetMapping("/baekjoon/users/{userId}")
    public Result updateUserBaekjoon(@PathVariable Long userId){
        challengeService.updateUserBaekjoon(userId);
        return responseService.getSuccessResult();
    }


    /**
     * 신대득
     * 선택한 유저가
     * 해당 날짜에 푼 문제를 조회하는 API
     * @param userId // 조회 할 유저의 id
     * @param selectDate // 조회 할 날짜
     * @return
     */
    @GetMapping("/baekjoon/users/date")
    public SingleResult<SolvedListResponseDto> checkDateUserBaekjoon(@RequestParam Long userId, @RequestParam String selectDate){
        return responseService.getSingleResult(challengeService.checkDateUserBaekjoon(userId, selectDate));
    }




    /** 사진 인증 저장 **/
    @PostMapping("photo-record")
    public ResponseEntity<String> createChallengeRecord(@ModelAttribute ChallengeRecordRequestDto requestDto) throws IOException{

        challengeService.createPhotoRecord(requestDto);
        return ResponseEntity.status(HttpStatus.OK).body("인증기록 저장완료");
    }

    /** 사진 인증 상세 조회 (Auth) 로그인이 반드시 필요함) api 변경필요합니다. **/
    @GetMapping("photo-record/{recordId}/users/{userId}")
    public SingleResult<PhotoRecordDetailResponseDto> getPhotoRecordDetail(@PathVariable("recordId") Long recordId , @PathVariable("userId") Long userId){

        return responseService.getSingleResult(challengeService.getPhotoRecordDetail(userId ,recordId));

    }



    /** 나의 인증 기록 불러오기 **/
    @GetMapping("{challengeId}/record/users/{userId}")
    public ListResult<?> getSelfChallengeRecord(@PathVariable("challengeId") Long challengeRoomId ,@PathVariable("userId") Long userId,@RequestParam("view") String viewType){

        return responseService.getListResult(challengeService.getSelfPhotoRecord(challengeRoomId,userId,viewType));
    }

    // 위에거
    // 이걸로 바꿀 예정
    /*
    @GetMapping("{challengeId}/record/users/{userId}")
    public ListResult<?> getSelfRecord(@PathVariable("challengeId") Long challengeRoomId ,@PathVariable("userId") Long userId,@RequestParam("view") String viewType, @RequestParam("category") String category){
        return responseService.getListResult(challengeService.getSelfRecord(challengeRoomId,userId,viewType,category));
    }
     */

    /** 팀원의 인증 기록 불러오기 테스트 코드 (로그인이 되어있어야함) **/
    @GetMapping("{challengeId}/record")
    public ListResult<?> getTeamChallengeRecord(@PathVariable("challengeId")Long challengeRoomId ,@RequestParam("view") String viewType){

        return responseService.getListResult(challengeService.getTeamPhotoRecord(challengeRoomId,viewType));

    }


    /** 사진 기록 신고하기(반드시 로그인이 되어있어야함) **/
    @PostMapping ("report/record")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result reportRecord (@RequestBody ReportRecordRequestDto reportRequestDto){

        challengeService.reportRecord(reportRequestDto);
        return responseService.getSuccessResult();
    }

    /**
     * 스케줄링으로 실행되는 메서드의 테스트
     * @return
     */
    @GetMapping("/test/record")
    public Result testRecord (){
        challengeService.createDailyRecord();
        return responseService.getSuccessResult();
    }

    @GetMapping("/test/payment")
    public Result testPayment (){
        challengeService.culcDailyPayment();
        return responseService.getSuccessResult();
    }


    /** 기본 사진 업로드 **/
    @PostMapping("upload/image")
    public String updateDefaultImage(@RequestParam("file") MultipartFile multipartFile , @RequestParam("dir") String dir) throws IOException{

        return  s3Service.upload(multipartFile,dir);
    }


    /** (헤더 유저 id **) 나의 챌린지 기록 조회 (완료 , 진행중 , 시작전)

     */


    /**
     * author : 홍금비
     * explain : 내가 참여한 챌린지 조회
     * @param status : PROCEED | DONE | NOT_OPEN
     */
    @GetMapping("my-challenge")
    public  ListResult<MyChallengeResponseDto> getMyChallengeList(HttpServletRequest request,@NotBlank @RequestParam ("status") String status){
        Long userId= Long.parseLong(request.getHeader("userId"));
        return responseService.getListResult(challengeService.getMyChallengeList(userId,status));
    }



}
