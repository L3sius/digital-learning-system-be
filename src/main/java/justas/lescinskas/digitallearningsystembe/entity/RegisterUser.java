package justas.lescinskas.digitallearningsystembe.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterUser {
    // Setters
    // Getters
    private String email;
    private String name;
    private String surname;
    private String password;
    private String grade;

    public RegisterUser() {}

}

