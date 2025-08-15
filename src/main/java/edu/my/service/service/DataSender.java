package edu.my.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class DataSender {

//    public void sendNomenclature(String nomenclature) {
//        // отправляем на REST контроллер
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://localhost:8080/internal/receive"))
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(dto)))
//                .build();
//
//        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            Files.delete(file); // удаляем файл
//        } else {
//            System.err.println("Ошибка при отправке: " + response.body());
//            // можно переместить в error-папку
//        }
//    }
}
