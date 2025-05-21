/*Marcos Luque Montiel y Soraya Bennai Sadqi */

package com.uma.example.springuma.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.Duration;
import java.util.List;

import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ImagenControllerWebTestClientIT {

    @LocalServerPort
    private Integer port;

    private WebTestClient client;

    private Paciente paciente;

    private Medico medico;

    @PostConstruct
    public void init() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(30000))
                .build();

        // Crear un médico
        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Dr. Juan");
        medico.setDni("12345678A"); 

        // Crear un paciente
        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Soraya");
        paciente.setDni("12345678B");
        paciente.setMedico(medico);
    }


//Método privado para guardar un médico    
    private void guardarMedico(Medico medico) throws Exception {
        // Guardar el médico
        client.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated() ;
    }

//Método privado para guardar un paciente
    private void guardarPaciente(Paciente paciente) throws Exception {
        // Guardar el paciente
        client.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();
    }

// En esta prueba comprobamos que la imagne se sube correctamente, que el get con url "/imagen/info/id" devuelve la imagen correcta 
// y que el get con url "/imagen/paciente/id" devuelve la lista de imágenes del paciente
    @Test
    @DisplayName("Sube una imagen correctamente para un paciente y comprueba dos gets")
    public void uploadImage_multipartForm() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);


        File file = new File("./src/test/resources/healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(file));
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        // Subir la imagen
        FluxExchangeResult<String> response = client.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String result = response.getResponseBody().blockFirst();
        assertEquals("{\"response\" : \"file uploaded successfully : healthy.png\"}", result);
    
        // Obtener la lista de imágenes del paciente
        List<Imagen> imagenes = client.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Imagen.class)
                .returnResult()
                .getResponseBody();

        // Comprobar que hay una imagen y que el nombre es correcto
        assertEquals(1, imagenes.size());
        Imagen imagen = imagenes.get(0);
        assertEquals("healthy.png", imagen.getNombre());
        assertEquals(paciente.getNombre(), imagen.getPaciente().getNombre());

        // Obtener la info de la imagen por id y comprobar los datos
        Imagen imagenInfo = client.get()
                .uri("/imagen/info/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen.class)
                .returnResult()
                .getResponseBody();

        assertEquals(imagen.getId(), imagenInfo.getId());
        assertEquals("healthy.png", imagenInfo.getNombre());
        assertEquals(paciente.getNombre(), imagenInfo.getPaciente().getNombre());

    }
// En esta prueba comprobamos que el get con url "/imagen/id" da error si la imagen no existe
    @Test
    @DisplayName("Falla al obtener imagen binaria porque el ID no existe")
    public void getImageById_notFound() {
        client.get()
            .uri("/imagen/9999")
            .exchange()
            .expectStatus().is5xxServerError();
    }

// En esta prueba comprobamos que el get con url "/imagen/info/id" da error si la imagen no existe
    @Test
    @DisplayName("Falla al obtener info de imagen porque el ID no existe")
    public void getImagenInfo_notFound() {
        client.get()
            .uri("/imagen/info/9999")
            .exchange()
            .expectStatus().is5xxServerError();
    }

// En esta prueba comprobamos que el get con url "/imagen/predict/id" nos da la predicción de la imagen
// no podemos comprobar el resultado porque la predicción es aleatoria
// pero comprobamos que el resultado contiene la palabra "status"
    @Test
    @DisplayName("Obtiene la predicción correctamente para una imagen")
    public void getImagenPrediction_success() throws Exception {
        guardarMedico(medico);
        guardarPaciente(paciente);

        File file = new File("./src/test/resources/healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(file));
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk();


        FluxExchangeResult<String> response = client.get()
            .uri("/imagen/predict/" + paciente.getId())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class);

        String prediction = response.getResponseBody().blockFirst();
        assertEquals(true, prediction.contains("status"));
    }

// En esta prueba comprobamos que el delete elimina la imagen correctamente y 
// no aparece en la lista de imágenes del paciente
    @Test
    @DisplayName("Elimina la imagen correctamente por ID")
    public void deleteImagenById_success() throws Exception {
        guardarMedico(medico);
        guardarPaciente(paciente);

        File file = new File("./src/test/resources/healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(file));
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk();

        List<Imagen> imagenes = client.get()
            .uri("/imagen/paciente/" + paciente.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Imagen.class)
            .returnResult()
            .getResponseBody();

        Long imagenId = imagenes.get(0).getId();

        //Borrar la imagen
        client.delete()
            .uri("/imagen/" + imagenId)
            .exchange()
            .expectStatus().isNoContent();

        // Comprobar que la imagen ha sido eliminada
        client.get()
            .uri("/imagen/info/" + imagenId)
            .exchange()
            .expectStatus().is5xxServerError();

        //Comprobar que la imagen no aparece en la lista de imágenes del paciente
        List<Imagen> imagenesAfterDelete = client.get()
            .uri("/imagen/paciente/" + paciente.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Imagen.class)
            .returnResult()
            .getResponseBody();

        assertEquals(0, imagenesAfterDelete.size());
    }

// En esta prueba comprobamos que el post da error si el paciente asociado no existe
    @Test
    @DisplayName("Falla al subir imagen porque el paciente no existe")
    public void uploadImage_pacienteNotFound() throws Exception {
        File file = new File("./src/test/resources/healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(file));
       // builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isBadRequest();
    }

// En esta prueba comprobamos que el post da error si la imagen no existe
    @Test
    @DisplayName("Falla al subir imagen porque la imagen no existe")
    public void uploadImage_imageNotFound() throws Exception {
        guardarMedico(medico);
        guardarPaciente(paciente);

        //File file = new File("./src/test/resources/noexiste.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        //builder.part("image", new FileSystemResource(file));
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isBadRequest();
    }

    
}