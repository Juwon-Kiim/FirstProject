package com.ssafy.five.controller;

import com.ssafy.five.controller.dto.DeleteUserReqDto;
import com.ssafy.five.controller.dto.FindUserResDto;
import com.ssafy.five.controller.dto.SignUpReqDto;
import com.ssafy.five.domain.entity.Users;
import com.ssafy.five.domain.repository.UserRepository;
import com.ssafy.five.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    // 성공하면 return true
    @PostMapping("/user")
    public boolean signUp(@Valid @RequestBody SignUpReqDto signUpReqDto){
        if(userService.signUp(signUpReqDto)){
            return true;
        }
        return false;
    }

    // 회원 한명 조회
    @GetMapping("/user/{userId}")
    public FindUserResDto findUser(@PathVariable String userId){
        Users user = userService.findUserByUserId(userId);

        FindUserResDto findUserResDto = FindUserResDto.builder()
                .userId(user.getUserId())
                .password(user.getPassword())
                .birth(user.getBirth())
                .emailId(user.getEmailId())
                .emailDomain(user.getEmailDomain())
                .name(user.getName())
                .nickname(user.getNickname())
//                .ment(user.getMent())
                .number(user.getNumber())
//                .gender(user.getGender())
//                .picture(user.getPicture())
                .point(user.getPoint())
                .build();

        return findUserResDto;
    }

    // 회원 정보 수정
    @PutMapping("/user")
    public void updateUser(@Valid @RequestBody Users user){
        userService.updateUser(user);
    }

    // 회원 탈퇴
    @DeleteMapping("/user")
    public void deleteUser(@Valid @RequestBody DeleteUserReqDto deleteUserReqDto){
        userService.deleteUser(deleteUserReqDto);
    }
}
