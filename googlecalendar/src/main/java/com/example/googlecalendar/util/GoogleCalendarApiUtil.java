package com.example.googlecalendar.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GoogleCalendarApiUtil {

    private static final String APPLICATION_NAME = "Demo";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String REFRESHTOKENS_DIRECTORY_PATH =""; //ENTER_PATH_WHERE_YOU_WANT_TO_STORE_REFRESHTOKEN_WHILE_USING_OAUTH2
    private static String oAuth2Token;
    private static String serviceAccountToken;
    private static String subjectEmail;


    @Value("${google.credential.file}")
    private void setOAuth2Token(String oAuth2Token) {
        GoogleCalendarApiUtil.oAuth2Token = oAuth2Token;
    }

    @Value("${google.serviceaccount.credential.file}")
    private void setServiceAccountToken(String serviceAccountToken) {
        GoogleCalendarApiUtil.serviceAccountToken = serviceAccountToken;
    }

    @Value("${google.calendar.api.subject.email}")
    private void setSubjectEmail(String subjectEmail) {
        GoogleCalendarApiUtil.subjectEmail = subjectEmail;
    }

    public static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) {

        try {
            // Load client secrets.
            InputStream in = new ClassPathResource(oAuth2Token).getInputStream();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
                    Collections.singletonList(CalendarScopes.CALENDAR))
                    .setDataStoreFactory(new FileDataStoreFactory(new File(REFRESHTOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        } catch (IOException e) {
            log.error("Error while fetching credentials or creating authorization request");
        }
        return null;
    }

    public static Calendar getCalendarService() throws GeneralSecurityException, IOException {
        // Create the Http Transport.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Calendar getCalendarServiceByServiceAccount(String subjectEmail) throws IOException, GeneralSecurityException {

        InputStream in = new ClassPathResource(serviceAccountToken).getInputStream();

        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR))
                .createDelegated(subjectEmail);

        return new Calendar.Builder(credential.getTransport(), credential.getJsonFactory(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Boolean createGoogleCalendarEvent(String summary, List<String> attendeesEmail, String description
            , String startDate, String endDate
            , String startTime, String endTime
            , String calendarId) {
        try {
            String startDateTime = getISO8601FormatDate(startDate, startTime);
            String endDateTime = getISO8601FormatDate(endDate, endTime);

            // Get the calendar service
//            Calendar service = getCalendarService();
            Calendar service = getCalendarServiceByServiceAccount(subjectEmail);

            // Create event
            Event event = new Event()
                    .setSummary(summary)
                    .setAttendees(convertToEventAttendees(attendeesEmail))
                    .setDescription(description);

            EventDateTime startEventDate = new EventDateTime()
                    .setDateTime(new DateTime(startDateTime));
            event.setStart(startEventDate);

            EventDateTime endEventDate = new EventDateTime()
                    .setDateTime(new DateTime(endDateTime));
            event.setEnd(endEventDate);

            // Insert event
            service.events().insert(calendarId, event).setSendNotifications(true).execute(); //calendarId = "primary"
            return true;

        } catch (Exception e) {
            log.error("Exception while creating the Google Calendar event: {}", e);
            return false;
        }

    }

    public static Boolean createGoogleCalendarEventTask(String summary, String description
            , String startDate, String endDate
            , String startTime, String endTime
            , String calendarId) {

        try {
            String startDateTime = getISO8601FormatDate(startDate, startTime);
            String endDateTime = getISO8601FormatDate(endDate, endTime);

            // Get the calendar service
            Calendar service = getCalendarServiceByServiceAccount(subjectEmail);

            // Create event
            Event event = new Event()
                    .setSummary(summary)
                    .setDescription(description);

            EventDateTime startEventDate = new EventDateTime()
                    .setDateTime(new DateTime(startDateTime))
                    .setTimeZone("IST");
            event.setStart(startEventDate);

            EventDateTime endEventDate = new EventDateTime()
                    .setDateTime(new DateTime(endDateTime));
            event.setEnd(endEventDate);

            // Set additional properties to mark it as a task
            event.setReminders(null); // No reminders for tasks
            event.setKind("calendar#event");
            event.setSequence(0);

            // Mark it as a task using ExtendedProperties
            Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
            extendedProperties.setPrivate(Collections.singletonMap("isTask", "true"));
            event.setExtendedProperties(extendedProperties);

            // Insert event task
            service.events().insert(calendarId, event).execute();
//            service.events().insert(calendarId, event).execute(); //calendarId = "primary"
            return true;

        } catch (Exception e) {
            log.error("Exception while creating the Google Calendar event: {}", e);
            return false;
        }

    }

    public static String getISO8601FormatDate(String originalDateString, String originalISTTime) {
        LocalDateTime dateTime = LocalDateTime.of(
                LocalDate.parse(originalDateString, DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                LocalTime.parse(originalISTTime, DateTimeFormatter.ofPattern("HH:mm:ss"))
        ).minus(Duration.ofHours(5).plusMinutes(30));

        return dateTime.toString();
    }

    public static List<EventAttendee> convertToEventAttendees(List<String> emailList) {
        List<EventAttendee> eventAttendees = new ArrayList<>();

        for (String email : emailList) {
            EventAttendee attendee = new EventAttendee();
            attendee.setEmail(email);
            eventAttendees.add(attendee);
        }
        return eventAttendees;
    }

}
