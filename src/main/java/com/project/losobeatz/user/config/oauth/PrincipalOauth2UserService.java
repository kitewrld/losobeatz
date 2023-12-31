package com.project.losobeatz.user.config.oauth;

import com.project.losobeatz.user.config.auth.PrincipalDetails;
import com.project.losobeatz.user.config.oauth.provider.FacebookUserInfo;
import com.project.losobeatz.user.config.oauth.provider.GoogleUserInfo;
import com.project.losobeatz.user.config.oauth.provider.NaverUserInfo;
import com.project.losobeatz.user.config.oauth.provider.OAuth2Userinfo;
import com.project.losobeatz.user.model.User;
import com.project.losobeatz.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserRepository userRepository;

    // 구글로 부터 받은 userRequest 데이터에 대한 후처리되는 함수.
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("getClientRegistration = " + userRequest.getClientRegistration());
        System.out.println("getAccessToken = " + userRequest.getAccessToken().getTokenValue());

        OAuth2User oauth2User = super.loadUser(userRequest);
        // 구글 로그인 버튼 클릭 -> 구글 로그인 창 -> 로그인 완료 -> code리턴(OAuth-Client라이브러리) -> AccessTooken요청
        // userRequest정보 -> loadUser함수 호출 -> 구글로부터 회원 프로필 받아줌.
        System.out.println("getAttributes :"+oauth2User.getAttributes());

        OAuth2Userinfo oAuth2Userinfo = null;
        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
            System.out.println("구글 로그인 요청");
            oAuth2Userinfo = new GoogleUserInfo(oauth2User.getAttributes());
        } else if(userRequest.getClientRegistration().getRegistrationId().equals("facebook")) {
            System.out.println("페이스북 로그인 요청");
            oAuth2Userinfo = new FacebookUserInfo(oauth2User.getAttributes());
        } else if(userRequest.getClientRegistration().getRegistrationId().equals("naver")) {
            System.out.println("네이버 로그인 요청");
            oAuth2Userinfo = new NaverUserInfo((Map)oauth2User.getAttributes().get("response"));
        } else {
            System.out.println("우리는 구글과 페이스북, 네이버만 지원합니다.");
        }

        String provider = oAuth2Userinfo.getProvider();
        String providerId = oAuth2Userinfo.getProviderId();
        String username = provider+"_"+providerId;
        String password = bCryptPasswordEncoder.encode("겟인데어");
        String email = oAuth2Userinfo.getEmail();
        String role = "ROLE_USER";

        User userEntity = userRepository.findByUsername(username);

        if (userEntity == null) {
            System.out.println("OAuth 로그인이 최초입니다.");
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(userEntity);
        } else {
            System.out.println("로그인을 이미 한적이 있습니다. 당신은 자동회원가입이 되어 있습니다.");
        }

        return new PrincipalDetails(userEntity, oauth2User.getAttributes());
    }
}
