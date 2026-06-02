package br.com.baba.tibia_analyzer.api.auth;

import br.com.baba.tibia_analyzer.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class DiscordOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        String discordId = user.getAttribute("id");
        String username = user.getAttribute("global_name");
        if (username == null) {
            username = user.getAttribute("username");
        }
        String avatar = user.getAttribute("avatar");
        String avatarUrl = buildAvatarUrl(discordId, avatar);

        if (discordId != null) {
            userService.upsertFromDiscord(discordId, username, avatarUrl);
        }

        return user;
    }

    private String buildAvatarUrl(String discordId, String avatarHash) {
        if (discordId == null || avatarHash == null) {
            return null;
        }
        return "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png";
    }
}
