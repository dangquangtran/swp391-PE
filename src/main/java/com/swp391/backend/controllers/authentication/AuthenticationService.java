/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.swp391.backend.controllers.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.swp391.backend.model.token.Token;
import com.swp391.backend.model.token.TokenService;
import com.swp391.backend.model.user.Role;
import com.swp391.backend.model.user.User;
import com.swp391.backend.model.user.UserDTO;
import com.swp391.backend.model.user.UserService;
import com.swp391.backend.security.JwtService;
import com.swp391.backend.utils.mail.ConfirmCodeTemplete;
import com.swp391.backend.utils.mail.EmailSender;
import com.swp391.backend.utils.mail.ForgetCodeTemplete;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Lenovo
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticatedManager authenticatedManager;
    private final EmailSender gmailSender;

    public AuthenticationResponse authentication(AuthenticationRequest request) {
        User user = (User) userService.loadUserByUsername(request.getEmail());
        user.setLogout(false);
        userService.save(user);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        Token authToken = tokenService.findByUserAndType(user, "auth");
        if (authToken != null) {
            if (jwtService.isTokenValid(authToken.getValue(), user)) {
                return AuthenticationResponse.builder()
                        .token(authToken.getValue())
                        .build();
            } else {
                tokenService.delete(authToken);
            }
        }
        user.setTimeout(new Date(System.currentTimeMillis() + 1000 * 60 * 30));
        userService.save(user);
        String jwtToken = jwtService.generateToken(user);
        LocalDateTime expiredAt = jwtService.extractExpiration(jwtToken)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        authToken = Token.builder()
                .createAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .value(jwtToken)
                .user(user)
                .type("auth")
                .build();

        tokenService.save(authToken);
        authenticatedManager.setAuthenticatedUser(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse google(GoogleRequest request) {
        String email = request.getEmail();
        User user = (User) userService.loadUserByUsername(email, "google");
        Token authToken;
        if (user != null) {
            user.setLogout(false);
            userService.save(user);

            authToken = tokenService.findByUserAndType(user, "auth");
            if (authToken != null) {
                if (jwtService.isTokenValid(authToken.getValue(), user)) {
                    return AuthenticationResponse.builder()
                            .token(authToken.getValue())
                            .build();
                } else {
                    tokenService.delete(authToken);
                }
            }
        } else {
            user = User.builder()
                    .firstname(request.getGiven_name())
                    .lastname(request.getFamily_name())
                    .email(request.getEmail())
                    .gender(null)
                    .imageurl(request.getPicture())
                    .enabled(true)
                    .locked(false)
                    .password(null)
                    .role(Role.CUSTOMER)
                    .build();
        }
        UsernamePasswordAuthenticationToken usernameToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(usernameToken);

        user.setTimeout(new Date(System.currentTimeMillis() + 1000 * 60 * 30));
        userService.save(user);
        String jwtToken = jwtService.generateToken(user);
        LocalDateTime expiredAt = jwtService.extractExpiration(jwtToken)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        authToken = Token.builder()
                .createAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .value(jwtToken)
                .user(user)
                .type("auth")
                .build();

        tokenService.save(authToken);
        authenticatedManager.setAuthenticatedUser(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public RegistrationResponse registration(RegistrationRequest request) throws Exception {
        boolean isExist = userService.isExist(request.getEmail());
        if (isExist) {
            throw new IllegalStateException("Email is already taken!");
        }
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .gender(request.getGender())
                .enabled(false)
                .locked(false)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();
        userService.save(user);
        String confToken = UUID.randomUUID().toString();
        Token registration = Token.builder()
                .createAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .value(confToken)
                .user(user)
                .type("regis")
                .build();
        tokenService.save(registration);
        String templete = ConfirmCodeTemplete.getTemplete("Bird Trading Platform", user.getFirstname(), "http://localhost:3000/confirm?token=" + confToken);
        gmailSender.send("Registration Confirmation", templete, user.getEmail());

        return RegistrationResponse.builder()
                .email(user.getEmail())
                .status("Registered successfully. Please verify your email to activate your account!")
                .build();
    }

    public RegistrationResponse registrationConfirm(String token) {
        Token confToken = tokenService.findByValue(token);

        if (confToken.getConfirmedAt() != null) {
            throw new IllegalStateException("Email already confirmed");
        }

        LocalDateTime expiredAt = confToken.getExpiredAt();
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }
        confToken.setConfirmedAt(LocalDateTime.now());
        tokenService.save(confToken);
        userService.enableUser(confToken.getUser());
        return RegistrationResponse.builder()
                .email(confToken.getUser().getEmail())
                .status("Verify email successfully.")
                .build();
    }

    public String signout() {
        User user = (User) authenticatedManager.getAuthenticatedUser();
        if (user == null) {
            return "Authentication First";
        }
        user.setLogout(true);
        userService.save(user);
        authenticatedManager.setAuthenticatedUser(null);
        return "Logout Successfully!";
    }

    public UserDTO resetFind(@RequestParam("email") String email) {
        User user = (User) userService.loadUserByUsername(email);
        return UserDTO.builder()
                .email(user.getEmail())
                .fullname(user.getFirstname() + " " + user.getLastname())
                .imageurl(user.getImageurl())
                .build();
    }

    public ResetResponse resetSend(UserDTO userDTO) throws Exception {
        User user = (User) userService.loadUserByUsername(userDTO.getEmail());
        Token checkToken = tokenService.findByUserAndType(user, "reset");
        if (checkToken != null) {
            tokenService.delete(checkToken);
        }
        String forgetCode = Math.round((Math.random() * 899999 + 100000)) + "";
        System.out.println("Forget Code: " + forgetCode);
        String token = passwordEncoder.encode(forgetCode);
        Token resetToken = Token.builder()
                .createAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusSeconds(60))
                .value(token)
                .user(user)
                .type("reset")
                .build();
        tokenService.save(resetToken);
        String templete = ForgetCodeTemplete.getTemplete("Bird Trading Platform", user.getFirstname(), forgetCode);
        gmailSender.send("Forget Password Confirmation", templete, user.getEmail());
        return ResetResponse.builder()
                .email(user.getEmail())
                .status("Send confirm code succesfully")
                .build();
    }

    public ResetResponse resetConfirm(UserDTO userDTO, String code) {
        String status = "Confirm Succesfully";
        User user = (User) userService.loadUserByUsername(userDTO.getEmail());
        Token resetToken = tokenService.findByUserAndType(user, "reset");
        if (!tokenService.isValid(resetToken)) {
            status = "Confirm code is expired!";
            throw new IllegalStateException(status);
        }
        boolean isMatching = passwordEncoder.matches(code, resetToken.getValue());
        if (!isMatching) {
            status = "Code isn't match";
            throw new IllegalStateException(status);
        }
        return ResetResponse.builder()
                .email(user.getEmail())
                .status(status)
                .build();
    }

    public ResetResponse resetNew(UserDTO userDTO, String newPass) {
        String status = "Reset Password Succesfully";
        User user = (User) userService.loadUserByUsername(userDTO.getEmail());
        String newPassword = passwordEncoder.encode(newPass);
        user.setPassword(newPassword);
        userService.save(user);
        return ResetResponse.builder()
                .email(user.getEmail())
                .status(status)
                .build();
    }
}
