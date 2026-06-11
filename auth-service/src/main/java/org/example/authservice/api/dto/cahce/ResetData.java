package org.example.authservice.api.dto.cahce;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetData {
    String email;
    String code;
    long timestamp;



    public ResetData(String email, String code) {
        this.email = email;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 15 * 60 * 1000;
    }
}
