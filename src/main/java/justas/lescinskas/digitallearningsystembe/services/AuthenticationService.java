package justas.lescinskas.digitallearningsystembe.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import justas.lescinskas.digitallearningsystembe.entity.RegisterUser;
import justas.lescinskas.digitallearningsystembe.entity.User;
import justas.lescinskas.digitallearningsystembe.entity.UserData;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AuthenticationService {

    public final Firestore db;
    private final String FIREBASE_WEB_API_KEY = System.getenv("FIREBASE_WEB_API_KEY");

    public AuthenticationService(Firestore db) {
        this.db = db;
    }

    public String login(User user) {
        String email = user.getEmail();
        String password = user.getPassword();

        RestTemplate restTemplate = new RestTemplate();
        String uri = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_WEB_API_KEY;

        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        request.put("returnSecureToken", true);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(uri, request, Map.class);
            Map<String, String> responseBody = response.getBody();
            // Extract the ID token and return it or use it for further operations
            String idToken = responseBody.get("idToken");
            return idToken;
        } catch (Exception e) {
            // Handle error (e.g., invalid credentials, user not found)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login failed");
        }
    }

    public String register(RegisterUser registerUser) {

        ApiFuture<QuerySnapshot> future = db.collection("users").whereEqualTo("email", registerUser.getEmail()).get();

        try {
            QuerySnapshot snapshot = future.get();
            if (!snapshot.getDocuments().isEmpty()) {
                // Email already exists
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
            } else {
                // Register new user
                db.collection("users").document(registerUser.getEmail()).set(registerUser);

                UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest();
                createRequest.setEmail(registerUser.getEmail());
                createRequest.setPassword(registerUser.getPassword());
                FirebaseAuth.getInstance().createUser(createRequest);

                return "User registered successfully";
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed", e);
        }
    }

    public UserData fetchUserData(String authToken) throws ExecutionException, InterruptedException {
        FirebaseToken token;
        try {
            token = FirebaseAuth.getInstance().verifyIdToken(authToken);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }

        ApiFuture<DocumentSnapshot> future = db.collection("users").document(token.getEmail()).get();

        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(UserData.class);
        }

        throw new RuntimeException();
    }
}
