package com.spring.mongodb.controllers;

import com.spring.mongodb.models.ERole;
import com.spring.mongodb.models.Role;
import com.spring.mongodb.models.User;
import com.spring.mongodb.models.UserLogRecord;
import com.spring.mongodb.payload.request.LoginRequest;
import com.spring.mongodb.payload.request.SignupRequest;
import com.spring.mongodb.payload.response.JwtResponse;
import com.spring.mongodb.payload.response.MessageResponse;
import com.spring.mongodb.repository.RoleRepository;
import com.spring.mongodb.repository.UserLogRepository;
import com.spring.mongodb.repository.UserRepository;
import com.spring.mongodb.security.jwt.JwtUtils;
import com.spring.mongodb.security.services.UserDetailsImpl;
import com.spring.mongodb.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserLogRepository userLogRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        String currentDate = new Date().toString();
        UserLogRecord userLogRecord = new UserLogRecord(userDetails.getUsername(), currentDate);
        userLogRepository.save(userLogRecord);

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getNic(),
                userDetails.getPhone(),
                userDetails.getDate(),
                userDetails.getTheme(),
                roles
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

        if (userRepository.existsByNic(signUpRequest.getNic()) && userRepository.existsByUsername(signUpRequest.getUsername()) ) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: NIC and Email is already in use!"));
        }

        else if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        else if (userRepository.existsByNic(signUpRequest.getNic())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: NIC is already in use!"));
        }



        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getName(),
                signUpRequest.getNic(),
                signUpRequest.getPhone(),
                signUpRequest.getDate(),
                signUpRequest.getTheme(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;

                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);


        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/users")
    public List<User> findAll () {

        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<User> findByUsername (@PathVariable String id){

        return userRepository.findById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser (@RequestBody User user, @PathVariable String id){
        Optional<User> userData = userRepository.findById(id);
        if (userData.isPresent()) {
            System.out.println("reading user");
            User _user = userData.get();

            _user.setUsername(user.getUsername());
            _user.setName(user.getName());
            _user.setNic(user.getNic());
            _user.setPhone(user.getPhone());

            return new ResponseEntity<>(userRepository.save(_user), HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("color/{id}")
    public ResponseEntity<User> updateUserTheme (@RequestBody User user, @PathVariable String id){
        Optional<User> userData = userRepository.findById(id);
        if (userData.isPresent()) {
            System.out.println("reading theme color");
            User _user = userData.get();
            _user.setTheme(user.getTheme());

            return new ResponseEntity<>(userRepository.save(_user), HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> removeUser(@PathVariable String id) {
        try {
            userRepository.deleteById(id);
            System.out.println("Deleted user: "+ id);
            ResponseEntity.ok(new MessageResponse("User deleted!"));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}