/*Marcos Luque Montiel y Soraya Bennai Sadqi */

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

//Método privado para guardar un médico    
    private void guardarMedico(Medico medico) throws Exception {
        // Guardar el médico
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

// En esta prueba se comprueba que el médico se guarda correctamente y se obtiene correctamente.
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

// En esta prueba se comprueba que un médico borrado no se puede obtener
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

// En esta prueba se comprueba que un médico se puede modificar y se obtiene correctamente
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

// En esta prueba comprobamos que no se puede crear un médico con el mismo dni
    @Test
    @DisplayName("Crear médico con dni existente da error")
    void testCrearMedicoConDniExistente() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Crear otro médico con el mismo dni
        Medico medico2 = new Medico();
        medico2.setNombre("Dr. Maria");
        medico2.setDni("12345678A");

        // Comprobar que no se puede crear el médico
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico2)))
                .andExpect(status().is5xxServerError());
        
    }


// En esta prueba comprobamos que no se puede actualizar un médico con el mismo dni de otro que ya existe
    @Test
    @DisplayName("Actualizar médico con dni existente da error")
    void testActualizarMedicoConDniExistente() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Crear otro médico con un dni diferente
        Medico medico2 = new Medico();
        medico2.setId(2);
        medico2.setNombre("Dr. Maria");
        medico2.setDni("12345678B");

        // Guardar el médico
        guardarMedico(medico2);

        // Comprobar que se guarda correctamente
        this.mockMvc.perform(get("/medico/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dr. Maria"))
                .andExpect(jsonPath("$.dni").value("12345678B"));

        // Modificar el médico con el mismo dni que el primero
        medico2.setDni("12345678A");

        // Comprobar que no se puede actualizar el médico
        this.mockMvc.perform(put("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico2)))
                .andExpect(status().is5xxServerError());
        
    }


// En esta prueba comprobaremos que no se puede borrar un médico que no existe
    @Test
    @DisplayName("Borrar médico que no existe da error")
    void testBorrarMedicoQueNoExiste() throws Exception {
        // Comprobar que no se puede borrar el médico
        this.mockMvc.perform(delete("/medico/1"))
                .andExpect(status().is5xxServerError());
        
    }

// En esta prueba comprobaremos que no se puede obtener un médico que no existe
    @Test
    @DisplayName("Obtener médico que no existe da error")
    void testObtenerMedicoQueNoExiste() throws Exception {
        // Comprobar que no se puede obtener el médico
        this.mockMvc.perform(get("/medico/1"))
                .andExpect(status().is5xxServerError());
        
    }      

}
