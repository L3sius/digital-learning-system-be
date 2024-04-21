package justas.lescinskas.digitallearningsystembe.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import justas.lescinskas.digitallearningsystembe.entity.RegisterUser;
import justas.lescinskas.digitallearningsystembe.entity.Results;
import justas.lescinskas.digitallearningsystembe.entity.User;
import justas.lescinskas.digitallearningsystembe.entity.UserData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public String saveResults(Results results) {
        // Get a reference to the collection "results"
        CollectionReference resultsCollection = db.collection("results");

        // Create a new document with a generated ID
        DocumentReference newResultRef = resultsCollection.document();

        // Set the fields of the document
        try {
            newResultRef.set(results).get(); // Assuming "results" is a POJO representing the data
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        // Return the ID of the newly created document
        return newResultRef.getId();
    }

    public List<Results> getResults(String email) throws ExecutionException, InterruptedException {
        List<Results> userResults = new ArrayList<>();

        // Get a reference to the collection "results"
        CollectionReference resultsCollection = db.collection("results");

        // Create a query to filter results by email
        Query query = resultsCollection.whereEqualTo("email", email);

        // Execute the query
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        // Retrieve documents from the query snapshot
        for (QueryDocumentSnapshot document : querySnapshot.get().getDocuments()) {
            // Map document data to Results object
            Results result = document.toObject(Results.class);
            userResults.add(result);
        }

        return userResults;
    }

    public String getFullName(String userEmail) throws ExecutionException, InterruptedException {
        // Get a reference to the users collection
        CollectionReference usersCollection = db.collection("users");

        // Query the users collection based on email
        DocumentReference userDocRef = usersCollection.document(userEmail);

        // Get the document snapshot
        DocumentSnapshot userSnapshot = userDocRef.get().get();

        if (userSnapshot.exists()) {
            // Extract name and surname from the document
            String name = userSnapshot.getString("name");
            String surname = userSnapshot.getString("surname");

            // Return the full name
            return name + " " + surname;
        } else {
            // User not found
            return null;
        }
    }
}
