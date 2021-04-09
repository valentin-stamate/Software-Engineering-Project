package com.bfourclass.euopendata.user;

import com.bfourclass.euopendata.email.EmailService;
import com.bfourclass.euopendata.external_api.ExternalAPI;
import com.bfourclass.euopendata.external_api.instance.aqicn_data.AirPollution;
import com.bfourclass.euopendata.external_api.instance.covid_information.CovidInformation;
import com.bfourclass.euopendata.external_api.instance.weather.Weather;
import com.bfourclass.euopendata.hotel.HotelModel;
import com.bfourclass.euopendata.hotel.json.Hotel;
import com.bfourclass.euopendata.hotel.json.HotelJSONRequest;
import com.bfourclass.euopendata.requests.APIError;
import com.bfourclass.euopendata.security.StringGenerator;
import com.bfourclass.euopendata.user.auth.SecurityContext;
import com.bfourclass.euopendata.user.json.HotelInformationJSON;
import com.bfourclass.euopendata.user.json.UserLoginJSONRequest;
import com.bfourclass.euopendata.user.json.UserRegisterJSONRequest;
import com.bfourclass.euopendata.user_verification.UserVerification;
import com.bfourclass.euopendata.user_verification.UserVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserVerificationService userVerificationService;
    private final SecurityContext securityContext;

    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService, UserVerificationService userVerificationService, SecurityContext securityContext) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userVerificationService = userVerificationService;
        this.securityContext = securityContext;
    }

    private boolean checkTokenIsValid(String token) {
        return securityContext.exists(token);
    }

    public UserModel getUserFromToken(String token) {
        String username = securityContext.extractUsername(token);
        return userRepository.findUserByUsername(username);
    }

    public void createUserByForm(UserRegisterJSONRequest registerForm) {
        UserModel userModel = registerForm.toUser();

        String verificationKey = StringGenerator.generate();
        UserVerification userVerification = new UserVerification(userModel, verificationKey);

        userVerificationService.save(userVerification);

        userRepository.save(userModel);
        emailService.sendEmailVerificationEmail(userModel.getUsername(), userModel.getEmail(), verificationKey);
    }

    public String loginUserReturnToken(UserLoginJSONRequest userLoginJSONRequest) {
        return securityContext.authenticateUserReturnToken(userLoginJSONRequest.username);
    }

    public boolean userExists(String username) {
        return userRepository.findUserByUsername(username) != null;
    }

    private boolean verifyEmail(String email) {
        String regex = "([a-zA-Z0-9]+(?:[._+-][a-zA-Z0-9]+)*)@([a-zA-Z0-9]+(?:[.-][a-zA-Z0-9]+)*[.][a-zA-Z]{2,})";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

    public void updateUser(UserModel userModel) {
        userRepository.save(userModel);
    }

    public ResponseEntity<Object> checkUserToken(String token) {
        // check if token exists in request
        if (token == null) {
            return new ResponseEntity<>(
                    new APIError("Missing Authorization header"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        // check if token exists in SecurityContext
        if (!checkTokenIsValid(token)) {
            return new ResponseEntity<>(
                    new APIError("Invalid Authorization header"),
                    HttpStatus.UNAUTHORIZED
            );
        }

        return null;
    }

    public UserModel getUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    public List<HotelInformationJSON> getHotelInformation(UserModel userModel) {
        List<HotelInformationJSON> response = new ArrayList<>();

        List<HotelModel> hotels = userModel.getUserHotels();

        for (HotelModel hotelModel : hotels) {
            HotelJSONRequest hotelJSONRequest = new HotelJSONRequest(hotelModel.getHotelName(), hotelModel.getLocationName());
            response.add(getHotelInformation(hotelJSONRequest));
        }

        return response;
    }

    public HotelInformationJSON getHotelInformation(HotelJSONRequest hotelJSONRequest) {
        Hotel hotel = new Hotel(hotelJSONRequest.hotelName, hotelJSONRequest.locationName);
        Weather weather = ExternalAPI.getWeather(hotelJSONRequest.locationName);
        CovidInformation covidInformation = ExternalAPI.getCovidInformation(hotelJSONRequest.locationName);
        AirPollution airPollution = ExternalAPI.getAirPollution(hotelJSONRequest.locationName);

        return new HotelInformationJSON(hotel, weather, covidInformation, airPollution);
    }
}
