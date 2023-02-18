package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.exception.impl.LoginAuthException;
import com.dayone.model.Auth;
import com.dayone.persist.MemberRepository;
import com.dayone.persist.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));
    }

    public MemberEntity register(Auth.SignUp member) {
        // 아이디가 존재하는 경우 exception 발생
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        var result = this.memberRepository.save(member.toEntity());
        return result;
    }

    public MemberEntity authenticate(Auth.SignIn member) {
        // id 로 멤버 조회
        var user = this.memberRepository.findByUsername(member.getUsername()).orElseThrow(()->new UsernameNotFoundException("없는 user 입니다."));
        // 패스워드 일치 여부 확인
        //      - 일치하지 않는 경우 400 status 코드와 적합한 에러 메시지 반환
        //      - 일치하는 경우, 해당 멤버 엔티티 반환
        if(!this.passwordEncoder.matches(member.getPassword(), user.getPassword())){
            throw new LoginAuthException();
        }

        return user;
    }
}
