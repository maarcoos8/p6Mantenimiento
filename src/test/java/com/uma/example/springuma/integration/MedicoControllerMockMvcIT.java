package com.uma.example.springuma.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration{
    
    @Autowired
	private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() {
        // Crear un médico
        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Dr. Juan");
        medico.setDni("12345678A");
    }

    private void guardarMedico(Medico medico) throws Exception {
        // Guardar el médico
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

    // Pruebas para crear, actualizar, obtener y eliminar médicos.

    @Test
    @DisplayName("Crear médico y obtenerlo correctamente")
    void testCrearMedicoYObtenerlo() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Obtener el médico
        this.mockMvc.perform(get("/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Juan"))
                .andExpect(jsonPath("$.dni").value("12345678A"));

    }

    @Test
    @DisplayName("Crear médico, borrarlo y comprobar que no existe")
    void testCrearMedicoYBorrarlo() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Obtener el médico
        this.mockMvc.perform(get("/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Juan"))
                .andExpect(jsonPath("$.dni").value("12345678A"));

        // Borrar el médico
        this.mockMvc.perform(delete("/medico/1"))
                .andExpect(status().isOk());

        // Comprobar que el médico no existe
        this.mockMvc.perform(get("/medico/1"))
                .andExpect(status().is5xxServerError());
        
    }

    @Test
    @DisplayName("Crear médico, editarlo y comprobar que se actualiza correctamente")
    void testCrearModificarYObtenerlo() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Obtener el médico
        this.mockMvc.perform(get("/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Juan"))
                .andExpect(jsonPath("$.dni").value("12345678A"));

        // Modificar el médico
        medico.setNombre("Dr. Pedro");

        this.mockMvc.perform(put("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)));

        // Comprobar que se actualiza correctamente usando el otro método get
        this.mockMvc.perform(get("/medico/dni/12345678A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Pedro"))
                .andExpect(jsonPath("$.dni").value("12345678A"));
        
    }


    /*
        Probar post con medico existente
        Probar put con error al actualizar
        Probar delete con error al eliminar uno que no exista
        Probar get con dni que no existe
    */    

}
