package justas.lescinskas.digitallearningsystembe.controller;

import justas.lescinskas.digitallearningsystembe.entity.RegisterUser;
import justas.lescinskas.digitallearningsystembe.entity.User;
import justas.lescinskas.digitallearningsystembe.entity.UserData;
import justas.lescinskas.digitallearningsystembe.services.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterUser registerUser) {
        return new ResponseEntity<>(authenticationService.register(registerUser), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        return new ResponseEntity<>(authenticationService.login(user), HttpStatus.OK);
    }

    @GetMapping("/fetchUserData")
    public ResponseEntity<UserData> fetchUserData(@RequestHeader String authToken) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(authenticationService.fetchUserData(authToken), HttpStatus.OK);
    }

}