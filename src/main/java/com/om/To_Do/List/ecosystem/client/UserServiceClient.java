package com.om.To_Do.List.ecosystem.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("AUTHENTICATION-SERVICE")
public interface UserServiceClient {

    @PostMapping("/users/get-ids-by-phone-numbers")
    public ResponseEntity<List<Long>> getUserIdsByPhoneNumbers(@RequestBody List<String> phoneNumbers);

    @PostMapping("/get-id-by-phone-numbers")
    public ResponseEntity<Long> getUseridByPhoneNumber(@RequestBody String phoneNumber);
}
