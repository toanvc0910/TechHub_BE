package com.techhub.app.courseservice.service.youtube;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Service
public class YouTubeUploadService {

        public Video uploadWithAccessToken(String accessToken,
                        Path file,
                        String title,
                        String description,
                        String privacy) throws Exception {
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                                .setAccessToken(accessToken);

                YouTube yt = new YouTube.Builder(httpTransport, jsonFactory, credential)
                                .setApplicationName("techhub-course-service")
                                .build();

                Video video = new Video();

                VideoStatus status = new VideoStatus();
                status.setPrivacyStatus(privacy != null ? privacy : "private");
                video.setStatus(status);

                VideoSnippet snippet = new VideoSnippet();
                snippet.setTitle(title != null ? title : file.getFileName().toString());
                snippet.setDescription(description != null ? description : "");
                snippet.setCategoryId("22");
                snippet.setTags(Collections.singletonList("techhub"));
                video.setSnippet(snippet);

                String contentType = Files.probeContentType(file);
                InputStream is = Files.newInputStream(file);
                InputStreamContent mediaContent = new InputStreamContent(
                                contentType != null ? contentType : "video/*",
                                is);
                mediaContent.setLength(Files.size(file));

                YouTube.Videos.Insert insert = yt.videos().insert(Collections.singletonList("snippet,status"), video,
                                mediaContent);
                return insert.execute();
        }
}
