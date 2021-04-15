package com.bfourclass.euopendata.user.json;

import com.bfourclass.euopendata.security.SimpleHashingAlgo;
import com.bfourclass.euopendata.user.UserModel;
import com.bfourclass.euopendata.user.forms.FormValidator;

public class UserRegisterJSONRequest {
    public final String username;
    public final String email;
    public final String password;
    public final String profilePhotoLink;

    public UserRegisterJSONRequest(String username, String email, String password, String profilePhotoLink) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.profilePhotoLink = profilePhotoLink;
    }

    public UserModel toUser() {
        return new UserModel(username, email, SimpleHashingAlgo.hash(password), profilePhotoLink);
    }

    public boolean isValid() {
        return FormValidator.isValidRegisterForm(this);
    }
}