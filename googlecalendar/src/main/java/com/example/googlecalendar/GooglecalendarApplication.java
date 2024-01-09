package com.example.googlecalendar;

import com.example.googlecalendar.util.GoogleCalendarApiUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class GooglecalendarApplication {

	public static void main(String[] args) {
		SpringApplication.run(GooglecalendarApplication.class, args);

        List<String> listOfAttendee = Arrays.asList(
//          LIST_OF_MAIL_YOU_WANT_TO_ADD_IN_EVENT_(GUESTS)_OR_ATTENDEE
        );

        try {
            String startDate = "08-01-2024";
            String startTime = "01:50:40";
            String endDate = "08-01-2024";
            String endTime = "01:55:40";

            GoogleCalendarApiUtil.createGoogleCalendarEvent("NAME_OF_EVENT"
                    , listOfAttendee
                    , "Demo test event by spring boot"
                    , startDate
                    , endDate
                    , startTime
                    , endTime
                    , "primary");

        } catch (Exception e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
    }
}
