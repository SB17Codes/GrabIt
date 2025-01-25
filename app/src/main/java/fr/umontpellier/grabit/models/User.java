package fr.umontpellier.grabit.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String email;
    private String name;
    private String imgurl;
    private String userType;
    private List<String> interests;
    private String location;

    // Required empty constructor for Firebase
    public User() {
        this.interests = new ArrayList<>();
    }

    // Main constructor
    public User(@NonNull String uid, @NonNull String email, @NonNull String userType, @NonNull String name) {
        this.uid = Objects.requireNonNull(uid, "UID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.userType = Objects.requireNonNull(userType, "User type cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.interests = new ArrayList<>();
    }

    // Legacy constructor
    public User(@NonNull String uid, @NonNull String email, @NonNull String userType) {
        this(uid, email, userType, "");
    }

    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) {
        this.uid = Objects.requireNonNull(uid);
    }

    @NonNull
    public String getEmail() { return email; }
    public void setEmail(@NonNull String email) {
        this.email = Objects.requireNonNull(email);
    }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) {
        this.name = Objects.requireNonNull(name);
    }

    @NonNull
    public String getUserType() { return userType; }
    public void setUserType(@NonNull String userType) {
        this.userType = Objects.requireNonNull(userType);
    }

    @NonNull
    public List<String> getInterests() {
        return interests != null ? interests : new ArrayList<>();
    }
    public void setInterests(@Nullable List<String> interests) {
        this.interests = interests != null ? interests : new ArrayList<>();
    }

    @Nullable
    public String getLocation() { return location; }
    public void setLocation(@Nullable String location) {
        this.location = location;
    }


    public String getImgurl() {
        return imgurl;
    }

    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    @Exclude
    public boolean isManager() {
        return "manager".equals(userType);
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", userType='" + userType + '\'' +
                ", interests=" + interests +
                ", location='" + location + '\'' +
                '}';
    }
}