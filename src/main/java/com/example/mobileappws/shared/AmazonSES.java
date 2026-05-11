package com.example.mobileappws.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.mobileappws.shared.dto.UserDto;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class AmazonSES {
    @Value("${app.base.url}")
    private String baseUrl;

    final String FROM = "reirasouffrances.x.x@gmail.com";
    final String SUBJECT = "One last step to complete your registration with Layla's Backend App";
    final String PASSWORD_RESET_SUBJECT = "Password reset request";

    public void verifyEmail(UserDto userDto) {
        SesClient client = SesClient.builder()
            .region(Region.US_EAST_2)
            .build();

        String htmlBody = "<h1>Please verify your email address</h1>"
            + "<p>Thank you for registering with our mobile app. To complete registration process and be able to log in,"
            + " click on the following link: "
            + "<a href='" + baseUrl + "/verification-service/email-verification.html?token=" + userDto.getEmailVerificationToken() + "'>"
            + "Final step to complete your registration" + "</a><br/><br/>"
            + "Thank you! And we are waiting for you inside!";

        String textBody = "Please verify your email address. "
            + "Thank you for registering with our mobile app. To complete registration process and be able to log in,"
            + " open then the following URL in your browser window: "
            + baseUrl + "/verification-service/email-verification.html?token=" + userDto.getEmailVerificationToken()
            + " Thank you! And we are waiting for you inside!";

        SendEmailRequest request = SendEmailRequest.builder()
            .destination(Destination.builder()
                .toAddresses(userDto.getEmail())
                .build())
            .message(Message.builder()
                .body(Body.builder()
                    .html(Content.builder()
                        .charset("UTF-8")
                        .data(htmlBody)
                        .build())
                    .text(Content.builder()
                        .charset("UTF-8")
                        .data(textBody)
                        .build())
                    .build())
                .subject(Content.builder()
                    .charset("UTF-8")
                    .data(SUBJECT)
                    .build())
                .build())
            .source(FROM)
            .build();

        client.sendEmail(request);
        System.out.println("Email sent!");
        client.close();
    }

    public boolean sendPasswordResetRequest(String firstName, String email, String token) {
        boolean returnValue = false;

        SesClient client = SesClient.builder()
            .region(Region.US_EAST_2)
            .build();

        String htmlBody = "<h1>A request to reset your password</h1>"
            + "<p>Hi, " + firstName + "!</p> "
            + "<p>Someone has requested to reset your password with our project. If it were not you, please ignore it."
            + " otherwise please click on the link below to set a new password: "
            + "<a href='" + baseUrl + "/verification-service/password-reset.html?token=" + token + "'>"
            + " Click this link to Reset Password"
            + "</a><br/><br/>"
            + "Thank you!";

        String textBody = "A request to reset your password "
            + "Hi, " + firstName + "! "
            + "Someone has requested to reset your password with our project. If it were not you, please ignore it."
            + " otherwise please open the link below in your browser window to set a new password:"
            + baseUrl + "/verification-service/password-reset.html?token=" + token
            + " Thank you!";

        SendEmailRequest request = SendEmailRequest.builder()
            .destination(Destination.builder()
                .toAddresses(email)
                .build())
            .message(Message.builder()
                .body(Body.builder()
                    .html(Content.builder()
                        .charset("UTF-8")
                        .data(htmlBody)
                        .build())
                    .text(Content.builder()
                        .charset("UTF-8")
                        .data(textBody)
                        .build())
                    .build())
                .subject(Content.builder()
                    .charset("UTF-8")
                    .data(PASSWORD_RESET_SUBJECT)
                    .build())
                .build())
            .source(FROM)
            .build();

        SendEmailResponse result = client.sendEmail(request);
        if (result != null && result.messageId() != null && !result.messageId().isEmpty()) {
            returnValue = true;
        }

        client.close();
        return returnValue;
    }
}