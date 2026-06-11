package org.example.authservice.api.dto.cahce;

import lombok.Getter;
import lombok.Setter;
import org.example.authservice.db.UserEntity;

@Getter
@Setter
public class RegistrationData {
    UserEntity user;
    String verificationCode;
    long timestamp;

    public RegistrationData(UserEntity user, String verificationCode) {
        this.user = user;
        this.verificationCode = verificationCode;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 15 * 60 * 1000;
    }


}
