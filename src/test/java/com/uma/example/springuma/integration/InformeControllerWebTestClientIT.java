/*Marcos Luque Montiel y Soraya Bennai Sadqi */

package com.uma.example.springuma.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.time.Duration;
import java.util.List;

import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InformeControllerWebTestClientIT {

    @LocalServerPort
    private Integer port;

    private WebTestClient client;

    private Medico medico;
    private Paciente paciente;

    @PostConstruct
    public void init() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(30000))
                .build();

        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Dr. Juan");
        medico.setDni("12345678A");

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Soraya");
        paciente.setDni("12345678B");
        paciente.setMedico(medico);
    }

// Método privado para guardar un médico    
    private void guardarMedico(Medico medico) {
        client.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();
    }

// Método privado para guardar un paciente    
    private void guardarPaciente(Paciente paciente) {
        client.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();
    }

// Método privado que devuelve la imagen healthy.png después de subirla
    private Imagen subirImagen() {
        File file = new File("./src/test/resources/healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(file));
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        client.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

        return client.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Imagen.class)
                .returnResult()
                .getResponseBody()
                .get(0);
    }

// En esta prueba se comprueba que el informe se guarda correctamente y se obtiene correctamente.
// Se comprueba que la imagen asociada al informe es la misma que la subida y que el informe tiene una predicción.
    @Test
    @DisplayName("Guarda un informe y lo obtiene correctamente por ID")
    public void saveInformeAndGetById_success() {
        guardarMedico(medico);
        guardarPaciente(paciente);
        Imagen imagen = subirImagen();

        Informe informe = new Informe();
        informe.setId(1L);
        informe.setImagen(imagen);

        client.post().uri("/informe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        Informe result = client.get()
                .uri("/informe/" + informe.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Informe.class)
                .returnResult()
                .getResponseBody();

        assertEquals(informe.getId(), result.getId());
        assertNotNull(result.getPrediccion());
        assertEquals(imagen.getId(), result.getImagen().getId());
    }

// En esta prueba se comprueba que el informe se guarda correctamente y se obtiene correctamente por ID de imagen.
    @Test
    @DisplayName("Obtiene los informes relacionados a una imagen")
    public void getInformesByImagenId_success() {
        guardarMedico(medico);
        guardarPaciente(paciente);
        Imagen imagen = subirImagen();

        Informe informe = new Informe();
        informe.setId(1L);
        informe.setImagen(imagen);

        client.post().uri("/informe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        List<Informe> informes = client.get()
                .uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .returnResult()
                .getResponseBody();

        assertEquals(1, informes.size());
        assertEquals(informe, informes.get(0));
    }

    @Test
    @DisplayName("Elimina un informe correctamente")
    public void deleteInforme_success() {
        guardarMedico(medico);
        guardarPaciente(paciente);
        Imagen imagen = subirImagen();

        Informe informe = new Informe();
        informe.setId(1L);
        informe.setImagen(imagen);

        client.post().uri("/informe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        client.delete()
                .uri("/informe/" + informe.getId())
                .exchange()
                .expectStatus().isNoContent();

        List<Informe> informes = client.get()
                .uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .returnResult()
                .getResponseBody();

        assertEquals(0, informes.size());
    }

}